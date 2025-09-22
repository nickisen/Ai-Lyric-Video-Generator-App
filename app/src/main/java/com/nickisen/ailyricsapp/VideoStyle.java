package com.nickisen.ailyricsapp;

public class VideoStyle {
    String fontColor;
    int fontSize;
    String fontFamily;
    String animationIn;
    String animationOut;

    // Standardwerte für den Fall, dass die KI ungültige Daten liefert
    public VideoStyle() {
        this.fontColor = "#FFFFFF";
        this.fontSize = 28;
        this.fontFamily = "Roboto";
        this.animationIn = "fadeIn";
        this.animationOut = "fadeOut";
    }

    @Override
    public String toString() {
        return "VideoStyle{" +
                "fontColor='" + fontColor + '\'' +
                ", fontSize=" + fontSize +
                ", fontFamily='" + fontFamily + '\'' +
                ", animationIn='" + animationIn + '\'' +
                ", animationOut='" + animationOut + '\'' +
                '}';
    }
}