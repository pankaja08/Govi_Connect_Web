from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image
import numpy as np
import tensorflow as tf
import json, io

app = FastAPI(
    title       = "GoවිCONNECT — Paddy Disease API",
    description = "AI-based paddy disease detection and treatment recommendation",
    version     = "2.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins  = ["*"],
    allow_methods  = ["*"],
    allow_headers  = ["*"],
)

# ── Load both models and JSON files on startup ────────────────────────────────
print("Loading models...")
validator_model = tf.keras.models.load_model("validator_model.keras")
disease_model   = tf.keras.models.load_model("disease_model.keras")

with open("validator_classes.json") as f:
    VALIDATOR_CLASSES = json.load(f)

with open("disease_classes.json") as f:
    DISEASE_CLASSES = json.load(f)

with open("treatment_kb.json") as f:
    TREATMENT_KB = json.load(f)

IMG_SIZE = 224
print(f"Models loaded")
print(f"   Validator classes : {VALIDATOR_CLASSES}")
print(f"   Disease classes   : {DISEASE_CLASSES}")


def preprocess(image: Image.Image) -> np.ndarray:
    """Resize, normalize and expand dims for model input."""
    img = image.resize((IMG_SIZE, IMG_SIZE))
    arr = np.array(img, dtype=np.float32) / 255.0
    return np.expand_dims(arr, axis=0)


@app.get("/")
def root():
    return {
        "status"  : "GoවිCONNECT Paddy Disease API is running",
        "version" : "2.0",
        "models"  : ["validator_model", "disease_model"]
    }


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/predict")
async def predict(file: UploadFile = File(...)):

    # ── Step 1: Validate file type ────────────────────────────────────────────
    if not file.content_type.startswith("image/"):
        raise HTTPException(
            status_code = 400,
            detail      = "Uploaded file is not an image. Please upload JPG, PNG, or WEBP."
        )

    contents = await file.read()

    # ── Step 2: Open image ────────────────────────────────────────────────────
    try:
        img = Image.open(io.BytesIO(contents)).convert("RGB")
    except Exception:
        raise HTTPException(
            status_code = 400,
            detail      = "Could not read image file. It may be corrupted."
        )

    arr = preprocess(img)

    # ── Step 3: Model 1 — Is this a paddy leaf? ───────────────────────────────
    val_preds    = validator_model.predict(arr, verbose=0)[0]
    val_class    = VALIDATOR_CLASSES[int(np.argmax(val_preds))]
    paddy_idx    = VALIDATOR_CLASSES.index("paddy")
    paddy_conf   = round(float(val_preds[paddy_idx]) * 100, 2)

    if val_class == "non_paddy":
        raise HTTPException(
            status_code = 422,
            detail      = f"This does not appear to be a paddy leaf (paddy similarity: {paddy_conf}%). Please upload a clear paddy leaf image."
        )

    # ── Step 4: Model 2 — What disease? ──────────────────────────────────────
    dis_preds  = disease_model.predict(arr, verbose=0)[0]
    dis_class  = DISEASE_CLASSES[int(np.argmax(dis_preds))]
    dis_conf   = round(float(np.max(dis_preds)) * 100, 2)
    all_scores = {
        DISEASE_CLASSES[i]: round(float(dis_preds[i]) * 100, 2)
        for i in range(len(DISEASE_CLASSES))
    }

    # ── Step 5: Get treatment recommendation ─────────────────────────────────
    kb = TREATMENT_KB.get(dis_class, {})

    # ── Step 6: Build warning if needed ──────────────────────────────────────
    warning = None
    if paddy_conf < 70:
        warning = f"Image is only {paddy_conf}% similar to a paddy leaf. Result may not be fully accurate."
    elif dis_conf < 50:
        warning = f"Low disease confidence ({dis_conf}%). Please consult an agricultural expert."

    return {
        "status"          : "success",
        "disease"         : dis_class,
        "display_name"    : kb.get("display_name", dis_class),
        "confidence"      : dis_conf,
        "severity"        : kb.get("severity", "Unknown"),
        "paddy_confidence": paddy_conf,
        "image_status"    : "uncertain" if paddy_conf < 70 else "paddy",
        "warning"         : warning,
        "pathogen"        : kb.get("pathogen", "-"),
        "symptoms"        : kb.get("symptoms", []),
        "treatment"       : kb.get("treatment", []),
        "prevention"      : kb.get("prevention", []),
        "all_scores"      : all_scores
    }