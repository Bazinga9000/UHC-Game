package xyz.baz9k.UHCGame.util;

import java.awt.*;

public class ColorGradient {
    public static Color twoColorGradient(double distance, Color a, Color b) {
        int deltaRed = b.getRed() - a.getRed();
        int deltaGreen = b.getGreen() - a.getGreen();
        int deltaBlue = b.getBlue() - a.getBlue();

        int newRed = a.getRed() + (int) Math.round(distance * (double) deltaRed);
        int newGreen = a.getGreen() + (int) Math.round(distance * (double) deltaGreen);
        int newBlue = a.getBlue() + (int) Math.round(distance * (double) deltaBlue);
        return new Color(newRed, newGreen, newBlue);
    }
}
