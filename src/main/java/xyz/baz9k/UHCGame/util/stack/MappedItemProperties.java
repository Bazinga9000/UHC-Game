package xyz.baz9k.UHCGame.util.stack;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;

import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

public class MappedItemProperties<T> implements ItemProperties {
    private static final Style DEFAULT_NAME_STYLE = TransItemStack.DEFAULT_NAME_STYLE;
    private static final Style DEFAULT_DESC_STYLE = TransItemStack.DEFAULT_DESC_STYLE;

    private Supplier<T> propertyObject = () -> null;
    private Function<T, Material> matGet = v -> Material.AIR;
    private Function<T, Style> nameStyle = v -> DEFAULT_NAME_STYLE;
    private Function<T, Style> descStyle = v -> DEFAULT_DESC_STYLE;
    private Function<T, Object[]> formatArgs = v -> new Object[]{v};
    private Predicate<T> enchGlint = v -> false;
    private BiConsumer<T, ItemMeta> miscMetaChanges = (v, m) -> {};
    private Function<T, ExtraLore> elGet = v -> new ExtraLore();

    public MappedItemProperties() {}
    public MappedItemProperties(Function<T, Material> mat) { mat(mat); }
    public MappedItemProperties(Material mat) { mat(mat); }

    public MappedItemProperties<T> useObject(Supplier<T> uo) {
        this.propertyObject = uo;
        return this;
    }
    public MappedItemProperties<T> mat(Function<T, Material> mat) {
        this.matGet = mat;
        return this;
    }
    public MappedItemProperties<T> nameStyle(Function<T, Style> style) {
        this.nameStyle = style;
        return this;
    }
    public MappedItemProperties<T> descStyle(Function<T, Style> style) {
        this.descStyle = style;
        return this;
    }
    public MappedItemProperties<T> formatArgs(Function<T, Object[]> formatArgs) {
        this.formatArgs = formatArgs;
        return this;
    }
    public MappedItemProperties<T> enchGlint(Predicate<T> eg) {
        this.enchGlint = eg;
        return this;
    }
    public MappedItemProperties<T> metaChanges(BiConsumer<T, ItemMeta> mc) {
        this.miscMetaChanges = mc;
        return this;
    }
    public MappedItemProperties<T> extraLore(Function<T, ExtraLore> el) {
        this.elGet = el;
        return this;
    }
    
    public MappedItemProperties<T> mat(Material mat) { return mat(v -> mat); }
    public MappedItemProperties<T> nameStyle(Style s) { return nameStyle(v -> s); }
    public MappedItemProperties<T> nameStyle(TextColor clr) { return nameStyle(noDeco(clr)); }
    public MappedItemProperties<T> nameStyle(int clr) { return nameStyle(TextColor.color(clr)); }
    public MappedItemProperties<T> descStyle(Style s) { return descStyle(v -> s); }
    public MappedItemProperties<T> descStyle(TextColor clr) { return descStyle(noDeco(clr)); }
    public MappedItemProperties<T> descStyle(int clr) { return descStyle(TextColor.color(clr)); }
    public MappedItemProperties<T> formatArg(Function<T, Object> formatArg) { return formatArgs(formatArg.andThen(o -> new Object[]{o})); }

    private T propertyObject() {
        return propertyObject.get();
    }

    // querying
    @Override
    public Material mat() {
        return matGet.apply(propertyObject());
    }
    @Override
    public Style nameStyle() {
        return nameStyle.apply(propertyObject());
    }
    @Override
    public Style descStyle() {
        return descStyle.apply(propertyObject());
    }
    @Override
    public Object[] formatArgs() {
        return formatArgs.apply(propertyObject());
    }
    @Override
    public boolean customEnchGlint() {
        return enchGlint.test(propertyObject());
    }
    @Override
    public void editMeta(ItemMeta m) {
        miscMetaChanges.accept(propertyObject(), m);
    }
    @Override
    public List<Component> extraLore() {
        return elGet.apply(propertyObject()).component();
    }

    public Object[] format(T a) { return formatArgs.apply(a); }

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
            
            return tKey.transMultiline(DEFAULT_DESC_STYLE, tArgs);
        }

        /**
         * @return an extra lore that is either ACTIVE/INACTIVE based on if the provided object is true or false
         */
        public static <T> Function<T, ExtraLore> fromBool() {
            return o -> {
                var active = (boolean) o;

                // keeps the description untouched, adds Status: ACTIVE/INACTIVE below it
                Component status;
                if (active) {
                    status = new Key("menu.bool_valued.on").trans().style(noDeco(NamedTextColor.GREEN));
                } else {
                    status = new Key("menu.bool_valued.off").trans().style(noDeco(NamedTextColor.RED));
                }
                return new ExtraLore(new Key("menu.bool_valued.status"), status);
            };
        }
    }
}
