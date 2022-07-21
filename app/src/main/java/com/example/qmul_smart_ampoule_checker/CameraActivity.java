package com.example.qmul_smart_ampoule_checker;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;

import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.widget.Toast;

import com.example.qmul_smart_ampoule_checker.ImageProcess.ImageProcessor;
import com.example.qmul_smart_ampoule_checker.ImageProcess.ObjectProcessor;
import com.example.qmul_smart_ampoule_checker.ImageProcess.TextProcessor;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class CameraActivity extends AppCompatActivity {
    private static final String CAMERA_TAG_LOG = "Camera Process";

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
    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        Log.i(CAMERA_TAG_LOG, "Camera Application Start");

        cameraView = findViewById(R.id.camera_previewView);
        cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    SetAudioSettings();
                }
            }
        });

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
        if (imageProcessor != null) {
            imageProcessor.stop();
        }

        bindCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (imageProcessor != null) {
            imageProcessor.stop();
        }

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
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

        Log.i(CAMERA_TAG_LOG, "Object Detection process Start");

        try {
            LocalModel localModel = new LocalModel.Builder().setAssetFilePath(OBJECT_CLASSIFICATION_MODEL_FILE).build();
            CustomObjectDetectorOptions.Builder objectDetectorBuilder = new CustomObjectDetectorOptions.Builder(localModel).setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE);
            objectDetectorBuilder.enableClassification().setMaxPerObjectLabelCount(MAX_OBJECT_LABEL_COUNT);
            objectDetectorOptions = objectDetectorBuilder.build();

            InputStream inputStream = getAssets().open(OBJECT_CLASSIFICATION_LABEL_FILE);
            String[] labels = getModelLabels(inputStream);

            imageProcessor = new ObjectProcessor(this, objectDetectorOptions, labels);
        } catch (Exception ex) {
            Log.e(CAMERA_TAG_LOG, "Can not create image processor: ", ex);
            Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }

        imageAnalysisProcess = new ImageAnalysis.Builder().build();
        imageAnalysisProcess.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
            try {
                imageProcessor.processImageProxy(imageProxy);
                if (ObjectProcessor.hasAmpouleDetected()) {
                    imageProcessor.stop();

                    Toast.makeText(this, "Ampoule Found", Toast.LENGTH_SHORT).show();
                    bindTextRecognition(ObjectProcessor.getResults());
                }
            } catch (MlKitException ex) {
                Log.e(CAMERA_TAG_LOG, "Failed to process image. Error: " + ex);
                Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysisProcess);
    }

    private void bindTextRecognition(List<DetectedObject> objectResults) {
        if (imageAnalysisProcess != null) {
            cameraProvider.unbind(imageAnalysisProcess);
        }
        if (imageProcessor != null) {
            imageProcessor.stop();
        }

        Log.i(CAMERA_TAG_LOG, "Text Recognition process Start");

        try {
            imageProcessor = new TextProcessor(this, new TextRecognizerOptions.Builder().build(), objectResults);
        } catch (Exception ex) {
            Log.e(CAMERA_TAG_LOG, "Can not create image processor: ", ex);
            Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        imageAnalysisProcess = new ImageAnalysis.Builder().build();
        imageAnalysisProcess.setAnalyzer(ContextCompat.getMainExecutor(this), imageProxy -> {
            try {
                imageProcessor.processImageProxy(imageProxy);
                if (TextProcessor.hasReadAllValues()) {
                    imageProcessor.stop();

                    labelSetOutput(TextProcessor.getAmpouleLabelRelevantText());
                } else if (TextProcessor.getTextNotDetectedFlag()) {
                    imageProcessor.stop();
                    Toast.makeText(this, "Text not detected", Toast.LENGTH_SHORT).show();
                    Log.i(CAMERA_TAG_LOG, "Text not detected");
                    bindDetectionProcess();
                } else if (TextProcessor.getTextDataIncompleteFlag()) {
                    imageProcessor.stop();
                    Toast.makeText(this, "The data is incomplete", Toast.LENGTH_SHORT).show();
                    Log.i(CAMERA_TAG_LOG, "Data is incomplete");
                    labelSetOutput(TextProcessor.getAmpouleLabelRelevantText());
                }
            } catch (MlKitException ex) {
                Toast.makeText(this, "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysisProcess);
    }

    private void labelSetOutput(String labelText) {
        if (imageProcessor != null) {
            imageProcessor.stop();
        }
        if (imageAnalysisProcess != null) {
            cameraProvider.unbind(imageAnalysisProcess);
        }

        textToSpeech.speak(labelText, TextToSpeech.QUEUE_FLUSH, null);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Label data")
                .setMessage(labelText)
                .setCancelable(false)
                .setPositiveButton("Correct", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        bindCamera();
                        Log.i(CAMERA_TAG_LOG, "Output process \t" + labelText.replaceAll("\n", "\t") + "\t Correct");
                    }
                }).setNegativeButton("Wrong", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                bindCamera();
                Log.i(CAMERA_TAG_LOG, "Output process \t" + labelText.replaceAll("\n", "\t") + "\t Wrong");
            }
        }).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void SetAudioSettings() {
        if (MainActivity.SettingsAudioValue != null) {
            Voice settingsVoice = null;
            List<Voice> voiceList = textToSpeech.getVoices().stream().filter(v -> v.getLocale().equals(Locale.UK) && v.isNetworkConnectionRequired() == false).collect(Collectors.toList());
            for (Voice voice: voiceList) {
                if (voice.getName().equals(MainActivity.SettingsAudioValue.voiceName)) {
                    settingsVoice = voice;
                    break;
                }
            }
            if (settingsVoice != null) {
                textToSpeech.setLanguage(settingsVoice.getLocale());
                textToSpeech.setVoice(settingsVoice);
                textToSpeech.setPitch(MainActivity.SettingsAudioValue.pitch);
                textToSpeech.setSpeechRate(MainActivity.SettingsAudioValue.speechRate);
            } else {
                textToSpeech.setLanguage(Locale.UK);
                textToSpeech.setPitch(1.0f);
                textToSpeech.setSpeechRate(1.0f);
            }
        }
        else {
            textToSpeech.setLanguage(Locale.UK);
            textToSpeech.setPitch(1.0f);
            textToSpeech.setSpeechRate(1.0f);
        }

        textToSpeech.speak("", TextToSpeech.QUEUE_FLUSH, null);
    }
}
