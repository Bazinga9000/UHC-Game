package xyz.baz9k.UHCGame.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import xyz.baz9k.UHCGame.UHCGame;

public abstract class Node {
    protected BranchNode parent;
    protected NodeItemStack itemStack;
    protected static UHCGame plugin;
    protected static FileConfiguration cfg;
    protected int parentSlot;

    /**
     * @param parent Parent node
     * @param parentSlot lot of this node in parent's inventory
     * @param item Item stack of this node in parent's inventory
     */
    public Node(BranchNode parent, int parentSlot, NodeItemStack item) {
        this.parent = parent;
        this.itemStack = item;

        this.parentSlot = parentSlot;
        if (parent != null) {
            parent.setChild(parentSlot, this);
        }
    }

    /**
     * Set the plugin for all plugins to use the config of
     * @param plugin The plugin
     */
    public static void setPlugin(UHCGame plugin) {
        Node.plugin = plugin;
        Node.cfg = plugin.getConfig();
    }

    /**
     * Action to run when this node is clicked in parent's inventory
     * @param p Player who clicked the node
     */
    public abstract void click(@NotNull Player p);
}
