package xyz.baz9k.UHCGame.util;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.*;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.flattener.FlattenerListener;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import xyz.baz9k.UHCGame.UHCGamePlugin;

public final class ComponentUtils {
    private ComponentUtils() {}

    public static record Key(String key) {
        public static final Path prefix = Path.of("xyz.baz9k.uhc");

        public Key {
            Path kp = Path.of(key);
            key = kp.relativeTo(prefix)
                .orElse(kp)
                .toString();
        }

        public Key(String key, Object... args) {
            this(String.format(key, parseArgs(args)));
        }

        public Key sub(Object... args) {
            return new Key(key, args);
        }

        /**
         * @return the full translation key for this Key
         */
        public String key() {
            return prefix.append(key).toString();
        }
        
        private static Object[] parseArgs(Object... args) {
            return Arrays.stream(args)
                .map(o -> {
                    if (o instanceof Key k) return k.key;
                    return o;
                })
                .toArray();
        }

        /**
         * Shorthand for {@link Component#translatable}. 
         * Creates a translatable component that uses this key as the key and the specified objects as parameters.
         * @param args Objects passed as parameters to the translatable component. (Components stay as components)
         * @return component
         */
        public TranslatableComponent trans(Object... args) {
            Component[] cargs = Arrays.stream(args)
            .map(ComponentUtils::componentize)
            .toArray(Component[]::new);

            return Component.translatable(key()).args(cargs);
        }

        // watch this already be a thing
        // does the work for transMultiline
        private static List<Component> splitLines(Component transComp) {
            // if there's no text, just trash the component
            if (renderString(transComp).isBlank()) return List.of();

            var rendered = render(transComp);
            
            List<Component> lines = new ArrayList<>();
            Deque<Style> styles = new ArrayDeque<>();
            // trans text is already rendered, so this should work fine
            ComponentFlattener.basic().flatten(rendered, new FlattenerListener() {

                @Override
                public void component(@NotNull String text) {
                    Style cs = styles.stream().reduce(Style.empty(), Style::merge); // computed style
                    Iterator<String> it = Arrays.asList(text.split("\n")).iterator();

                    // there's no new lines between iterations
                    // so, if lines has an element, then the ends of iteration have to be joined
                    if (lines.size() > 0 && it.hasNext()) {
                        int lastIndex = lines.size() - 1;
                        Component seg = Component.text(it.next(), cs);
                        lines.set(lastIndex, lines.get(lastIndex).append(seg));
                    }
                    it.forEachRemaining(cline -> {
                        lines.add(Component.text(cline, cs));
                    });
                }

                @Override
                public void pushStyle(@NotNull Style s) { styles.addLast(s); }
                
                @Override
                public void popStyle(@NotNull Style s) { styles.removeLast(); }
                
            });

            return Collections.unmodifiableList(lines);
        }
        
        /**
         * Creates a rendered component of the key and breaks it into a list of lines. Useful for lore.
         * @param s Style of the text by default
         * @param args Objects passed as parameters to translatable component.
         * @return a list of lines
         */
        public List<Component> transMultiline(Style s, Object... args) {
            return splitLines(trans(args).style(s));
        }
        /**
         * Creates a rendered component of the key and breaks it into a list of lines. Useful for lore.
         * @param args Objects passed as parameters to translatable component.
         * @return a list of lines
         */
        public List<Component> transMultiline(Object... args) {
            return splitLines(trans(args));
        }

        /**
         * Returns an exception with a translatable component message.
         * @param args Objects which are passed as strings to the translatable component. (Components stay as components)
         * @return The exception, which can be thrown.
         */
        public <X extends Throwable> X transErr(Class<X> exc, Object... args) {
            return componentErr(exc, trans(args));
        }
    }

    /**
     * Style with color, but no formatting, no italic, bold, etc.
     * @param clr Color of style
     * @return the new {@link Style}
     */
    public static Style noDeco(TextColor clr) {
        Style.Builder st = Style.style().color(clr);

        for (TextDecoration deco : TextDecoration.values()) {
            st.decoration(deco, false);
        }

        return st.build();
    }

    /**
     * Convert a {@link TranslatableComponent} into readable text in the form of a {@link Component}.
     * @param c unrendered Component
     * @return rendered Component
     */
    public static Component render(Component c) {
        return GlobalTranslator.render(c, UHCGamePlugin.getLocale());
    }

    private static PlainTextComponentSerializer plainSerializer;

    @SuppressWarnings("deprecation") // shhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh this will be dealt with when paper and adventure get their shit together
    public static PlainTextComponentSerializer plainSerializer() {
        if (plainSerializer != null)
            return plainSerializer;
        return plainSerializer = PlainTextComponentSerializer.builder()
            .flattener(Bukkit.getUnsafe().componentFlattener())
            .build();
    }

    /**
     * Converts a {@link Component} (which may consist of components of type {@link TextComponent} 
     * and {@link TranslatableComponent}) into readable text.
     * @param c component
     * @return string of the component text
     */
    public static String renderString(Component c) {
        return plainSerializer().serialize(c);
    }

    /**
     * Returns an exception with a translatable component message
     * @param exc Exception type
     * @param msg Component message
     * @return The exception, which can be thrown.
     */
    public static <X extends Throwable> X componentErr(Class<X> exc, Component msg){
        try {
            return exc.getConstructor(String.class).newInstance(renderString(msg));
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static Component componentize(Object o) {
        if (o instanceof ComponentLike cl) return cl.asComponent();
        return Component.text(String.valueOf(o));
    }
}
