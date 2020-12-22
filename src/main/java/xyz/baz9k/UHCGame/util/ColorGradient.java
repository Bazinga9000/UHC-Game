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
        int numIntervals = color.length - 1;
        double scaledDistance = distance * numIntervals;
        int scaledDistanceFloor = (int) Math.floor(scaledDistance);
        
        if (scaledDistanceFloor >= numIntervals) {
            return color[numIntervals];
        }

        double interDist = scaledDistance - scaledDistanceFloor;
        return twoColorGradient(interDist, color[scaledDistanceFloor], color[scaledDistanceFloor + 1]);
    }
}
