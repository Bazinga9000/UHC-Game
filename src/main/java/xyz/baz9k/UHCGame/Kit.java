package xyz.baz9k.UHCGame;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

public record Kit(ItemStack[] storage, ItemStack[] armor, ItemStack offhand, int xpLevels) implements ConfigurationSerializable {
    static {
        ConfigurationSerialization.registerClass(Kit.class);
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return Map.ofEntries(
            Map.entry("storage",  Objects.requireNonNullElse(storage, new ItemStack[0])),
            Map.entry("armor",    Objects.requireNonNullElse(armor,   new ItemStack[4])),
            Map.entry("offhand",  Objects.requireNonNullElse(offhand, new ItemStack(Material.AIR))),
            Map.entry("xpLevels", xpLevels)
        );
    }

    @SuppressWarnings("unchecked")
    public static Kit deserialize(Map<String, Object> map) {
        var storage  = (List<ItemStack>) map.get("storage");
        var armor    = (List<ItemStack>) map.get("armor");
        var offhand  = (ItemStack) map.get("offhand");
        var xpLevels = (int) map.get("xpLevels");

        if (offhand.getType() == Material.AIR) offhand = null;
        return new Kit(
            storage.toArray(ItemStack[]::new), 
            armor.toArray(ItemStack[]::new), 
            offhand, 
            xpLevels
        );
    }


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
