package xyz.baz9k.UHCGame;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class ConfigManager implements Listener {
    private final UHCGame plugin;
    private final GameManager gameManager;
    private Inventory menu;

    public ConfigManager(UHCGame plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;

        createMenu();
    }
    private void createMenu() {
        menu = Bukkit.createInventory(null, 54, "Config");
    }

    private void clickSlot(int slot) {
        Bukkit.broadcastMessage(String.valueOf(slot));
    }

    public void openMenu(Player p) {
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
