package xyz.baz9k.UHCGame.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * {@link Node} that contains an {@link Inventory}.
 * <p>
 * Each slot contains items that represent other Nodes.
 */
public class BranchNode extends Node {
    private final int slotCount;
    private final Node[] children;
    private final @NotNull Inventory inventory;
    private Predicate<Configuration> check = cfg -> true;

    /**
     * Create a root {@link BranchNode}. This node does not have a parent.
     * @param guiHeight Number of rows in this node's inventory
     */
    // remove tmp when impl complete
    public BranchNode(int guiHeight) {
        this(null, 0, null, null, guiHeight);
    }

    /**
     * @param parent Parent node
     * @param slot lot of this node in parent's inventory
     * @param nodeName Node name, which is used to determine the ID
     * @param props {@link NodeItemStack#ItemProperties}
     * @param guiHeight Number of rows in this node's inventory
     */
    public BranchNode(@Nullable BranchNode parent, int slot, String nodeName, NodeItemStack.ItemProperties props, int guiHeight) {
        super(parent, slot, nodeName, props);
        slotCount = 9 * guiHeight;

        int arrLen = parent == null ? slotCount : slotCount - 1;
        children = new Node[arrLen];

        inventory = Bukkit.createInventory(null, slotCount, NodeItemStack.nameFromID(langKey()));
        initInventory();
    }

    /**
     * Adds check to node that checks that the config is not in an invalid state (e.g. incompatibility),<p>
     * and undoes an event if so
     * @param check
     * @return this
     */
    public BranchNode check(Predicate<Configuration> check) {
        this.check = check;
        return this;
    }
    
    private ItemStack emptyGlass() {
        ItemStack emptyGlass = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta m = emptyGlass.getItemMeta();
        m.displayName(Component.space());
        emptyGlass.setItemMeta(m);

        return emptyGlass;
    }

    private void initInventory() {
        // add glass to all slots
        ItemStack emptyGlass = emptyGlass();
        
        for (int i = 0; i < slotCount; i++) {
            inventory.setItem(i, emptyGlass);
        }

        // If we aren't root, add a slot for the "Go Back" button
        if (parent != null) {
            ItemStack goBack = new NodeItemStack("go_back", new NodeItemStack.ItemProperties(Material.ARROW).style(NamedTextColor.RED));

            inventory.setItem(slotCount - 1, goBack);
        }
    }

    /**
     * Set child to a slot. If child is null, the child at specified slot is removed from the slot.
     * @param slot
     * @param child
     */
    public void setChild(int slot, @Nullable Node child) {
        Objects.checkIndex(slot, children.length);

        if (child == null) {
            children[slot] = null;
            inventory.setItem(slot, emptyGlass());
            return;
        }
        children[slot] = child;
        inventory.setItem(slot, child.itemStack());
    }

    /**
     * Handles what happens when a player clicks the item in the slot in this node's inventory.
     * @param p
     * @param slot
     */
    public void onClick(@NotNull Player p, int slot) {
        Objects.checkIndex(0, slotCount);

        // 0 = nothing
        // 1 = success
        // 2 = failure
        int sound = 0; 

        // if not root, add go back trigger
        if (parent != null && slot == slotCount - 1) {
            p.openInventory(parent.inventory);
            sound = 1;
        } else {
            Node node = children[slot];
            if (node != null) {
                node.click(p);
                sound = 1;
                
                if (node instanceof ValuedNode vnode && !check.test(Node.cfg)) {
                    vnode.undo(p);
                    sound = 2;
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

    public void click(Player p) {
        // update all slots to make sure each item is up to date
        updateAllSlots();
        p.openInventory(inventory);
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
    public void updateAllSlots() {
        for (int i = 0; i < children.length; i++) updateSlot(i);
    }

    @NotNull
    public Inventory getInventory() {
        return inventory;
    }

    public Node[] getChildren() {
        return children;
    }

    public Node findChild(String name) {
        for (Node n : children) {
            if (n == null) continue;
            if (n.nodeName == name) return n;
        }
        return null;
    }

    /**
     * Traverses the tree of a node to find the node that has a specified inventory
     * @param inventory The inventory
     * @param node The node tree to traverse
     * @return The node (or null if absent)
     */
    public BranchNode getNodeFromInventory(Inventory inventory) {
        if (this.inventory == inventory) {
            return this;
        }

        for (Node child : this.children) {
            if (child instanceof BranchNode bChild) {
                BranchNode check = bChild.getNodeFromInventory(inventory);
                if (check != null) {
                    return check;
                }
            }
        }
        return null;
    }
}
