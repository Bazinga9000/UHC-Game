package xyz.baz9k.UHCGame.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import xyz.baz9k.UHCGame.UHCGamePlugin;
import xyz.baz9k.UHCGame.util.Path;
import xyz.baz9k.UHCGame.util.stack.ItemProperties;
import xyz.baz9k.UHCGame.util.stack.TransItemStack;

import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public abstract class Node {
    protected final BranchNode parent;
    private TransItemStack itemStack;
    protected final ItemProperties itemProperties;
    protected final int parentSlot;
    protected final String nodeName;
    protected BooleanSupplier lock = () -> false;

    protected static UHCGamePlugin plugin;
    protected static FileConfiguration cfg;

    /**
     * @param parent Parent node
     * @param parentSlot Slot of this node in parent's inventory
     * @param nodeName Name of the node
     * @param props {@link ItemProperties}
     */
    public Node(BranchNode parent, int parentSlot, String nodeName, ItemProperties props) {
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
     * @param p
     * @return true/false if action was successful
     */
    public abstract boolean click(@NotNull Player p);

    /**
     * Disable use of this node if the predicate is matched
     * @param lockIf If returns true, node is locked and cannot be used.
     * @return this
     */
    @SuppressWarnings("unchecked")
    public <T extends Node> T lock(Predicate<T> lockIf) {
        this.lock = () -> lockIf.test((T) this);
        return (T) this;
    }
    /**
     * Disable use of this node if the predicate is matched
     * @param lockIf If returns true, node is locked and cannot be used.
     * @return this
     */
    @SuppressWarnings("unchecked")
    public <T extends Node> T lock(BooleanSupplier lockIf) {
        this.lock = lockIf;
        return (T) this;
    }

    public boolean locked() {
        return lock.getAsBoolean();
    }

    private ItemStack lockedItem() {
        ItemStack lockedItem = new ItemStack(Material.BLACK_CONCRETE);
        lockedItem.editMeta(m -> {
            m.displayName(Component.space());
        });
        return lockedItem;
    }
    /**
     * @return this node's item stack in inventory
     */
    public ItemStack itemStack() {
        // test that node isn't locked
        if (locked()) {
            return lockedItem();
        }
        if (itemStack == null) {
            // item stack is accessed for first time. make it
            itemStack = itemProperties != null ? new TransItemStack(langKey(), itemProperties) : null;
            return itemStack;
        }
        return itemStack.refresh();
    }
    
    /**
     * @return path relative to the root node
     */
    public Path path() {
        if (parent == null) return Path.of();
        return parent.path().append(nodeName);
    }

    /**
     * @param b Node to find relative path
     * @return path relative to the specified node
     */
    public Optional<Path> pathRelativeTo(BranchNode b) {
        return path().relativeTo(b.path());
    }

    /**
      * @return the lang key of this node, an identifier used in the lang files to give node name and description.<p>
      * Lang key is just the path + .root if the node is a Branch Node.
      */
    public String langKey() {
        Path p = Path.of("menu.inv").append(path());
        if (this instanceof BranchNode) {
            p = p.append("root");
        }
        return p.toString();
    }

}
