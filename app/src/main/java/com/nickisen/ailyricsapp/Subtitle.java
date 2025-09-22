package com.nickisen.ailyricsapp;

public class Subtitle {
    private final long startTime;
    private final long endTime;
    private final String text;

    public Subtitle(long startTime, long endTime, String text) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.text = text;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "Subtitle{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                ", text='" + text + '\'' +
                '}';
    }
}