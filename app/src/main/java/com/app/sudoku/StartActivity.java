package com.app.sudoku;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
    }


    @SuppressLint("NonConstantResourceId")
    public void onButtonClick(View view) {
        Intent intent = new Intent(StartActivity.this, MainActivity.class);


        switch (view.getId()) {
            case R.id.buttonEasy:
                intent.putExtra("difficultyLevel", 0);
                startActivity(intent);
                break;

            case R.id.buttonNormal:
                intent.putExtra("difficultyLevel", 1);
                startActivity(intent);
                break;

            case R.id.buttonHard:
                intent.putExtra("difficultyLevel", 2);
                startActivity(intent);
                break;
            
            case R.id.buttonExpert:
                intent.putExtra("difficultyLevel", 3);
                startActivity(intent);
                break;
        }
    }
}