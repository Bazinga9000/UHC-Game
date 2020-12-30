package xyz.baz9k.UHCGame.config;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class Node {
    private final BranchNode parent;
    private final ItemStack itemStack;

    public Node(BranchNode parent, ItemStack itemStack) {
        this.parent = parent;
        this.itemStack = itemStack;
    }

    public BranchNode getParent() {
        return parent;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}
