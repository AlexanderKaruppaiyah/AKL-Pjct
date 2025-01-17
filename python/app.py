from flask import Flask, request, jsonify
import os
import cv2  # OpenCV for video playback
import sign_language_recognition as slt

app = Flask(__name__)

# Directory to save uploaded videos
UPLOAD_FOLDER = 'uploads'
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

@app.route('/', methods=['POST'])
def upload_video():
    try:
        # Check if the POST request has the file part
        if 'file' not in request.files:
            return jsonify({'error': 'No file part in the request'}), 400

        file = request.files['file']
        
        # If the user does not select a file, the browser submits an empty file without a filename
        if file.filename == '':
            return jsonify({'error': 'No file selected'}), 400

        # Save the file
        file_path = os.path.join(UPLOAD_FOLDER, file.filename)
        file.save(file_path)


        # Process the video file (dummy processing for demonstration)
        # Replace this with your actual video processing logic
        predicted_text = slt.process_video(file_path)
        #result_text = f"Video '{file.filename}' received and processed."

        return jsonify(predicted_text), 200
        #return jsonify( result_text), 200

    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(debug=True)








