package com.example.qmul_smart_ampoule_checker;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {
    private static final String APPLICATION_TAG_LOG = "Application";

    private static final int CAMERA_PERMISSION_CODE = 100;
    private Button startButton, settingsButton;

    public static SettingsActivity.Settings SettingsAudioValue;
    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(APPLICATION_TAG_LOG, "Start Application");

        startButton = findViewById(R.id.Start_button);
        settingsButton = findViewById(R.id.Settings_button);

        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onInit(int status) {
                Voice voiceDefault = textToSpeech.getVoice();
                SettingsAudioValue = new SettingsActivity.Settings(voiceDefault.getLocale(), voiceDefault.getName(), 1.0f, 1.0f, textToSpeech.getVoices()
                        .stream().filter(v -> v.getLocale().equals(Locale.UK) && v.isNetworkConnectionRequired() == false).collect(Collectors.toList()));
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isCameraPermissionGranted()) {
                    startCameraActivity();
                } else {
                    getCameraPermission();
                }
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    private boolean isCameraPermissionGranted() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private void startCameraActivity() {
        Intent startIntent = new Intent(MainActivity.this, CameraActivity.class);
        textToSpeech.stop();
        startActivity(startIntent);
    }

    private void getCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraActivity();
            } else {
                Toast.makeText(MainActivity.this, "The application has no right to access the camera!. Please verify.", Toast.LENGTH_LONG).show();
            }
        }
    }
}