package xyz.baz9k.UHCGame.config;

import org.bukkit.inventory.ItemStack;

/**
 * Provides all the data for one option of the options selectable in an {@link OptionValuedNode}.
 */
public class OptionData {
    private String name;
    private ItemStack itemStack;
    private String value;

    public OptionData(String name, ItemStack itemStack, String value) {
        this.name = name;
        this.itemStack = itemStack;
        this.value = value;
    }

    public String getName() {
        return name;
    }
    public ItemStack getItemStack() {
        return itemStack;
    }
    public String getValue() {
        return value;
    }
}
