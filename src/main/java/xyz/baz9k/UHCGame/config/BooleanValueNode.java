package xyz.baz9k.UHCGame.config;

import org.bukkit.inventory.ItemStack;
import xyz.baz9k.UHCGame.ConfigManager;

public class BooleanValueNode extends Node {
    private final String id;
    private ConfigManager manager;

    public BooleanValueNode(BranchNode parent, ItemStack itemStack, String id, ConfigManager manager, boolean defaultValue) {
        super(parent, itemStack);
        this.id = id;
        this.manager = manager;
    }

    public String getId() {
        return id;
    }


    public void updateValue(boolean newValue) {
        manager.setValue(id, newValue);
        updateItemStack();
    }

    private void updateItemStack() {

    }
}
