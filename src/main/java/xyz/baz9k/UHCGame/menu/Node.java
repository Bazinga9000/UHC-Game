package xyz.baz9k.UHCGame.menu;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import xyz.baz9k.UHCGame.UHCGamePlugin;

public abstract class Node {
    protected final BranchNode parent;
    private final NodeItemStack itemStack;
    protected final int parentSlot;
    protected final String nodeName;
    
    protected static UHCGamePlugin plugin;
    protected static FileConfiguration cfg;

    /**
     * @param parent Parent node
     * @param parentSlot lot of this node in parent's inventory
     * @param nodeName Node name, which is used to determine the ID
     * @param props {@link NodeItemStack#ItemProperties}
     */
    public Node(BranchNode parent, int parentSlot, String nodeName, NodeItemStack.ItemProperties props) {
        this.parent = parent;
        this.nodeName = nodeName;
        this.itemStack = props == null ? null : new NodeItemStack(id(), props);

        this.parentSlot = parentSlot;
        if (parent != null) {
            parent.setChild(parentSlot, this);
        }
    }

    /**
     * Set the plugin for all plugins to use the config of
     * @param plugin The plugin
     */
    public static void setPlugin(UHCGamePlugin plugin) {
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

    public ItemStack itemStack() {
        return itemStack.updateAll();
    }
}
