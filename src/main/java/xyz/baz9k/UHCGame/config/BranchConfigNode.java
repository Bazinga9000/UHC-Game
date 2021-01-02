package xyz.baz9k.UHCGame.config;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class BranchConfigNode extends ConfigNode {
    private final int slotCount;
    private final ConfigNode[] children;
    private final Inventory inventory;

    public BranchConfigNode(BranchConfigNode parent, ItemStack itemStack, String guiName, int guiHeight) {
        super(parent, itemStack);
        slotCount = 9 * guiHeight;
        children = new ConfigNode[slotCount];
        inventory = Bukkit.createInventory(null, slotCount, guiName);

        if (parent != null) {
            ItemStack goBack = new ItemStack(Material.ARROW);
            goBack.getItemMeta().setDisplayName(ChatColor.RED + "Go Back");
            inventory.setItem(slotCount - 1, goBack);
        }
    }

    public void addChild(int slot, ConfigNode node) {
        if (0 > slot || slot > (slotCount - 1)) {
            throw new IllegalArgumentException("Slot cannot be negative or the final slot.");
        }

        children[slot] = node;
        inventory.setItem(slot, node.getItemStack());
    }

    public void onClick(Player p, int slot) {
        if (0 > slot || slot >= slotCount) {
            throw new IllegalArgumentException("Invalid slot in onClick");
        }

        if (slot == slotCount - 1) {
            p.openInventory(getParent().inventory);
            return;
        }

        ConfigNode node = children[slot];
        if (node != null) {
            if (node instanceof BranchConfigNode) {
                p.openInventory(((BranchConfigNode) node).inventory);
            }
        }
    }

    public Inventory getInventory() {
        return inventory;
    }
}
