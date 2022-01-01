package xyz.baz9k.UHCGame.menu;

import java.text.MessageFormat;
import java.util.*;
import java.util.function.*;

import org.bukkit.Material;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.*;
import net.kyori.adventure.text.format.*;

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
    private final String langKey;

    /**
     * Properties to generate the item stack
     */
    private final ItemProperties<?> props;

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
     * Class provides additional properties about the ItemStack.<p>
     * This class stores functions to map "an object" to the specified property.
     * If this ItemStack is for a {@link ValuedNode}, the used object is the config value for the node.
     * Otherwise, there is no object by default (and all the functions can be treated as suppliers).
     * An object can be defined via the {@link ItemProperties#useObject} method.
     * <p>
     * The properties this class defines:
     * <p> - Material
     * <p> - Name style
     * <p> - Format arguments for the description
     * <p> - Function to perform miscellaneous ItemMeta edits (ench hide flags, ench glint)
     * <p> - extra lore, which provides information other than the description of the node
     * @param <T> type of object passed through each function
     */
    public static class ItemProperties<T> {
        private Supplier<T> propsObjSupplier = () -> null;
        private Function<T, Material> matGet = v -> Material.AIR;
        private Function<T, Style> nameStyle = v -> DEFAULT_NAME_STYLE;
        private Function<T, Object[]> formatArgs = v -> new Object[]{v};
        private BiConsumer<T, ItemMeta> miscMetaChanges = (v, m) -> {};
        private Function<T, ExtraLore> elGet = v -> new ExtraLore();

        public ItemProperties() {}
        public ItemProperties(Function<T, Material> mat) { mat(mat); }
        public ItemProperties(Material mat) { mat(mat); }

        public ItemProperties<T> useObject(Supplier<T> uo) {
            this.propsObjSupplier = uo;
            return this;
        }
        public ItemProperties<T> mat(Function<T, Material> mat) {
            this.matGet = mat;
            return this;
        }
        public ItemProperties<T> style(Function<T, Style> style) {
            this.nameStyle = style;
            return this;
        }
        public ItemProperties<T> formatArgs(Function<T, Object[]> formatArgs) {
            this.formatArgs = formatArgs;
            return this;
        }
        public ItemProperties<T> metaChanges(BiConsumer<T, ItemMeta> mc) {
            this.miscMetaChanges = mc;
            return this;
        }
        public ItemProperties<T> extraLore(Function<T, ExtraLore> el) {
            this.elGet = el;
            return this;
        }
        
        public ItemProperties<T> mat(Material mat) { return mat(v -> mat); }
        public ItemProperties<T> style(Style s) { return style(v -> s); }
        public ItemProperties<T> style(TextColor clr) { return style(noDeco(clr)); }
        public ItemProperties<T> formatArg(Function<T, Object> formatArg) { return formatArgs(formatArg.andThen(o -> new Object[]{o})); }

        private Material getMat() {
            return matGet.apply(propsObjSupplier.get());
        }
        private Style getStyle() {
            return nameStyle.apply(propsObjSupplier.get());
        }
        private Object[] getFormatArgs() {
            return formatArgs.apply(propsObjSupplier.get());
        }
        private void editMeta(ItemMeta m) {
            miscMetaChanges.accept(propsObjSupplier.get(), m);
        }
        private ExtraLore getExtraLore() {
            return elGet.apply(propsObjSupplier.get());
        }
    }

    /**
     * Class that encapsulates extra lore. The extra lore can either be a component/list of lines or a translation key.
     */
    public static class ExtraLore { // extra lore can either be a translatable key or a list of components
        private List<Component> lore = null;

        private Key tKey = null;
        private Object[] tArgs = null;

        public ExtraLore(Component... lore) { this.lore = List.of(Objects.requireNonNull(lore)); }
        public ExtraLore(List<Component> lore) { this.lore = List.copyOf(Objects.requireNonNull(lore)); }

        public ExtraLore(Key key, Object... args) {
            this.tKey = Objects.requireNonNull(key);
            this.tArgs = Objects.requireNonNull(args);
        }

        /**
         * @return the extra lore in component form
         */
        public List<Component> component() {
            if (lore != null) {
                return List.copyOf(lore)
                    .stream()
                    .map(c -> c.hasStyling() ? c : c.style(DEFAULT_DESC_STYLE))
                    .toList();
            }

            Object[] args = Arrays.stream(tArgs)
                .map(o -> {
                    if (o instanceof Component c) return render(c);
                    return o;
                })
                .toArray();

            List<Component> lines = splitLines(render(tKey.trans(args)));
            lines.replaceAll(l -> l.style(DEFAULT_DESC_STYLE));
            return lines;
        }

        /**
         * @return an extra lore that is either ACTIVE/INACTIVE based on if the provided object is true or false
         */
        public static <T> Function<T, ExtraLore> fromBool() {
            return o -> {
                var active = (boolean) o;

                // keeps the description untouched, adds Status: ACTIVE/INACTIVE below it
                TranslatableComponent status;
                if (active) {
                    status = new Key("menu.bool_valued.on").trans().style(noDeco(NamedTextColor.GREEN));
                } else {
                    status = new Key("menu.bool_valued.off").trans().style(noDeco(NamedTextColor.RED));
                }
                return new NodeItemStack.ExtraLore(new Key("menu.bool_valued.status"), status);
            };
        }
    }

    /**
     * @param langKey the node's lang key
     * @param props the node's item properties
     */
    public NodeItemStack(String langKey, ItemProperties<?> props) {
        super(props.getMat());

        this.langKey = langKey;
        this.props = props;

        editMeta(m -> m.addItemFlags(ItemFlag.HIDE_ENCHANTS));
        updateAll();

    }

    /**
     * Gets the formatted description of the item.
     * @return the description
     */
    public List<Component> desc() {
        return descFromID(langKey).stream()
            .map(c -> {
                if (c instanceof TextComponent tc) {
                    String content = tc.content();
                    Object[] fmtArgs = props.getFormatArgs();
                    return tc.content(MessageFormat.format(content, fmtArgs));
                } else return c;
            })
            .toList();
    }

    /**
     * Gets the extra lore of the item.
     * @return the extra lore
     */
    public List<Component> extraLore() {
        return props.getExtraLore().component();
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
     * Updates the ItemStack to be up-to-date with all properties.
     */
    public NodeItemStack updateAll() {
        setType(props.getMat());
        editMeta(m -> {
            m.displayName(nameFromID(langKey, props.getStyle()));
            props.editMeta(m);
        });
        updateLore();

        return this;
    }

    /**
     * Splits a component into a list consisting of lines
     * @param comp Component to split into
     * @apiNote Don't put non-text/-translatable components in.
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

    private static final Key NAME_KEY_FORMAT = new Key("menu.inv.%s.name");
    private static final Key DESC_KEY_FORMAT = new Key("menu.inv.%s.desc");

    /**
     * Gets rendered name by translating the lang key<p>
     * Rendered components are text components that are already translated.
     * @param langKey lang key
     * @return rendered {@link Component}
     */
    public static Component nameFromID(String langKey) {
        return nameFromID(langKey, DEFAULT_NAME_STYLE);
    }

    /**
     * Gets rendered name by translating the lang key<p>
     * Rendered components are text components that are already translated.
     * @param langKey lang key
     * @param s Style of component
     * @return rendered {@link Component}
     */
    public static Component nameFromID(String langKey, Style s) {
        Key fullLangKey = NAME_KEY_FORMAT.args(langKey);
        return render(fullLangKey.trans().style(s));
    }

    /**
     * Gets a rendered description by translating the lang key<p>
     * Rendered components are text components that are already translated.
     * @param langKey lang key
     * @return lines of rendered Component
     */
    public static List<Component> descFromID(String langKey) {
        return descFromID(langKey, DEFAULT_DESC_STYLE);
    }
    /**
     * Gets a rendered description by translating the lang key<p>
     * Rendered components are text components that are already translated.
     * @param langKey lang key
     * @param s Style of component
     * @return lines of rendered Component
     */
    public static List<Component> descFromID(String langKey, Style s) {
        Key fullLangKey = DESC_KEY_FORMAT.args(langKey);

        Component rendered = render(fullLangKey.trans().style(s));
        if (renderString(rendered).equals("")) return List.of();

        return splitLines(rendered);
    }
}
