package xyz.baz9k.UHCGame.menu;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import xyz.baz9k.UHCGame.UHCGamePlugin;

public abstract class Node {
    protected final BranchNode parent;
    private NodeItemStack itemStack;
    private final NodeItemStack.ItemProperties itemProperties;
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
        this.itemProperties = props;

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

    public ItemStack itemStack() {
        if (itemStack == null) {
            // item stack is accessed for first time. make it
            itemStack = itemProperties != null ? new NodeItemStack(langKey(), itemProperties) : null;
            return itemStack;
        }
        return itemStack.updateAll();
    }

    private static String appendNodeName(String parentName, String nodeName) {
        if (parentName == null) return null;
        if (parentName.equals("")) return nodeName;
        return parentName + "." + nodeName;
    }

    public String path() {
        if (parent == null) return "";
        return appendNodeName(parent.path(), nodeName);
    }

    public String pathRelativeTo(BranchNode b) {
        if (parent.equals(b)) return "";
        if (parent == null) return null;
        return appendNodeName(parent.pathRelativeTo(b), nodeName);
    }

    /**
     * Gets the lang key of this node, which is an identifier used in the lang files to give this node a name and description.
     * (Also it looks cleaner to have all the langKey code in one place so BranchNode isn't overriding this method)
     * 
     * The lang key is just the path (the names of the nodes dotted together) + .root if the node is a BranchNode.
     */
    public String langKey() {
        if (this instanceof BranchNode) {
            return appendNodeName(path(), "root");
        }
        return path();
    }

}
