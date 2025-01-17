import cv2
import mediapipe as mp
import json

# Initialize MediaPipe Pose
mp_pose = mp.solutions.pose
pose = mp_pose.Pose(static_image_mode=False, min_detection_confidence=0.5, min_tracking_confidence=0.5)

# Initialize video capture
video_path = r'D:\up\PrOject_E\clips\hello\sequence_12\sequence_12.mp4'
cap = cv2.VideoCapture(video_path)

# List to store keypoints
keypoints = []

frame_idx = 0
while cap.isOpened():
    ret, frame = cap.read()
    if not ret:
        break
    print("Processing...")

    # Convert the BGR image to RGB
    image_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)

    # Process the image and detect pose
    results = pose.process(image_rgb)

    if results.pose_landmarks:
        frame_keypoints = []
        for landmark in results.pose_landmarks.landmark:
            frame_keypoints.append({
                'x': landmark.x,
                'y': landmark.y,
                'z': landmark.z,
                'visibility': landmark.visibility
            })
        keypoints.append({'frame': frame_idx, 'keypoints': frame_keypoints})

    frame_idx += 1

# Release resources
cap.release()
pose.close()

# Save keypoints to a JSON file
with open('keypoints\\keypoints.json', 'w') as f:
    json.dump(keypoints, f, indent=4)
    print("Keypoints saved")
