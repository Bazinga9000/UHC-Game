package xyz.baz9k.UHCGame.config;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ValuedNode extends Node {
    private ValuedNodeType type;
    private String id;

    public ValuedNode(BranchNode parent, int slot, ItemStack item, ValuedNodeType type, String id) {
        super(parent, slot, item);
        this.type = type;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public void click(@NotNull Player p) {

    }
}
