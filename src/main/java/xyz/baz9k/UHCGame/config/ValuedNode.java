package xyz.baz9k.UHCGame.config;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class ValuedNode extends Node {
    protected final ValuedNodeType type;
    protected final String id;

    public ValuedNode(BranchNode parent, int slot, ItemStack item, ValuedNodeType type, String id) {
        super(parent, slot, item);
        this.type = type;
        this.id = id;
        updateItemStack();
    }

    public String getId() {
        return id;
    }

    @Override
    public void click(@NotNull Player p) {
        Object val;
        switch (type) {
            // TODO impl prompts for int, dbl, string  
            case INTEGER:
                val = cfg.getInt(id);
                break;
            case DOUBLE:
                val = cfg.getDouble(id);
                break;
            case STRING:
                val = cfg.getString(id);
                break;

            case BOOLEAN:
                val = !cfg.getBoolean(id);
                break;
            
            // OPTION impl in OptionValuedNode
            
            default:
                throw new UnsupportedOperationException("Type not supported");
            }
        cfg.set(id, val);
    }

    /**
     * Update the item stack based on the current config value for the node
     */
    public void updateItemStack() {
        ItemMeta m = itemStack.getItemMeta();

        // perform updates based on data type
        switch (type) {
            case INTEGER:
                int configValue = cfg.getInt(id);
                this.itemStack.setAmount(configValue);
                break;
            case DOUBLE:
                break;
            case STRING:
                break;
            case BOOLEAN:
                break;

            // OPTION impl in OptionValuedNode

            default:
                throw new UnsupportedOperationException("Type not supported");
        }

        itemStack.setItemMeta(m);
        // since updating the item does not update it in the inventory, parent has to
        parent.updateSlot(parentSlot);
    }
}
