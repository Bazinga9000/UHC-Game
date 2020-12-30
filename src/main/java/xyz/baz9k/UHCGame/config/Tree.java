package xyz.baz9k.UHCGame.config;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import xyz.baz9k.UHCGame.ConfigManager;

import java.util.ArrayList;

public class Tree {
    private final ArrayList<Node> tree;
    ConfigManager configManager;
    public Tree(ConfigManager manager) {
        configManager = manager;
        tree = new ArrayList<>();

        //ROOT
        BranchNode root = new BranchNode(null, null, "Config", 1);
        tree.add(root);

        BranchNode current = root;
        for (int i = 0; i < 10; i++) {
            BranchNode node = new BranchNode(current, new ItemStack(Material.DIAMOND), Integer.toString(i), 1);
            current.addChild(0, node);
            current = node;
            tree.add(node);
        }

    }

    public ArrayList<Node> getTree() {
        return tree;
    }

    public Node getRoot() {
        return tree.get(0);
    }

    public ArrayList<Node> getNodes() {
        return tree;
    }
}
