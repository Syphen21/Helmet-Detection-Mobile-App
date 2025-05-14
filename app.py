from fastapi import FastAPI, File, UploadFile
from fastapi.responses import JSONResponse
from ultralytics import YOLO
import cv2, numpy as np, shutil, os

MODEL_PATH      = "best.pt"
UPLOAD_FOLDER   = "temp_images"

app = FastAPI()
model = None

@app.on_event("startup")
async def load_model():
    global model
    model = YOLO(MODEL_PATH)
    os.makedirs(UPLOAD_FOLDER, exist_ok=True)

@app.get("/")
def home():
    return {"message": "Helmet Detection API is running!"}

@app.post("/predict/")
async def predict(file: UploadFile = File(...)):
    try:
        # save upload
        file_path = os.path.join(UPLOAD_FOLDER, file.filename)
        with open(file_path, "wb") as buf:
            shutil.copyfileobj(file.file, buf)

        # read & infer
        img     = cv2.imread(file_path)
        results = model(img)

        # process boxes
        detections = []
        for r in results:
            for box in r.boxes:
                x1,y1,x2,y2 = map(int, box.xyxy[0])
                conf       = float(box.conf[0])
                cls        = int(box.cls[0])
                label      = model.names[cls]
                detections.append({
                    "class":      label,
                    "confidence": round(conf,2),
                    "bbox":       [x1,y1,x2,y2]
                })
                # annotate
                color = (0,255,0) if label=="With Helmet" else (0,0,255)
                cv2.rectangle(img,(x1,y1),(x2,y2),color,2)
                cv2.putText(img,f"{label} {conf:.2f}",(x1,y1-10),
                            cv2.FONT_HERSHEY_SIMPLEX,0.5,color,2)

        # save annotated image
        out_path = os.path.join(UPLOAD_FOLDER, f"detection_{file.filename}")
        cv2.imwrite(out_path, img)
        os.remove(file_path)

        return JSONResponse({
            "detections":           detections,
            "detection_image_path": out_path
        })

    except Exception as e:
        return JSONResponse({"error": str(e)}, status_code=500)
