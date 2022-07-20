package com.example.qmul_smart_ampoule_checker;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SettingsActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    public static class Settings {

        public Locale locale;
        public String voiceName;
        public float pitch;
        public float speechRate;
        public List<Voice> voiceList;

        public Settings(Locale locale, String voiceName, float pitch, float speechRate, List<Voice> voiceList) {
            this.locale = locale;
            this.voiceName = voiceName;
            this.pitch = pitch;
            this.speechRate = speechRate;
            this.voiceList = voiceList;
        }
    }

    private TextToSpeech tts;

    private List<Voice> voiceListSet;
    private Voice voice = null;
    private float pitch, speechRate;
    private SeekBar pitchSeekBar, speechRateSeekBar;
    private Button testButton, saveButton;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        pitchSeekBar = findViewById(R.id.pitch_seekBar);
        speechRateSeekBar = findViewById(R.id.speechRate_seekBar);
        testButton = findViewById(R.id.test_button);
        saveButton = findViewById(R.id.save_button);

        voiceListSet = MainActivity.SettingsAudioValue.voiceList;
        List<Voice> voicelist = MainActivity.SettingsAudioValue.voiceList.stream().filter(v -> v.getName().equals(MainActivity.SettingsAudioValue.voiceName)).collect(Collectors.toList());
        if (voicelist.size() > 0) {
            voice = voicelist.get(0);
        } else {
            voice = MainActivity.SettingsAudioValue.voiceList.get(0);
        }
        pitchSeekBar.setProgress((int)(MainActivity.SettingsAudioValue.pitch * 50));
        speechRateSeekBar.setProgress((int)(MainActivity.SettingsAudioValue.speechRate * 50));

        Spinner voiceListSpinner = findViewById(R.id.voiceList_spinner);
        List<String> spinnerOptions = new ArrayList<>();
        for (int i = 0; i < voiceListSet.size(); i++) {
            spinnerOptions.add("Voice " + Integer.toString(i));
        }
        ArrayAdapter<String> arrayAdapter =new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, spinnerOptions);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        voiceListSpinner.setAdapter(arrayAdapter);
        voiceListSpinner.setOnItemSelectedListener(this);


        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tts = new TextToSpeech(SettingsActivity.this, new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status == TextToSpeech.SUCCESS) {
                            TestAudio();
                        }
                    }
                });
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.SettingsAudioValue.speechRate = (float) speechRateSeekBar.getProgress() / 50;
                MainActivity.SettingsAudioValue.pitch = (float) pitchSeekBar.getProgress() / 50;
                MainActivity.SettingsAudioValue.voiceName = voice.getName();
                MainActivity.SettingsAudioValue.locale = voice.getLocale();

                finish();
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        voice = MainActivity.SettingsAudioValue.voiceList.get(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Pass
    }

    private void TestAudio() {
        tts.setLanguage(voice.getLocale());
        tts.setVoice(voice);

        pitch = (float) pitchSeekBar.getProgress() / 50;
        pitch = (pitch > 0.1f) ? pitch : 0.1f;
        tts.setPitch(pitch);

        speechRate = (float) speechRateSeekBar.getProgress() / 50;
        speechRate = (speechRate > 0.1f) ? speechRate : 0.1f;
        tts.setSpeechRate(speechRate);

        tts.speak("Testing Audio", TextToSpeech.QUEUE_FLUSH, null);
    }
}