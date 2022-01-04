package xyz.baz9k.UHCGame.util.stack;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;

/**
 * Interface defining what properties are needed for {@link TransItemStack}.
 */
public interface ItemProperties {
    @NotNull Material mat();
    @Nullable Style nameStyle();
    @Nullable Style descStyle();
    @NotNull Object[] formatArgs();
    boolean customEnchGlint();
    void editMeta(ItemMeta m);

    default List<Component> extraLore() {
        return List.of();
    }
}
