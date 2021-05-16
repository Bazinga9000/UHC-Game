package xyz.baz9k.UHCGame.config;

import net.md_5.bungee.api.ChatColor;

import java.util.stream.IntStream;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * {@link Node} that contains an {@link Inventory}.
 * <p>
 * Each slot contains items that represent other Nodes.
 */
public class BranchNode extends Node {
    private final int slotCount;
    private final Node[] children;
    private final @NotNull Inventory inventory;

    /**
     * Create a root {@link BranchNode}.
     * @param guiName
     * @param guiHeight
     */
    public BranchNode(@NotNull String guiName, int guiHeight) {
        this(null, 0, null, guiName, guiHeight);
    }

    public BranchNode(@Nullable BranchNode parent, int slot, @Nullable ItemStack itemStack, @NotNull String guiName, int guiHeight) {
        super(parent, slot, itemStack);
        slotCount = 9 * guiHeight;

        int arrLen = parent == null ? slotCount : slotCount - 1;
        children = new Node[arrLen];

        inventory = Bukkit.createInventory(null, slotCount, guiName);
        initInventory();
    }

    private void initInventory() {
        /* If we aren't root, add a slot for the "Go Back" button */
        ItemStack emptyGlass = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta m = emptyGlass.getItemMeta();
        m.setDisplayName(ChatColor.BLACK + "");
        emptyGlass.setItemMeta(m);

        for (int i = 0; i < slotCount; i++) {
            inventory.setItem(i, emptyGlass);
        }
        if (getParent() != null) {
            ItemStack goBack = new ItemStack(Material.ARROW);

            m = goBack.getItemMeta();
            m.setDisplayName(ChatColor.RED + "Go Back");
            goBack.setItemMeta(m);

            inventory.setItem(slotCount - 1, goBack);
        }
    }

    /**
     * Set child to a slot. If child is null, the child at specified slot is removed from the slot.
     * @param slot
     * @param child
     */
    public void setChild(int slot, @Nullable Node child) {
        if (0 > slot || slot > (slotCount - 1)) {
            throw new IllegalArgumentException("Invalid slot (Slot cannot be negative or the final slot.)");
        }

        if (child == null) {
            children[slot] = null;
            inventory.setItem(slot, null);
            return;
        }
        children[slot] = child;
        inventory.setItem(slot, child.getItemStack());
    }

    /**
     * Handles what happens when a player clicks the item in the slot in this node's inventory.
     * @param p
     * @param slot
     */
    public void onClick(@NotNull Player p, int slot) {
        if (0 > slot || slot >= slotCount) {
            throw new IllegalArgumentException("Invalid slot clicked (Slot cannot be negative or greater than" + slotCount + ".)");
        }

        if (slot == children.length - 1) {
            p.openInventory(getParent().inventory);
            return;
        }

        Node node = children[slot];
        if (node != null) {
            node.click(p);
        }
    }

    public void click(Player p) {
        p.openInventory(inventory);
    }

    /**
     * Updates the {@link ItemStack} of the specified child of the inventory.
     * @param child
     */
    public void updateChild(Node child) {
        int ind = IntStream.range(0, children.length)
                .filter(i -> children[i].equals(child))
                .findFirst()
                .orElseThrow();

        inventory.setItem(ind, child.getItemStack());
    }

    @NotNull
    public Inventory getInventory() {
        return inventory;
    }

    public Node[] getChildren() {
        return children;
    }
}
