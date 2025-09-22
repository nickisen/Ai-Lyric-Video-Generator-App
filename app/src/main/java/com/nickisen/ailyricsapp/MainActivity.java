package com.nickisen.ailyricsapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // Deklaration der UI-Elemente
    private Button btnSelectSrt;
    private Button btnGenerateVideo;
    private Button btnDownloadVideo;
    private EditText etArtStyle;
    private TextView tvSelectedFile;
    private TextView tvStatus;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisierung der UI-Elemente
        // Wir verknüpfen die Variablen mit den IDs aus der XML-Datei.
        btnSelectSrt = findViewById(R.id.btn_select_srt);
        btnGenerateVideo = findViewById(R.id.btn_generate_video);
        btnDownloadVideo = findViewById(R.id.btn_download_video);
        etArtStyle = findViewById(R.id.et_art_style);
        tvSelectedFile = findViewById(R.id.tv_selected_file);
        tvStatus = findViewById(R.id.tv_status);
        progressBar = findViewById(R.id.progress_bar);

        // Setzen von Click-Listenern für die Buttons
        // Hier definieren wir, was passieren soll, wenn ein Button geklickt wird.
        btnSelectSrt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Logik zur Dateiauswahl hinzufügen
                tvStatus.setText("SRT-Auswahl wird implementiert...");
            }
        });

        btnGenerateVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Logik zur Videogenerierung hinzufügen
                String artStyle = etArtStyle.getText().toString();
                if (artStyle.isEmpty()) {
                    tvStatus.setText("Bitte gib einen Art-Style ein.");
                } else {
                    tvStatus.setText("Videogenerierung für Stil: " + artStyle);
                    progressBar.setVisibility(View.VISIBLE);
                }
            }
        });

        btnDownloadVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Logik zum Herunterladen des Videos hinzufügen
                tvStatus.setText("Download wird implementiert...");
            }
        });
    }
}