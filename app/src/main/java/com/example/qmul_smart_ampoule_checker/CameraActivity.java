package com.example.qmul_smart_ampoule_checker;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;

import android.os.Bundle;
import android.widget.Toast;

import com.example.qmul_smart_ampoule_checker.ImageProcess.ImageProcessor;
import com.example.qmul_smart_ampoule_checker.ImageProcess.ObjectProcessor;
import com.example.qmul_smart_ampoule_checker.ImageProcess.TextProcessor;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;

import java.io.IOException;
import java.io.InputStream;

public class CameraActivity extends AppCompatActivity {

    private static final int MAX_OBJECT_LABEL_COUNT = 1;
    private static final String OBJECT_CLASSIFICATION_MODEL_FILE = "ampoule_model.tflite";
    private static final String OBJECT_CLASSIFICATION_LABEL_FILE = "labels.txt";

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

    private String[] getModelLabels(InputStream is) {
        try {
            String[] labels = new String[1];
            byte[] buffer = new byte[is.available()];
            int input_stream_string = is.read(buffer);
            is.close();
            String content = new String(buffer);
            if (content != null && content != "") {
                labels = content.split("\n");
            }

            return labels;
        } catch (Exception ex) {
            return null;
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

        try {
            LocalModel localModel = new LocalModel.Builder().setAssetFilePath(OBJECT_CLASSIFICATION_MODEL_FILE).build();
            CustomObjectDetectorOptions.Builder objectDetectorBuilder = new CustomObjectDetectorOptions.Builder(localModel).setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE);
            objectDetectorBuilder.enableClassification().setMaxPerObjectLabelCount(MAX_OBJECT_LABEL_COUNT);
            objectDetectorOptions = objectDetectorBuilder.build();

            InputStream inputStream = getAssets().open(OBJECT_CLASSIFICATION_LABEL_FILE);
            String[] labels = getModelLabels(inputStream);

            imageProcessor = new ObjectProcessor(this, objectDetectorOptions, labels);
        } catch (Exception ex) {
            Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }

        imageAnalysisProcess = new ImageAnalysis.Builder().build();
        imageAnalysisProcess.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
            try {
                imageProcessor.processImageProxy(imageProxy);
                if (imageProcessor.hasAmpouleDetected()) {
                    imageProcessor.stop();

                    bindTextRecognition();
                }
            } catch (MlKitException ex) {
                Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysisProcess);
    }

    private void bindTextRecognition() {
        if (imageAnalysisProcess != null) {
            cameraProvider.unbind(imageAnalysisProcess);
        }
        if (imageProcessor != null) {
            imageProcessor.stop();
        }

        try {

        } catch (Exception ex) {
            Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }

        imageAnalysisProcess = new ImageAnalysis.Builder().build();
        imageAnalysisProcess.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
            try {
                imageProcessor.processImageProxy(imageProxy);
            } catch (MlKitException ex) {
                Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysisProcess);
    }
}
