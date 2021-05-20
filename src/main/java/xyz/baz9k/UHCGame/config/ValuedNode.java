package xyz.baz9k.UHCGame.config;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import static xyz.baz9k.UHCGame.util.Utils.*;

import java.util.function.UnaryOperator;

public class ValuedNode extends Node {
    protected final Type type;
    protected final String id;
    protected UnaryOperator<Number> restrict = UnaryOperator.identity();

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
     * @implNote Inheriting classes should cancel the updateItemStack and recall it after
     * all its properties are set.
     */
    public ValuedNode(BranchNode parent, int slot, NodeItemStack item, Type type, String id) {
        super(parent, slot, item);
        this.type = type;
        this.id = id;
        
        updateItemStack();
    }

    /**
     * @param parent Parent node
     * @param slot lot of this node in parent's inventory
     * @param item Item stack of this node in parent's inventory
     * <p>
     * If format strings are included in the item's description (%s, %.1f, etc.), 
     * those will be substituted with the config value.
     * @param type Type of data this value stores
     * <p>
     * WITH A RESTRICTING FUNCTION, THE TYPE MUST BE NUMERIC.
     * @param id The config ID for this node
     * @param restrict This function maps invalid numeric values to the correct values.
     * @implNote Inheriting classes should cancel the updateItemStack and recall it after
     * all its properties are set.
     */
    public ValuedNode(BranchNode parent, int slot, NodeItemStack item, Type type, String id, UnaryOperator<Number> restrict) {
        this(parent, slot, item, switch (type) {
            case INTEGER, DOUBLE -> type;
            default -> throw new IllegalArgumentException(String.format("Type %s is not a numeric type.", type));
        }, id);
        
        this.restrict = restrict;
        cfg.set(id, restrict.apply((Number) cfg.get(id)));
        updateItemStack();
    }

    public String getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    @Override
    public void click(@NotNull Player p) {
        switch (type) {
            case INTEGER, DOUBLE, STRING -> new ValueRequest(plugin, p, this);
            case BOOLEAN -> this.set(!cfg.getBoolean(id));
            // case OPTION -> see OptionValuedNode#click
            default -> {
                throw new IllegalArgumentException(String.format("Type %s not implemented", type));
            }
        }
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
                .append(Component.text("Status: ", noDeco(NamedTextColor.WHITE)));
                
                if (active) {
                    status.append(Component.text("ACTIVE", noDeco(NamedTextColor.GREEN)));
                } else {
                    status.append(Component.text("INACTIVE", noDeco(NamedTextColor.RED)));
                }
                itemStack.extraLore(status.asComponent());
                
                itemStack.updateMeta(m -> {
                    if (active) {
                        m.addEnchant(Enchantment.SILK_TOUCH, 1, true);
                    } else {
                        m.removeEnchant(Enchantment.SILK_TOUCH);
                    }
                });
                break;
                
                // OPTION impl in OptionValuedNode
                
            default:
                throw new IllegalArgumentException(String.format("Type %s not implemented", type));
        }

        // since updating the item does not update it in the inventory, parent has to
        parent.updateSlot(parentSlot);
    }

    public void set(Object value) {
        cfg.set(id, value);
        updateItemStack();
    }
}
