package xyz.baz9k.UHCGame.config;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class ConfigNode {
    private final BranchConfigNode parent;
    private final ItemStack itemStack;

    public ConfigNode(BranchConfigNode parent, ItemStack itemStack) {
        this.parent = parent;
        this.itemStack = itemStack;
    }

    public BranchConfigNode getParent() {
        return parent;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}
