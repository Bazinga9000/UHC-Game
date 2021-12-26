package xyz.baz9k.UHCGame.util;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TranslatableComponent;
import xyz.baz9k.UHCGame.UHCGamePlugin;

import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

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
     * Get a time string of the provided {@link Duration}'s duration.
     * <p>
     * Minutes and seconds are provided by default, and hours are provided if the amount of time provided exceeds an hour.
     * <p>
     * If d is empty, then the fallback string is used instead.
     * @param d
     * @param fallback
     * @return the time stiring
     * 
     * @see #getLongTimeString(Optional)
     */
    public static String getTimeString(Optional<Duration> d, String fallback) {
        return d.map(Utils::getTimeString)
            .orElse(fallback);
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
     * Get a long time string of the provided {@link Duration}'s duration.
     * <p>
     * Hours, minutes, and seconds are all provided in the string.
     * <p>
     * If d is empty, then the fallback string is used instead.
     * @param d
     * @param fallback
     * @return the time string
     */
    public static String getLongTimeString(Optional<Duration> d, String fallback) {
        return d.map(Utils::getLongTimeString)
            .orElse(fallback);
    }

    /**
     * Get a string of format "XhXmXs" from a number of seconds.
     * <p>
     * A clause can be omitted if there is zero of the unit of that clause.
     * @param s
     * @return the time string
     */
    public static Component getWordTime(long s) {
        if (s == 0) return Component.empty();

        TranslatableComponent[] units = {
            trans("xyz.baz9k.uhc.time.hour"),
            trans("xyz.baz9k.uhc.time.minute"),
            trans("xyz.baz9k.uhc.time.second"),
        };

        List<Long> segs = new ArrayList<>();
        for (int i = 0; i < units.length; i++) {
            segs.add(0, s % 60);
            s /= 60;
        }

        ComponentBuilder<?, ?> buf = Component.text();

        for (int i = 0; i < segs.size(); i++) {
            long seg = segs.get(i);
            TranslatableComponent unit = units[i];

            if (seg == 0) continue;
            buf.append(unit.args(Component.text(seg)));
        }
    
        return buf.build();
    }

    /**
     * Get a string of format "XhXmXs" from the provided {@link Duration}'s duration.
     * <p>
     * A clause can be omitted if there is zero of the unit of that clause.
     * @param d
     * @return the time string
     */
    public static Component getWordTime(Duration d) {
        return getWordTime(d.toSeconds());
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
     * Send a message after some delay.
     * @param m
     * @param plugin
     * @param delay
     * @return BukkitTask of the message (can be cancelled)
     */
    public static BukkitTask delayedMessage(ComponentLike m, UHCGamePlugin plugin, long delay) {
        return Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.getServer().sendMessage(m);
        }, delay);
    }

    /**
     * Get highest location that a player can be tp'd to and be standing.
     * @param w
     * @param X
     * @param Z
     * @return loc
     */
    public static Location getHighestLoc(World w, double x, double z) {
        return new Location(w, x, 0, z).toHighestLocation().add(0, 1, 0);
    }

    /**
     * Get highest location that a player can be tp'd to and be standing.
     * @param w
     * @param p
     * @return loc
     */
    public static Location getHighestLoc(World w, Point2D p) {
        return getHighestLoc(w, p.x(), p.z());
    }

    public static double rand(double min, double max) {
        return min + ((max - min) * Math.random());
    }

    /**
     * Clamps x to the bound min and max. If it exceeds min or max, the respective value is returned.
     * @param min
     * @param x
     * @param max
     * @return
     */
    public static int clamp(int min, int x, int max) {
        return Math.max(min, Math.min(x, max));
    }

    /**
     * Clamps x to the bound min and max. If it exceeds min or max, the respective value is returned.
     * @param min
     * @param x
     * @param max
     * @return
     */
    public static double clamp(double min, double x, double max) {
        return Math.max(min, Math.min(x, max));
    }
}
