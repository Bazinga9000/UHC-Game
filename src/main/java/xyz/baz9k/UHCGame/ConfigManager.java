package xyz.baz9k.UHCGame;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import xyz.baz9k.UHCGame.config.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ConfigManager implements Listener {
    private final UHCGame plugin;

    public ConfigManager(UHCGame plugin) {
        this.plugin = plugin;
    }

    public void openMenu(@NotNull Player p) {
        ConfigTree.getRoot().click(p);
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        BranchNode b = ConfigTree.getNodeFromInventory(e.getInventory());

        if (b == null) return; // interacted view does not have a node inventory
        if (e.getInventory() == e.getClickedInventory()) { // handle clicks IF the clicked inv is the top of the view
            if (e.getCurrentItem() != null) b.onClick((Player) e.getWhoClicked(), e.getSlot());
            e.setCancelled(true);
        }

    }

    /**
     * Takes an {@link Inventory} and returns the {@link BranchNode} that is linked to that Inventory.
     * @param inv The inventory to check
     * @return the BranchNode matching the Inventory (null if no BranchNode matches the Inventory).
     */

}
