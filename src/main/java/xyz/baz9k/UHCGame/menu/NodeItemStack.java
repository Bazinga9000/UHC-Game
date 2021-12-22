package xyz.baz9k.UHCGame.menu;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.*;

import org.bukkit.Material;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;

import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

/**
 * {@link ItemStack} modified to be more simple for {@link Node} use.
 * 
 * The material, display name, description, and extra lore are modifiable through this class directly.
 * The lore consists of a formatted description with the extra lore.
 */
public class NodeItemStack extends ItemStack {
    /**
     * ID used for name & desc
     */
    private final String id;

    /**
     * Class that provides additional properties about the ItemStack, particularly:
     * <p> - fn to map current config value to material
     * <p> - name style
     * <p> - fn to map current config value to object that can be substituted in the main desc
     * <p> - fn to do miscellaneous meta edits (ench hide flags, ench glint)
     * <p> - fn to map current config value to extra lore
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
    
    private static final String NAME_ID_FORMAT = "xyz.baz9k.uhc.menu.inv.%s.name";
    private static final String DESC_ID_FORMAT = "xyz.baz9k.uhc.menu.inv.%s.desc";

    public static class ItemProperties {
        private Function<Object, Material> matGet = v -> Material.AIR;
        private Style nameStyle = DEFAULT_NAME_STYLE;
        private Function<Object, String> formatter = String::valueOf;
        private BiConsumer<Object, ItemMeta> miscMetaChanges = (v, m) -> {};
        private Function<Object, ExtraLore> elGet = v -> new ExtraLore();

        public ItemProperties() {}
        public ItemProperties(Function<Object, Material> mat) { mat(mat); }
        public ItemProperties(Material mat) { mat(mat); }

        public ItemProperties mat(Function<Object, Material> mat) {
            this.matGet = mat;
            return this;
        }
        public ItemProperties style(Style s) {
            this.nameStyle = s;
            return this;
        }
        public ItemProperties formatter(Function<Object, String> formatter) {
            this.formatter = formatter;
            return this;
        }
        public ItemProperties metaChanges(BiConsumer<Object, ItemMeta> mc) {
            this.miscMetaChanges = mc;
            return this;
        }
        public ItemProperties extraLore(Function<Object, ExtraLore> el) {
            this.elGet = el;
            return this;
        }
        
        public ItemProperties mat(Material mat) { return mat(v -> mat); }
        public ItemProperties style(TextColor clr) { return style(noDeco(clr)); }
        

        private Material getMat(Object o) {
            return matGet.apply(o);
        }
        private Style getStyle() {
            return nameStyle;
        }
        private String format(Object o) {
            return formatter.apply(o);
        }
        private void editMeta(Object o, ItemMeta m) {
            miscMetaChanges.accept(o, m);
        }
        private ExtraLore getExtraLore(Object o) {
            return elGet.apply(o);
        }
    }

    public static class ExtraLore { // extra lore can either be a translatable key or a list of components
        private List<Component> lore = null;

        private String tKey = null;
        private Object[] tArgs = null;

        public ExtraLore(Component... lore) { this.lore = List.of(Objects.requireNonNull(lore)); }
        public ExtraLore(List<Component> lore) { this.lore = List.copyOf(Objects.requireNonNull(lore)); }

        public ExtraLore(String key, Object... args) {
            this.tKey = Objects.requireNonNull(key);
            this.tArgs = Objects.requireNonNull(args);
        }

        public List<Component> component() {
            if (lore != null) return Collections.unmodifiableList(lore);

            Object[] args = Arrays.stream(tArgs)
                .map(o -> {
                    if (o instanceof Component c) return render(c);
                    return o;
                })
                .toArray();

            return splitLines(render(trans(tKey, args).style(DEFAULT_DESC_STYLE)));
        }
    }

    public NodeItemStack(String id, ItemProperties props) {
        super(props.getMat(Node.cfg.get(id)));

        this.id = id;
        this.props = props;

        editMeta(m -> { m.addItemFlags(ItemFlag.HIDE_ENCHANTS); });
        updateAll();

    }

    /**
     * Gets the formatted description of the item.
     * @return the description
     */
    public List<Component> desc() {
        return descFromID(id).stream()
            .map(c -> {
                if (c instanceof TextComponent tc) {
                    String content = tc.content();
                    String fmtObj = props.format(Node.cfg.get(id));
                    return tc.content(MessageFormat.format(content, fmtObj));
                } else return c;
            })
            .toList();
    }

    /**
     * Gets the extra lore of the item.
     * @return the extra lore
     */
    public List<Component> extraLore() {
        return props.getExtraLore(Node.cfg.get(id)).component();
    }

    private void updateLore() {
        List<Component> lore = new ArrayList<>(desc());
        
        var el = extraLore();
        if (el.size() > 0) {
            lore.add(Component.empty());
            lore.addAll(el);
        }

        lore(lore);
    }

    /**
     * Updates the ItemStack to be up to date with all properties.
     */
    public NodeItemStack updateAll() {
        var o = Node.cfg.get(id);

        setType(props.getMat(o));
        editMeta(m -> {
            m.displayName(nameFromID(id, props.getStyle()));
            props.editMeta(o, m);
        });
        updateLore();

        return this;
    }

    /**
     * Splits a component into a list consisting of lines
     * @param comp Component to split into
     * <p> Non-text components cannot be split, and will be returned in a list.
     * @return list
     */
    private static List<Component> splitLines(Component comp) {
        if (!(comp instanceof TextComponent text)) return List.of(comp); // "fuck it i dunno"
        List<TextComponent> components = new ArrayList<>();
        components.add(text);
        for (Component child : text.children()) {
            if (child instanceof TextComponent tc) components.add(tc);
            else components.add(Component.text(child.toString(), child.style())); // "fuck it i dunno"
        }

        List<Component> lines = new ArrayList<>();

        for (TextComponent c : components) {
            var content = new ArrayList<>(Arrays.asList(c.content().split("\n")));

            if (lines.size() > 0) {
                int i = lines.size() - 1;
                Component segment = Component.text(content.remove(0), c.style());
                lines.set(i, lines.get(i).append(segment));
            }

            for (String cline : content) {
                lines.add(Component.text(cline, c.style()));
            }
        }

        return lines;
    }

    /**
     * Gets a rendered name by translation key
     * <p> Rendered components are text components that are already translated.
     * @param id translation key
     * @return rendered Component
     */
    public static Component nameFromID(String id) {
        return nameFromID(id, DEFAULT_NAME_STYLE);
    }
    /**
     * Gets a rendered name by translation key
     * <p> Rendered components are text components that are already translated.
     * @param id translation key
     * @param s
     * @return rendered Component
     */
    public static Component nameFromID(String id, Style s) {
        String key = String.format(NAME_ID_FORMAT, id);
        return render(trans(key).style(s));
    }

    /**
     * Gets a rendered description by translation key
     * <p> Rendered components are text components that are already translated.
     * @param id translation key
     * @return lines of rendered Component
     */
    public static List<Component> descFromID(String id) {
        return descFromID(id, DEFAULT_DESC_STYLE);
    }
    /**
     * Gets a rendered description by translation key
     * <p> Rendered components are text components that are already translated.
     * @param id translation key
     * @param s
     * @return lines of rendered Component
     */
    public static List<Component> descFromID(String id, Style s) {
        String key = String.format(DESC_ID_FORMAT, id);

        Component rendered = render(trans(key).style(s));
        if (renderString(rendered).equals("")) return List.of();

        return splitLines(rendered);
    }
}
