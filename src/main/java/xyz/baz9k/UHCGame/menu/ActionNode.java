
package xyz.baz9k.UHCGame.menu;

import org.jetbrains.annotations.NotNull;

import xyz.baz9k.UHCGame.util.Debug;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.function.Consumer;

/**
 * {@link Node} that does not open an {@link Inventory} or store a config value.
 * <p>
 * Runs an action that is not just setting a config store to a specified value.
 */
public class ActionNode extends Node {
    private Consumer<Player> fn;

    /**
     * @param parent Parent node
     * @param slot Slot of this node in parent's inventory
     * @param nodeName Node name, which is used to determine the ID
     * @param props {@link NodeItemStack#ItemProperties}
     * @param fn Function ran when this node is clicked
     */
    public ActionNode(@NotNull BranchNode parent, int slot, String nodeName, @NotNull NodeItemStack.ItemProperties props, @NotNull Consumer<Player> fn) {
        super(parent, slot, nodeName, props);
        this.fn = fn;
    }

    public void click(Player p) {
        try {
            fn.accept(p);
        } catch (Exception e) {
            Debug.printError(e);
        }
    }

}