package xyz.baz9k.UHCGame;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class ConfigManager {
    private final UHCGame plugin;
    private final GameManager gameManager;
    private Inventory menu; // TODO, make inventory not editable

    public ConfigManager(UHCGame plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;

        createMenu();
    }
    private void createMenu() {
        menu = Bukkit.createInventory(null, 54, "Config");
    }

    public void openMenu(Player p) {
        p.openInventory(menu);
    }
    
}
