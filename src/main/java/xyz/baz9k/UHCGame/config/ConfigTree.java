package xyz.baz9k.UHCGame.config;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;
import xyz.baz9k.UHCGame.UHCGame;

import static xyz.baz9k.UHCGame.util.Utils.*;

/**
 * Setup for the config GUI tree
 */
public class ConfigTree {
    private BranchNode root;
    private UHCGame plugin;

    private static final int ROOT_GUI_HEIGHT = 3;

    public ConfigTree(UHCGame plugin) {
        this.plugin = plugin;
        Node.setPlugin(plugin);
        root = generateTree();
    }

    private static int slotAt(int row, int col) {
        return row * 9 + col;
    }

    private static ItemStack itemStack(Material type, String name, String... lore) {

        Component[] loreComps = Arrays.stream(lore)
            .map(Node::withDefaultDescStyle)
            .toArray(Component[]::new);

        return itemStack(type, Component.text(name, noDecoStyle(null)), loreComps);
    }
    private static ItemStack itemStack(Material type, Component name, Component... lore) {
        ItemStack stack = new ItemStack(type);
        ItemMeta m = stack.getItemMeta();

        m.displayName(name);
        m.lore(Arrays.asList(lore));
        
        stack.setItemMeta(m);
        return stack;
    }

    public BranchNode getRoot() {
        return root;
    }

    /**
     * Returns the node in the tree that has the specified inventory
     * @param inventory The inventory
     * @return The node (or null if absent)
     */
    public BranchNode getNodeFromInventory(Inventory inventory) {
        return scanAllChildrenForInventory(inventory, root);
    }

    /**
     * Traverses the tree of a node to find the node that has a specified inventory
     * @param inventory The inventory
     * @param node The node tree to traverse
     * @return The node (or null if absent)
     */
    private static BranchNode scanAllChildrenForInventory(Inventory inventory, BranchNode node) {
        if (node.getInventory() == inventory) {
            return node;
        }

        for (Node child : node.getChildren()) {
            if (child instanceof BranchNode bChild) {
                BranchNode check = scanAllChildrenForInventory(inventory, bChild);
                if (check != null) {
                    return check;
                }
            }
        }
        return null;
    }

