package xyz.baz9k.UHCGame.menu;

import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.baz9k.UHCGame.util.Path;
import xyz.baz9k.UHCGame.util.stack.ItemProperties;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * {@link Node} that holds other Nodes. These children node are accessible through its inventory.
 */
public final class BranchNode extends InventoryNode {
    private final Node[] children;
    private Predicate<Configuration> check = cfg -> true;

    /**
     * Create a root {@link BranchNode}. This node does not have a parent.
     * @param guiHeight Number of rows in this node's inventory
     */
    public BranchNode(int guiHeight) {
        this(null, 0, null, null, guiHeight);
    }

    /**
     * @param parent Parent node
     * @param slot Slot of this node in parent's inventory
     * @param nodeName Name of the node
     * @param props {@link ItemProperties}
     * @param guiHeight Number of rows in this node's inventory
     */
    public BranchNode(@Nullable BranchNode parent, int slot, String nodeName, ItemProperties props, int guiHeight) {
        super(parent, slot, nodeName, props, guiHeight, ReserveSlots.all());

        int arrLen = parent == null ? slotCount : slotCount - 1;
        children = new Node[arrLen];
    }

    /**
     * Adds a check to node that verifies that the config is not in an invalid state (e.g. incompatible settings),<p>
     * and undoes an event if so
     * @param check the check
     * @return this
     */
    public BranchNode check(Predicate<Configuration> check) {
        this.check = check;
        return this;
    }

    @Override
    void initInventory() {
        super.initInventory();

        // fill the inventory with all the items
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < children.length; i++) {
            Node child = children[i];
            if (child != null) {
                contents[i] = child.itemStack();
            }
        }
        inventory.setContents(contents);
    }

    @Override
    void updateInventory() {
        for (int i = 0; i < children.length; i++) updateSlot(i);
    }

    /**
     * Set child to a slot. If child is null, the child at specified slot is removed from the slot.
     * @param slot Slot to set child to
     * @param child The child
     */
    public void setChild(int slot, @Nullable Node child) {
        Objects.checkIndex(slot, children.length);

        children[slot] = child;
        if (hasInventoryViewed) {
            ItemStack item = child != null ? child.itemStack() : filler();
            inventory.setItem(slot, item);
        }
    }

    
    @Override
    protected int clickHandler(@NotNull Player p, int slot) {
        int sound = 0;
        Node node = children[slot];
        if (node != null) {
            // lock = fail
            if (node.locked()) {
                sound = 2;
            } else {
                // test click
                boolean succ = node.click(p);
                sound = succ ? 1 : 2;

                // check click was valid
                if (node instanceof ValuedNode vnode && !check.test(Node.cfg)) {
                    vnode.undo();
                    sound = 2;
                }
            }
        }

        updateSlot(slot);
        return sound;
    }

    /**
     * Updates the {@link ItemStack} of the specified child of the inventory.
     * @param slot the slot
     */
    protected void updateSlot(int slot) {
        if (children[slot] != null) {
            inventory.setItem(slot, children[slot].itemStack());
        }
    }

    public Node[] getChildren() {
        return children;
    }

    /**
     * @param name node name of child
     * @return the direct descendant who has the name
     */
    private Optional<Node> findChild(String name) {
        return Arrays.stream(children)
            .filter(Objects::nonNull)
            .filter(c -> Objects.equals(c.nodeName, name))
            .findAny();
    }

    /**
     * @param path path of child relative to this node
     * @return the descendant with the matching path
     */
    public Optional<Node> findDescendant(String path) {
        return Path.of(path).traverse(this, BranchNode::findChild);
    }
    
    /**
     * Traverses the tree of node to find the descendant with a matching inventory
     * @param inventory the inventory
     * @return the node (null if absent)
     */
    public @Nullable InventoryNode getNodeFromInventory(Inventory inventory) {
        if (this.inventory == inventory) {
            return this;
        }

        for (Node child : this.children) {
            if (child instanceof InventoryNode iChild) {
                if (iChild.inventory == inventory) {
                    return iChild;
                }

                if (child instanceof BranchNode bChild) {
                    InventoryNode check = bChild.getNodeFromInventory(inventory);
                    if (check != null) {
                        return check;
                    }
                }
            }
        }
        return null;
    }
}
