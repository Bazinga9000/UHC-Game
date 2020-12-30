package xyz.baz9k.UHCGame.config;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BranchNode extends Node {
    private final int slotCount;
    private final Node[] children;
    private final @NotNull Inventory inventory;

    public BranchNode(@Nullable BranchNode parent, @Nullable ItemStack itemStack, @NotNull String guiName, int guiHeight) {
        super(parent, itemStack);
        slotCount = 9 * guiHeight;
        children = new Node[slotCount];
        inventory = Bukkit.createInventory(null, slotCount, guiName);

        if (parent != null) {
            ItemStack goBack = new ItemStack(Material.ARROW);
            goBack.getItemMeta().setDisplayName(ChatColor.RED + "Go Back");
            inventory.setItem(slotCount - 1, goBack);
        }
    }

    public void addChild(int slot, @NotNull Node node) {
        if (0 > slot || slot > (slotCount - 1)) {
            throw new IllegalArgumentException("Invalid slot (Slot cannot be negative or the final slot.)");
        }

        children[slot] = node;
        inventory.setItem(slot, node.getItemStack());
    }

    public void onClick(@NotNull Player p, int slot) {
        if (0 > slot || slot >= slotCount) {
            throw new IllegalArgumentException("Invalid slot clicked (Slot cannot be negative or greater than" + slotCount + ".)");
        }

        if (slot == slotCount - 1) {
            p.openInventory(getParent().inventory);
            return;
        }

        Node node = children[slot];
        if (node != null) {
            if (node instanceof BranchNode) {
                p.openInventory(((BranchNode) node).inventory);
            }
        }
    }

    @NotNull
    public Inventory getInventory() {
        return inventory;
    }
}