    /**
     * @return the root of the tree, once built
     */
    private BranchNode generateTree() {
        BranchNode root = new BranchNode("Config", ROOT_GUI_HEIGHT);

        BranchNode intervals = new BranchNode(root, slotAt(1, 2), itemStack(Material.CLOCK, "Stage Durations", "Set the various timings between game events."),                                 "Stage Durations", 3);
        BranchNode wbSize    = new BranchNode(root, slotAt(1, 3), itemStack(Material.BLUE_STAINED_GLASS_PANE, "Worldborder Sizes", "Change the widths of the worldborders at various stages."), "Worldborder Sizes", 3);
        BranchNode teamCount = new BranchNode(root, slotAt(1, 5), itemStack(Material.PLAYER_HEAD, "Team Counts", "Set the number of teams."),                                                   "Team Counts", 3);
        BranchNode esoterics = new BranchNode(root, slotAt(1, 6), itemStack(Material.NETHER_STAR, "Esoteric", "Toggle various additional settings."),                                           "Esoteric", 6);

        /* INTERVALS (in secs) */
        new ValuedNode(intervals, slotAt(1, 2), itemStack(Material.RED_CONCRETE,    "Still Border", "Duration: %sm"),          ValuedNode.Type.INTEGER, "intervals.start");
        new ValuedNode(intervals, slotAt(1, 3), itemStack(Material.ORANGE_CONCRETE, "Border 1", "Duration: %sm"),              ValuedNode.Type.INTEGER, "intervals.movement1");
        new ValuedNode(intervals, slotAt(1, 4), itemStack(Material.YELLOW_CONCRETE, "Border Stops", "Duration: %sm"),          ValuedNode.Type.INTEGER, "intervals.stop");
        new ValuedNode(intervals, slotAt(1, 5), itemStack(Material.GREEN_CONCRETE,  "Border 2", "Duration: %sm"),              ValuedNode.Type.INTEGER, "intervals.movement2");
        new ValuedNode(intervals, slotAt(1, 6), itemStack(Material.BLUE_CONCRETE,   "Time Until Deathmatch", "Duration: %sm"), ValuedNode.Type.INTEGER, "intervals.dmwait");

        /* WB SIZE (diameter) */
        new ValuedNode(wbSize, slotAt(1, 2), itemStack(Material.RED_STAINED_GLASS,    "Initial World Border", "Diameter: %s"),    ValuedNode.Type.DOUBLE, "wbsize.initial");
        new ValuedNode(wbSize, slotAt(1, 3), itemStack(Material.ORANGE_STAINED_GLASS, "First Movement", "Diameter: %s"),          ValuedNode.Type.DOUBLE, "wbsize.border1");
        new ValuedNode(wbSize, slotAt(1, 5), itemStack(Material.GREEN_STAINED_GLASS,  "Second Movement", "Diameter: %s"),         ValuedNode.Type.DOUBLE, "wbsize.border2");
        new ValuedNode(wbSize, slotAt(1, 6), itemStack(Material.PURPLE_STAINED_GLASS, "Deathmatch World Border", "Diameter: %s"), ValuedNode.Type.DOUBLE, "wbsize.deathmatch");

        /* TEAM COUNT */
        new ValuedNode(teamCount, 0, itemStack(Material.DIAMOND, "Set Team Count", "Number of teams: %s"), ValuedNode.Type.INTEGER, "team_count");
        new ActionNode(teamCount, slotAt(2, 0), itemStack(Material.RED_DYE, "Solos", "Teams of 1"),      p -> { plugin.getTeamManager().setTeamSize("solos");    });
        new ActionNode(teamCount, slotAt(2, 1), itemStack(Material.ORANGE_DYE, "Duos", "Teams of 2"),    p -> { plugin.getTeamManager().setTeamSize("duos");     });
        new ActionNode(teamCount, slotAt(2, 2), itemStack(Material.YELLOW_DYE, "Trios", "Teams of 3"),   p -> { plugin.getTeamManager().setTeamSize("trios");    });
        new ActionNode(teamCount, slotAt(2, 3), itemStack(Material.GREEN_DYE, "Quartets", "Teams of 4"), p -> { plugin.getTeamManager().setTeamSize("quartets"); });
        new ActionNode(teamCount, slotAt(2, 4), itemStack(Material.BLUE_DYE, "Quintets", "Teams of 5"),  p -> { plugin.getTeamManager().setTeamSize("quintets"); });

        /* ESOTERICS */
        // TODO fill in esoteric descs, final action node
        new ValuedNode(esoterics, 0, itemStack(Material.DIAMOND, "Gone Fishing", ""),         ValuedNode.Type.BOOLEAN, "esoteric.gone_fishing");
        new ValuedNode(esoterics, 1, itemStack(Material.DIAMOND, "Boss Team", ""),            ValuedNode.Type.BOOLEAN, "esoteric.boss_team");
        new ValuedNode(esoterics, 2, itemStack(Material.DIAMOND, "Always Elytra", ""),        ValuedNode.Type.BOOLEAN, "esoteric.always_elytra");
        new ValuedNode(esoterics, 3, itemStack(Material.DIAMOND, "Sardines", ""),             ValuedNode.Type.BOOLEAN, "esoteric.sardines");
        new ValuedNode(esoterics, 4, itemStack(Material.DIAMOND, "Wither Bonus Round", ""),   ValuedNode.Type.BOOLEAN, "esoteric.wither_bonus");
        new ValuedNode(esoterics, 5, itemStack(Material.DIAMOND, "Mafia", ""),                ValuedNode.Type.BOOLEAN, "esoteric.mafia");
        new ValuedNode(esoterics, 6, itemStack(Material.DIAMOND, "Fast Day-Night Cycle", ""), ValuedNode.Type.BOOLEAN, "esoteric.fast_dn_cycle");
        new ValuedNode(esoterics, 7, itemStack(Material.DIAMOND, "Always Day", ""),           ValuedNode.Type.BOOLEAN, "esoteric.always_day");
        new ValuedNode(esoterics, 8, itemStack(Material.DIAMOND, "Always Night", ""),         ValuedNode.Type.BOOLEAN, "esoteric.always_night");
        new ValuedNode(esoterics, 9, itemStack(Material.DIAMOND, "Spawn in Nether", ""),      ValuedNode.Type.BOOLEAN, "esoteric.nether_spawn");
        new ValuedNode(esoterics, 10, itemStack(Material.DIAMOND, "Bomberman", ""),           ValuedNode.Type.BOOLEAN, "esoteric.bomberman");
        new OptionValuedNode(esoterics, 11, itemStack(Material.DIAMOND, "Player Health", ""), "esoteric.max_health",
            new OptionData("❤️ 05", Material.IRON_INGOT),
            new OptionData("❤️ 10", Material.GOLD_INGOT),
            new OptionData("❤️ 20", Material.DIAMOND),
            new OptionData("❤️ 30", Material.EMERALD)
        );
        new OptionValuedNode(esoterics, 12, itemStack(Material.DIAMOND, "Movement Speed", ""), "esoteric.mv_speed",
            new OptionData("0.5x", Material.IRON_INGOT),
            new OptionData("1.0x", Material.GOLD_INGOT),
            new OptionData("2.0x", Material.DIAMOND),
            new OptionData("3.0x", Material.EMERALD)
        );
        new ActionNode(esoterics, 52, itemStack(Material.CREEPER_HEAD, "Reset to Defaults", ""), p -> { Bukkit.getServer().sendMessage(Component.text("todo")); });
    
        return root;
    }
}
