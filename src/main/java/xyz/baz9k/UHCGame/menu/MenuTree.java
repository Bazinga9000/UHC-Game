package xyz.baz9k.UHCGame.menu;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;

import net.kyori.adventure.text.*;
import net.kyori.adventure.text.format.TextColor;
import xyz.baz9k.UHCGame.Kit;
import xyz.baz9k.UHCGame.UHCGamePlugin;
import xyz.baz9k.UHCGame.util.Debug;
import xyz.baz9k.UHCGame.util.stack.DynItemProperties;
import xyz.baz9k.UHCGame.util.stack.StaticItemProperties;
import xyz.baz9k.UHCGame.util.stack.DynItemProperties.ExtraLore;

import static xyz.baz9k.UHCGame.util.Utils.*;
import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

import java.io.InputStreamReader;
import java.util.*;

/**
 * Setup for the config GUI tree
 */
public class MenuTree {
    private final BranchNode root;
    private final UHCGamePlugin plugin;

    public MenuTree(UHCGamePlugin plugin) {
        this.plugin = plugin;
        Node.setPlugin(plugin);

        root = createCtrlPanelBranch();
        createConfigBranch(root);
    }

    public BranchNode root() {
        return root;
    }

    private static int slotAt(int row, int col) {
        return row * 9 + col;
    }

    private YamlConfiguration loadYMLResource(String filename) {
        var resource = new InputStreamReader(plugin.getResource(filename));
        return YamlConfiguration.loadConfiguration(resource);
    }

    /**
     * Returns the node in the tree that has the specified inventory
     * @param inventory The inventory
     * @return The node (or null if absent)
     */
    public InventoryNode getNodeFromInventory(Inventory inventory) {
        return root.getNodeFromInventory(inventory);
    }
    
