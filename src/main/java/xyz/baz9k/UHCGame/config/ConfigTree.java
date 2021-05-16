package xyz.baz9k.UHCGame.config;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import xyz.baz9k.UHCGame.UHCGame;

public class ConfigTree {
    private BranchNode root;
    private UHCGame plugin;
    private static final int ROOT_HEIGHT = 6;

    private static int getSlotCoordinate(int x, int y) {
        return y * 9 + x;
    }

    private static ItemStack itemStack(Material type, String name, String... lore) {
        ItemStack stack = new ItemStack(type);
        ItemMeta m = stack.getItemMeta();

        m.setDisplayName(name);
        m.setLore(Arrays.asList(lore));
        
        stack.setItemMeta(m);
        return stack;
    }
    
    public ConfigTree(UHCGame plugin) {
        Node.setPlugin(plugin);
        root = generateTree();
    }

    private BranchNode generateTree() {
        BranchNode root = new BranchNode("Config", ROOT_HEIGHT);

        new ValuedNode(root, getSlotCoordinate(3, 3), itemStack(Material.DIAMOND, "Dice", "number %s"), ValuedNodeType.INTEGER, "team_count");

        new ActionNode(root, getSlotCoordinate(5, 3), itemStack(Material.EMERALD, "Shiny Button", "Click me I dare you"), player -> {
            Bukkit.broadcastMessage("Clicky Click.");
        });

        BranchNode subLevel = new BranchNode(root, getSlotCoordinate(4, 4), itemStack(Material.REDSTONE, "schrodinger's box", "except the cat is dead"), "Fuck", 1);

        new ActionNode(subLevel, getSlotCoordinate(5,0), itemStack(Material.DIAMOND, "Dice 2"), player -> {
            Bukkit.broadcastMessage("Fuck.");
        });

        return root;
    }

    public BranchNode getRoot() {
        return root;
    }

    public BranchNode getNodeFromInventory(Inventory inventory) {
        return scanAllChildrenForInventory(inventory, root);
    }


    private static BranchNode scanAllChildrenForInventory(Inventory inventory, BranchNode node) {
        if (node.getInventory() == inventory) {
            return node;
        }

        for (Node child : node.getChildren()) {
            if (child instanceof BranchNode) {
                BranchNode check = scanAllChildrenForInventory(inventory, (BranchNode) child);
                if (check != null) {
                    return check;
                }
            }
        }
        return null;
    }
}
