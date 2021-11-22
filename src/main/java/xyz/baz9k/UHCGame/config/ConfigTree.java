package xyz.baz9k.UHCGame.config;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;

import net.kyori.adventure.text.format.TextColor;
import xyz.baz9k.UHCGame.UHCGamePlugin;

import static xyz.baz9k.UHCGame.util.Utils.*;
import static xyz.baz9k.UHCGame.config.NodeItemStack.ItemProperties;

/**
 * Setup for the config GUI tree
 */
public class ConfigTree {
    private BranchNode root;
    private UHCGamePlugin plugin;

    private static final int ROOT_GUI_HEIGHT = 3;

    public ConfigTree(UHCGamePlugin plugin) {
        this.plugin = plugin;
        Node.setPlugin(plugin);
        root = generateTree();
    }

    private static int slotAt(int row, int col) {
        return row * 9 + col;
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
        BranchNode root = new BranchNode(ROOT_GUI_HEIGHT);

        BranchNode intervals = new BranchNode(root, slotAt(1, 2), "intervals",  new ItemProperties(Material.CLOCK),                   3);
        BranchNode wbSize    = new BranchNode(root, slotAt(1, 3), "wb_size",    new ItemProperties(Material.BLUE_STAINED_GLASS_PANE), 3);
        BranchNode teamCount = new BranchNode(root, slotAt(1, 5), "team_count", new ItemProperties(Material.PLAYER_HEAD),             3);
        BranchNode esoterics = new BranchNode(root, slotAt(1, 6), "esoteric",   new ItemProperties(Material.NETHER_STAR),             6);

        /* INTERVALS (in secs) */
        new ValuedNode(intervals, slotAt(1, 2), "start",     new ItemProperties(v -> (int) v == 0 ? Material.BLACK_CONCRETE : Material.RED_CONCRETE)   .formatter(i -> getTimeString((int) i)), ValuedNode.Type.INTEGER, i -> clamp(0, i.intValue(), 7200));
        new ValuedNode(intervals, slotAt(1, 3), "movement1", new ItemProperties(v -> (int) v == 0 ? Material.BLACK_CONCRETE : Material.ORANGE_CONCRETE).formatter(i -> getTimeString((int) i)), ValuedNode.Type.INTEGER, i -> clamp(0, i.intValue(), 7200));
        new ValuedNode(intervals, slotAt(1, 4), "stop",      new ItemProperties(v -> (int) v == 0 ? Material.BLACK_CONCRETE : Material.YELLOW_CONCRETE).formatter(i -> getTimeString((int) i)), ValuedNode.Type.INTEGER, i -> clamp(0, i.intValue(), 7200));
        new ValuedNode(intervals, slotAt(1, 5), "movement2", new ItemProperties(v -> (int) v == 0 ? Material.BLACK_CONCRETE : Material.GREEN_CONCRETE) .formatter(i -> getTimeString((int) i)), ValuedNode.Type.INTEGER, i -> clamp(0, i.intValue(), 7200));
        new ValuedNode(intervals, slotAt(1, 6), "dmwait",    new ItemProperties(v -> (int) v == 0 ? Material.BLACK_CONCRETE : Material.BLUE_CONCRETE)  .formatter(i -> getTimeString((int) i)), ValuedNode.Type.INTEGER, i -> clamp(0, i.intValue(), 7200));

        /* WB SIZE (diameter) */
        new ValuedNode(wbSize, slotAt(1, 2), "initial",    new ItemProperties(Material.RED_STAINED_GLASS),    ValuedNode.Type.DOUBLE, d -> clamp(0, d.doubleValue(), 60000000));
        new ValuedNode(wbSize, slotAt(1, 3), "border1",    new ItemProperties(Material.ORANGE_STAINED_GLASS), ValuedNode.Type.DOUBLE, d -> clamp(0, d.doubleValue(), 60000000));
        new ValuedNode(wbSize, slotAt(1, 5), "border2",    new ItemProperties(Material.GREEN_STAINED_GLASS),  ValuedNode.Type.DOUBLE, d -> clamp(0, d.doubleValue(), 60000000));
        new ValuedNode(wbSize, slotAt(1, 6), "deathmatch", new ItemProperties(Material.PURPLE_STAINED_GLASS), ValuedNode.Type.DOUBLE, d -> clamp(0, d.doubleValue(), 60000000));

        /* TEAM COUNT */
        new ValuedNode(teamCount, 0,            "team_count", new ItemProperties(Material.DIAMOND),    ValuedNode.Type.INTEGER, i -> Math.min(i.intValue(), 1));
        new ActionNode(teamCount, slotAt(2, 0), "solos",      new ItemProperties(Material.RED_DYE),    p -> { plugin.getTeamManager().setTeamSize("solos");    });
        new ActionNode(teamCount, slotAt(2, 1), "duos",       new ItemProperties(Material.ORANGE_DYE), p -> { plugin.getTeamManager().setTeamSize("duos");     });
        new ActionNode(teamCount, slotAt(2, 2), "trios",      new ItemProperties(Material.YELLOW_DYE), p -> { plugin.getTeamManager().setTeamSize("trios");    });
        new ActionNode(teamCount, slotAt(2, 3), "quartets",   new ItemProperties(Material.GREEN_DYE),  p -> { plugin.getTeamManager().setTeamSize("quartets"); });
        new ActionNode(teamCount, slotAt(2, 4), "quintets",   new ItemProperties(Material.BLUE_DYE),   p -> { plugin.getTeamManager().setTeamSize("quintets"); });

        /* ESOTERICS */
        int i = 0;
        new ValuedNode(esoterics, i++, "gone_fishing",  new ItemProperties(Material.FISHING_ROD)          .style(TextColor.color(0x3730FF)), ValuedNode.Type.BOOLEAN);
        new ValuedNode(esoterics, i++, "boss_team",     new ItemProperties(Material.DRAGON_HEAD)          .style(TextColor.color(0xA100FF)), ValuedNode.Type.BOOLEAN);
        new ValuedNode(esoterics, i++, "always_elytra", new ItemProperties(Material.ELYTRA)               .style(TextColor.color(0xB5B8FF)), ValuedNode.Type.BOOLEAN);
        new ValuedNode(esoterics, i++, "sardines",      new ItemProperties(Material.TROPICAL_FISH)        .style(TextColor.color(0xFFBC70)), ValuedNode.Type.BOOLEAN);
        new ValuedNode(esoterics, i++, "wither_bonus",  new ItemProperties(Material.WITHER_SKELETON_SKULL).style(TextColor.color(0x503754)), ValuedNode.Type.BOOLEAN);
        new OptionValuedNode(esoterics, i++, "dn_cycle", new ItemProperties().style(TextColor.color(0xFFEB85)),
            Material.CLOCK,
            Material.COMPASS,
            Material.SNOWBALL,
            Material.GLOWSTONE,
            Material.LIGHT_GRAY_CONCRETE
        );
        new ValuedNode(esoterics, i++, "nether_spawn",  new ItemProperties(Material.NETHERRACK).style(TextColor.color(0x9C4040)), ValuedNode.Type.BOOLEAN);
        new ValuedNode(esoterics, i++, "bomberman",     new ItemProperties(Material.GUNPOWDER) .style(TextColor.color(0x800000)), ValuedNode.Type.BOOLEAN);
        new OptionValuedNode(esoterics, i++, "max_health", new ItemProperties().style(TextColor.color(0xFF2121)),
            Material.SPIDER_EYE,
            Material.APPLE,
            Material.GOLDEN_APPLE,
            Material.ENCHANTED_GOLDEN_APPLE
        );
        new OptionValuedNode(esoterics, i++, "mv_speed", new ItemProperties().style(TextColor.color(0x61A877)),
            Material.SOUL_SAND,
            Material.GRASS_BLOCK,
            Material.ICE,
            Material.EMERALD_BLOCK
        );
        new ActionNode(esoterics, 52, "reset_to_defaults", new ItemProperties(Material.CREEPER_HEAD).style(TextColor.color(0x3BEBD3)), p -> { 
            var defaults = plugin.getConfig().getConfigurationSection("esoteric").getDefaultSection();
            for (Node n : esoterics.getChildren()) {
                if (n instanceof ValuedNode vn) {
                    vn.set(defaults.get(vn.id()));
                }
            }
         });
    
        return root;
    }
}
