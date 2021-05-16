package xyz.baz9k.UHCGame.config;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;
import xyz.baz9k.UHCGame.UHCGame;

import static xyz.baz9k.UHCGame.util.Utils.*;

/**
 * Setup for the config GUI tree
 */
public class ConfigTree {
    private BranchNode root;
    private static final int ROOT_GUI_HEIGHT = 6;

    public ConfigTree(UHCGame plugin) {
        Node.setPlugin(plugin);
        root = generateTree();
    }

    private static int getSlotCoordinate(int x, int y) {
        return y * 9 + x;
    }

    private static ItemStack itemStack(Material type, String name, String... lore) {

        Component[] loreComps = Arrays.stream(lore)
            .map(Node::withDefaultDescStyle)
            .toArray(Component[]::new);

        return itemStack(type, Component.text(name, noDecoStyle(null)), loreComps);
    }
    private static ItemStack itemStack(Material type, Component name, Component... lore) {
        ItemStack stack = new ItemStack(type);
        ItemMeta m = stack.getItemMeta();

        m.displayName(name);
        m.lore(Arrays.asList(lore));
        
        stack.setItemMeta(m);
        return stack;
    }

    /**
     * @return the root of the tree, once built
     */
    private BranchNode generateTree() {
        BranchNode root = new BranchNode("Config", ROOT_GUI_HEIGHT);

        new ValuedNode(root, getSlotCoordinate(3, 3), itemStack(Material.DIAMOND, "Dice", "number %s"), ValuedNodeType.INTEGER, "team_count");

        new ActionNode(root, getSlotCoordinate(5, 3), itemStack(Material.EMERALD, "Shiny Button", "Click me I dare you"), player -> {
            Bukkit.getServer().sendMessage(Component.text("Clicky Click."));
        });

        BranchNode subLevel = new BranchNode(root, getSlotCoordinate(4, 4), itemStack(Material.REDSTONE, "schrodinger's box", "except the cat is dead"), "Fuck", 1);

        new ActionNode(subLevel, getSlotCoordinate(5,0), itemStack(Material.DIAMOND, "Dice 2"), player -> {
            Bukkit.getServer().sendMessage(Component.text("Fuck."));
        });

        return root;
    }

    public BranchNode getRoot() {
        return root;
    }

    /**
     * Returns the node in the tree that has the specified inventory
     * @param inventory The inventory
     * @return The node (or null if not present)
     */
    public BranchNode getNodeFromInventory(Inventory inventory) {
        return scanAllChildrenForInventory(inventory, root);
    }

    /**
     * Traverses the tree of a node for the node that has a specified inventory
     * @param inventory The inventory
     * @param node The node tree to traverse
     * @return The node (or null if not present)
     */
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
