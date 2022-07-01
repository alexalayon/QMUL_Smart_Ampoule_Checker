package com.example.qmul_smart_ampoule_checker;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.example.qmul_smart_ampoule_checker.ImageProcess.ImageProcessor;
import com.example.qmul_smart_ampoule_checker.ImageProcess.ObjectProcessor;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.objects.ObjectDetectorOptionsBase;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;

public class CameraActivity extends AppCompatActivity {

    private static final int MAX_OBJECT_LABEL_COUNT = 1;
    private PreviewView cameraView;
    @Nullable
    private ProcessCameraProvider cameraProvider;
    private CameraSelector cameraSelector;
    private MutableLiveData<ProcessCameraProvider> cameraProviderLiveData;
    private ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture;
    @Nullable
    private Preview previewUseCase;

    @Nullable
    private ImageAnalysis imageAnalysisProcess;
    @Nullable
    private ImageProcessor imageProcessor;

    private CustomObjectDetectorOptions objectDetectorOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        cameraView = findViewById(R.id.camera_previewView);
        cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        startCameraLiveData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
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
            bindPreviewView();
            bindDetectionProcess();
        }
    }

    private void bindPreviewView() {
        previewUseCase = new Preview.Builder().build();
        previewUseCase.setSurfaceProvider(cameraView.getSurfaceProvider());
        cameraProvider.bindToLifecycle(this, cameraSelector, previewUseCase);
    }

    private void bindDetectionProcess() {
        if (imageAnalysisProcess != null) {
            cameraProvider.unbind(imageAnalysisProcess);
        }
        if (imageProcessor != null) {
            imageProcessor.stop();
        }

        LocalModel localModel = new LocalModel.Builder().setAssetFilePath(String.valueOf(R.string.asset_model_file_name)).build();
        CustomObjectDetectorOptions.Builder objectDetectorBuilder = new CustomObjectDetectorOptions.Builder(localModel).setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE);
        objectDetectorBuilder.enableClassification().setMaxPerObjectLabelCount(MAX_OBJECT_LABEL_COUNT);
        objectDetectorOptions = objectDetectorBuilder.build();

        imageProcessor = new ObjectProcessor(this, objectDetectorOptions);
    }
}
