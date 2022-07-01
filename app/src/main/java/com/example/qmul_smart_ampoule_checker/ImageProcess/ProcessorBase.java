package com.example.qmul_smart_ampoule_checker.ImageProcess;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class ProcessorBase<T> implements ImageProcessor {

    private boolean isShutdown;

    @NonNull
    private final Executor executor;
    private final AtomicBoolean shutdownVal = new AtomicBoolean();

    protected ProcessorBase(Context context) {
        executor = TaskExecutors.MAIN_THREAD;
    }

    @Override
    @ExperimentalGetImage
    public void processImageProxy(ImageProxy imageProxy) throws MlKitException {
        if (isShutdown) {
            imageProxy.close();
            return;
        }

        Bitmap bitmapImage =null;
        detectObjectInImageProcess(InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees()), bitmapImage).addOnCompleteListener(results -> imageProxy.close());
    }

    private Task<T> detectObjectInImageProcess(final InputImage image, @Nullable final Bitmap imageBitmap) {
        return setListener(detectInImage(image), imageBitmap);
    }

    private Task<T> setListener(Task<T> task, @Nullable final Bitmap imageBitmap) {
        return task.addOnSuccessListener(executor, results -> {
            ProcessorBase.this.onSuccess(results);
        }).addOnFailureListener(executor, e -> {
            ProcessorBase.this.onFailure(e);
        });
    }


    protected abstract Task<T> detectInImage(InputImage image);

    @Override
    public void stop() {
        shutdownVal.set(true);
        isShutdown = true;
    }

    protected abstract void onSuccess(@NonNull T results);

    protected abstract void onFailure(@NonNull Exception e);

    @Override
    public abstract boolean hasAmpouleDetected();
}
