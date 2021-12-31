package xyz.baz9k.UHCGame.menu;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * {@link Node} that holds an Inventory, for whatever reason.
 */
public abstract class InventoryNode extends Node {
    protected final @NotNull Inventory inventory;
    protected final int slotCount;
    protected boolean hasInventoryViewed = false;
    protected final ReserveSlots rs;

    protected record ReserveSlots(int left, int right) {
        public ReserveSlots(int left) { this(left, -1); }

        public static ReserveSlots rows(int left, int right) { return new ReserveSlots(left * 9, right * 9); }
        public static ReserveSlots all() { return new ReserveSlots(0); }
        public static ReserveSlots none() { return new ReserveSlots(0, 0); }
        
        public boolean contains(int slot) {
            boolean leftCond = (left == -1)  || (left <= slot),
                   rightCond = (right == -1) || (slot < right);
            
            return leftCond && rightCond;
        }
    }
    /**
     * @param parent Parent node
     * @param slot Slot of this node in parent's inventory
     * @param nodeName Name of the node
     * @param props {@link NodeItemStack.ItemProperties}
     * @param guiHeight Number of rows in this node's inventory
     * @param rs There are two types of slots: storage slots and reserve slots.
     * <p> Storage slots can be modified and edited, reserve slots are readonly and reserved for actions, as defined by the {@link #onClick} method.
     */
    public InventoryNode(@Nullable BranchNode parent, int slot, String nodeName, NodeItemStack.ItemProperties<?> props, int guiHeight, ReserveSlots rs) {
        super(parent, slot, nodeName, props);
        this.slotCount = 9 * guiHeight;
        this.rs = rs;

        inventory = Bukkit.createInventory(null, slotCount, NodeItemStack.nameFromID(langKey()));
    }

    /**
     * Handles what happens when a player clicks the item in the slot in this node's inventory.
     * @param p Player who clicked the node
     * @param slot The slot of the clicked node
     */
    public abstract void onClick(@NotNull Player p, int slot);
    
    protected static ItemStack emptyGlass() {
        ItemStack emptyGlass = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        emptyGlass.editMeta(m -> {
            m.displayName(Component.space());
        });
        return emptyGlass;
    }
    
    /**
     * When the inventory is first loaded, this function is run.
     */
    void initInventory() {
        hasInventoryViewed = true;

        ItemStack[] contents = inventory.getContents();
        // fill reserve slots with empty glass
        int l = rs.left(),
            r = rs.right();
        if (l == -1) l = 0;
        if (r == -1) r = slotCount;

        ItemStack empty = emptyGlass();
        Arrays.fill(contents, l, r, empty);

        // set last reserve slot to go back button
        if (rs.contains(r - 1)) {
            ItemStack goBack = new NodeItemStack("go_back", 
                new NodeItemStack.ItemProperties<>(Material.ARROW).style(NamedTextColor.RED)
            );
            
            contents[r - 1] = goBack;
        }

        inventory.setContents(contents);
    }

    /**
     * When the inventory is loaded after the first time, this function is loaded.
     */
    void updateInventory() {}

    @Override
    public void click(@NotNull Player p) {
        if (hasInventoryViewed) {
            initInventory();
        } else {
            updateInventory();
        }

        p.openInventory(inventory);
    }

    @NotNull
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Check if the slot is handled by this node's onClick handler.
     * If this slot is handled by this node's onClick handler, it is readonly and cannot be removed.
     * @param slot slot to check
     * @return boolean
     */
    public boolean handlesSlot(int slot) {
        return rs.contains(slot);
    }
}
