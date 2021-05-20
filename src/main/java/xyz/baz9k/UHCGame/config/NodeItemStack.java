package xyz.baz9k.UHCGame.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;

import static xyz.baz9k.UHCGame.util.Utils.*;

/**
 * {@link ItemStack} modified to be more simple for {@link Node} use.
 * 
 * The material, display name, description, and extra lore are modifiable through this class directly.
 * The lore consists of a formatted description with the extra lore.
 */
public class NodeItemStack extends ItemStack {
    /**
     * Raw description without replaced strings.
     */
    private List<Component> rawDesc;
    /**
     * Description after replaced strings.
     */
    private List<Component> displayDesc;

    /**
     * Additional lore below the description that is not part of the description.
     */
    private List<Component> extraLore = List.of();

    /**
     * Function that maps the value to another value, which is used in formatting strings.
     */
    private UnaryOperator<Object> mapper;

    // TEXT STYLES
    /**
     * This text style (color & formatting) will be used in the description by default
     */
    public static final Style DEFAULT_NAME_STYLE = noDeco(null);
    /**
     * This text style (color & formatting) will be used in the description by default
     */
    public static final Style DEFAULT_DESC_STYLE = noDeco(NamedTextColor.GRAY);

    /**
     * @param mat Material of item
     * @param name Name of item
     * @param mapper Function that maps the value to another value, which is used in formatting strings.
     * @param desc Description of item
     */
    public NodeItemStack(Material mat, Component name, UnaryOperator<Object> mapper, Component... desc) {
        super(mat);
        this.rawDesc = List.of(desc);
        this.displayDesc = new ArrayList<>(this.rawDesc);
        this.mapper = mapper;

        updateLore();

        ItemMeta m = getItemMeta();
        m.displayName(name);
        m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        setItemMeta(m);
        
    }

    /**
     * @param mat Material of item
     * @param name Name of item
     * @param mapper Function that maps the value to another value, which is used in formatting strings.
     * @param desc Description of item
     */
    public NodeItemStack(Material mat, String name, UnaryOperator<Object> mapper, String... desc) {
        this(mat, Component.text(name, DEFAULT_NAME_STYLE), mapper, 
            Arrays.stream(desc).map(l -> Component.text(l, DEFAULT_DESC_STYLE)).toArray(Component[]::new)
        );
    }

    /**
     * @param mat Material of item
     * @param name Name of item
     * @param desc Description of item
     */
    public NodeItemStack(Material mat, Component name, Component... desc) {
        this(mat, name, UnaryOperator.identity(), desc);
    }

    /**
     * @param mat Material of item
     * @param name Name of item
     * @param desc Description of item
     */
    public NodeItemStack(Material mat, String name, String... desc) {
        this(mat, name, UnaryOperator.identity(), desc);
    }

    /**
     * Internal method to update the lore to the right information.
     */
    private void updateLore() {
        List<Component> lore = new ArrayList<>(displayDesc);
        
        if (extraLore == null) extraLore = List.of();
        if (extraLore.size() > 0) {
            lore.add(Component.empty());
            lore.addAll(extraLore);
        }

        lore(lore);
    }

    /**
     * Gets the formatted description of the item.
     * @return the description
     */
    public List<Component> desc() {
        return Collections.unmodifiableList(displayDesc);
    }
    /**
     * Sets the object for the formatted description of the item. This changes the lore.
     * @param o Object to use for formatting the description
     */
    public void desc(Object o) {
        Object p = mapper.apply(o);

        displayDesc = this.rawDesc.stream()
            .map(c -> {
                if (c instanceof TextComponent tc) {
                    String content = tc.content();
                    return tc.content(String.format(content, p));
                } else return c;
            })
            .collect(Collectors.toList());

        updateLore();
    }

    /**
     * Gets the extra lore of the item.
     * @return the extra lore
     */
    public List<Component> extraLore() {
        return Collections.unmodifiableList(extraLore);
    }

    /**
     * Sets the extra lore of the item
     * @param lore the extra lore
     */
    public void extraLore(Component lore) {
        extraLore = List.of(lore);
        updateLore();
    }
    /**
     * Sets the extra lore of the item
     * @param lore the extra lore
     */
    public void extraLore(List<Component> lore) {
        extraLore = List.copyOf(lore);
        updateLore();
    }

    public void updateMeta(Consumer<ItemMeta> mf) {
        ItemMeta m = getItemMeta();
        mf.accept(m);
        setItemMeta(m);
    }
}
