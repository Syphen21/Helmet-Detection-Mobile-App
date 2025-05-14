from fastapi import FastAPI, File, UploadFile
from fastapi.responses import StreamingResponse, JSONResponse
from ultralytics import YOLO
import cv2
import shutil
import os
import io
import uvicorn

# Initialize app and model
app = FastAPI()
model = YOLO("best.pt")           # adjust path if needed
UPLOAD_FOLDER = "temp_images"
os.makedirs(UPLOAD_FOLDER, exist_ok=True)


@app.get("/")
def home():
    return {"message": "Helmet Detection API is running!"}


@app.post("/predict/")
async def predict(file: UploadFile = File(...)):
    # 1) Save upload to disk
    fp = os.path.join(UPLOAD_FOLDER, file.filename)
    with open(fp, "wb") as buf:
        shutil.copyfileobj(file.file, buf)

    # 2) Read image and run YOLO inference
    img = cv2.imread(fp)
    results = model(img)

    # 3) Draw boxes & labels
    for r in results:
        for b in r.boxes:
            x1, y1, x2, y2 = map(int, b.xyxy[0])
            cls, conf = int(b.cls[0]), float(b.conf[0])
            label = model.names[cls]
            color = (0, 255, 0) if label == "With Helmet" else (0, 0, 255)
            cv2.rectangle(img, (x1, y1), (x2, y2), color, 2)
            cv2.putText(
                img,
                f"{label} {conf:.2f}",
                (x1, y1 - 10),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.5,
                color,
                2,
            )

    # 4) Cleanup the uploaded file
    os.remove(fp)

    # 5) Encode as JPEG and stream back
    success, encoded = cv2.imencode(".jpg", img)
    if not success:
        return JSONResponse({"error": "Image encoding failed"}, status_code=500)

    return StreamingResponse(io.BytesIO(encoded.tobytes()), media_type="image/jpeg")


if __name__ == "__main__":
    # Run on http://127.0.0.1:8000
    uvicorn.run(app, host="127.0.0.1", port=8000)
