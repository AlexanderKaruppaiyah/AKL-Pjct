import cv2
import numpy as np
import mediapipe as mp
import tensorflow as tf
from scipy.spatial.transform import Rotation as R
import joblib

# Load models and label encoders
cnn_rnn_model = tf.keras.models.load_model('model\\cnn_rnn_model.h5')
lstm_model = tf.keras.models.load_model("model\\sign_to_text_model_3.h5")
cnn_rnn_label_encoder = joblib.load('label\\label_encoder.pkl')
lstm_label_encoder = joblib.load("label\\label_enc_3.pkl")

cnn_rnn_class_labels = cnn_rnn_label_encoder.classes_
lstm_class_labels = lstm_label_encoder.classes_
unified_class_labels = sorted(set(cnn_rnn_class_labels).union(set(lstm_class_labels)))
n = len(unified_class_labels)

# Set weights and confidence threshold
cnn_rnn_weight = 0.4
lstm_weight = 0.4
confidence_threshold = (1/n) + (0.2 * (1/n))

# Initialize MediaPipe
mp_hands = mp.solutions.hands
mp_holistic = mp.solutions.holistic
mp_drawing = mp.solutions.drawing_utils

# Function to preprocess keypoints for CNN-RNN model
def preprocess_keypoints(keypoints):
    while len(keypoints) < 42:
        keypoints.append([0, 0, 0])
    return np.array(keypoints).flatten()

# Feature extraction functions
def extract_features_from_frame(results, hand_type, previous_wrist):
    features = []
    hand_landmarks = []
    wrist_position = None

    if hand_type == 'right_hand' and results.right_hand_landmarks:
        hand_landmarks = results.right_hand_landmarks.landmark
        orientation = calculate_orientation(hand_landmarks)
        features.extend(orientation)
        wrist_position = np.array([hand_landmarks[mp_holistic.HandLandmark.WRIST].x,
                                   hand_landmarks[mp_holistic.HandLandmark.WRIST].y,
                                   hand_landmarks[mp_holistic.HandLandmark.WRIST].z])
    elif hand_type == 'left_hand' and results.left_hand_landmarks:
        hand_landmarks = results.left_hand_landmarks.landmark
        orientation = calculate_orientation(hand_landmarks)
        features.extend(orientation)
        wrist_position = np.array([hand_landmarks[mp_holistic.HandLandmark.WRIST].x,
                                   hand_landmarks[mp_holistic.HandLandmark.WRIST].y,
                                   hand_landmarks[mp_holistic.HandLandmark.WRIST].z])
    else:
        features.extend([0] * 3)
    
    finger_angles = calculate_finger_angles(hand_landmarks)
    features.extend(finger_angles)

    if results.face_landmarks:
        relative_distances = calculate_relative_distances(results.face_landmarks, hand_landmarks)
        features.extend(relative_distances)
    else:
        features.extend([0] * 10)
    
    if wrist_position is not None and previous_wrist is not None:
        trajectory = wrist_position - previous_wrist
    else:
        trajectory = np.zeros(3)
    features.extend(trajectory)

    if len(features) < 41:
        features.extend([0] * (41 - len(features)))
    
    return features, wrist_position

def calculate_orientation(hand_landmarks):
    wrist = np.array([hand_landmarks[mp_holistic.HandLandmark.WRIST].x,
                      hand_landmarks[mp_holistic.HandLandmark.WRIST].y,
                      hand_landmarks[mp_holistic.HandLandmark.WRIST].z])
    middle_finger_mcp = np.array([hand_landmarks[mp_holistic.HandLandmark.MIDDLE_FINGER_MCP].x,
                                  hand_landmarks[mp_holistic.HandLandmark.MIDDLE_FINGER_MCP].y,
                                  hand_landmarks[mp_holistic.HandLandmark.MIDDLE_FINGER_MCP].z])
    palm_vector = middle_finger_mcp - wrist
    palm_normal = np.cross(palm_vector, np.array([0, 0, 1]))
    rot = R.from_rotvec(palm_normal)
    yaw, pitch, roll = rot.as_euler('xyz', degrees=True)
    return [yaw, pitch, roll]

def calculate_finger_angles(hand_landmarks):
    finger_angles = []
    if hand_landmarks:
        for i in range(5):
            mcp_idx = mp_holistic.HandLandmark(i * 4 + 1)
            pip_idx = mp_holistic.HandLandmark(i * 4 + 2)
            dip_idx = mp_holistic.HandLandmark(i * 4 + 3)
            tip_idx = mp_holistic.HandLandmark(i * 4 + 4)
            
            if mcp_idx in hand_landmarks and pip_idx in hand_landmarks and dip_idx in hand_landmarks and tip_idx in hand_landmarks:
                mcp = np.array([hand_landmarks[mcp_idx].x, hand_landmarks[mcp_idx].y, hand_landmarks[mcp_idx].z])
                pip = np.array([hand_landmarks[pip_idx].x, hand_landmarks[pip_idx].y, hand_landmarks[pip_idx].z])
                dip = np.array([hand_landmarks[dip_idx].x, hand_landmarks[dip_idx].y, hand_landmarks[dip_idx].z])
                tip = np.array([hand_landmarks[tip_idx].x, hand_landmarks[tip_idx].y, hand_landmarks[tip_idx].z])
                angle_mcp_pip = calculate_angle(mcp, pip, dip)
                angle_pip_dip = calculate_angle(pip, dip, tip)
                finger_angles.extend([angle_mcp_pip, angle_pip_dip])
            else:
                finger_angles.extend([0, 0])
    else:
        finger_angles.extend([0, 0] * 5)
    return finger_angles

