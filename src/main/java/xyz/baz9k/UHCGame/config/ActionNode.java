
package xyz.baz9k.UHCGame.config;

import org.jetbrains.annotations.NotNull;
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
     * @param itemStack Item stack of this node in parent's inventory
     * @param fn Function ran when this node is clicked
     */
    public ActionNode(@NotNull BranchNode parent, int slot, @NotNull NodeItemStack itemStack, @NotNull Consumer<Player> fn) {
        super(parent, slot, itemStack);
        this.fn = fn;
    }

    public void click(Player p) {
        fn.accept(p);
    }

}