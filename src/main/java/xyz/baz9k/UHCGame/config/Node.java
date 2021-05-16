package xyz.baz9k.UHCGame.config;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import xyz.baz9k.UHCGame.UHCGame;

public abstract class Node {
    protected BranchNode parent;
    protected ItemStack itemStack;
    protected List<String> itemDesc;
    protected static UHCGame plugin;
    protected static FileConfiguration cfg;
    protected int parentSlot;

    public Node(BranchNode parent, int parentSlot, ItemStack item) {
        this.parent = parent;

        this.itemStack = item;

        // Creates a copy of the lore. 
        // This is necessary because ValuedNodes use the original desc as a template, so they must use this to update their descs.
        ItemMeta m = item.getItemMeta();
        this.itemDesc = m.hasLore() ? m.getLore() : new ArrayList<>();

        this.parentSlot = parentSlot;
        if (parent != null) {
            parent.setChild(parentSlot, this);
        }
    }

    /**
     * Set the plugin for all plugins to use the config of
     * @param plugin The plugin
     */
    public static void setPlugin(UHCGame plugin) {
        Node.plugin = plugin;
        Node.cfg = plugin.getConfig();
    }

    public abstract void click(@NotNull Player p);
}