    private BranchNode createCtrlPanelBranch() {
        BranchNode ctrlRoot = new BranchNode(6);
        
        new ActionNode(ctrlRoot, slotAt(1, 2), "start_game",
            new DynItemProperties<List<String>>(l -> l.size() == 0 ? Material.IRON_SWORD : Material.NETHERITE_SWORD)
                .useObject(plugin.getGameManager()::checkStartPanel)
                .extraLore(checks -> {
                    var lines = new ArrayList<Component>();
                    if (checks.size() > 0) {
                        lines.add(render(new Key("err.menu.panel.check_start_failed").trans()));

                        for (var l : checks) {
                            lines.add(Component.text(l));
                        }
                    }
                    return new ExtraLore(lines);
                }
            ),
            p -> {
                p.closeInventory();
                plugin.getGameManager().startUHC(false);
            }
        );
        new ActionNode(ctrlRoot, slotAt(1, 6), "end_game",
            new DynItemProperties<List<String>>(l -> l.size() == 0 ? Material.IRON_SHOVEL : Material.NETHERITE_SHOVEL)
                .useObject(plugin.getGameManager()::checkEndPanel)
                .extraLore(checks -> {
                    var lines = new ArrayList<Component>();
                    if (checks.size() > 0) {
                        lines.add(render(new Key("err.menu.panel.check_end_failed").trans()));

                        for (var l : checks) {
                            lines.add(Component.text(l));
                        }
                    }
                    return new ExtraLore(lines);
                }
            ),
            p -> {
                p.closeInventory();
                plugin.getGameManager().endUHC(false);
            }
        );

        new ActionNode(ctrlRoot, slotAt(3, 1), "reseed_worlds", 
            new StaticItemProperties(Material.APPLE), 
            p -> {
                p.closeInventory();
                plugin.getWorldManager().reseedWorlds();
            }
        ).lock(plugin.getGameManager()::hasUHCStarted);
        new ActionNode(ctrlRoot, slotAt(3, 2), "debug_toggle",
            new DynItemProperties<Boolean>(d -> d ? Material.GLOWSTONE : Material.BLACKSTONE)
                .useObject(Debug::isDebugging)
                .extraLore(ExtraLore.fromBool()),
            p -> {
                Debug.setDebug(!Debug.isDebugging());
            }
        );
        new ActionNode(ctrlRoot, slotAt(3, 3), "stage_next", 
            new StaticItemProperties(Material.SUNFLOWER), 
            p -> {
                plugin.getGameManager().incrementStage();
            }
        );

        new ActionNode(ctrlRoot, slotAt(4, 1), "assign_teams_x", 
            new DynItemProperties<Void>(Material.DIAMOND)
                .extraLore(o -> {
                    var tm = plugin.getTeamManager();
                    int n_combs = tm.getCombatants().online().size();
                    int n_specs = tm.getSpectators().online().size();
                    return new ExtraLore(
                        new Key("menu.inv.assign_teams_x.extra_lore"), n_combs, n_specs
                    );
                }),
            p -> {
                var tm = plugin.getTeamManager();
                new ValueRequest(plugin, p, ValueRequest.Type.NUMBER_REQUEST, "team_count", t -> {
                    tm.setNumTeams((int) t);
                    // why did everything have to resolve cleanly EXCEPT for this
                    ActionNode.NodeAction ta = pl -> tm.tryAssignTeams();
                    ta.eval(p);
                });
            }
        ).lock(plugin.getGameManager()::hasUHCStarted);

        Material[] mats = new Material[]{
            Material.RED_DYE,
            Material.ORANGE_DYE,
            Material.YELLOW_DYE,
            Material.GREEN_DYE,
            Material.BLUE_DYE,
        };
        for (int i = 1; i <= 5; i++) {
            final int n = i;
            new ActionNode(ctrlRoot, slotAt(4, 1 + i), String.format("assign_teams_%s", i), 
                new StaticItemProperties(mats[i - 1]), 
                p -> {
                    p.closeInventory();
                    var tm = plugin.getTeamManager();
                    tm.setTeamSize(n);
                    tm.tryAssignTeams();
                }
            ).lock(plugin.getGameManager()::hasUHCStarted);
        }
        new ActionNode(ctrlRoot, slotAt(4, 7), "clear_teams",
                new StaticItemProperties(Material.BLACK_DYE),
                p -> {
                    p.closeInventory();
                    var tm = plugin.getTeamManager();
                    tm.resetAllPlayers();
                }
        ).lock(plugin.getGameManager()::hasUHCStarted);

        return ctrlRoot;
    }

