# Start from plain Python 3.9
FROM python:3.9-slim

# Set working directory
WORKDIR /app

# Copy in requirements and install
COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade -r requirements.txt

# Copy over your app code and model checkpoint
COPY app.py .
COPY best.pt .

# Create the folder for temporary images
RUN mkdir temp_images

# Expose the port you’ll serve on (Spaces default is 7860)
EXPOSE 7860

# Launch Uvicorn to serve your FastAPI app
CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "7860"]
