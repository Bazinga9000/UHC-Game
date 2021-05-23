package xyz.baz9k.UHCGame.util;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.translation.GlobalTranslator;
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
     * Takes the euclidean distance between points (x1, y1) and (x2, y2).
     * @param x1
     * @param x2
     * @param y1
     * @param y2
     * @return distance calculation
     * @see Point2D#dist(Point2D)
     */
    public static double euclideanDistance(double x1, double x2, double y1, double y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }

    /**
     * Send a message after some delay.
     * @param m
     * @param plugin
     * @param delay
     * @return BukkitTask of the message (can be cancelled)
     */
    public static BukkitTask delayedMessage(ComponentLike m, UHCGame plugin, long delay) {
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

    //random location generation
    public static boolean isLocationSpawnable(Location l) {
        return (!isLocationOverLava(l) && !isLocationOverWater(l));
    }

    public static boolean isLocationOverLava(Location l) {
        Location blockLocation = l.add(0,-1,0);
        return (blockLocation.getBlock().getType() == Material.LAVA);
    }

    public static boolean isLocationOverWater(Location l) {
        Location blockLocation = l.add(0,-1,0);
        return (blockLocation.getBlock().getType() == Material.WATER);
    }

    /**
     * Takes two iterables, apply a function with the iterator elements as arguments.
     * @param <T>
     * @param <U>
     * @param i1
     * @param i2
     * @param fn
     */
    public static <T, U> void zip(Iterable<T> i1, Iterable<U> i2, BiConsumer<? super T, ? super U> fn) {
        Iterator<T> it1 = i1.iterator();
        Iterator<U> it2 = i2.iterator();

        while (it1.hasNext() && it2.hasNext()) fn.accept(it1.next(), it2.next());
    }

    /**
     * Takes two iterables, apply a function with iterator elements as arguments.
     * @param <T>
     * @param <U>
     * @param <R>
     * @param i1
     * @param i2
     * @param fn
     * @return a collection with the results of fn as elements
     */
    public static <T, U, R> Collection<R> zip(Iterable<T> i1, Iterable<U> i2, BiFunction<? super T, ? super U, ? extends R> fn) {
        Collection<R> c = new ArrayList<>();
        Iterator<T> it1 = i1.iterator();
        Iterator<U> it2 = i2.iterator();

        while (it1.hasNext() && it2.hasNext()) {
            c.add(fn.apply(it1.next(), it2.next()));
        }

        return Collections.unmodifiableCollection(c);
    }

    /**
     * Style with color, but no formatting, no italic, bold, etc.
     * @param clr
     * @return
     */
    public static Style noDeco(TextColor clr) {
        Style.Builder st = Style.style().color(clr);

        for (TextDecoration deco : TextDecoration.values()) {
            st.decoration(deco, false);
        }

        return st.build();
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

    /**
     * Converts a component to text (for TextComponent & TranslatableComponent)
     * @param l locale to use
     * @param c component
     * @return string of the component text
     */
    public static String componentString(Locale l, Component c) {
        if (c instanceof TextComponent tc && c.children().size() == 0) return tc.content();

        List<Component> components = new ArrayList<>();
        components.add(c);
        components.addAll(c.children());

        return components.stream()
            .map(cpt -> {
                Component rendered = GlobalTranslator.render(cpt, l);
                if (rendered instanceof TextComponent renderedText) {
                    String buf = "";
                    buf += renderedText.content();
                    for (Component child : renderedText.children()) buf += componentString(l, child);
                    return buf;
                };
                return rendered.toString(); // if not text, then can't really do anything
            })
            .collect(Collectors.joining());
    }
    
    /**
     * Converts a component to text (for TextComponent & TranslatableComponent). 
     * This uses the plugin's locale.
     * @param c component
     * @return string of the component text
     */
    public static String componentString(Component c) {
        return componentString(UHCGame.getLocale(), c);
    }

    /**
     * Returns an exception of type X, with a translatable component message
     * @param <X> 
     * @param exc Exception type
     * @param l Locale
     * @param msg Component message
     * @return The exception, which can be thrown.
     */
    public static <X extends Throwable> X translatableErr(Class<X> exc, Locale l, Component msg) {
        try {
            return exc.getConstructor(String.class).newInstance(componentString(l, msg));
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Returns an exception of type X, with a translatable component message.
     * This uses the plugin's locale.
     * @param <X> 
     * @param exc Exception type
     * @param msg Component message
     * @return The exception, which can be thrown.
     */
    public static <X extends Throwable> X translatableErr(Class<X> exc, Component c) {
        return translatableErr(exc, UHCGame.getLocale(), c);
    }

    /**
     * Shorthand for {@link Component#translatable}. 
     * @param key Translation key
     * @param args Objects which are passed as strings to the translatable component. (Components stay as components)
     * @return component
     */
    public static TranslatableComponent trans(String key, Object... args) {
        Component[] cargs = Arrays.stream(args)
            .map(o -> {
                if (o instanceof ComponentLike cl) return cl.asComponent();
                return Component.text(String.valueOf(o));
            })
            .toArray(Component[]::new);
        return Component.translatable(key).args(cargs);
    }

    /**
     * Returns an exception of type X, with a translatable component message.
     * This uses the plugin's locale.
     * @param <X> 
     * @param key Translation key
     * @param args Objects which are passed as strings to the translatable component. (Components stay as components)
     * @return The exception, which can be thrown.
     */
    public static <X extends Throwable> X translatableErr(Class<X> exc, String key, Object... args) {
        return translatableErr(exc, trans(key, args));
    }
}
