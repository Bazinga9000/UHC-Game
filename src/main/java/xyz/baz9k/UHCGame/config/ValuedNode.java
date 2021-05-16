package xyz.baz9k.UHCGame.config;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.baz9k.UHCGame.UHCGame;

public class ValuedNode extends Node {
    private final ValuedNodeType type;
    private final String id;

    public ValuedNode(UHCGame plugin, BranchNode parent, int slot, ItemStack item, ValuedNodeType type, String id) {
        super(plugin, parent, slot, item);
        this.type = type;
        this.id = id;
        updateItemStack();
    }

    public String getId() {
        return id;
    }

    @Override
    public void click(@NotNull Player p) {

    }

    public void updateItemStack() {
        switch(type) {
            case INTEGER:
                int configValue = plugin.getConfig().getInt(id);
                this.itemStack.setAmount(configValue);
                break;
            case DOUBLE:
            case STRING:
            case BOOLEAN:
            case OPTION:
            default:
        }

        parent.updateSlot(parentSlot);
    }
}
