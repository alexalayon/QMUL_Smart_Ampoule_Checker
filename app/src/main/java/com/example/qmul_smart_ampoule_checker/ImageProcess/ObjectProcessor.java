package com.example.qmul_smart_ampoule_checker.ImageProcess;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.common.server.converter.StringToIntConverter;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.ObjectDetectorOptionsBase;

import java.util.List;

public class ObjectProcessor extends ProcessorBase<List<DetectedObject>> {
    private static final float CONFIDENTIALITY_THRESHOLD = 0.85f;

    private final ObjectDetector objectDetector;

    private static int no_ampoule_label_index;
    private static int ampoule_label_index;

    private static boolean ampoule_detected = false;
    private static List<DetectedObject> objectDetectedResults;

    public ObjectProcessor(Context context, ObjectDetectorOptionsBase options, String[] labels) {
        super(context);
        objectDetector = ObjectDetection.getClient(options);

        if (labels.length > 0) {
            no_ampoule_label_index = Integer.parseInt(labels[0].substring(0, labels[0].indexOf(" ")));
            ampoule_label_index = Integer.parseInt(labels[1].substring(0, labels[1].indexOf(" ")));
        }

        ampoule_detected = false;
    }

    protected Task<List<DetectedObject>> detectInImage(InputImage image) {
        return objectDetector.process(image);
    }

    @Override
    public void stop() {
        super.stop();
        objectDetector.close();
    }

    @Override
    protected void onSuccess(@NonNull List<DetectedObject> results) {
        if (results.size() > 0){
            for (DetectedObject.Label label : results.get(0).getLabels()) {
                if (label.getIndex() == ampoule_label_index && label.getConfidence() > CONFIDENTIALITY_THRESHOLD) {
                    ampoule_detected = true;
                    objectDetectedResults = results;
                }
            }
        }
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
    }

    public static boolean hasAmpouleDetected() {
        return ampoule_detected;
    }

    public static List<DetectedObject> getResults() {
        return  objectDetectedResults;
    }
}
