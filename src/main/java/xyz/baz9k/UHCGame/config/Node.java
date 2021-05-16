package xyz.baz9k.UHCGame.config;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.baz9k.UHCGame.UHCGame;

public abstract class Node {
    protected BranchNode parent;
    protected ItemStack itemStack;
    protected UHCGame plugin;
    protected int parentSlot;

    public Node(UHCGame plugin, BranchNode parent, int parentSlot, ItemStack item) {
        this.plugin = plugin;
        this.parent = parent;
        this.itemStack = item;
        this.parentSlot = parentSlot;
        if (parent != null) {
            parent.setChild(parentSlot, this);
        }
    }

    public abstract void click(@NotNull Player p);
}
