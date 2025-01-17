package com.example.slt;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;



public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Button centerButton1 = findViewById(R.id.centerButton1);
        centerButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to SLTActivity
                Intent intent = new Intent(MainActivity.this, SLTActivity.class);
                startActivity(intent);
            }
        });




        Button centerButton2 = findViewById(R.id.centerButton2);
        centerButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to TTSActivity
                Intent intent = new Intent(MainActivity.this, TTSActivity.class);
                startActivity(intent);
            }
        });


    }
}
