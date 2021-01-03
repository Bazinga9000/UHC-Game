package xyz.baz9k.UHCGame;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ConfigManager implements Listener {
    private Inventory menu;
    private static int SLOTS = 54;

    public ConfigManager() {
        createMenu();
    }
    
    private void createMenu() {
        menu = Bukkit.createInventory(null, SLOTS, "Config");
    }

    private void clickSlot(int slot) {
        if (slot < 0 || slot >= SLOTS) {
            throw new IllegalArgumentException("Invalid slot (Slot must be positive and less than " + SLOTS + ".)");
        }
    }

    public void openMenu(@NotNull Player p) {
        p.openInventory(menu);
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        if (e.getInventory() == menu) { // check if inv view is menu
            if (e.getClickedInventory() == menu) { // check if *clicked* inventory is menu
                if (e.getCurrentItem() != null) clickSlot(e.getSlot());
            }
            e.setCancelled(true);
        }
        
    }
}
