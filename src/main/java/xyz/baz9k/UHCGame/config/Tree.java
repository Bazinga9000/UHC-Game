package xyz.baz9k.UHCGame.config;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.baz9k.UHCGame.ConfigManager;

import java.util.ArrayList;
import java.util.List;

public class Tree {
    private final List<Node> tree;
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

    @NotNull
    public List<Node> getTree() {
        return tree;
    }

    @NotNull
    public Node getRoot() {
        return tree.get(0);
    }

    @NotNull
    public List<Node> getNodes() {
        return tree;
    }

    @Nullable
    public BranchNode getCorrespondingNode(@NotNull Inventory inv) {
        for (Node n : tree) {
            if (!(n instanceof BranchNode)) continue;
            BranchNode b = (BranchNode) n;
            if (b.getInventory() == inv) return b;
        }
        return null;
    }
}
