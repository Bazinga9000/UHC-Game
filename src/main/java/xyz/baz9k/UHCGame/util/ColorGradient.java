package xyz.baz9k.UHCGame.util;

import java.awt.*;

public class ColorGradient {
    public static Color twoColorGradient(double distance, Color a, Color b) {
        int minRed = Math.min(a.getRed(), b.getRed());
        int maxRed = Math.max(a.getRed(), b.getRed());
        int deltaRed = maxRed - minRed;
        int minGreen = Math.min(a.getGreen(), b.getGreen());
        int maxGreen = Math.max(a.getGreen(), b.getGreen());
        int deltaGreen = maxGreen - minGreen;
        int minBlue = Math.min(a.getBlue(), b.getBlue());
        int maxBlue = Math.max(a.getBlue(), b.getBlue());
        int deltaBlue = maxBlue - minBlue;

        int newRed = minRed + (int) Math.round(distance * (double) deltaRed);
        int newGreen = minGreen + (int) Math.round(distance * (double) deltaGreen);
        int newBlue = minBlue + (int) Math.round(distance * (double) deltaBlue);
        return new Color(newRed, newGreen, newBlue);
    }
}
