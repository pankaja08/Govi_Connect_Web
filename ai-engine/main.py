from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image
import numpy as np
import tensorflow as tf
import cv2
import json, io

app = FastAPI(
    title       = "GoviCONNECT — Paddy Disease API",
    description = "AI-based paddy disease detection and treatment recommendation",
    version     = "3.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins  = ["http://localhost:8080", "http://127.0.0.1:8080"],
    allow_methods  = ["POST", "GET"],
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
PADDY_CONFIDENCE_THRESHOLD = 70.0  # ML model must be >= 70% confident it's paddy
GREEN_PIXEL_THRESHOLD      = 12.0  # At least 12% of pixels must be green-dominant

print("Models loaded")
print(f"   Validator classes : {VALIDATOR_CLASSES}")
print(f"   Disease classes   : {DISEASE_CLASSES}")


# ── Helper: Pre-check using HSV color analysis ────────────────────────────────
def check_green_content(pil_img: Image.Image) -> tuple:
    """
    Uses OpenCV HSV color analysis to verify the image has sufficient
    green vegetation content before running the ML model.

    Paddy leaves are green. Stickers, badges, photos of people/food/objects
    will have very little green and will be rejected here.

    Returns: (is_green_enough: bool, green_pct: float)
    """
    # Resize to small thumbnail for fast analysis
    thumb = pil_img.resize((128, 128))
    # PIL RGB -> OpenCV BGR -> HSV
    bgr  = cv2.cvtColor(np.array(thumb), cv2.COLOR_RGB2BGR)
    hsv  = cv2.cvtColor(bgr, cv2.COLOR_BGR2HSV)

    # Green hue range in HSV: H ~35-85 degrees (OpenCV scale: 0-179)
    # Covers yellow-green to blue-green (typical paddy/grass colors)
    lower_green = np.array([25, 30, 30])
    upper_green = np.array([90, 255, 255])

    mask       = cv2.inRange(hsv, lower_green, upper_green)
    green_pct  = round(float(mask.sum() / 255) / (128 * 128) * 100, 1)

    return green_pct >= GREEN_PIXEL_THRESHOLD, green_pct


def preprocess(image: Image.Image) -> np.ndarray:
    """Resize, normalize and expand dims for model input."""
    img = image.resize((IMG_SIZE, IMG_SIZE))
    arr = np.array(img, dtype=np.float32) / 255.0
    return np.expand_dims(arr, axis=0)


@app.get("/")
def root():
    return {
        "status"  : "GoviCONNECT Paddy Disease API is running",
        "version" : "3.0",
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

    # ── Step 3: Layer 1 — Color/vegetation check (fast, no ML needed) ─────────
    # Paddy leaves MUST have visible green color. Stickers, badges, food,
    # people, etc. will be caught here immediately.
    is_green, green_pct = check_green_content(img)
    if not is_green:
        raise HTTPException(
            status_code = 422,
            detail      = (
                f"This is NOT a paddy leaf image (Green vegetation detected: {green_pct}%). "
                f"Stickers, badges, and non-plant images are not valid. "
                f"Please upload a clear, close-up photo of a real paddy (rice) leaf."
            )
        )

    # ── Step 4: Layer 2 — ML Validator model ─────────────────────────────────
    arr        = preprocess(img)
    val_preds  = validator_model.predict(arr, verbose=0)[0]
    val_class  = VALIDATOR_CLASSES[int(np.argmax(val_preds))]
    paddy_idx  = VALIDATOR_CLASSES.index("paddy")
    paddy_conf = round(float(val_preds[paddy_idx]) * 100, 2)

    # Reject if ML model says non_paddy OR paddy confidence is below threshold
    if val_class == "non_paddy" or paddy_conf < PADDY_CONFIDENCE_THRESHOLD:
        raise HTTPException(
            status_code = 422,
            detail      = (
                f"This is NOT a paddy leaf image (Paddy confidence: {paddy_conf}%). "
                f"Please upload a clear, close-up image of a paddy (rice) leaf for accurate disease analysis."
            )
        )

    # ── Step 5: Layer 3 — Disease detection model ────────────────────────────
    dis_preds  = disease_model.predict(arr, verbose=0)[0]
    dis_class  = DISEASE_CLASSES[int(np.argmax(dis_preds))]
    dis_conf   = round(float(np.max(dis_preds)) * 100, 2)
    all_scores = {
        DISEASE_CLASSES[i]: round(float(dis_preds[i]) * 100, 2)
        for i in range(len(DISEASE_CLASSES))
    }

    # ── Step 6: Get treatment recommendation ─────────────────────────────────
    kb = TREATMENT_KB.get(dis_class, {})

    # ── Step 7: Build warning if confidence is low ────────────────────────────
    warning = None
    if paddy_conf < 80:
        warning = f"Image paddy confidence is {paddy_conf}%. Results may not be fully accurate."
    elif dis_conf < 50:
        warning = f"Low disease confidence ({dis_conf}%). Please consult an agricultural expert."

    return {
        "status"          : "success",
        "disease"         : dis_class,
        "display_name"    : kb.get("display_name", dis_class),
        "confidence"      : dis_conf,
        "severity"        : kb.get("severity", "Unknown"),
        "paddy_confidence": paddy_conf,
        "green_pct"       : green_pct,
        "image_status"    : "paddy",
        "warning"         : warning,
        "pathogen"        : kb.get("pathogen", "-"),
        "symptoms"        : kb.get("symptoms", []),
        "treatment"       : kb.get("treatment", []),
        "prevention"      : kb.get("prevention", []),
        "all_scores"      : all_scores
    }