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
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // UI-Elemente
    private Button btnSelectSrt;
    private Button btnGenerateVideo;
    private Button btnDownloadVideo;
    private EditText etArtStyle;
    private TextView tvSelectedFile;
    private TextView tvStatus;
    private ProgressBar progressBar;

    private Uri srtFileUri; // Variable zum Speichern des Pfads zur ausgewählten Datei

    // ActivityResultLauncher für die Dateiauswahl
    private final ActivityResultLauncher<Intent> selectSrtLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            srtFileUri = data.getData();
                            Log.d(TAG, "SRT-Datei ausgewählt: " + srtFileUri.toString());
                            String fileName = getFileName(srtFileUri);
                            tvSelectedFile.setText("Ausgewählt: " + fileName);
                            tvStatus.setText("SRT-Datei erfolgreich geladen.");
                            btnGenerateVideo.setEnabled(true); // Aktiviere den "Video erstellen"-Button
                        }
                    } else {
                        tvStatus.setText("Dateiauswahl abgebrochen.");
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI-Elemente initialisieren
        btnSelectSrt = findViewById(R.id.btn_select_srt);
        btnGenerateVideo = findViewById(R.id.btn_generate_video);
        btnDownloadVideo = findViewById(R.id.btn_download_video);
        etArtStyle = findViewById(R.id.et_art_style);
        tvSelectedFile = findViewById(R.id.tv_selected_file);
        tvStatus = findViewById(R.id.tv_status);
        progressBar = findViewById(R.id.progress_bar);

        // Click-Listener
        btnSelectSrt.setOnClickListener(v -> openFilePicker());

        btnGenerateVideo.setOnClickListener(v -> {
            String artStyle = etArtStyle.getText().toString();
            if (artStyle.isEmpty()) {
                tvStatus.setText("Bitte gib einen Art-Style ein.");
                return;
            }
            if (srtFileUri == null) {
                tvStatus.setText("Bitte wähle zuerst eine SRT-Datei aus.");
                return;
            }
            // TODO: Logik zur Videogenerierung
            tvStatus.setText("Starte Videogenerierung für Stil: " + artStyle);
            progressBar.setVisibility(View.VISIBLE);
        });

        btnDownloadVideo.setOnClickListener(v -> {
            // TODO: Logik zum Herunterladen
            tvStatus.setText("Download wird implementiert...");
        });
    }

    // Methode zum Öffnen des Dateimanagers
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); // Erlaube alle Dateitypen, wir filtern auf .srt
        String[] mimetypes = {"application/x-subrip", "text/plain"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);


        tvStatus.setText("Bitte wähle eine .srt-Datei aus.");
        selectSrtLauncher.launch(intent);
    }

    // Hilfsmethode, um den Dateinamen aus der Uri zu extrahieren
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