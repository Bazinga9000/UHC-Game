package xyz.baz9k.UHCGame.config;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import xyz.baz9k.UHCGame.ConfigManager;

public class BooleanValueNode extends Node {
    private final @NotNull String id;
    private ConfigManager manager;

    public BooleanValueNode(@NotNull BranchNode parent, @NotNull ItemStack itemStack, @NotNull String id, ConfigManager manager, boolean defaultValue) {
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
