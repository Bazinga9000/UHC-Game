package xyz.baz9k.UHCGame.config;

import org.bukkit.inventory.ItemStack;
import xyz.baz9k.UHCGame.ConfigManager;

public class BooleanValueConfigNode extends ConfigNode {
    private final String id;
    private ConfigManager manager;

    public BooleanValueConfigNode(BranchConfigNode parent, ItemStack itemStack, String id, ConfigManager manager, boolean defaultValue) {
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