def calculate_angle(a, b, c):
    ba = a - b
    bc = c - b
    cosine_angle = np.dot(ba, bc) / (np.linalg.norm(ba) * np.linalg.norm(bc))
    angle = np.arccos(cosine_angle)
    return np.degrees(angle)

def calculate_relative_distances(face_landmarks, hand_landmarks):
    specific_facial_points = [
        face_landmarks.landmark[33],
        face_landmarks.landmark[263],
        face_landmarks.landmark[1],
        face_landmarks.landmark[61],
        face_landmarks.landmark[291]
    ]
    
    specific_fingertips = [
        hand_landmarks[mp_holistic.HandLandmark.THUMB_TIP] if hand_landmarks else None,
        hand_landmarks[mp_holistic.HandLandmark.INDEX_FINGER_TIP] if hand_landmarks else None,
        hand_landmarks[mp_holistic.HandLandmark.MIDDLE_FINGER_TIP] if hand_landmarks else None,
        hand_landmarks[mp_holistic.HandLandmark.RING_FINGER_TIP] if hand_landmarks else None,
        hand_landmarks[mp_holistic.HandLandmark.PINKY_TIP] if hand_landmarks else None
    ]
    
    distances = []
    for i in range(5):
        for j in range(5):
            if specific_facial_points[i] and specific_fingertips[j]:
                distance = np.linalg.norm(
                    np.array([specific_facial_points[i].x, specific_facial_points[i].y, specific_facial_points[i].z]) -
                    np.array([specific_fingertips[j].x, specific_fingertips[j].y, specific_fingertips[j].z])
                )
                distances.append(distance)
            else:
                distances.append(0)
    return distances

def process_video(video_path):
    cap = cv2.VideoCapture(video_path)
    previous_right_wrist = None
    sequence_buffer = []
    predicted_signs = []
    frame_counter = 1

    with mp_hands.Hands(static_image_mode=False, max_num_hands=2, min_detection_confidence=0.7, min_tracking_confidence=0.5) as hands, \
         mp_holistic.Holistic(static_image_mode=False, min_detection_confidence=0.5) as holistic:
        
        while cap.isOpened():
            ret, frame = cap.read()
            if not ret:
                break
            
            image_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            hand_results = hands.process(image_rgb)
            holistic_results = holistic.process(image_rgb)

            keypoints = []
            if hand_results.multi_hand_landmarks:
                for landmarks in hand_results.multi_hand_landmarks[:2]:
                    hand_keypoints = []
                    for landmark in landmarks.landmark:
                        hand_keypoints.append([landmark.x, landmark.y, landmark.z])
                    keypoints.extend(hand_keypoints)

            if frame_counter % 30 == 0 and cnn_rnn_weight > 0:
                cnn_rnn_input = preprocess_keypoints(keypoints).reshape(1, 126, 1)
                if cnn_rnn_input is not None:
                    cnn_rnn_prediction = cnn_rnn_model.predict(cnn_rnn_input)
                    cnn_rnn_probabilities = cnn_rnn_prediction[0]
                else:
                    cnn_rnn_probabilities = np.zeros(len(cnn_rnn_class_labels))
            else:
                cnn_rnn_probabilities = np.zeros(len(cnn_rnn_class_labels))

            lstm_features, previous_right_wrist = extract_features_from_frame(holistic_results, 'right_hand', previous_right_wrist)
            if len(lstm_features) == 41:
                sequence_buffer.append(lstm_features)
            
            if len(sequence_buffer) == 30 and lstm_weight > 0:
                lstm_input = np.array(sequence_buffer).reshape(1, 30, 41)
                lstm_prediction = lstm_model.predict(lstm_input)
                lstm_probabilities = lstm_prediction[0]
                sequence_buffer = []
            else:
                lstm_probabilities = np.zeros(len(lstm_class_labels))

            cnn_rnn_probabilities_unified = np.zeros(len(unified_class_labels))
            lstm_probabilities_unified = np.zeros(len(unified_class_labels))

            for idx, cnn_rnn_label in enumerate(cnn_rnn_class_labels):
                if cnn_rnn_label in unified_class_labels:
                    unified_idx = unified_class_labels.index(cnn_rnn_label)
                    cnn_rnn_probabilities_unified[unified_idx] = cnn_rnn_probabilities[idx]

            for idx, lstm_label in enumerate(lstm_class_labels):
                if lstm_label in unified_class_labels:
                    unified_idx = unified_class_labels.index(lstm_label)
                    lstm_probabilities_unified[unified_idx] = lstm_probabilities[idx]

            ensemble_probabilities = (
                cnn_rnn_weight * cnn_rnn_probabilities_unified
                + lstm_weight * lstm_probabilities_unified
            )

            final_predicted_class = np.argmax(ensemble_probabilities)
            confidence = ensemble_probabilities[final_predicted_class]

            if confidence >= confidence_threshold:
                final_label = unified_class_labels[final_predicted_class]
                predicted_signs.append(final_label)

            frame_counter += 1

    cap.release()
    return ' '.join(predicted_signs)



