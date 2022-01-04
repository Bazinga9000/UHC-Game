package xyz.baz9k.UHCGame.util.stack;

import java.util.function.Consumer;

import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.format.Style;

/**
 * Record implementing {@link ItemProperties}, assuming the properties are static and unchanging.
 */
public record StaticItemProperties(
    Material mat, 
    Style nameStyle, 
    Style descStyle, 
    Object[] formatArgs, 
    boolean customEnchGlint,
    Consumer<? super ItemMeta> metaEditor) implements ItemProperties {
    
    public StaticItemProperties {
        if (formatArgs == null) formatArgs = new Object[0];
    }

    public StaticItemProperties(Material mat) { this(mat, null, null, null, false, m -> {}); }
    public StaticItemProperties(Material mat, Style nameStyle) { this(mat, nameStyle, null, null, false, m -> {}); }

    public void editMeta(ItemMeta m) {
        metaEditor.accept(m);
    }
}
