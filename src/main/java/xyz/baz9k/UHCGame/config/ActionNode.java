
package xyz.baz9k.UHCGame.config;

import org.bukkit.inventory.ItemStack;
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

    public ActionNode(@NotNull BranchNode parent, int slot, @NotNull ItemStack itemStack, @NotNull Consumer<Player> fn) {
        super(parent, slot, itemStack);
        this.fn = fn;
    }

    public void click(Player p) {
        fn.accept(p);
    }

}