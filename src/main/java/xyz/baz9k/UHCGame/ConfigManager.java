package xyz.baz9k.UHCGame;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import xyz.baz9k.UHCGame.config.BranchConfigNode;
import xyz.baz9k.UHCGame.config.ConfigNode;
import xyz.baz9k.UHCGame.config.ConfigTree;

import java.util.ArrayList;
import java.util.HashMap;

public class ConfigManager implements Listener {
    private final ConfigTree tree;

    private final HashMap<String, Boolean> booleanValues;
    private final HashMap<String, Integer> integerValues;
    private final HashMap<String, Double> doubleValues;

    public void setValue(String id, boolean value) {
        booleanValues.put(id, value);
    }

    public void setValue(String id, int value) {
        integerValues.put(id, value);
    }

    public void setValue(String id, double value) {
        doubleValues.put(id, value);
    }

    public ConfigManager() {
        tree = new ConfigTree(this);
        booleanValues = new HashMap<>();
        integerValues = new HashMap<>();
        doubleValues = new HashMap<>();
    }

    public void openMenu(@NotNull Player p) {
        p.openInventory(((BranchConfigNode) tree.getRoot()).getInventory());
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        for (ConfigNode node : tree.getNodes()) {
            if (node instanceof BranchConfigNode) {
                BranchConfigNode b = (BranchConfigNode) node;
                if (b.getInventory() == e.getInventory()) {
                    if (b.getInventory() == e.getClickedInventory()) {
                        b.onClick((Player) e.getWhoClicked(), e.getSlot());
                    }
                    e.setCancelled(true);
                }
            }
        }
    }
}
