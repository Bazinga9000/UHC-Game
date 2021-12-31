package xyz.baz9k.UHCGame.menu;

import org.bukkit.Sound;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     * @param props {@link NodeItemStack.ItemProperties}
     * @param guiHeight Number of rows in this node's inventory
     */
    public BranchNode(@Nullable BranchNode parent, int slot, String nodeName, NodeItemStack.ItemProperties<?> props, int guiHeight) {
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
        for (int i = 0; i < slotCount; i++) {
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
            ItemStack item = child != null ? child.itemStack() : emptyGlass();
            inventory.setItem(slot, item);
        }
    }

    /**
     * Handles what happens when a player clicks the item in the slot in this node's inventory.
     * @param p Player who clicked the node
     * @param slot The slot of the clicked node
     */
    @Override
    public void onClick(@NotNull Player p, int slot) {
        Objects.checkIndex(0, slotCount);

        // 0 = nothing
        // 1 = success
        // 2 = failure
        int sound = 0; 

        // if not root, add go back trigger
        if (parent != null && slot == slotCount - 1) {
            parent.click(p);
            sound = 1;
        } else {
            Node node = children[slot];
            if (node != null) {
                // lock = fail
                if (node.locked()) {
                    sound = 2;
                } else {
                    // test click
                    node.click(p);
                    sound = 1;

                    // check click was valid
                    if (node instanceof ValuedNode vnode && !check.test(Node.cfg)) {
                        vnode.undo();
                        sound = 2;
                    }
                }
            }

            updateSlot(slot);
        }

        
        switch (sound) {
            case 1 -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, 2);
            case 2 -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_COW_BELL, 0.5f, 0);
            default -> {}
        }
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

    private Optional<Node> findChild(String name) {
        return Arrays.stream(children)
            .filter(Objects::nonNull)
            .filter(c -> c.nodeName.equals(name))
            .findAny();
    }

    public Optional<Node> findDescendant(String path) {
        if (path.isBlank()) return Optional.of(this);

        String[] nodeNames = path.split("\\.");
        BranchNode n = this;

        for (int i = 0; i < nodeNames.length - 1; i++) {
            String nodeName = nodeNames[i];
            var match = n.findChild(nodeName);
            if (match.isPresent() && match.get() instanceof BranchNode b) {
                    n = b;
                    continue;
            }
            return Optional.empty();
        }

        return n.findChild(nodeNames[nodeNames.length - 1]);
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
