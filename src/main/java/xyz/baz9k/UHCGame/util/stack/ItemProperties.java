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
    /**
     * @return the material type of the ItemStack
     */
    @NotNull Material mat();

    /**
     * @return the style of the name
     */
    @Nullable Style nameStyle();

    /**
     * @return the style of the description
     */
    @Nullable Style descStyle();

    /**
     * @return format arguments that fill in parameters in the description
     */
    @NotNull Object[] formatArgs();

    /**
     * @return whether or not to add an enchant glint to the item
     */
    boolean customEnchGlint();

    /**
     * A method to perform miscellaneous edits to the item meta
     */
    void editMeta(ItemMeta m);

    /**
     * @return extra lore (any additional information besides the description)
     */
    default List<Component> extraLore() {
        return List.of();
    }
}
