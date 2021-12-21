package xyz.baz9k.UHCGame;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import xyz.baz9k.UHCGame.menu.*;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MenuManager implements Listener {
    private final MenuTree menuTree;

    public MenuManager(UHCGamePlugin plugin) {
        this.menuTree = new MenuTree(plugin);
    }

    public void openMenu(@NotNull Player p) {
        menuTree.root().click(p);
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        BranchNode b = menuTree.getNodeFromInventory(e.getInventory());

        if (b == null) return; // interacted view does not have a node inventory
        if (e.getInventory() == e.getClickedInventory()) { // handle clicks IF the clicked inv is the top of the view
            try {
                if (e.getCurrentItem() != null) b.onClick((Player) e.getWhoClicked(), e.getSlot());
            } finally {
                e.setCancelled(true);
            }
        }

    }
}
