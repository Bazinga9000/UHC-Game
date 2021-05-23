package xyz.baz9k.UHCGame.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import xyz.baz9k.UHCGame.UHCGame;

public abstract class Node {
    protected final BranchNode parent;
    protected final NodeItemStack itemStack;
    protected final int parentSlot;
    protected final String nodeName;
    
    protected static UHCGame plugin;
    protected static FileConfiguration cfg;

    /**
     * @param parent Parent node
     * @param parentSlot lot of this node in parent's inventory
     * @param nodeName Node name, which is used to determine the ID
     * @param info {@link NodeItemStack#Info}
     */
    public Node(BranchNode parent, int parentSlot, String nodeName, NodeItemStack.Info info) {
        this.parent = parent;
        this.nodeName = nodeName;
        this.itemStack = info.mat() == null ? null : new NodeItemStack(nodeName, info);

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

    /**
     * Gets the ID of this node, created from the names of the node.
     * <p> This is used for storing config values ({@link ValuedNode}) and names and descriptions in the inventory.
     * @return the ID
     */
    public String id() {
        if (parent == null) return "";

        String pid = parent.id().replaceFirst("\\.?root", "");
        if (pid.equals("")) return nodeName;
        return pid + "." + nodeName;
    }
}
