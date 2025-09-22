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
import androidx.core.content.FileProvider;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    // FÜGE HIER DEINEN GEMINI API SCHLÜSSEL EIN
    private static final String GEMINI_API_KEY = "DEIN_API_SCHLUESSEL_HIER";


    // UI-Elemente
    private Button btnSelectSrt, btnGenerateVideo, btnDownloadVideo;
    private EditText etArtStyle;
    private TextView tvSelectedFile, tvStatus;
    private ProgressBar progressBar;

    private Uri srtFileUri;
    private List<Subtitle> subtitles;
    private String finalVideoPath; // Zum Speichern des Videopfads

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
                            tvSelectedFile.setText("Ausgewählt: " + fileName);
                            tvStatus.setText(subtitles.size() + " Untertitel erfolgreich geladen.");
                            btnGenerateVideo.setEnabled(true);
                            btnDownloadVideo.setVisibility(View.GONE);
                            finalVideoPath = null;
                        } else {
                            tvStatus.setText("Fehler: Die SRT-Datei konnte nicht gelesen oder geparst werden.");
                            btnGenerateVideo.setEnabled(false);
                        }
                    }
                } else {
                    tvStatus.setText("Dateiauswahl abgebrochen.");
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
        btnDownloadVideo.setOnClickListener(v -> {
            if (finalVideoPath != null && !finalVideoPath.isEmpty()) {
                try {
                    File videoFile = new File(finalVideoPath);
                    Uri videoUri = FileProvider.getUriForFile(
                            MainActivity.this,
                            getApplicationContext().getPackageName() + ".provider",
                            videoFile
                    );
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(videoUri, "video/mp4");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(intent, "Video öffnen mit..."));
                } catch (Exception e) {
                    Log.e(TAG, "Error opening video", e);
                    Toast.makeText(this, "Konnte keinen Video-Player finden.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void generateVideo() {
        String artStyle = etArtStyle.getText().toString().trim();
        if (artStyle.isEmpty()) {
            tvStatus.setText("Bitte beschreibe einen Kunststil.");
            return;
        }
        if (srtFileUri == null || subtitles == null || subtitles.isEmpty()) {
            tvStatus.setText("Bitte wähle zuerst eine gültige SRT-Datei aus.");
            return;
        }

        setUiLoading(true);
        callGeminiApi(artStyle);
    }

    private void callGeminiApi(String artStyle) {
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", GEMINI_API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

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

                try {
                    Gson gson = new Gson();
                    VideoStyle videoStyle = gson.fromJson(jsonResponse, VideoStyle.class);
                    if (videoStyle == null) throw new Exception("JSON could not be parsed.");
                    Log.d(TAG, "Parsed Style: " + videoStyle.toString());

                    runOnUiThread(() -> tvStatus.setText("Stil von Gemini erhalten! Starte Video-Rendering..."));

                    VideoRenderer renderer = new VideoRenderer(MainActivity.this, subtitles, videoStyle, new VideoRenderer.RenderCallback() {
                        @Override
                        public void onProgress(int progress) {
                            runOnUiThread(() -> {
                                progressBar.setProgress(progress, true);
                                tvStatus.setText("Video wird erstellt... " + progress + "%");
                            });
                        }

                        @Override
                        public void onComplete(String filePath) {
                            runOnUiThread(() -> {
                                setUiLoading(false);
                                tvStatus.setText("Video erfolgreich erstellt!");
                                finalVideoPath = filePath;
                                btnDownloadVideo.setVisibility(View.VISIBLE);
                            });
                        }

                        @Override
                        public void onError(String message) {
                            runOnUiThread(() -> {
                                setUiLoading(false);
                                tvStatus.setText("Fehler: " + message);
                            });
                        }
                    });
                    renderer.startRendering();

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing Gemini response", e);
                    runOnUiThread(() -> {
                        setUiLoading(false);
                        tvStatus.setText("Fehler: Die Antwort der KI war ungültig.");
                    });
                }
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
        if(isLoading) progressBar.setProgress(0);
        btnGenerateVideo.setEnabled(!isLoading);
        btnSelectSrt.setEnabled(!isLoading);
        etArtStyle.setEnabled(!isLoading);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimetypes = {"application/x-subrip", "text/plain"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        tvStatus.setText("Bitte wähle eine .srt-Datei aus.");
        selectSrtLauncher.launch(intent);
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}