package xyz.baz9k.UHCGame.config;

import java.util.List;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import static xyz.baz9k.UHCGame.util.Utils.*;

public class ValuedNode extends Node {
    protected final Type type;
    protected final String id;

    /**
     * Enum of the supported types for a {@link ValuedNode}.
     */
    public static enum Type {
        INTEGER, DOUBLE, STRING, BOOLEAN, OPTION
    }


    /**
     * @param parent Parent node
     * @param slot lot of this node in parent's inventory
     * @param item Item stack of this node in parent's inventory
     * <p>
     * If format strings are included in the item's description (%s, %.1f, etc.), 
     * those will be substituted with the config value.
     * @param type Type of data this value stores
     * @param id The config ID for this node
     */
    public ValuedNode(BranchNode parent, int slot, NodeItemStack item, Type type, String id) {
        this(parent, slot, item, type, id, true);
    }

    /**
     * @param parent Parent node
     * @param slot lot of this node in parent's inventory
     * @param item Item stack of this node in parent's inventory
     * <p>
     * If format strings are included in the item's description (%s, %.1f, etc.), 
     * those will be substituted with the config value.
     * @param type Type of data this value stores
     * @param id The config ID for this node
     * @param updateStack ItemStack should always be updated at init, but in the case of 
     * inheriting classes, updateItemStack needs to occur at the end of the child class's
     * constructor.
     */
    protected ValuedNode(BranchNode parent, int slot, NodeItemStack item, Type type, String id, boolean updateStack) {
        super(parent, slot, item);
        this.type = type;
        this.id = id;
        if (updateStack) updateItemStack();
    }

    public String getId() {
        return id;
    }

    @Override
    public void click(@NotNull Player p) {
        Object val;
        switch (type) {
            // TODO request value from player for integer/double/string
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
        itemStack.desc(cfg.get(id));
        
        // perform updates based on data type
        switch (type) {
            case INTEGER:
            case DOUBLE:
            case STRING:
                break;
            case BOOLEAN:
                boolean active = cfg.getBoolean(id);
                // keeps the description untouched, adds Status: ACTIVE/INACTIVE below it
                TextComponent.Builder status = Component.text()
                .append(Component.text("Status: ", noDecoStyle(NamedTextColor.WHITE)));
                
                if (active) {
                    status.append(Component.text("ACTIVE", noDecoStyle(NamedTextColor.GREEN)));
                } else {
                    status.append(Component.text("INACTIVE", noDecoStyle(NamedTextColor.RED)));
                }
                itemStack.extraLore(List.of(status.asComponent()));
                
                ItemMeta m = itemStack.getItemMeta();
                if (active) {
                    m.addEnchant(Enchantment.SILK_TOUCH, 1, true);
                } else {
                    m.removeEnchant(Enchantment.SILK_TOUCH);
                }
                itemStack.setItemMeta(m);
                break;
                
                // OPTION impl in OptionValuedNode
                
            default:
                throw new UnsupportedOperationException("Type not supported");
        }

        // since updating the item does not update it in the inventory, parent has to
        parent.updateSlot(parentSlot);
    }
}