    private BranchNode createConfigBranch(BranchNode root) {
        BranchNode cfgRoot = new BranchNode(root, slotAt(3, 7), "config", new StaticItemProperties(Material.GOLDEN_PICKAXE), 4)
            .lock(plugin.getGameManager()::hasUHCStarted);
        ValuedNode.cfgRoot = cfgRoot;

        BranchNode intervals = new BranchNode(cfgRoot, slotAt(1, 3), "intervals",  new StaticItemProperties(Material.CLOCK),                   3);
        BranchNode wbSize    = new BranchNode(cfgRoot, slotAt(1, 5), "wb_size",    new StaticItemProperties(Material.BLUE_STAINED_GLASS_PANE), 3);

        BranchNode globalSettings = new BranchNode(cfgRoot, slotAt(2, 2), "global",  new StaticItemProperties(Material.GRASS_BLOCK),  6);
        BranchNode teamSettings   = new BranchNode(cfgRoot, slotAt(2, 3), "team",    new StaticItemProperties(Material.COOKED_BEEF),  6);
        BranchNode playerSettings = new BranchNode(cfgRoot, slotAt(2, 4), "player",  new StaticItemProperties(Material.PLAYER_HEAD),  6);
        BranchNode kitSettings    = new BranchNode(cfgRoot, slotAt(2, 5), "kit",     new StaticItemProperties(Material.GOLDEN_SWORD), 6);
        BranchNode presetSettings = new BranchNode(cfgRoot, slotAt(2, 6), "presets", new StaticItemProperties(Material.ACACIA_BOAT),  6);
        
        /* INTERVALS (in secs) */
        new ValuedNode(intervals, slotAt(1, 2), "start",     new DynItemProperties<>(v -> (int) v == 0 ? Material.BLACK_CONCRETE : Material.RED_CONCRETE)   .formatArg(i -> getTimeString((int) i)), ValuedNode.Type.INTEGER, i -> clamp(0, i.intValue(), 7200));
        new ValuedNode(intervals, slotAt(1, 3), "movement1", new DynItemProperties<>(v -> (int) v == 0 ? Material.BLACK_CONCRETE : Material.ORANGE_CONCRETE).formatArg(i -> getTimeString((int) i)), ValuedNode.Type.INTEGER, i -> clamp(0, i.intValue(), 7200));
        new ValuedNode(intervals, slotAt(1, 4), "stop",      new DynItemProperties<>(v -> (int) v == 0 ? Material.BLACK_CONCRETE : Material.YELLOW_CONCRETE).formatArg(i -> getTimeString((int) i)), ValuedNode.Type.INTEGER, i -> clamp(0, i.intValue(), 7200));
        new ValuedNode(intervals, slotAt(1, 5), "movement2", new DynItemProperties<>(v -> (int) v == 0 ? Material.BLACK_CONCRETE : Material.GREEN_CONCRETE) .formatArg(i -> getTimeString((int) i)), ValuedNode.Type.INTEGER, i -> clamp(0, i.intValue(), 7200));
        new ValuedNode(intervals, slotAt(1, 6), "dmwait",    new DynItemProperties<>(v -> (int) v == 0 ? Material.BLACK_CONCRETE : Material.BLUE_CONCRETE)  .formatArg(i -> getTimeString((int) i)), ValuedNode.Type.INTEGER, i -> clamp(0, i.intValue(), 7200));

        /* WB SIZE (diameter) */
        new ValuedNode(wbSize, slotAt(1, 2), "initial",    new DynItemProperties<>(Material.RED_STAINED_GLASS),    ValuedNode.Type.DOUBLE, d -> clamp(0, d.doubleValue(), 60000000));
        new ValuedNode(wbSize, slotAt(1, 3), "border1",    new DynItemProperties<>(Material.ORANGE_STAINED_GLASS), ValuedNode.Type.DOUBLE, d -> clamp(0, d.doubleValue(), 60000000));
        new ValuedNode(wbSize, slotAt(1, 5), "border2",    new DynItemProperties<>(Material.GREEN_STAINED_GLASS),  ValuedNode.Type.DOUBLE, d -> clamp(0, d.doubleValue(), 60000000));
        new ValuedNode(wbSize, slotAt(1, 6), "deathmatch", new DynItemProperties<>(Material.PURPLE_STAINED_GLASS), ValuedNode.Type.DOUBLE, d -> clamp(0, d.doubleValue(), 60000000));

        /* GLOBAL SETTINGS */
        int i = 0;
        new ValuedNode(globalSettings, i++, "wither_bonus",   new DynItemProperties<>(Material.WITHER_SKELETON_SKULL).nameStyle(0x503754), ValuedNode.Type.BOOLEAN);
        new ValuedNode(globalSettings, i++, "nether_spawn",   new DynItemProperties<>(Material.NETHERRACK).nameStyle(0x9C4040),            ValuedNode.Type.BOOLEAN);
        new OptionValuedNode(globalSettings, i++, "dn_cycle", new DynItemProperties<>().nameStyle(0xFFEB85),
            Material.CLOCK,
            Material.COMPASS,
            Material.SNOWBALL,
            Material.GLOWSTONE,
            Material.LIGHT_GRAY_CONCRETE
        );
        new OptionValuedNode(globalSettings, i++, "spreadplayers", new DynItemProperties<>(),
            Material.WHEAT_SEEDS,
            Material.WHEAT
        );
        new ValuedNode(globalSettings, i++, "auto_smelt",   new DynItemProperties<>(Material.IRON_INGOT).nameStyle(0xCFCFCF),      ValuedNode.Type.BOOLEAN);
        new ValuedNode(globalSettings, i++, "auto_cook",    new DynItemProperties<>(Material.COOKED_PORKCHOP).nameStyle(0xF9E9C9), ValuedNode.Type.BOOLEAN);
        new ValuedNode(globalSettings, i++, "always_flint", new DynItemProperties<>(Material.FLINT).nameStyle(0x9F9F9F),           ValuedNode.Type.BOOLEAN);
        new OptionValuedNode(globalSettings, i++, "apple_drop_rate", new DynItemProperties<>().nameStyle(0x50BC50),
            Material.POISONOUS_POTATO,
            Material.OAK_SAPLING,
            Material.OAK_LEAVES,
            Material.APPLE,
            Material.GOLDEN_APPLE
        );
        new ValuedNode(globalSettings, i++, "shear_apple", new DynItemProperties<>(Material.SHEARS).nameStyle(0x9C7C40),        ValuedNode.Type.BOOLEAN);
        new ValuedNode(globalSettings, i++, "all_leaves",  new DynItemProperties<>(Material.SPRUCE_LEAVES).nameStyle(0x309C00), ValuedNode.Type.BOOLEAN);

        /* TEAM SETTINGS */
        i = 0;
        new OptionValuedNode(teamSettings, i++, "hide_teams", new DynItemProperties<>(),
            Material.RED_STAINED_GLASS,
            Material.RED_TERRACOTTA,
            Material.RED_CONCRETE
        );
        new ValuedNode(teamSettings, i++, "friendly_fire", new DynItemProperties<>(Material.FLINT_AND_STEEL).nameStyle(0xFF9F5F), ValuedNode.Type.BOOLEAN);
        new ValuedNode(teamSettings, i++, "boss_team", 
            new DynItemProperties<>(v -> (int) v == 0 ? Material.DRAGON_EGG : Material.DRAGON_HEAD)
                .nameStyle(0xA100FF) 
                .formatArg(v -> {
                    int nPlayers = (int) v;
                    if (nPlayers < 1) return new Key("menu.inv.config.presets.disabled").trans();
                    return new Key("menu.inv.team.boss_team.players").trans(nPlayers);
                }),
            ValuedNode.Type.INTEGER, 
            v -> Math.max(0, (int) v)
        );
        new ValuedNode(teamSettings, i++, "sardines", new DynItemProperties<>(Material.TROPICAL_FISH).nameStyle(0xFFBC70), ValuedNode.Type.BOOLEAN);

        /* PLAYER SETTINGS */
        i = 0;
        new OptionValuedNode(playerSettings, i++, "max_health", new DynItemProperties<>().nameStyle(0xFF2121),
            Material.SPIDER_EYE,
            Material.APPLE,
            Material.GOLDEN_APPLE,
            Material.ENCHANTED_GOLDEN_APPLE
        );
        new OptionValuedNode(playerSettings, i++, "mv_speed", new DynItemProperties<>().nameStyle(0x61A877),
            Material.SOUL_SAND,
            Material.GRASS_BLOCK,
            Material.ICE,
            Material.EMERALD_BLOCK
        );
        new ValuedNode(playerSettings, i++, "grace_period",
            new DynItemProperties<>(v -> (int) v >= 0 ? Material.SHIELD : Material.BLACK_CONCRETE)
                .formatArg(v -> {
                    int secs = (int) v;
                    if (secs < 0) return new Key("menu.inv.config.presets.disabled").trans();
                    return getTimeString(secs);
                }), 
            ValuedNode.Type.INTEGER, 
            n -> Math.max(-1, (int) n)
        );
        new ValuedNode(playerSettings, i++, "final_heal",
            new DynItemProperties<>(v -> (int) v >= 0 ? Material.GLOW_BERRIES : Material.BLACK_CONCRETE)
                .formatArg(v -> {
                    int secs = (int) v;
                    if (secs < 0) return new Key("menu.inv.config.presets.disabled").trans();
                    return getTimeString(secs);
                }), 
            ValuedNode.Type.INTEGER, 
            n -> Math.max(-1, (int) n)
        );
        new ValuedNode(playerSettings, i++, "natural_regen", new DynItemProperties<>(Material.CARROT), ValuedNode.Type.BOOLEAN);
        new ValuedNode(playerSettings, i++, "hasty_boys",
            new DynItemProperties<>(Material.GOLDEN_PICKAXE)
                .formatArg(v -> Component.translatable(String.format("enchantment.level.%s", v))),
                ValuedNode.Type.INTEGER, 
                v -> clamp(0, (int) v, 255)
        );
        new ValuedNode(playerSettings, i++, "lucky_boys",
            new DynItemProperties<>(Material.GOLD_INGOT)
                .formatArg(v -> Component.translatable(String.format("enchantment.level.%s", v))),
                ValuedNode.Type.INTEGER, 
                v -> clamp(0, (int) v, 255)
        );
        new ValuedNode(playerSettings, i++, "prox_track", new DynItemProperties<>(Material.COMPASS), ValuedNode.Type.BOOLEAN);
        new OptionValuedNode(playerSettings, i++, "player_drops", new DynItemProperties<>().nameStyle(0xDFCFAF),
            Material.SKELETON_SKULL,
            Material.ZOMBIE_HEAD,
            Material.PLAYER_HEAD
        );

        new ValuedNode(playerSettings, 18,  "drowning_damage", new DynItemProperties<>(Material.TURTLE_HELMET),      ValuedNode.Type.BOOLEAN);
        new ValuedNode(playerSettings, 19, "fall_damage",      new DynItemProperties<>(Material.FEATHER),            ValuedNode.Type.BOOLEAN);
        new ValuedNode(playerSettings, 20, "fire_damage",      new DynItemProperties<>(Material.FLINT_AND_STEEL),    ValuedNode.Type.BOOLEAN);
        new ValuedNode(playerSettings, 21, "freeze_damage",    new DynItemProperties<>(Material.POWDER_SNOW_BUCKET), ValuedNode.Type.BOOLEAN);

        /* KIT SETTINGS */
        var kitsYml = loadYMLResource("kits.yml");
        var kitPropsList = kitsYml.getMapList("kits");
        Map<String, Kit> kits = new HashMap<>();

        var kitNode = new KitNode(kitSettings, 52, "custom", 
            new DynItemProperties<>(Material.DIAMOND_PICKAXE).nameStyle(0x7FCFCF),
            kits);

        for (int j = 0; j < kitPropsList.size(); j++) {
            Map<?, ?> kitProps = kitPropsList.get(j);

            String nodeName = (String) kitProps.get("node_name");
            String matType  = (String) kitProps.get("material");
            int clrHex      = (int) kitProps.get("style_color");
            Kit kit         = (Kit) kitProps.get("kit");

            Material mat = Material.valueOf(matType);
            TextColor clr = TextColor.color(clrHex);
            kits.put(nodeName, kit);

            new ActionNode(kitSettings, j, nodeName,
                new StaticItemProperties(mat, noDeco(clr)), 
                p -> kitNode.set(nodeName)
            );
        }

        /* PRESETS */
        var presetsYml = loadYMLResource("presets.yml");
        var presetPropsList = presetsYml.getMapList("presets");

        for (int j = 0; j < presetPropsList.size(); j++) {
            Map<?, ?> presetProps = presetPropsList.get(j);

            var nodeName = (String) presetProps.get("node_name");
            var matType  = (String) presetProps.get("material");
            var clrHex   = (int) presetProps.get("style_color");
            var preset   = (Map<?, ?>) presetProps.get("preset");

            Material mat = Material.valueOf(matType);
            TextColor clr = TextColor.color(clrHex);

            new PresetNode(presetSettings, j, nodeName, 
                new DynItemProperties<>(mat).nameStyle(clr), preset
            );
        }
        
        return cfgRoot;
    }
}
