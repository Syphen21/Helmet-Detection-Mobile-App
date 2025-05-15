
# Helmet Detection

This project implements an AI-based helmet detection system to identify whether individuals in images or video streams are wearing helmets. It leverages a YOLOv8 pretrained model fine-tuned on a local dataset for accurate helmet detection. The system uses FASTAPI for the backend, is deployed via Hugging Face Spaces, and includes a mobile app built with Kotlin to provide a user-friendly interface for real-time detection. The project aims to enhance road safety by monitoring helmet-wearing compliance, particularly for motorcyclists.

## Table of Contents
- [Features](#features)
- [Technologies Used](#technologies-used)
- [Model Training](#model-training)
- [API Development and Deployment](#api-development-and-deployment)
- [Mobile App](#mobile-app)
- [Installation](#installation)
- [Usage](#usage)
- [App UI Showcase](#app-ui-showcase)
- [Contributing](#contributing)
- [License](#license)
- [Contact](#contact)

## Features
- Real-time helmet detection in images via a mobile app.
- High accuracy using a fine-tuned YOLOv8 model.
- User-friendly Kotlin-based mobile app with options to use the camera or gallery.
- Bounding box visualization with labels ("With Helmet" or "Without Helmet") and confidence scores.
- Scalable backend with FASTAPI, deployed on Hugging Face Spaces.

## Technologies Used
- **Python**: Core language for model training and backend.
- **YOLOv8**: Pretrained model for helmet detection.
- **Ultralytics YOLO**: Framework for training and inference with YOLOv8.
- **FASTAPI**: Backend API framework for serving the model.
- **Hugging Face Spaces**: Platform for API deployment.
- **Kotlin**: Language for Android mobile app development.
- **OpenCV**: For image processing during inference.
- **PyTorch**: Backend for YOLOv8 model (`.pt` file).

## Model Training
The helmet detection model is based on a pretrained YOLOv8 model from Ultralytics. We fine-tuned it on a local dataset containing images of motorcyclists with and without helmets. The steps are as follows:

1. **Dataset Preparation**:
   - Collected a local dataset of images with motorcyclists.
   - Annotated the dataset using a tool like LabelImg to mark helmets in YOLO format (`.txt` files with bounding box coordinates).
   - Organized the dataset into `images/` and `labels/` directories, split into training and validation sets.

2. **Fine-Tuning YOLOv8**:
   - Used the Ultralytics YOLOv8 framework to load the pretrained model (`yolov8n.pt` or similar).
   - Fine-tuned the model on the local dataset:
     ```bash
     yolo train model=yolov8n.pt data=data.yaml epochs=50 imgsz=640
     ```
   - The `data.yaml` file specifies paths to the training and validation datasets and class names (`With Helmet`, `Without Helmet`).

3. **Export to `.pt` File**:
   - After training, the model weights were saved as a `.pt` file (PyTorch format) for deployment:
     ```bash
     yolo export model=runs/train/exp/weights/best.pt format=torchscript
     ```
   - The resulting `.pt` file is used for inference in the API.

## API Development and Deployment
The backend API is built using FASTAPI to serve the helmet detection model.

1. **FASTAPI Setup**:
   - Created a FASTAPI application to handle image uploads and return detection results.
   - The API loads the trained `.pt` model and uses OpenCV for image preprocessing.
   - Endpoint example: `/detect` accepts an image and returns bounding box coordinates, labels, and confidence scores.

2. **Deployment on Hugging Face Spaces**:
   - Deployed the FASTAPI application on Hugging Face Spaces for scalable hosting.
   - The API is accessible via a public URL provided by Hugging Face, allowing the mobile app to communicate with it.

## Mobile App
The frontend is an Android mobile app developed using Kotlin. It provides a simple interface for users to:
- Upload images from the gallery or capture them using the camera.
- Send images to the FASTAPI backend for helmet detection.
- Display results with bounding boxes and labels ("With Helmet" or "Without Helmet").

The app communicates with the Hugging Face-hosted API to perform detections and render the results on the uploaded image.

## Installation

### Backend (API)
1. **Clone the Repository**:
   ```bash
   git clone https://github.com/BhavyaPatel9/Helmet-Detection.git
   cd Helmet-Detection
   ```

2. **Set Up a Virtual Environment**:
   ```bash
   python -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   ```

3. **Install Dependencies**:
   ```bash
   pip install -r requirements.txt
   ```
   Ensure `requirements.txt` includes `fastapi`, `uvicorn`, `ultralytics`, `opencv-python`, and `torch`.

4. **Run the FASTAPI Server Locally**:
   ```bash
   uvicorn main:app --host 0.0.0.0 --port 8000
   ```

### Mobile App (Kotlin)
1. Open the `android/` directory in Android Studio.
2. Build and run the app on an emulator or physical device.
3. Ensure the app is configured to point to the Hugging Face API URL (update the API endpoint in the app's code if needed).

## Usage

1. **Launch the Mobile App**:
   - Open the Helmet Detector app on your Android device.
   - Use the "Gallery" button to upload an image or the "Camera" button to capture a new one.

2. **Detect Helmets**:
   - Press the "Detect Helmet" button to send the image to the API.
   - The app will display the image with bounding boxes and labels indicating whether each person is wearing a helmet.

3. **API Usage (Optional)**:
   - If testing the API directly, send a POST request to the `/detect` endpoint with an image file:
     ```bash
     curl -X POST -F "file=@image.jpg" http://<hugging-face-api-url>/detect
     ```



## App UI Showcase
Below is a sample of the app's UI demonstrating the helmet detection functionality:

- **Before Detection**:
  - ![Before Detection](temp_images/6.jpeg)
  - Description: The original image uploaded via the gallery or camera, showing motorcyclists without any annotations.

- **After Detection**:
  - ![After Detection](temp_images/5.jpeg)
  - Description: The processed image with green bounding boxes labeled "With Helmet" and red boxes labeled "Without Helmet", along with confidence scores.

To include these images in the README, upload them to the `screenshots/` directory in the repository and update the placeholders with the correct file paths.

## Contributing
Contributions are welcome! To contribute:
1. Fork the repository.
2. Create a new branch (`git checkout -b feature/your-feature`).
3. Make your changes and commit (`git commit -m 'Add your feature'`).
4. Push to the branch (`git push origin feature/your-feature`).
5. Open a Pull Request with a clear description of your changes.

Please ensure your code follows the project's coding style and includes appropriate documentation.

## License
This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Contact
For questions or suggestions, feel free to reach out:
- **GitHub**: [BhavyaPatel9](https://github.com/BhavyaPatel9)

Thank you for exploring the Helmet Detection project! 🚴‍♂️🪖
