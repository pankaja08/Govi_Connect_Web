"""
Rice Disease Model Trainer — GoviCONNECT
=========================================
Uses MobileNetV2 transfer learning for high accuracy.

SETUP:
1. Download the Rice Disease Dataset from Kaggle:
   https://www.kaggle.com/datasets/vbookshelf/rice-leaf-diseases
2. Extract and place images in ai-engine/dataset/ with this structure:
   dataset/
     Bacterial Blight/  (images here)
     Blast/             (images here)
     Brown Spot/        (images here)
     healthy/           (images here)
3. Run:  python train.py

OUTPUT: Saves new disease_model.keras and disease_classes.json
        Then restart uvicorn to use the new model.
"""

import os
import json
import numpy as np
import tensorflow as tf
from tensorflow.keras import layers, Model
from tensorflow.keras.applications import MobileNetV2
from tensorflow.keras.callbacks import EarlyStopping, ReduceLROnPlateau, ModelCheckpoint

# ── Config ────────────────────────────────────────────────────────────────────
DATASET_DIR   = "dataset"       # Put your images here, one folder per class
IMG_SIZE      = 224
BATCH_SIZE    = 16
EPOCHS_FROZEN = 10              # Phase 1: train head only
EPOCHS_FINE   = 15              # Phase 2: fine-tune top layers
LEARNING_RATE = 1e-4
SEED          = 42

# Map dataset folder names → canonical class keys used by the app
# Adjust if your folder names are different
CLASS_MAP = {
    "Bacterial Blight" : "bacterial_leaf_blight",
    "Blast"            : "blast",
    "Brown Spot"       : "brown_spot",
    "healthy"          : "normal",
    # Add any extras your dataset has. Unknown folders will be skipped.
}


# ── 1. Verify dataset exists ──────────────────────────────────────────────────
if not os.path.isdir(DATASET_DIR):
    print(f"\n[ERROR] Dataset folder '{DATASET_DIR}/' not found.")
    print("Please download the Kaggle rice disease dataset and place it here.")
    print("https://www.kaggle.com/datasets/vbookshelf/rice-leaf-diseases")
    exit(1)

found_classes = sorted([
    d for d in os.listdir(DATASET_DIR)
    if os.path.isdir(os.path.join(DATASET_DIR, d))
])
print(f"\n[INFO] Found {len(found_classes)} class folders: {found_classes}")

# Build class index in the order the dataset loader will see them
label_names = found_classes  # will be mapped after loading


# ── 2. Load data ──────────────────────────────────────────────────────────────
print("\n[INFO] Loading dataset ...")

train_ds = tf.keras.utils.image_dataset_from_directory(
    DATASET_DIR,
    validation_split = 0.2,
    subset           = "training",
    seed             = SEED,
    image_size       = (IMG_SIZE, IMG_SIZE),
    batch_size       = BATCH_SIZE,
    label_mode       = "categorical",
    shuffle          = True,
)

val_ds = tf.keras.utils.image_dataset_from_directory(
    DATASET_DIR,
    validation_split = 0.2,
    subset           = "validation",
    seed             = SEED,
    image_size       = (IMG_SIZE, IMG_SIZE),
    batch_size       = BATCH_SIZE,
    label_mode       = "categorical",
    shuffle          = False,
)

class_names = train_ds.class_names
num_classes = len(class_names)
print(f"[INFO] Classes ({num_classes}): {class_names}")

# Map folder names to canonical keys for disease_classes.json
disease_classes_output = [CLASS_MAP.get(c, c.lower().replace(" ", "_")) for c in class_names]
print(f"[INFO] Output class keys: {disease_classes_output}")


# ── 3. Data augmentation + normalization ──────────────────────────────────────
augment = tf.keras.Sequential([
    layers.RandomFlip("horizontal_and_vertical"),
    layers.RandomRotation(0.2),
    layers.RandomZoom(0.15),
    layers.RandomBrightness(0.1),
    layers.RandomContrast(0.1),
], name="augmentation")

