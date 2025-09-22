package com.nickisen.ailyricsapp;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class SrtParser {

    private static final String TAG = "SrtParser";

    public static List<Subtitle> parse(Context context, Uri uri) {
        List<Subtitle> subtitles = new ArrayList<>();
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;
            while ((line = reader.readLine()) != null) {
                // Überspringe leere Zeilen
                if (line.trim().isEmpty()) {
                    continue;
                }

                // 1. Lese die Indexnummer (und ignoriere sie)
                try {
                    Integer.parseInt(line.trim());
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Erwartete Indexnummer, fand aber: " + line);
                    continue;
                }

                // 2. Lese die Zeitstempel
                String timeLine = reader.readLine();
                if (timeLine == null) break;

                String[] timeParts = timeLine.split(" --> ");
                if (timeParts.length != 2) {
                    Log.w(TAG, "Ungültiges Zeitformat: " + timeLine);
                    continue;
                }

                long startTime = parseTime(timeParts[0]);
                long endTime = parseTime(timeParts[1]);

                // 3. Lese den Text
                StringBuilder textBuilder = new StringBuilder();
                String textLine;
                while ((textLine = reader.readLine()) != null && !textLine.trim().isEmpty()) {
                    if (textBuilder.length() > 0) {
                        textBuilder.append("\n");
                    }
                    textBuilder.append(textLine);
                }

                subtitles.add(new Subtitle(startTime, endTime, textBuilder.toString()));
                Log.d(TAG, "Geparster Untertitel: " + subtitles.get(subtitles.size() - 1));
            }

        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Parsen der SRT-Datei", e);
        }
        return subtitles;
    }

    private static long parseTime(String time) {
        String[] parts = time.split("[:,]");
        long hours = Long.parseLong(parts[0].trim());
        long minutes = Long.parseLong(parts[1].trim());
        long seconds = Long.parseLong(parts[2].trim());
        long milliseconds = Long.parseLong(parts[3].trim());
        return (hours * 3600 + minutes * 60 + seconds) * 1000 + milliseconds;
    }
}