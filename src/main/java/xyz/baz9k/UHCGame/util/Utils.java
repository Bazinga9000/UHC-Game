package xyz.baz9k.UHCGame.util;

import java.time.Duration;

public class Utils {
    public static String getTimeString(long s) {
        if (s < 3600) return String.format("%02d:%02d", s / 60, (s % 60));
        return String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
    }

    public static String getTimeString(Duration d) {
        return getTimeString(d.toSeconds());
    }

    public static String getLongTimeString(long s) {
        return String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
    }

    public static String getLongTimeString(Duration d) {
        return getLongTimeString(d.toSeconds());
    }

    public static int mod(int x, int y) {
        return ((x % y) + y) % y;
    }

    public static double mod(double x, double y) {
        return ((x % y) + y) % y;
    }

}
