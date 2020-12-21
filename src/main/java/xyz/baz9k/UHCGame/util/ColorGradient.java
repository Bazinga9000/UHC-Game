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

    public static Color multiColorGradient(double distance, Color... color) {
        if(distance < 0) distance = 0;
        int numColors = color.length;
        int leftColorIndex = (int) Math.floor(distance * (numColors-1));
        if (leftColorIndex >= numColors - 1) {
            // distance is exactly 1
            // check for overheal here ig
            return color[numColors - 1];
        }

        double lowerBound = (double) leftColorIndex / (double) numColors;
        double upperBound = (double) (leftColorIndex + 1) / (double) numColors;

        double newDistance = (distance - lowerBound) / (upperBound - lowerBound);
        return twoColorGradient(newDistance, color[leftColorIndex], color[leftColorIndex + 1]);
    }
}
