package xyz.baz9k.UHCGame;

import java.util.Map;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public record Kit(ItemStack[] storage, ItemStack[] armor, ItemStack offhand, int xpLevels) {
    public void apply(Player p) {
        PlayerInventory inv = p.getInventory();
        // set armor and offhand
        inv.setArmorContents(armor);
        inv.setItemInOffHand(offhand);

        // give player storage contents
        Map<Integer, ItemStack> excess = inv.addItem(storage);

        World w = p.getWorld();
        Location l = p.getLocation();
        excess.values().forEach(stack -> w.dropItem(l, stack));

        // give exp
        p.setLevel(xpLevels);
    };

    public static Kit none() {
        return new Kit(new ItemStack[0], new ItemStack[4], null, 0);
    }
}
