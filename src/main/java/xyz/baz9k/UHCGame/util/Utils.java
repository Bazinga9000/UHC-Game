package xyz.baz9k.UHCGame.util;

import java.time.Duration;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.chat.BaseComponent;
import xyz.baz9k.UHCGame.UHCGame;

public final class Utils {
    private Utils() {}

    /**
     * Get a time string of the provided number of seconds.
     * <p>
     * Minutes and seconds are provided by default, and hours are provided if the amount of time provided exceeds an hour.
     * @param s
     * @return the time string
     * 
     * @see #getLongTimeString(long)
     */
    public static String getTimeString(long s) {
        if (s < 3600) return String.format("%02d:%02d", s / 60, (s % 60));
        return String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
    }

    /**
     * Get a time string of the provided {@link Duration}'s duration.
     * <p>
     * Minutes and seconds are provided by default, and hours are provided if the amount of time provided exceeds an hour.
     * @param d
     * @return the time string
     * 
     * @see #getLongTimeString(Duration)
     */
    public static String getTimeString(Duration d) {
        return getTimeString(d.toSeconds());
    }

    /**
     * Get a long time string of the provided number of seconds.
     * <p>
     * Hours, minutes, and seconds are all provided in the string.
     * @param s
     * @return the time string
     */
    public static String getLongTimeString(long s) {
        return String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
    }

    /**
     * Get a long time string of the provided {@link Duration}'s duration.
     * <p>
     * Hours, minutes, and seconds are all provided in the string.
     * @param d
     * @return the time string
     */
    public static String getLongTimeString(Duration d) {
        return getLongTimeString(d.toSeconds());
    }

    /**
     * Get a string of format "X hours, X minutes, and X seconds" from a number of seconds.
     * <p>
     * A clause can be comitted if there is zero of the unit of that clause.
     * @param s
     * @return the time string
     */
    public static String getWordTimeString(long s) {
        if (s == 0) return "";

        long[] segs = {s / 3600, (s % 3600) / 60, (s % 60)};
        String[] unit = {"hour", "minute", "second"};
        ArrayList<String> segStrs = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            long seg = segs[i];
            if (seg == 0) continue;
            if (seg == 1) {
                segStrs.add(String.format("%s %s", seg, unit[i]));
                continue;
            }
            segStrs.add(String.format("%s %ss", seg, unit[i]));
        }
    
        int size = segStrs.size();
        if (size == 1) return segStrs.get(0);
        return String.format("%s, and %s", String.join(", ", segStrs.subList(0, size - 1)), segStrs.get(size - 1));
    }

    /**
     * Get a string of format "X hours, X minutes, and X seconds" from the provided {@link Duration}'s duration.
     * <p>
     * A clause can be comitted if there is zero of the unit of that clause.
     * @param s
     * @return the time string
     */
    public static String getWordTimeString(Duration d) {
        return getWordTimeString(d.toSeconds());
    }

    /**
     * Take the modulo where 0 <= x < y.
     * <p>
     * Java's operation % takes the remainder and is negative if x is negative.
     * @param x
     * @param y
     * @return result of the modulo
     */
    public static int mod(int x, int y) {
        return ((x % y) + y) % y;
    }

    /**
     * Take the modulo where 0 <= x < y.
     * <p>
     * Java's operation % takes the remainder and is negative if x is negative.
     * @param x
     * @param y
     * @return result of the modulo
     */
    public static double mod(double x, double y) {
        return ((x % y) + y) % y;
    }

    /**
     * Takes the euclidean distance between points (x1, y1) and (x2, y2).
     * @param x1
     * @param x2
     * @param y1
     * @param y2
     * @return distance calculation
     */
    public static double euclideanDistance(double x1, double x2, double y1, double y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }

    /**
     * Send a message after some delay.
     * @param m
     * @param plugin
     * @param delay
     */
    public static void delayedMessage(String m, UHCGame plugin, long delay) {
        new BukkitRunnable() {
            public void run() {
                Bukkit.broadcastMessage(m);
            }
        }.runTaskLater(plugin, delay);
    }
    
    /**
     * Send a message after some delay.
     * @param m
     * @param plugin
     * @param delay
     */
    public static void delayedMessage(BaseComponent[] m, UHCGame plugin, long delay) {
        new BukkitRunnable() {
            public void run() {
                Bukkit.broadcast(m);
            }
        }.runTaskLater(plugin, delay);
    }
}