normalize = layers.Rescaling(1.0 / 255.0)

def prepare(ds, augment_data=False):
    ds = ds.map(lambda x, y: (normalize(x), y), num_parallel_calls=tf.data.AUTOTUNE)
    if augment_data:
        ds = ds.map(lambda x, y: (augment(x, training=True), y), num_parallel_calls=tf.data.AUTOTUNE)
    return ds.prefetch(tf.data.AUTOTUNE)

train_ds = prepare(train_ds, augment_data=True)
val_ds   = prepare(val_ds, augment_data=False)


# ── 4. Build model (MobileNetV2 transfer learning) ───────────────────────────
print("\n[INFO] Building model ...")

base_model = MobileNetV2(
    input_shape = (IMG_SIZE, IMG_SIZE, 3),
    include_top = False,
    weights     = "imagenet",
)
base_model.trainable = False  # Freeze for phase 1

inputs  = layers.Input(shape=(IMG_SIZE, IMG_SIZE, 3))
x       = base_model(inputs, training=False)
x       = layers.GlobalAveragePooling2D()(x)
x       = layers.Dense(256, activation="relu")(x)
x       = layers.Dropout(0.4)(x)
outputs = layers.Dense(num_classes, activation="softmax")(x)

model = Model(inputs, outputs, name="paddy_disease_mobilenetv2")
model.summary()


# ── 5. Phase 1: Train the head only ──────────────────────────────────────────
print("\n[INFO] Phase 1 — Training classification head (backbone frozen) ...")

model.compile(
    optimizer = tf.keras.optimizers.Adam(LEARNING_RATE),
    loss      = "categorical_crossentropy",
    metrics   = ["accuracy"],
)

callbacks_p1 = [
    EarlyStopping(patience=5, restore_best_weights=True, verbose=1),
    ReduceLROnPlateau(factor=0.5, patience=3, verbose=1),
]

model.fit(
    train_ds,
    validation_data = val_ds,
    epochs          = EPOCHS_FROZEN,
    callbacks       = callbacks_p1,
)


# ── 6. Phase 2: Fine-tune top layers of MobileNetV2 ──────────────────────────
print("\n[INFO] Phase 2 — Fine-tuning top 30 layers of backbone ...")

base_model.trainable = True
for layer in base_model.layers[:-30]:   # Freeze all but last 30
    layer.trainable = False

model.compile(
    optimizer = tf.keras.optimizers.Adam(LEARNING_RATE / 10),  # Smaller LR for fine-tuning
    loss      = "categorical_crossentropy",
    metrics   = ["accuracy"],
)

callbacks_p2 = [
    EarlyStopping(patience=5, restore_best_weights=True, verbose=1),
    ReduceLROnPlateau(factor=0.5, patience=3, verbose=1),
    ModelCheckpoint("disease_model_best.keras", save_best_only=True, monitor="val_accuracy", verbose=1),
]

model.fit(
    train_ds,
    validation_data = val_ds,
    epochs          = EPOCHS_FINE,
    callbacks       = callbacks_p2,
)


# ── 7. Evaluate ───────────────────────────────────────────────────────────────
print("\n[INFO] Final Evaluation:")
loss, acc = model.evaluate(val_ds)
print(f"   Validation Accuracy: {acc*100:.2f}%")
print(f"   Validation Loss:     {loss:.4f}")


# ── 8. Save model + class map ─────────────────────────────────────────────────
print("\n[INFO] Saving model ...")

# Back up old model
if os.path.exists("disease_model.keras"):
    os.rename("disease_model.keras", "disease_model_old_backup.keras")
    print("   Old model backed up as disease_model_old_backup.keras")

model.save("disease_model.keras")
print("   Saved: disease_model.keras")

with open("disease_classes.json", "w") as f:
    json.dump(disease_classes_output, f, indent=2)
print("   Saved: disease_classes.json")
print(f"   Class order: {disease_classes_output}")

print("\n[DONE] Retraining complete! Now restart the uvicorn server:")
print("       uvicorn main:app --port 8000")
