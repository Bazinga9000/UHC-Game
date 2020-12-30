package xyz.baz9k.UHCGame;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import xyz.baz9k.UHCGame.config.BranchNode;
import xyz.baz9k.UHCGame.config.Tree;

import java.util.HashMap;

public class ConfigManager implements Listener {
    private final Tree tree;

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
        tree = new Tree(this);
        booleanValues = new HashMap<>();
        integerValues = new HashMap<>();
        doubleValues = new HashMap<>();
    }

    public void openMenu(@NotNull Player p) {
        p.openInventory(((BranchNode) tree.getRoot()).getInventory());
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        BranchNode b = tree.getCorrespondingNode(e.getInventory());

        if (b == null) return; // interacted view does not have a node inventory
        if (e.getInventory() == e.getClickedInventory()) { // handle clicks IF the clicked inv is the top of the view
            if (e.getCurrentItem() != null) b.onClick((Player) e.getWhoClicked(), e.getSlot());
        }

    }
}
