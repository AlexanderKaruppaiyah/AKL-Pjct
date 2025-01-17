package com.example.slt;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

public class SltProcessActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView statusText;
    private Uri selectedVideoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sltprocessactivity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Translated");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);

        Intent intent = getIntent();
        String videoUriString = intent.getStringExtra("videoPath");
        if (videoUriString != null) {
            selectedVideoUri = Uri.parse(videoUriString);
            Log.d("SltProcessActivity", "Received Video URI: " + selectedVideoUri);

            if (selectedVideoUri != null) {
                progressBar.setVisibility(View.VISIBLE);
                statusText.setVisibility(View.VISIBLE);
                statusText.setText("Processing...");
                uploadVideoToServer(selectedVideoUri);
            } else {
                Toast.makeText(this, "No video selected to process", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void uploadVideoToServer(Uri videoUri) {
        String realPath = getRealPathFromURI(videoUri);
        if (realPath != null) {
            File videoFile = new File(realPath);
            if (videoFile.exists()) {
                OkHttpClient client = new OkHttpClient.Builder()
                        .connectTimeout(1, java.util.concurrent.TimeUnit.HOURS)
                        .writeTimeout(1, java.util.concurrent.TimeUnit.HOURS)
                        .readTimeout(1, java.util.concurrent.TimeUnit.HOURS)
                        .build();

                RequestBody videoStreamBody = new RequestBody() {
                    @Override
                    public MediaType contentType() {
                        return MediaType.parse("video/*");
                    }

                    @Override
                    public void writeTo(BufferedSink sink) throws IOException {
                        try (FileInputStream fileInputStream = new FileInputStream(videoFile)) {
                            BufferedSink bufferedSink = Okio.buffer(sink);
                            byte[] buffer = new byte[2048];
                            int bytesRead;
                            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                                bufferedSink.write(buffer, 0, bytesRead);
                            }
                            bufferedSink.flush();
                        }
                    }

                    @Override
                    public long contentLength() {
                        return videoFile.length();
                    }
                };

                Request request = new Request.Builder()
                        .url("https://46p3fprc-5000.inc1.devtunnels.ms/")
                        .post(new MultipartBody.Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart("file", videoFile.getName(), videoStreamBody)
                                .build())
                        .build();

                new Thread(() -> {
                    try {
                        Response response = client.newCall(request).execute();
                        if (response.isSuccessful()) {
                            String resultText = response.body().string();
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                statusText.setText(resultText);
                            });
                        } else {
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                statusText.setText("Error1: " + response.message());
                            });
                        }
                    } catch (IOException e) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            statusText.setText("Error: " + e.getMessage());
                        });
                    }
                }).start();
            } else {
                Toast.makeText(this, "Video file does not exist", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Failed to get the file path from URI", Toast.LENGTH_SHORT).show();
        }
    }

    private String getRealPathFromURI(Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { MediaStore.Video.Media.DATA };
            try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                    return cursor.getString(columnIndex);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        // Handle FileProvider URI
        try {
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), uri.getLastPathSegment());
            if (file.exists()) {
                return file.getAbsolutePath();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
