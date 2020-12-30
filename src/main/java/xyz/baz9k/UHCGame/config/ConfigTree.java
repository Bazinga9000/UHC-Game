package xyz.baz9k.UHCGame.config;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import xyz.baz9k.UHCGame.ConfigManager;

import java.util.ArrayList;

public class ConfigTree {
    private final ArrayList<ConfigNode> tree;
    ConfigManager configManager;
    public ConfigTree(ConfigManager manager) {
        configManager = manager;
        tree = new ArrayList<>();

        //ROOT
        BranchConfigNode root = new BranchConfigNode(null, null, "Config", 1);
        tree.add(root);

        BranchConfigNode current = root;
        for (int i = 0; i < 10; i++) {
            BranchConfigNode node = new BranchConfigNode(current, new ItemStack(Material.DIAMOND), Integer.toString(i), 1);
            current.addChild(0, node);
            current = node;
            tree.add(node);
        }

    }

    public ArrayList<ConfigNode> getTree() {
        return tree;
    }

    public ConfigNode getRoot() {
        return tree.get(0);
    }

    public ArrayList<ConfigNode> getNodes() {
        return tree;
    }
}
