package xyz.baz9k.UHCGame.config;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public abstract class Node {
    private BranchNode parent;
    private ItemStack itemStack;

    public Node(BranchNode parent, int parentSlot, ItemStack item) {
        this.parent = parent;
        this.itemStack = item;
        if (parent != null) {
            parent.setChild(parentSlot, this);
        }
    }

    public BranchNode getParent() {
        return parent;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public abstract void click(@NotNull Player p);
}
