package xyz.baz9k.UHCGame.config;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import static xyz.baz9k.UHCGame.util.Utils.*;

public class ValuedNode extends Node {
    protected final ValuedNodeType type;
    protected final String id;

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
    public ValuedNode(BranchNode parent, int slot, ItemStack item, ValuedNodeType type, String id) {
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
    protected ValuedNode(BranchNode parent, int slot, ItemStack item, ValuedNodeType type, String id, boolean updateStack) {
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
        ItemMeta m = itemStack.getItemMeta();
        m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        List<Component> newDesc = new ArrayList<>(itemDesc);

        // perform updates based on data type
        switch (type) {
            case INTEGER:
            case DOUBLE:
            case STRING:
                // use the initially provided desc for format
                Object val = cfg.get(id);
                for (int i = 0; i < itemDesc.size(); i++) {
                    // subst lines with the correct content
                    if (itemDesc.get(i) instanceof TextComponent comp) {
                        String line = comp.content();
                        newDesc.set(i, comp.content(String.format(line, val)));
                    }
                }
                break;
            case BOOLEAN:
                // keeps the description untouched, adds Status: ACTIVE/INACTIVE below it
                TextComponent.Builder status = Component.text()
                    .append(Component.text("Status: ", noDecoStyle(NamedTextColor.WHITE)));
                
                if (cfg.getBoolean(id)) {
                    status.append(Component.text("ACTIVE", noDecoStyle(NamedTextColor.GREEN)));
                    m.addEnchant(Enchantment.SILK_TOUCH, 1, true);
                } else {
                    status.append(Component.text("INACTIVE", noDecoStyle(NamedTextColor.RED)));
                    m.removeEnchant(Enchantment.SILK_TOUCH);
                }
                newDesc.add(Component.empty());
                newDesc.add(status.asComponent());
                break;
                
                // OPTION impl in OptionValuedNode
                
            default:
                throw new UnsupportedOperationException("Type not supported");
        }
            
        m.lore(newDesc);
        itemStack.setItemMeta(m);
        // since updating the item does not update it in the inventory, parent has to
        parent.updateSlot(parentSlot);
    }
}
