package xyz.baz9k.UHCGame.util;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import net.kyori.adventure.text.*;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.translation.GlobalTranslator;
import xyz.baz9k.UHCGame.UHCGamePlugin;

public final class ComponentUtils {
    private ComponentUtils() {}

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
     * @param l locale to use
     * @return rendered Component
     */
    public static Component render(Component c, Locale l) {
        return GlobalTranslator.render(c, l);
    }
    /**
     * Convert a {@link TranslatableComponent} into readable text in the form of a {@link Component}.
     * @param c unrendered Component
     * @return rendered Component
     */
    public static Component render(Component c) {
        return render(c, UHCGamePlugin.getLocale());
    }

    /**
     * Converts a {@link Component} (which may consist of components of type {@link TextComponent} 
     * and {@link TranslatableComponent}) into readable text.
     * @param c component
     * @param l locale to use
     * @return string of the component text
     */
    public static String renderString(Component c, Locale l) {
        if (c instanceof TextComponent tc && c.children().size() == 0) return tc.content();

        List<Component> components = new ArrayList<>();
        components.add(c);
        components.addAll(c.children());

        return components.stream()
            .map(cpt -> {
                Component rendered = render(cpt, l);
                if (rendered instanceof TextComponent renderedText) {
                    String buf = renderedText.content();
                    for (Component child : renderedText.children()) buf += renderString(child, l);
                    return buf;
                } else if (rendered instanceof TranslatableComponent renderedTrans) {
                    String buf = renderedTrans.key();
                    for (Component child : renderedTrans.children()) buf += renderString(child, l);
                    return buf;

                }
                return rendered.toString(); // if not text, then can't really do anything
            })
            .collect(Collectors.joining());
    }
    
    /**
     * Converts a {@link Component} (which may consist of components of type {@link TextComponent} 
     * and {@link TranslatableComponent}) into readable text.
     * This uses the plugin's locale.
     * @param c component
     * @return string of the component text
     */
    public static String renderString(Component c) {
        return renderString(c, UHCGamePlugin.getLocale());
    }

    /**
     * Shorthand for {@link Component#translatable}. Creates a translatable component that uses the specified objects as parameters.
     * @param key Translation key
     * @param args Objects which are passed as parameters to the translatable component. (Components stay as components)
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
     * Returns an exception with a translatable component message
     * @param exc Exception type
     * @param msg Component message
     * @param l Locale
     * @return The exception, which can be thrown.
     */
    public static <X extends Throwable> X translatableErr(Class<X> exc, Component msg, Locale l){
        try {
            return exc.getConstructor(String.class).newInstance(renderString(msg, l));
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns an exception with a translatable component message.
     * This uses the plugin's locale.
     * @param exc Exception type
     * @param c Component message
     * @return The exception, which can be thrown.
     */
    public static <X extends Throwable> X translatableErr(Class<X> exc, Component c){
        return translatableErr(exc, c, UHCGamePlugin.getLocale());
    }

    /**
     * Returns an exception with a translatable component message.
     * This uses the plugin's locale.
     * @param key Translation key
     * @param args Objects which are passed as strings to the translatable component. (Components stay as components)
     * @return The exception, which can be thrown.
     */
    public static <X extends Throwable> X translatableErr(Class<X> exc, String key, Object... args){
        return translatableErr(exc, trans(key, args));
    }
}
