
package xyz.baz9k.UHCGame.menu;

import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import xyz.baz9k.UHCGame.util.Debug;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * {@link Node} that does not open an {@link Inventory} or store a config value.
 * <p>
 * Runs an action that is not just setting a config store to a specified value.
 */
public class ActionNode extends Node {
    public static class ActionFailedException extends Exception {
        public ActionFailedException(String message) { super(message); }
        public ActionFailedException(Throwable cause) { super(cause); }
    }
    
    @FunctionalInterface
    public interface NodeAction {
        void run(Player p) throws ActionFailedException;
    }

    private final NodeAction fn;

    /**
     * @param parent Parent node
     * @param slot Slot of this node in parent's inventory
     * @param nodeName Name of the node
     * @param props {@link NodeItemStack.ItemProperties}
     * @param fn Function to run when this node is clicked
     */
    public ActionNode(@NotNull BranchNode parent, int slot, @NotNull String nodeName, @NotNull NodeItemStack.ItemProperties<?> props, @NotNull NodeAction fn) {
        super(parent, slot, nodeName, props);
        this.fn = fn;
    }

    @Override
    public boolean click(@NotNull Player p) {
        try {
            fn.run(p);
        } catch (ActionFailedException e) {
            p.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
            return false;
        } catch (Exception e) {
            Debug.printError(e);
        }
        return true;
    }

}