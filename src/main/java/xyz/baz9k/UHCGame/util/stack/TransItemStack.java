package xyz.baz9k.UHCGame.util.stack;

import java.util.*;

import org.bukkit.inventory.*;

import net.kyori.adventure.text.*;
import net.kyori.adventure.text.format.*;
import xyz.baz9k.UHCGame.util.Ench;

import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

/**
 * {@link ItemStack} modified to use a translation key for its name and description, 
 * as well as handling other utilities for this plugin (f.e. ench glint).
 */
public class TransItemStack extends ItemStack {
    /**
     * ID used for name & desc
     */
    private final String langKey;

    /**
     * Properties to generate the item stack
     */
    private final ItemProperties props;

    // TEXT STYLES
    /**
     * This text style (color & formatting) will be used in the name by default
     */
    public static final Style DEFAULT_NAME_STYLE = noDeco(null);
    /**
     * This text style (color & formatting) will be used in the description by default
     */
    public static final Style DEFAULT_DESC_STYLE = noDeco(NamedTextColor.GRAY);
    //

    /**
     * @param langKey the node's lang key
     * @param props the node's item properties
     */
    public TransItemStack(String langKey, ItemProperties props) {
        super(props.mat());

        this.langKey = langKey;
        this.props = props;

        refresh();
    }

    /**
     * Gets the formatted description of the item.
     * @return the description
     */
    public Component transName() {
        Style s = Optional.ofNullable(props.nameStyle()).orElse(DEFAULT_NAME_STYLE);
        return nameFromID(langKey, s);
    }

    /**
     * Gets the formatted description of the item.
     * @return the description
     */
    public List<Component> transLore() {
        // desc + extra lore
        Object[] fmtArgs = props.formatArgs();
        Style s = Optional.ofNullable(props.descStyle()).orElse(DEFAULT_DESC_STYLE);
        
        List<Component> lines = new ArrayList<>(descFromID(langKey, s, fmtArgs));
        List<Component> el = props.extraLore();

        if (el.size() > 0) {
            if (lines.size() > 0) lines.add(Component.empty());
            lines.addAll(el);
        }

        return lines;
    }

    /**
     * Updates the ItemStack to be up-to-date with all properties.
     */
    public TransItemStack refresh() {
        setType(props.mat());
        editMeta(m -> {
            m.displayName(transName());

            if (props.customEnchGlint()) {
                m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                m.addEnchant(Ench.SILK_TOUCH, 1, true);
            }

            props.editMeta(m);
        });
        lore(transLore());

        return this;
    }

    private static final Key NAME_KEY_FORMAT = new Key("%s.name");
    private static final Key DESC_KEY_FORMAT = new Key("%s.desc");

    /**
     * Gets rendered name by translating the lang key<p>
     * Rendered components are text components that are already translated.
     * @param langKey lang key
     * @return rendered {@link Component}
     */
    public static Component nameFromID(String langKey, Object... args) {
        return nameFromID(langKey, DEFAULT_NAME_STYLE, args);
    }

    /**
     * Gets rendered name by translating the lang key<p>
     * Rendered components are text components that are already translated.
     * @param langKey lang key
     * @param s Style of component
     * @return rendered {@link Component}
     */
    public static Component nameFromID(String langKey, Style s, Object... args) {
        Key fullLangKey = NAME_KEY_FORMAT.sub(langKey);
        return render(fullLangKey.trans(args).style(s));
    }

    /**
     * Gets a rendered description by translating the lang key<p>
     * Rendered components are text components that are already translated.
     * @param langKey lang key
     * @return lines of rendered Component
     */
    public static List<Component> descFromID(String langKey, Object... args) {
        return descFromID(langKey, DEFAULT_DESC_STYLE, args);
    }
    /**
     * Gets a rendered description by translating the lang key<p>
     * Rendered components are text components that are already translated.
     * @param langKey lang key
     * @param s Style of component
     * @return lines of rendered Component
     */
    public static List<Component> descFromID(String langKey, Style s, Object... args) {
        Key fullLangKey = DESC_KEY_FORMAT.sub(langKey);
        return fullLangKey.transMultiline(s, args);
    }
}
