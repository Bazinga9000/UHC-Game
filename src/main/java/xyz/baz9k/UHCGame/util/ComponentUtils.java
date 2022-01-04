package xyz.baz9k.UHCGame.util;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;

import net.kyori.adventure.text.*;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import xyz.baz9k.UHCGame.UHCGamePlugin;

public final class ComponentUtils {
    private ComponentUtils() {}

    public static record Key(String key) {
        public static final String PREFIX = "xyz.baz9k.uhc";

        public Key {
            if (!key.startsWith(PREFIX)) key = PREFIX + "." + key;
        }

        public Key(String key, Object... args) {
            this(String.format(key, args));
        }

        public Key args(Object... args) {
            return new Key(key(), args);
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
        private List<Component> splitLines(Component transComp) {
            // if there's no text, just trash the component
            if (renderString(transComp).isBlank()) return List.of();

            var rendered = render(transComp);
            Style topStyle = rendered.style();

            if (rendered instanceof TextComponent tc) {
                // create list of components in a sequential order
                List<TextComponent> components = new ArrayList<>();
                components.add(tc);
    
                for (Component child : tc.children()) {
                    if (child instanceof TextComponent tch) {
                        components.add(tch);
                    } else {
                        Style s = topStyle.merge(child.style());
                        components.add(Component.text(renderString(child), s));
                    }
                }

                // create the lines
                List<Component> lines = new ArrayList<>();
                for (TextComponent c : components) {
                    List<String> content = new ArrayList<>(Arrays.asList(c.content().split("\n")));
                    Iterator<String> it = content.iterator();
                    Style s = topStyle.merge(c.style());

                    // there's no new lines between iterations
                    // so, if lines has an element, then the connectors have to be joined
                    if (lines.size() > 0 && it.hasNext()) {
                        int lastIndex = lines.size() - 1;
                        Component seg = Component.text(it.next(), s);
                        lines.set(lastIndex, lines.get(lastIndex).append(seg));
                    }
                    
                    it.forEachRemaining(cline -> {
                        lines.add(Component.text(cline, s));
                    });
                }

                return Collections.unmodifiableList(lines);
            } else {
                // something went wrong?
                // c should be text component but it's not so, best we can do
                return List.of(rendered);
            }
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
