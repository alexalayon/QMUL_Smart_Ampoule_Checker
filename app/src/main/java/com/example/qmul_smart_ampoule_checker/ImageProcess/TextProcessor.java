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
    private static final int READING_COUNTER_MARGIN = 50;

    private final TextRecognizer textRecognizer;

    private final List<DetectedObject> ampouleDetectionResults;

    private boolean ampouleNameCheck = false;
    private boolean ampouleConcentrationWeightCheck = false;
    private boolean ampouleConcentrationVolumeCheck = false;
    private boolean ampouleExpiryDateCheck = false;

    private int maxHeight;
    private int maxWidth;

    private String ampouleName = "";
    private String ampouleConcentrationWeight;
    private String ampouleConcentrationVolume;
    private String ampouleExpiryDate = "";

    private int reading_counter = 0;

    public static boolean hasReadAllRelevantText;
    private static String ampouleLabelRelevantText;
    private static boolean textNotDetectedFlag;
    private static boolean dataIncompleteFlat;

    public TextProcessor(Context context, TextRecognizerOptionsInterface textRecognizerOptionsInterface, @Nullable List<DetectedObject> ampouleResults) {
        super(context);
        textRecognizer = TextRecognition.getClient(textRecognizerOptionsInterface);
        ampouleDetectionResults = ampouleResults;

        ampouleLabelRelevantText = "";
        hasReadAllRelevantText = false;
        textNotDetectedFlag = false;
        dataIncompleteFlat = false;
        maxWidth = 0;
        maxHeight = 0;
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

        reading_counter++;
        if (ampouleConcentrationWeightCheck && ampouleConcentrationVolumeCheck
                && ampouleExpiryDateCheck && ampouleNameCheck) {
            ampouleLabelRelevantText = ampouleName + "\n"
            + ampouleConcentrationWeight + " in " + ampouleConcentrationVolume + "\n"
            + "Expiry Date: " + ampouleExpiryDate;
            hasReadAllRelevantText = true;
        } else if (reading_counter >= READING_COUNTER_MARGIN) {
            if (ampouleNameCheck || ampouleExpiryDateCheck || ampouleConcentrationWeightCheck || ampouleConcentrationWeightCheck) {
                ampouleLabelRelevantText = extractData();
                dataIncompleteFlat = true;
            } else {
                textNotDetectedFlag = true;
            }
        }
    }

    private String extractData() {
        String data = "";
        if (ampouleNameCheck) {
            data += ampouleName + "\n";
        } else {
            data += "Name not found\n";
        }

        if (ampouleConcentrationWeightCheck) {
            data += ampouleConcentrationWeight + " in ";
        } else {
            data += "Weight not found, ";
        }

        if (ampouleConcentrationVolumeCheck) {
            data += ampouleConcentrationVolume + "\n";
        } else {
            data += "Volume not found\n";
        }

        if (ampouleExpiryDateCheck) {
            data += "Expiry Date: " + ampouleExpiryDate;
        } else {
            data += "Expiry Date: not found";
        }

        return data;
    }

    @Override
    protected void onFailure(@NonNull Exception e) {

    }

    private void FilterText(@NonNull Text results) {
        boolean concentrationFound = false;
        for (TextBlock block : results.getTextBlocks()) {
            if (textInsideAmpouleBoundary(block)) {
                for (Line line : block.getLines()) {
                    String ampouleWeight = getAmpouleConcentrationWeight(line.getText());
                    if (ampouleWeight != "") {
                        if (!ampouleConcentrationWeightCheck) {
                            ampouleConcentrationWeight = ampouleWeight;
                            ampouleConcentrationWeightCheck = true;
                            reading_counter = 0;
                        }
                        concentrationFound = true;
                    }

                    String ampouleVolume = getAmpouleConcentrationVolume(line.getText());
                    if (ampouleVolume != "") {
                        if (!ampouleConcentrationVolumeCheck) {
                            ampouleConcentrationVolume = ampouleVolume;
                            ampouleConcentrationVolumeCheck = true;
                            reading_counter = 0;
                        }
                        concentrationFound = true;
                    }

                    String ampouleDate = getAmpouleExpiryDate(line.getText());
                    if (ampouleDate != "" && !ampouleExpiryDateCheck) {
                        ampouleExpiryDate = ampouleDate;
                        ampouleExpiryDateCheck = true;
                        reading_counter = 0;
                    }
                }
            }
        }

        if (concentrationFound) {
            String name = getAmpouleName(results);
            if (name != "") {
                ampouleName = name;
                ampouleNameCheck = true;
                reading_counter = 0;
            }
        }
    }

    private boolean textInsideAmpouleBoundary(TextBlock block) {
        try {
            if (block.getBoundingBox().top > (ampouleDetectionResults.get(0).getBoundingBox().top - IMAGE_BOUNDARY_THRESHOLD)
                    && block.getBoundingBox().bottom < (ampouleDetectionResults.get(0).getBoundingBox().bottom + IMAGE_BOUNDARY_THRESHOLD)
                    && block.getBoundingBox().left > (ampouleDetectionResults.get(0).getBoundingBox().left - IMAGE_BOUNDARY_THRESHOLD)
                    && block.getBoundingBox().right < (ampouleDetectionResults.get(0).getBoundingBox().right + IMAGE_BOUNDARY_THRESHOLD)) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private String getAmpouleConcentrationWeight(String text) {
        try {
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

            number = substringFirstDigit(number, 0);
            if (Float.parseFloat(number) == 0.0f){
                return "";
            }
            return number + measure;
        } catch (Exception e) {
            return "";
        }
    }

    private String getAmpouleConcentrationVolume(String text) {
        try {
            String number;
            if (text.toLowerCase().contains("ml")) {
                number = text.toLowerCase().substring(0, text.toLowerCase().indexOf("ml")).replaceAll(" ", "");
            } else if (text.toLowerCase().contains("mililiter")) {
                number = text.toLowerCase().substring(0, text.toLowerCase().indexOf("mililiter")).replaceAll(" ", "");
            } else {
                return "";
            }

            if (number.contains("mg") || number.contains("mcg") || number.contains("gram")) {
                number = substringFirstDigit(number, number.indexOf("m"));
            }

            number = substringFirstDigit(number, 0);
            if (number.length() == 0) {
                number = "1";
            }

            if (ampouleConcentrationWeightCheck && ampouleConcentrationWeight.contains(number) && number != "1") {
                return "";
            }

            if (Float.parseFloat(number) == 0.0f) {
                return "";
            }
            return number + "ml";
        } catch (Exception e) {
            return "";
        }
    }

    private String getAmpouleExpiryDate(String text) {
        try {
            String date = "";
            text = text.replaceAll("/", "-").replaceAll(" ", "-");
            if (text.toLowerCase().startsWith("exp") || text.toLowerCase().startsWith("date")
                    || text.contains("-")) {
                text = substringFirstDigit(text, 0);
                if ((text.startsWith("0") || text.startsWith("1"))
                        && text.substring(text.indexOf("-") + 1).startsWith("20")
                        && (text.length() == 5 || text.length() == 7)
                        && text.matches("[\\d-]+")) {
                    date = substringFirstDigit(text, 0);
                }
            }

            date = date.replaceAll("-", " ");
            return getDate(date);
        } catch (Exception e) {
            return "";
        }
    }

    private String getAmpouleName(Text result) {
        try {
            String name = "";
            for (TextBlock block : result.getTextBlocks()) {
                for (Line line : block.getLines()) {
                    if (line.getText().toLowerCase().contains("mcg") || line.getText().toLowerCase().contains("mg") || line.getText().toLowerCase().contains("gram")
                            || line.getText().toLowerCase().contains("ml") || line.getText().toLowerCase().contains("liter")
                            || line.getText().toLowerCase().contains("tion")
                            || line.getText().toLowerCase().contains("solu")
                            || line.getText().toLowerCase().contains("injec")
                            || line.getText().toLowerCase().contains("for")
                            || line.getText().toLowerCase().contains("contain")) {
                        continue;
                    }

                    if (maxHeight < line.getBoundingBox().height() && maxWidth < line.getBoundingBox().width()) {
                        String[] nameTrim = line.getText().split(" ");
                        boolean correctName = true;
                        for (String nameWord : nameTrim) {
                            if (nameWord.length() <= 5 || !Character.isUpperCase(nameWord.charAt(0)) || !checkLowerCase(nameWord)) {
                                correctName = false;
                                break;
                            }
                        }

                        if (correctName && line.getText().replaceAll(" ", "").matches("[A-Za-z]+")) {
                            maxHeight = line.getBoundingBox().height();
                            maxWidth = line.getBoundingBox().width();
                            name = line.getText();
                        }
                    }
                }
            }
            return name;
        } catch (Exception e) {
            return "";
        }
    }

    private String getDate(String date) {
        String new_date = "";
        switch (date.substring(0, date.indexOf(" "))) {
            case "01": new_date = "January"; break;
            case "02": new_date = "February";break;
            case "03": new_date = "March";break;
            case "04": new_date = "April";break;
            case "05": new_date = "May";break;
            case "06": new_date = "June";break;
            case "07": new_date = "July";break;
            case "08": new_date = "August";break;
            case "09": new_date = "September";break;
            case "10": new_date = "October";break;
            case "11": new_date = "November";break;
            case "12": new_date = "December";break;
            default: return "";
        }
        new_date += date.substring(date.indexOf(" "));
        return new_date;
    }

    private boolean checkLowerCase (String text) {
        boolean is_lowercase = true;
        for (int i = 1; i < text.length(); i++) {
            if (Character.isUpperCase(text.charAt(i))) {
                is_lowercase = false;
                break;
            }
        }

        return is_lowercase;
    }

    private String substringFirstDigit(String text, int index) {
        try {
            String s = "";
            for (int i = index; i < text.length(); i++) {
                if (Character.isDigit(text.charAt(i))) {
                    s = text.substring(i);
                    break;
                }
            }
            return s;
        } catch (Exception e) { return "";}
    }

    public static boolean hasReadAllValues() {
        return hasReadAllRelevantText;
    }

    public static String getAmpouleLabelRelevantText() {
        return ampouleLabelRelevantText;
    }

    public static boolean getTextNotDetectedFlag() {
        return textNotDetectedFlag;
    }

    public static boolean getTextDataIncompleteFlag() {
        return dataIncompleteFlat;
    }
}


