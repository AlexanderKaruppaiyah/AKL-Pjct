package com.example.slt;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class SLTActivity extends AppCompatActivity {

    private static final int PICK_VIDEO_REQUEST = 1;
    private static final int CAPTURE_VIDEO_REQUEST = 2;
    private static final int REQUEST_STORAGE_PERMISSION = 3;
    private static final int REQUEST_CAMERA_PERMISSION = 4;

    private VideoView videoView;
    private Button processButton;
    private ProgressBar progressBar;
    private TextView statusText;
    private Uri selectedVideoUri;
    private String currentVideoPath;
    private boolean isCaptureAction; // To track if the action is for capturing video

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.slt_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        videoView = findViewById(R.id.videoView);
        processButton = findViewById(R.id.processButton);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);

        Button uploadVideoButton = findViewById(R.id.uploadVideoButton);
        Button captureVideoButton = findViewById(R.id.captureVideoButton);
        // Set listeners for upload and capture buttons to show dialog first
        uploadVideoButton.setOnClickListener(v -> checkAndRequestStoragePermissions(false));
        captureVideoButton.setOnClickListener(v -> checkAndRequestCameraPermissions(true));

        processButton.setOnClickListener(v -> processVideo());
    }

    private void showVideoUploadInfoDialog(boolean isCaptureAction) {
        // Only show the dialog before selecting or capturing video
        this.isCaptureAction = isCaptureAction;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_video_upload_info, null);

        builder.setView(dialogView)
                .setCancelable(false); // Make dialog cancellable by clicking outside

        // Create the AlertDialog
        AlertDialog dialog = builder.create();

        // Find OK button in the dialog layout and set up its click listener
        Button okButton = dialogView.findViewById(R.id.dismissButton);
        okButton.setOnClickListener(v -> {
            // When the OK button is clicked, proceed to the next step
            if (isCaptureAction) {
                captureVideo();
            } else {
                openVideoPicker();
            }
            dialog.dismiss();  // Dismiss the dialog after the action
        });

        // Show the dialog
        dialog.show();
    }

    private File createVideoFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String videoFileName = "VIDEO_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        return File.createTempFile(videoFileName, ".mp4", storageDir);
    }



    private void openVideoPicker() {
        Log.d("SLTActivity", "Opening video picker");
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        intent.setType("video/*");
        startActivityForResult(intent, PICK_VIDEO_REQUEST);
    }



    private void captureVideo() {
        Log.d("SLTActivity", "Starting video capture");
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            File videoFile;
            try {
                videoFile = createVideoFile();
                Log.d("SLTActivity", "Video file created: " + videoFile.getAbsolutePath());
            } catch (IOException ex) {
                Log.e("SLTActivity", "Error creating video file", ex);
                Toast.makeText(this, "Error creating video file", Toast.LENGTH_SHORT).show();
                return;
            }
            if (videoFile != null) {
                currentVideoPath = videoFile.getAbsolutePath();
                Uri videoURI = FileProvider.getUriForFile(this, "com.example.slt.fileprovider", videoFile);
                takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoURI);
                startActivityForResult(takeVideoIntent, CAPTURE_VIDEO_REQUEST);
            }
        } else {
            Log.d("SLTActivity", "No camera app available.");
            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkAndRequestStoragePermissions(boolean isCaptureAction) {
        Log.d("SLTActivity", "Checking storage permissions");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 and above
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                Log.d("SLTActivity", "READ_MEDIA_VIDEO permission not granted. Requesting permission.");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_VIDEO}, REQUEST_STORAGE_PERMISSION);
            } else {
                Log.d("SLTActivity", "READ_MEDIA_VIDEO permission already granted.");
                showVideoUploadInfoDialog(isCaptureAction);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // For Android 6 to 12
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.d("SLTActivity", "READ_EXTERNAL_STORAGE permission not granted. Requesting permission.");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            } else {
                Log.d("SLTActivity", "READ_EXTERNAL_STORAGE permission already granted.");
                showVideoUploadInfoDialog(isCaptureAction);
            }
        } else {
            Log.d("SLTActivity", "Device SDK < M, proceeding without requesting permissions.");
            showVideoUploadInfoDialog(isCaptureAction);
        }
    }

    private void checkAndRequestCameraPermissions(boolean isCaptureAction) {
        Log.d("SLTActivity", "Checking camera and storage permissions");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean cameraPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
            boolean storagePermissionGranted = ContextCompat.checkSelfPermission(this,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? Manifest.permission.READ_MEDIA_VIDEO : Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

            if (!cameraPermissionGranted || !storagePermissionGranted) {
                Log.d("SLTActivity", "Camera or storage permission not granted. Requesting permissions.");
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.CAMERA,
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? Manifest.permission.READ_MEDIA_VIDEO : Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQUEST_CAMERA_PERMISSION);
            } else {
                Log.d("SLTActivity", "Camera and storage permissions already granted.");
                showVideoUploadInfoDialog(true);
            }
        } else {
            Log.d("SLTActivity", "Device SDK < M, proceeding without requesting permissions.");
            showVideoUploadInfoDialog(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            Log.d("SLTActivity", "Camera permission request result received");
            if (grantResults.length > 0) {
                boolean cameraPermissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean storagePermissionGranted = grantResults.length > 1 && grantResults[1] == PackageManager.PERMISSION_GRANTED;

                if (cameraPermissionGranted && storagePermissionGranted) {
                    Log.d("SLTActivity", "Camera and storage permissions granted.");
                    showVideoUploadInfoDialog(true);
                } else {
                    Log.d("SLTActivity", "Camera or storage permission denied.");
                    Toast.makeText(this, "Camera or storage permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            Log.d("SLTActivity", "Storage permission request result received");
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("SLTActivity", "Storage permission granted.");
                showVideoUploadInfoDialog(false);
            } else {
                Log.d("SLTActivity", "Storage permission denied.");
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_VIDEO_REQUEST && resultCode == RESULT_OK && data != null) {
            Log.d("SLTActivity", "Video picked: " + data.getData());
            selectedVideoUri = data.getData();
            handleVideoSelection();
        } else if (requestCode == CAPTURE_VIDEO_REQUEST && resultCode == RESULT_OK) {
            File videoFile = new File(currentVideoPath);
            if (videoFile.exists()) {
                Log.d("SLTActivity", "Video captured: " + videoFile.getAbsolutePath());
                selectedVideoUri = FileProvider.getUriForFile(this, "com.example.slt.fileprovider", videoFile);
                handleVideoSelection();
            } else {
                Log.e("SLTActivity", "Captured video file not found.");
                Toast.makeText(this, "Error capturing video", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == CAPTURE_VIDEO_REQUEST) {
            Log.d("SLTActivity", "Video capture canceled or failed.");
            Toast.makeText(this, "Video capture canceled or failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleVideoSelection() {
        if (selectedVideoUri != null) {
            Log.d("SLTActivity", "Handling video selection: " + selectedVideoUri.toString());
            videoView.setVisibility(View.VISIBLE);
            processButton.setVisibility(View.VISIBLE);

            videoView.setVideoURI(selectedVideoUri);
            videoView.setOnPreparedListener(mediaPlayer -> {
                int videoWidth = mediaPlayer.getVideoWidth();
                int videoHeight = mediaPlayer.getVideoHeight();

                float videoAspectRatio = (float) videoWidth / videoHeight;

                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                int maxHeight = getResources().getDisplayMetrics().heightPixels / 3;

                int newVideoHeight = (int) (screenWidth / videoAspectRatio);
                if (newVideoHeight > maxHeight) {
                    newVideoHeight = maxHeight;
                }

                ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) videoView.getLayoutParams();
                layoutParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT;
                layoutParams.height = newVideoHeight;
                videoView.setLayoutParams(layoutParams);

                videoView.start();
            });

            findViewById(R.id.uploadVideoButton).setVisibility(View.GONE);
            findViewById(R.id.captureVideoButton).setVisibility(View.GONE);
        } else {
            Log.d("SLTActivity", "Failed to select video.");
            Toast.makeText(this, "Failed to select video", Toast.LENGTH_SHORT).show();
        }
    }

    private void processVideo() {
        if (selectedVideoUri != null) {
            Log.d("SLTActivity", "Processing video: " + selectedVideoUri);
            Intent intent = new Intent(SLTActivity.this, SltProcessActivity.class);
            intent.putExtra("videoPath", selectedVideoUri.toString()); // Pass the URI as a String
            startActivity(intent); // Start the SltProcessActivity
        } else {
            Log.d("SLTActivity", "No video selected to process.");
            Toast.makeText(this, "No video selected to process", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
