package xyz.baz9k.UHCGame.menu;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

import java.util.Objects;
import java.util.function.UnaryOperator;

public class ValuedNode extends Node {
    protected final Type type;
    protected UnaryOperator<Number> restrict = UnaryOperator.identity();
    private Object prevValue;
    public static BranchNode cfgRoot;

    /**
     * Enum of the supported types for a {@link ValuedNode}.
     */
    public static enum Type {
        INTEGER, DOUBLE, STRING, BOOLEAN, OPTION
    }

    /**
     * @param parent Parent node
     * @param slot lot of this node in parent's inventory
     * @param nodeName Node name, which is used to determine the ID
     * @param props {@link NodeItemStack#ItemProperties}
     * <p>
     * If format strings are included in the item's description (%s, %.1f, etc.), 
     * those will be substituted with the config value.
     * @param type Type of data this value stores
     * <p>
     * WITH A RESTRICTING FUNCTION, THE TYPE MUST BE NUMERIC.
     * @param restrict This function maps invalid numeric values to the correct values.
     * @implNote Inheriting classes should cancel the updateItemStack and recall it after
     * all its properties are set.
     */
    public ValuedNode(BranchNode parent, int slot, String nodeName, NodeItemStack.ItemProperties props, Type type, UnaryOperator<Number> restrict) {
        this(parent, slot, nodeName, props, switch (type) {
            case INTEGER, DOUBLE -> type;
            default -> throw translatableErr(IllegalArgumentException.class, "xyz.baz9k.uhc.err.menu.not_numeric_type", type);
        });
        
        this.restrict = restrict;
    }
    /**
     * @param parent Parent node
     * @param slot lot of this node in parent's inventory
     * @param nodeName Node name, which is used to determine the ID
     * @param props {@link NodeItemStack#ItemProperties}
     * <p>
     * If format strings are included in the item's description (%s, %.1f, etc.), 
     * those will be substituted with the config value.
     * @param type Type of data this value stores
     * @implNote Inheriting classes should cancel the updateItemStack and recall it after
     * all its properties are set.
     */
    public ValuedNode(BranchNode parent, int slot, String nodeName, NodeItemStack.ItemProperties props, Type type) {
        super(parent, slot, nodeName, props);
        this.type = type;
        
        if (type == Type.BOOLEAN) {
            props.metaChanges((o, m) -> {
                var active = (boolean) o;
                if (active) {
                    m.addEnchant(Enchantment.SILK_TOUCH, 1, true);
                } else {
                    m.removeEnchant(Enchantment.SILK_TOUCH);
                }
            }).extraLore(o -> {
                var active = (boolean) o;

                // keeps the description untouched, adds Status: ACTIVE/INACTIVE below it
                TranslatableComponent status;
                if (active) {
                    status = trans("xyz.baz9k.uhc.menu.bool_valued.on").style(noDeco(NamedTextColor.GREEN));
                } else {
                    status = trans("xyz.baz9k.uhc.menu.bool_valued.off").style(noDeco(NamedTextColor.RED));
                }
                return new NodeItemStack.ExtraLore("xyz.baz9k.uhc.menu.bool_valued.status", status);
            });
        }
    }

    public String cfgKey() {
        Objects.requireNonNull(cfgRoot, "Config root not yet declared, cannot initialize valued nodes");
        return pathRelativeTo(cfgRoot);
    }

    @Override
    public void click(@NotNull Player p) {
        switch (type) {
            case INTEGER, DOUBLE, STRING -> new ValueRequest(plugin, p, this);
            case BOOLEAN -> this.set(!cfg.getBoolean(cfgKey()));
            // case OPTION -> see OptionValuedNode#click
            default -> throw translatableErr(IllegalArgumentException.class, "xyz.baz9k.uhc.err.menu.needs_impl", type);
        }
    }

    public void undo(Player p) {
        set(prevValue);
    }

    /**
     * Sets the current object for the config key corresponding to this node.
     * @param value value to set key to
     */
    public void set(Object value) {
        prevValue = cfg.get(cfgKey());
        if (type == Type.INTEGER || type == Type.DOUBLE) value = restrict.apply((Number) value);
        cfg.set(cfgKey(), value);
    }
}
