package com.nickisen.ailyricsapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Environment;
import android.util.Log;
import org.jcodec.api.android.AndroidSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;
import java.io.File;
import java.util.List;

public class VideoRenderer {

    private static final String TAG = "VideoRenderer";

    public interface RenderCallback {
        void onProgress(int progress);
        void onComplete(String filePath);
        void onError(String message);
    }

    private final List<Subtitle> subtitles;
    private final VideoStyle style;
    private final RenderCallback callback;
    private final File outputFile;

    public VideoRenderer(Context context, List<Subtitle> subtitles, VideoStyle style, RenderCallback callback) {
        this.subtitles = subtitles;
        this.style = style;
        this.callback = callback;
        File moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
        this.outputFile = new File(moviesDir, "lyric_video_" + System.currentTimeMillis() + ".mp4");
    }

    public void startRendering() {
        new Thread(this::render).start();
    }

    private void render() {
        SeekableByteChannel channel = null;
        try {
            channel = NIOUtils.writableFileChannel(outputFile.getAbsolutePath());
            AndroidSequenceEncoder encoder = new AndroidSequenceEncoder(channel, Rational.R(30, 1));

            int width = 1920;
            int height = 1080;
            long totalDurationMs = subtitles.get(subtitles.size() - 1).getEndTime() + 1000; // +1 Sekunde Puffer
            int totalFrames = (int) (totalDurationMs * 30 / 1000);

            Paint paint = setupPaint();

            for (int i = 0; i < totalFrames; i++) {
                long currentMs = i * 1000 / 30;
                Bitmap frame = createFrame(width, height, currentMs, paint);
                encoder.encodeImage(frame);
                frame.recycle(); // Wichtig: Speicher freigeben

                int progress = (int) ((i / (float) totalFrames) * 100);
                callback.onProgress(progress);
            }

            encoder.finish();
            callback.onComplete(outputFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "Video rendering failed", e);
            callback.onError("Video rendering failed: " + e.getMessage());
        } finally {
            NIOUtils.closeQuietly(channel);
        }
    }

    private Paint setupPaint() {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        try {
            paint.setColor(Color.parseColor(style.fontColor));
        } catch (IllegalArgumentException e) {
            paint.setColor(Color.WHITE);
        }
        paint.setTextSize(style.fontSize * 3); // Skalierung für HD
        paint.setTextAlign(Paint.Align.CENTER);

        Typeface typeface = Typeface.create(style.fontFamily, Typeface.BOLD);
        paint.setTypeface(typeface);
        return paint;
    }

    private Bitmap createFrame(int width, int height, long currentMs, Paint paint) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Greenscreen-Hintergrund
        canvas.drawColor(Color.parseColor("#00FF00"));

        // Finde den aktuellen Untertitel
        for (Subtitle sub : subtitles) {
            if (currentMs >= sub.getStartTime() && currentMs <= sub.getEndTime()) {
                drawTextMultiLine(canvas, paint, sub.getText(), width / 2f, height * 0.8f);
                break;
            }
        }
        return bitmap;
    }

    private void drawTextMultiLine(Canvas canvas, Paint paint, String text, float x, float y) {
        String[] lines = text.split("\n");
        Rect bounds = new Rect();
        paint.getTextBounds(lines[0], 0, lines[0].length(), bounds);
        float y_pos = y;
        for (int i = lines.length - 1; i >= 0; i--) {
            canvas.drawText(lines[i], x, y_pos, paint);
            y_pos -= bounds.height() + 10;
        }
    }
}