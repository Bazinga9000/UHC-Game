package xyz.baz9k.UHCGame.config;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import xyz.baz9k.UHCGame.UHCGame;

public abstract class Node {
    protected BranchNode parent;
    protected ItemStack itemStack;
    protected List<Component> itemDesc;
    protected static UHCGame plugin;
    protected static FileConfiguration cfg;
    protected int parentSlot;
    
    /**
     * This text style (color & formatting) will be used in the description by default
     */
    public static final Style DEFAULT_LORE_STYLE = Style.style(NamedTextColor.DARK_GRAY);

    /**
     * @param parent Parent node
     * @param parentSlot lot of this node in parent's inventory
     * @param item Item stack of this node in parent's inventory
     */
    public Node(BranchNode parent, int parentSlot, ItemStack item) {
        this.parent = parent;

        this.itemStack = item;

        // Creates a copy of the lore. 
        // This is necessary because ValuedNodes use the original desc as a template, so they must use this to update their descs.
        ItemMeta m = item.getItemMeta();
        this.itemDesc = m.hasLore() ? m.lore() : new ArrayList<>();

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

    /**
     * Formats a {@link String} with the default description color style.
     * @param s
     * @return {@link Component}
     * @see #DEFAULT_LORE_STYLE
     */
    public static Component withDefaultDescStyle(String s) {
        return Component.text(s, DEFAULT_LORE_STYLE);
    }

    /**
     * Action to run when this node is clicked in parent's inventory
     * @param p Player who clicked the node
     */
    public abstract void click(@NotNull Player p);
}
