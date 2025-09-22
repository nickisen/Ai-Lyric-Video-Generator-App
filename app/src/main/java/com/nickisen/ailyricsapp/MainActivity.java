package com.nickisen.ailyricsapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    // FÜGE HIER DEINEN GEMINI API SCHLÜSSEL EIN
    private static final String GEMINI_API_KEY = "AIzaSyDeyo_ourD7EV8PlmUUH6GQmclGqVSI0Bc";

    // UI-Elemente
    private Button btnSelectSrt, btnGenerateVideo, btnDownloadVideo;
    private EditText etArtStyle;
    private TextView tvSelectedFile, tvStatus;
    private ProgressBar progressBar;

    private Uri srtFileUri;
    private List<Subtitle> subtitles;

    private final ActivityResultLauncher<Intent> selectSrtLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        srtFileUri = data.getData();
                        Log.d(TAG, "SRT file selected: " + srtFileUri.toString());
                        subtitles = SrtParser.parse(this, srtFileUri);
                        if (subtitles != null && !subtitles.isEmpty()) {
                            String fileName = getFileName(srtFileUri);
                            tvSelectedFile.setText("Selected: " + fileName);
                            tvStatus.setText(subtitles.size() + " subtitles loaded successfully.");
                            btnGenerateVideo.setEnabled(true);
                        } else {
                            tvStatus.setText("Error: Could not read or parse the SRT file.");
                            btnGenerateVideo.setEnabled(false);
                        }
                    }
                } else {
                    tvStatus.setText("File selection cancelled.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSelectSrt = findViewById(R.id.btn_select_srt);
        btnGenerateVideo = findViewById(R.id.btn_generate_video);
        btnDownloadVideo = findViewById(R.id.btn_download_video);
        etArtStyle = findViewById(R.id.et_art_style);
        tvSelectedFile = findViewById(R.id.tv_selected_file);
        tvStatus = findViewById(R.id.tv_status);
        progressBar = findViewById(R.id.progress_bar);

        btnSelectSrt.setOnClickListener(v -> openFilePicker());
        btnGenerateVideo.setOnClickListener(v -> generateVideo());
        btnDownloadVideo.setOnClickListener(v -> {/* TODO */});
    }

    private void generateVideo() {
        String artStyle = etArtStyle.getText().toString().trim();
        if (artStyle.isEmpty()) {
            tvStatus.setText("Please describe an art style.");
            return;
        }
        if (srtFileUri == null || subtitles == null || subtitles.isEmpty()) {
            tvStatus.setText("Please select a valid SRT file first.");
            return;
        }

        setUiLoading(true);
        callGeminiApi(artStyle);
    }

    private void callGeminiApi(String artStyle) {
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", GEMINI_API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        // Erstelle einen detaillierten Prompt für die KI
        String prompt = "Du bist ein Experte für Videodesign. Basierend auf dem folgenden Stil, erstelle ein JSON-Objekt mit spezifischen Designparametern. " +
                "Der Stil ist: '" + artStyle + "'. " +
                "Das JSON-Objekt sollte die folgenden Schlüssel haben: 'fontColor' (als Hex-Code, z.B. '#FFFFFF'), " +
                "'fontSize' (in sp, z.B. 24), 'fontFamily' (ein gängiger Font wie 'Roboto', 'Arial', 'Courier New'), " +
                "'animationIn' (wähle aus 'fadeIn', 'slideUp', 'zoomIn'), und 'animationOut' (wähle aus 'fadeOut', 'slideDown', 'zoomOut'). " +
                "Antworte NUR mit dem JSON-Objekt und keinem anderen Text.";

        Content content = new Content.Builder().addText(prompt).build();
        Executor executor = Executors.newSingleThreadExecutor();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String jsonResponse = result.getText();
                Log.d(TAG, "Gemini Response: " + jsonResponse);
                runOnUiThread(() -> {
                    tvStatus.setText("Stil von Gemini erhalten! Starte Video-Rendering...");
                    // TODO: Parse den JSON und starte den Video-Rendering-Prozess
                    // Für jetzt beenden wir den Ladezustand
                    setUiLoading(false);
                    btnDownloadVideo.setVisibility(View.VISIBLE); // Zeige den Download-Button (provisorisch)
                });
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Error calling Gemini API", t);
                runOnUiThread(() -> {
                    tvStatus.setText("Fehler bei der Kommunikation mit der AI.");
                    setUiLoading(false);
                });
            }
        }, executor);
    }

    private void setUiLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnGenerateVideo.setEnabled(!isLoading);
        btnSelectSrt.setEnabled(!isLoading);
        etArtStyle.setEnabled(!isLoading);
    }

    private void openFilePicker() { /*...*/ }
    private String getFileName(Uri uri) { /*...*/ return "..."; } // Diese Methoden bleiben unverändert
}