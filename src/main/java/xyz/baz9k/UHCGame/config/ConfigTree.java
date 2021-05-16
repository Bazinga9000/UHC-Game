package xyz.baz9k.UHCGame.config;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ConfigTree {
    private static final BranchNode root = generateTree();
    private static final int ROOT_HEIGHT = 6;

    private static int getSlotCoordinate(int x, int y) {
        return y * 9 + x;
    }

    private static BranchNode generateTree() {
        BranchNode root = new BranchNode("Config", ROOT_HEIGHT);

        ItemStack item1 = new ItemStack(Material.DIAMOND, 1);
        ValuedNode test1 = new ValuedNode(root, getSlotCoordinate(3, 3), item1, ValuedNodeType.INTEGER, "7");

        ItemStack item2 = new ItemStack(Material.EMERALD, 1);
        ActionNode test2 = new ActionNode(root, getSlotCoordinate(5, 3), item2, player -> {
            Bukkit.broadcastMessage("Clicky Click.");
        });

        ItemStack item3 = new ItemStack(Material.REDSTONE, 6);
        BranchNode subLevel = new BranchNode(root, getSlotCoordinate(4, 4), item3, "Fuck", 1);

        ActionNode test4 = new ActionNode(subLevel, getSlotCoordinate(5,0), item1, player -> {
            Bukkit.broadcastMessage("Fuck.");
        });

        return root;
    }

    public static BranchNode getRoot() {
        return root;
    }

    public static BranchNode getNodeFromInventory(Inventory inventory) {
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
