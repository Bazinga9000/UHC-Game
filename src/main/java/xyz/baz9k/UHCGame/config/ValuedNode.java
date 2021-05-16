package xyz.baz9k.UHCGame.config;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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

    /**
     * Update the item stack based on the current config value for the node
     */
    public void updateItemStack() {
        ItemMeta m = itemStack.getItemMeta();

        // perform updates based on data type
        switch (type) {
            case INTEGER:
                int configValue = plugin.getConfig().getInt(id);
                this.itemStack.setAmount(configValue);
                break;
            case DOUBLE:
                break;
            case STRING:
                break;
            case BOOLEAN:
                break;
            case OPTION:
                break;
            default:
                throw new UnsupportedOperationException("Type not supported");
        }

        itemStack.setItemMeta(m);
        // since updating the item does not update it in the inventory, parent has to
        parent.updateSlot(parentSlot);
    }
}
