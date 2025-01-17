package com.example.myapp;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.slt.R;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.process);

        // Retrieve the predicted text from the intent
        String predictedText = getIntent().getStringExtra("predictedText");

        // Display the predicted text
        TextView resultTextView = findViewById(R.id.resultTextView);
        resultTextView.setText(predictedText);
    }
}
