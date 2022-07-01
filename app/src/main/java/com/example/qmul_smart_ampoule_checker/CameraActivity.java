package com.example.qmul_smart_ampoule_checker;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;

import android.os.Bundle;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

public class CameraActivity extends AppCompatActivity {

    private PreviewView cameraView;
    @Nullable
    ProcessCameraProvider cameraProvider;
    private MutableLiveData<ProcessCameraProvider> cameraProviderLiveData;
    private ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        cameraView = findViewById(R.id.camera_previewView);
        startCameraLiveData();
    }

    private void startCameraLiveData() {
        try {
            if (cameraProviderLiveData == null) {
                cameraProviderLiveData = new MutableLiveData<>();
            }
            cameraProviderListenableFuture = ProcessCameraProvider.getInstance(getApplication());
            cameraProviderListenableFuture.addListener(() -> {
                try {
                    cameraProviderLiveData.setValue(cameraProviderListenableFuture.get());
                    cameraProviderLiveData.observe(this, provider -> {
                        cameraProvider = provider;
                        bindCamera();
                    });
                } catch (Exception ex) {

                }
            }, ContextCompat.getMainExecutor(getApplication()));
        } catch (Exception ex) {
            Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void bindCamera() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
}
