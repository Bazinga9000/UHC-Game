package xyz.baz9k.UHCGame.util;

import static xyz.baz9k.UHCGame.util.Utils.clamp;

import java.awt.Color;

public final class ColorGradient {
    private ColorGradient() {}

    public static Color twoColorGradient(double distance, Color a, Color b) {
        distance = clamp(0, distance, 1);

        float[] aComp = a.getRGBColorComponents(null);
        float[] bComp = b.getRGBColorComponents(null);
        float deltaRed = bComp[0] - aComp[0];
        float deltaGreen = bComp[1] - aComp[1];
        float deltaBlue = bComp[2] - aComp[2];

        float newRed   = aComp[0] + (float) distance * deltaRed;
        float newGreen = aComp[1] + (float) distance * deltaGreen;
        float newBlue  = aComp[2] + (float) distance * deltaBlue;
        return new Color(newRed, newGreen, newBlue);
    }

    public static Color multiColorGradient(double distance, Color... color) {
        distance = clamp(0, distance, 1);
        int nIntervals = color.length - 1;

        float scaledDistance = (float) distance * nIntervals;
        int scaledDistanceFloor = (int) scaledDistance;
        
        if (scaledDistanceFloor >= nIntervals) {
            return color[nIntervals];
        }

        float interDist = scaledDistance - scaledDistanceFloor;
        return twoColorGradient(interDist, color[scaledDistanceFloor], color[scaledDistanceFloor + 1]);
    }
}
