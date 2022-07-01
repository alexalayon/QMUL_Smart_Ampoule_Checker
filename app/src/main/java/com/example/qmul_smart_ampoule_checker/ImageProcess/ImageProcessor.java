package com.example.qmul_smart_ampoule_checker.ImageProcess;

import androidx.camera.core.ImageProxy;

import com.google.mlkit.common.MlKitException;

public interface ImageProcessor {

    void processImageProxy(ImageProxy imageProxy) throws MlKitException;

    void stop();
}
