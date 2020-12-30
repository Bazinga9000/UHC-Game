package xyz.baz9k.UHCGame.config;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public abstract class Node {
    private final @Nullable BranchNode parent;
    private final @Nullable ItemStack itemStack;

    public Node(@Nullable BranchNode parent, @Nullable ItemStack itemStack) {
        this.parent = parent;
        this.itemStack = itemStack;
    }

    @Nullable
    public BranchNode getParent() {
        return parent;
    }

    @Nullable
    public ItemStack getItemStack() {
        return itemStack;
    }
}
