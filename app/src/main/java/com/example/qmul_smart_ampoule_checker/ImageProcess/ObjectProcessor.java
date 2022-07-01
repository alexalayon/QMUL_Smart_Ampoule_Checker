package com.example.qmul_smart_ampoule_checker.ImageProcess;

import android.content.Context;

import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.ObjectDetectorOptionsBase;

import java.util.List;

public class ObjectProcessor extends ProcessorBase<List<DetectedObject>> {

    private final ObjectDetector objectDetector;

    public ObjectProcessor(Context context, ObjectDetectorOptionsBase options) {
        super(context);
        objectDetector = ObjectDetection.getClient(options);
    }

    @Override
    public void stop() {
        super.stop();
        objectDetector.close();
    }
}
