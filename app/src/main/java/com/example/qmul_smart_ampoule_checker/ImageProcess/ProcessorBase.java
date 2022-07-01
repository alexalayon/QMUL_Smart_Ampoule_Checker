package com.example.qmul_smart_ampoule_checker.ImageProcess;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.arch.core.executor.TaskExecutor;

import com.google.android.gms.tasks.TaskExecutors;

import java.util.concurrent.Executor;

public abstract class ProcessorBase<T> implements ImageProcessor {

    @NonNull
    private final Executor executor;

    protected ProcessorBase(Context context) {
        executor = TaskExecutors.MAIN_THREAD;
    }

    @Override
    public void stop() {
    }
}
