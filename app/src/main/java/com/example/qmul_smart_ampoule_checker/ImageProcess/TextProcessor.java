package com.example.qmul_smart_ampoule_checker.ImageProcess;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.Text.TextBlock;
import com.google.mlkit.vision.text.Text.Line;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface;

import java.util.List;

public class TextProcessor extends ProcessorBase<Text> {

    private static final int IMAGE_BOUNDARY_THRESHOLD = 200;

    private final TextRecognizer textRecognizer;

    private final List<DetectedObject> ampouleDetectionResults;

    private boolean ampouleNameCheck = false;
    private boolean ampouleConcentrationWeightCheck = false;
    private boolean ampouleConcentrationHeightCheck = false;
    private boolean ampouleExpiryDateCheck = false;

    private String ampouleName = "";
    private int ampouleConcentrationWeight;
    private int ampouleConcentrationHeight;
    private String ampouleExpiryDate = "";

    public static boolean hasReadAllRelevantText;
    private static String ampouleLabelRelevantText;

    public TextProcessor(Context context, TextRecognizerOptionsInterface textRecognizerOptionsInterface, @Nullable List<DetectedObject> ampouleResults) {
        super(context);
        textRecognizer = TextRecognition.getClient(textRecognizerOptionsInterface);
        ampouleDetectionResults = ampouleResults;

        ampouleLabelRelevantText = "";
        hasReadAllRelevantText = false;
    }

    @Override
    protected Task<Text> detectInImage(InputImage image) {
        return textRecognizer.process(image);
    }

    @Override
    public void stop() {
        super.stop();
        textRecognizer.close();
    }

    @Override
    protected void onSuccess(@NonNull Text results) {
        if (results.getTextBlocks().size() > 0) {
            FilterText(results);
        }
    }

    @Override
    protected void onFailure(@NonNull Exception e) {

    }

    private void FilterText(@NonNull Text results) {
        for (TextBlock block : results.getTextBlocks()) {
            if (textInsideAmpouleBoundary(block)) {
                for (Line line : block.getLines()) {
                    if (line.getText().toLowerCase().contains("mg")
                            || line.getText().toLowerCase().contains("mcg")
                            || line.getText().toLowerCase().contains("gram")) {
                        if (ampouleConcentrationWeightCheck) {
                            int lineWeight = getAmpouleConcentrationWeight(line.getText());
                            if (ampouleConcentrationWeight != lineWeight) {
                                ampouleConcentrationWeight = (lineWeight > ampouleConcentrationWeight ? lineWeight : ampouleConcentrationWeight);
                            }
                        } else {
                            ampouleConcentrationWeight = getAmpouleConcentrationWeight(line.getText());
                            if (ampouleConcentrationWeight > 0) {
                                ampouleConcentrationWeightCheck = true;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean textInsideAmpouleBoundary(TextBlock block) {
        if (block.getBoundingBox().top > (ampouleDetectionResults.get(0).getBoundingBox().top - IMAGE_BOUNDARY_THRESHOLD)
                && block.getBoundingBox().bottom < (ampouleDetectionResults.get(0).getBoundingBox().bottom + IMAGE_BOUNDARY_THRESHOLD)
                && block.getBoundingBox().left > (ampouleDetectionResults.get(0).getBoundingBox().left - IMAGE_BOUNDARY_THRESHOLD)
                && block.getBoundingBox().right < (ampouleDetectionResults.get(0).getBoundingBox().right + IMAGE_BOUNDARY_THRESHOLD)) {
            return true;
        }
        else {
            return false;
        }
    }

    private int getAmpouleConcentrationWeight(String text) {
        try {
            if (text.toLowerCase().contains("mg")){
                String substring = text.substring(0, text.toLowerCase().indexOf("mg")).replaceAll("\\D+", "");
                return Integer.getInteger(substring);
            }else if (text.toLowerCase().contains("mcg")){
                String substring = text.substring(0, text.toLowerCase().indexOf("mcg")).replaceAll("\\D+", "");
                return Integer.getInteger(substring);
            }else if (text.toLowerCase().contains("gram")){
                String substring = text.substring(0, text.toLowerCase().indexOf("gram")).replaceAll("\\D+", "");
                return Integer.getInteger(substring);
            } else {
                return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean hasReadAllValues() {
        return hasReadAllRelevantText;
    }

    public static String getAmpouleLabelRelevantText() {
        return ampouleLabelRelevantText;
    }
}

