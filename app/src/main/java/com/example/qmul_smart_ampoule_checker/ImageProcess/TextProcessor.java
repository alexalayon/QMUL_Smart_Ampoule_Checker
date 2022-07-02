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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public class TextProcessor extends ProcessorBase<Text> {

    private static final int IMAGE_BOUNDARY_THRESHOLD = 200;

    private final TextRecognizer textRecognizer;

    private final List<DetectedObject> ampouleDetectionResults;

    private boolean ampouleNameCheck = false;
    private boolean ampouleConcentrationWeightCheck = false;
    private boolean ampouleConcentrationVolumeCheck = false;
    private boolean ampouleExpiryDateCheck = false;

    private String ampouleName = "";
    private String ampouleConcentrationWeight;
    private String ampouleConcentrationVolume;
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

        if (ampouleConcentrationWeightCheck && ampouleConcentrationVolumeCheck
                && ampouleExpiryDateCheck && ampouleNameCheck) {
            ampouleLabelRelevantText = ampouleName + "\n"
            + ampouleConcentrationWeight + " in " + ampouleConcentrationVolume + "\n"
            + "Expiry Date: " + ampouleExpiryDate;
            hasReadAllRelevantText = true;
        }
    }

    @Override
    protected void onFailure(@NonNull Exception e) {

    }

    private void FilterText(@NonNull Text results) {
        for (TextBlock block : results.getTextBlocks()) {
            if (textInsideAmpouleBoundary(block)) {
                boolean concentrationFound = false;
                for (Line line : block.getLines()) {
                    String ampouleWeight =getAmpouleConcentrationWeight(line.getText());
                    if (ampouleWeight != "") {
                        if (!ampouleConcentrationWeightCheck) {
                            ampouleConcentrationWeight = ampouleWeight;
                            ampouleConcentrationWeightCheck = true;
                        }
                        concentrationFound = true;
                    }

                    if (!ampouleConcentrationVolumeCheck) {
                        String ampouleVolume = getAmpouleConcentrationVolume(line.getText());
                        if (ampouleVolume != "") {
                            ampouleConcentrationVolume = ampouleVolume;
                            ampouleConcentrationVolumeCheck = true;
                        }
                    }

                    if (!ampouleExpiryDateCheck) {
                        String ampouleDate = getAmpouleExpiryDate(line.getText());
                        if (ampouleDate != "") {
                            ampouleExpiryDate = ampouleDate;
                            ampouleExpiryDateCheck = true;
                        }
                    }
                }

                if (concentrationFound && !ampouleNameCheck) {
                    String name = getAmpouleName(block);
                    if (name != "") {
                        ampouleName = name;
                        ampouleNameCheck = true;
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

    private String getAmpouleConcentrationWeight(String text) {
        String number, measure;
        if (text.toLowerCase().contains("mg")) {
            number = text.toLowerCase().substring(0, text.toLowerCase().indexOf("mg")).replaceAll(" ", "");
            measure = "mg";
        } else if (text.toLowerCase().contains("mcg")) {
            number = text.toLowerCase().substring(0, text.toLowerCase().indexOf("mcg")).replaceAll(" ", "");
            measure = "mcg";
        } else if (text.toLowerCase().contains("miligram")) {
            number = text.toLowerCase().substring(0, text.toLowerCase().indexOf("miligram")).replaceAll(" ", "");
            measure = "mg";
        } else if (text.toLowerCase().contains("microgram")) {
            number = text.toLowerCase().substring(0, text.toLowerCase().indexOf("microgram")).replaceAll(" ", "");
            measure = "mcg";
        } else {
            return "";
        }

        number = substringFirstDigit(number);
        return number + measure;
    }

    private String getAmpouleConcentrationVolume(String text) {
        String number;
        if (text.toLowerCase().contains("ml")) {
            number = text.toLowerCase().substring(0, text.toLowerCase().indexOf("ml")).replaceAll(" ", "");
        } else if (text.toLowerCase().contains("mililiter")) {
            number = text.toLowerCase().substring(0, text.toLowerCase().indexOf("mililiter")).replaceAll(" ", "");
        } else {
            return "";
        }

        if (number.contains(ampouleConcentrationWeight.replaceAll("[^0-9.]", ""))) {
            number = number.substring(number.indexOf(ampouleConcentrationWeight.substring(0))).replace(ampouleConcentrationWeight.replaceAll("[^0-9.]", ""), "");
        }

        number = substringFirstDigit(number);
        if (number.length() == 0) {
            number = "1";
        }
        return number + "ml";
    }

    private String getAmpouleExpiryDate(String text) {
        String date = "";
        text = text.replaceAll("/", "-").replaceAll(" ", "-");
        if (text.toLowerCase().startsWith("exp") || text.toLowerCase().startsWith("date")
                || text.contains("-")) {
            if ((text.startsWith("0") || text.startsWith("1"))
                    && text.substring(text.indexOf("-")+1).startsWith("20")
                    && (text.length() == 5 || text.length() == 7)) {
                date = substringFirstDigit(text);
            }
        }

        date = date.replaceAll("-", " ");
        return date;
    }

    private String getAmpouleName(TextBlock block) {
        String name = "";
        int maxHeigth = 0, maxWidth = 0;
        for (Line line:block.getLines()) {
            if (line.getText().contains(ampouleConcentrationWeight.replaceAll("[^0-9.]", ""))
                    || line.getText().toLowerCase().contains("solution")
                    || line.getText().toLowerCase().contains("inject")
            ) {
                continue;
            }

            if (maxHeigth < line.getBoundingBox().height() && maxWidth < line.getBoundingBox().width()){
                maxHeigth = line.getBoundingBox().height();
                maxWidth = line.getBoundingBox().width();
                name = line.getText();
            }
        }
        return name;
    }

    private String substringFirstDigit(String text) {
        String s = "";
        for (int i = 0; i<text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) {
                s = text.substring(i);
                break;
            }
        }
        return s;
    }

    public static boolean hasReadAllValues() {
        return hasReadAllRelevantText;
    }

    public static String getAmpouleLabelRelevantText() {
        return ampouleLabelRelevantText;
    }
}

