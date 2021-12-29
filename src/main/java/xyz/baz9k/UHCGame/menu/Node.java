package xyz.baz9k.UHCGame.menu;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import xyz.baz9k.UHCGame.UHCGamePlugin;

public abstract class Node {
    protected final BranchNode parent;
    private NodeItemStack itemStack;
    private final NodeItemStack.ItemProperties<?> itemProperties;
    protected final int parentSlot;
    protected final String nodeName;
    
    protected static UHCGamePlugin plugin;
    protected static FileConfiguration cfg;

    /**
     * @param parent Parent node
     * @param parentSlot Slot of this node in parent's inventory
     * @param nodeName Name of the node
     * @param props {@link NodeItemStack.ItemProperties}
     */
    public Node(BranchNode parent, int parentSlot, String nodeName, NodeItemStack.ItemProperties<?> props) {
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

    /**
     * @return this node's item stack in inventory
     */
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

    /**
     * @return path relative to the root node
     */
    public String path() {
        if (parent == null) return "";
        return appendNodeName(parent.path(), nodeName);
    }

    /**
     * @param b Node to find relative path
     * @return path relative to the specified node
     */
    public String pathRelativeTo(BranchNode b) {
        if (this.equals(b)) return "";
        if (parent == null) return null;
        return appendNodeName(parent.pathRelativeTo(b), nodeName);
    }

    /**
      * @return the lang key of this node, an identifier used in the lang files to give node name and description.<p>
      * Lang key is just the path + .root if the node is a Branch Node.
      */
    public String langKey() {
        // it looks cleaner to have all the langKey node in one place since it's so small
        if (this instanceof BranchNode) {
            return appendNodeName(path(), "root");
        }
        return path();
    }

}
