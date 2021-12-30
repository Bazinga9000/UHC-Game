package xyz.baz9k.UHCGame.menu;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.kyori.adventure.text.*;
import net.kyori.adventure.text.format.TextColor;
import xyz.baz9k.UHCGame.Kit;
import xyz.baz9k.UHCGame.UHCGamePlugin;
import xyz.baz9k.UHCGame.util.Debug;

import static xyz.baz9k.UHCGame.menu.NodeItemStack.*;
import static xyz.baz9k.UHCGame.util.Utils.*;
import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

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
            new ItemProperties<List<String>>(l -> l.size() == 0 ? Material.IRON_SWORD : Material.NETHERITE_SWORD)
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
            new ItemProperties<List<String>>(l -> l.size() == 0 ? Material.IRON_SHOVEL : Material.NETHERITE_SHOVEL)
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
            new ItemProperties<>(Material.APPLE), 
            p -> {
            p.closeInventory();
            plugin.getWorldManager().reseedWorlds();
            }
        ).lock(plugin.getGameManager()::hasUHCStarted);
        new ActionNode(ctrlRoot, slotAt(3, 2), "debug_toggle",
            new ItemProperties<Boolean>(d -> d ? Material.GLOWSTONE : Material.BLACKSTONE)
                .useObject(Debug::isDebugging)
                .extraLore(ExtraLore.fromBool()),
            p -> {
                Debug.setDebug(!Debug.isDebugging());
            }
        );
        new ActionNode(ctrlRoot, slotAt(3, 3), "stage_next", 
            new ItemProperties<>(Material.SUNFLOWER), 
            p -> {
                plugin.getGameManager().incrementStage();
            }
        );

        new ActionNode(ctrlRoot, slotAt(4, 1), "assign_teams_x", 
            new ItemProperties<Void>(Material.DIAMOND)
                .extraLore(o -> {
                    var tm = plugin.getTeamManager();
                    int n_combs = tm.getCombatants().size();
                    int n_specs = tm.getSpectators().size();
                    return new ExtraLore(
                        new Key("menu.inv.assign_teams_x.extra_lore"), n_combs, n_specs
                    );
                }),
            p -> {
                var tm = plugin.getTeamManager();
                new ValueRequest(plugin, p, ValueRequest.Type.NUMBER_REQUEST, "team_count", t -> {
                    tm.setNumTeams((int) t);
                    tm.assignTeams();
                    tm.announceTeams();
                }, false);
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
                new ItemProperties<>(mats[i - 1]), 
                p -> {
                    p.closeInventory();
                    var tm = plugin.getTeamManager();
                    tm.setTeamSize(n);
                    tm.assignTeams();
                    tm.announceTeams();
                }
            ).lock(plugin.getGameManager()::hasUHCStarted);
        }
        new ActionNode(ctrlRoot, slotAt(4, 7), "clear_teams",
                new ItemProperties<>(Material.BLACK_DYE),
                p -> {
                    p.closeInventory();
                    var tm = plugin.getTeamManager();
                    tm.resetAllPlayers();
                }
        ).lock(plugin.getGameManager()::hasUHCStarted);

        return ctrlRoot;
    }

    private BranchNode createConfigBranch(BranchNode root) {
        // TODO bind these onto game
        BranchNode cfgRoot = new BranchNode(root, slotAt(3, 7), "config", new ItemProperties<>(Material.GOLDEN_PICKAXE), 4)
            .lock(plugin.getGameManager()::hasUHCStarted);
        ValuedNode.cfgRoot = cfgRoot;

        BranchNode intervals = new BranchNode(cfgRoot, slotAt(1, 3), "intervals",  new ItemProperties<>(Material.CLOCK),                   3);
        BranchNode wbSize    = new BranchNode(cfgRoot, slotAt(1, 5), "wb_size",    new ItemProperties<>(Material.BLUE_STAINED_GLASS_PANE), 3);

        BranchNode globalSettings = new BranchNode(cfgRoot, slotAt(2, 2), "global",  new ItemProperties<>(Material.GRASS_BLOCK), 6);
        BranchNode teamSettings   = new BranchNode(cfgRoot, slotAt(2, 3), "team",    new ItemProperties<>(Material.COOKED_BEEF), 6);
        BranchNode playerSettings = new BranchNode(cfgRoot, slotAt(2, 4), "player",  new ItemProperties<>(Material.PLAYER_HEAD), 6);
        BranchNode kitSettings    = new BranchNode(cfgRoot, slotAt(2, 5), "kit",     new ItemProperties<>(Material.GOLDEN_SWORD), 6);
        BranchNode presetSettings = new BranchNode(cfgRoot, slotAt(2, 6), "presets", new ItemProperties<>(Material.ACACIA_BOAT), 6);
        
        /* INTERVALS (in secs) */
        new ValuedNode(intervals, slotAt(1, 2), "start",     new ItemProperties<>(v -> (int) v == 0 ? Material.BLACK_CONCRETE : Material.RED_CONCRETE)   .formatter(i -> getTimeString((int) i)), ValuedNode.Type.INTEGER, i -> clamp(0, i.intValue(), 7200));
        new ValuedNode(intervals, slotAt(1, 3), "movement1", new ItemProperties<>(v -> (int) v == 0 ? Material.BLACK_CONCRETE : Material.ORANGE_CONCRETE).formatter(i -> getTimeString((int) i)), ValuedNode.Type.INTEGER, i -> clamp(0, i.intValue(), 7200));
        new ValuedNode(intervals, slotAt(1, 4), "stop",      new ItemProperties<>(v -> (int) v == 0 ? Material.BLACK_CONCRETE : Material.YELLOW_CONCRETE).formatter(i -> getTimeString((int) i)), ValuedNode.Type.INTEGER, i -> clamp(0, i.intValue(), 7200));
        new ValuedNode(intervals, slotAt(1, 5), "movement2", new ItemProperties<>(v -> (int) v == 0 ? Material.BLACK_CONCRETE : Material.GREEN_CONCRETE) .formatter(i -> getTimeString((int) i)), ValuedNode.Type.INTEGER, i -> clamp(0, i.intValue(), 7200));
        new ValuedNode(intervals, slotAt(1, 6), "dmwait",    new ItemProperties<>(v -> (int) v == 0 ? Material.BLACK_CONCRETE : Material.BLUE_CONCRETE)  .formatter(i -> getTimeString((int) i)), ValuedNode.Type.INTEGER, i -> clamp(0, i.intValue(), 7200));

        /* WB SIZE (diameter) */
        new ValuedNode(wbSize, slotAt(1, 2), "initial",    new ItemProperties<>(Material.RED_STAINED_GLASS),    ValuedNode.Type.DOUBLE, d -> clamp(0, d.doubleValue(), 60000000));
        new ValuedNode(wbSize, slotAt(1, 3), "border1",    new ItemProperties<>(Material.ORANGE_STAINED_GLASS), ValuedNode.Type.DOUBLE, d -> clamp(0, d.doubleValue(), 60000000));
        new ValuedNode(wbSize, slotAt(1, 5), "border2",    new ItemProperties<>(Material.GREEN_STAINED_GLASS),  ValuedNode.Type.DOUBLE, d -> clamp(0, d.doubleValue(), 60000000));
        new ValuedNode(wbSize, slotAt(1, 6), "deathmatch", new ItemProperties<>(Material.PURPLE_STAINED_GLASS), ValuedNode.Type.DOUBLE, d -> clamp(0, d.doubleValue(), 60000000));

        /* GLOBAL SETTINGS */
        int i = 0;
        new ValuedNode(globalSettings, i++, "wither_bonus",   new ItemProperties<>(Material.WITHER_SKELETON_SKULL).style(TextColor.color(0x503754)), ValuedNode.Type.BOOLEAN);
        new ValuedNode(globalSettings, i++, "nether_spawn",   new ItemProperties<>(Material.NETHERRACK).style(TextColor.color(0x9C4040)), ValuedNode.Type.BOOLEAN);
        new OptionValuedNode(globalSettings, i++, "dn_cycle", new ItemProperties<>().style(TextColor.color(0xFFEB85)),
            Material.CLOCK,
            Material.COMPASS,
            Material.SNOWBALL,
            Material.GLOWSTONE,
            Material.LIGHT_GRAY_CONCRETE
        );
        new OptionValuedNode(globalSettings, i++, "spreadplayers", new ItemProperties<>(),
            Material.WHEAT_SEEDS,
            Material.WHEAT
        );

        /* TEAM SETTINGS */
        i = 0;
        new OptionValuedNode(teamSettings, i++, "hide_teams", new ItemProperties<>(),
            Material.RED_STAINED_GLASS,
            Material.RED_TERRACOTTA,
            Material.RED_CONCRETE
        );
        new ValuedNode(teamSettings, i++, "friendly_fire", new ItemProperties<>(Material.FLINT_AND_STEEL).style(TextColor.color(0xFF9F5F)), ValuedNode.Type.BOOLEAN);
        new ValuedNode(teamSettings, i++, "boss_team",     new ItemProperties<>(Material.DRAGON_HEAD).style(TextColor.color(0xA100FF)), ValuedNode.Type.BOOLEAN);
        new ValuedNode(teamSettings, i++, "sardines",      new ItemProperties<>(Material.DRAGON_HEAD).style(TextColor.color(0xFFBC70)), ValuedNode.Type.BOOLEAN);

        /* PLAYER SETTINGS */
        // TODO, add limits to grace_period, final_heal, and also show disabled, etc etc
        i = 0;
        new OptionValuedNode(playerSettings, i++, "max_health", new ItemProperties<>().style(TextColor.color(0xFF2121)),
            Material.SPIDER_EYE,
            Material.APPLE,
            Material.GOLDEN_APPLE,
            Material.ENCHANTED_GOLDEN_APPLE
        );
        new OptionValuedNode(playerSettings, i++, "mv_speed", new ItemProperties<>().style(TextColor.color(0x61A877)),
            Material.SOUL_SAND,
            Material.GRASS_BLOCK,
            Material.ICE,
            Material.EMERALD_BLOCK
        );
        new ValuedNode(playerSettings, i++, "grace_period",  new ItemProperties<>(Material.SHIELD), ValuedNode.Type.INTEGER);
        new ValuedNode(playerSettings, i++, "final_heal",    new ItemProperties<>(Material.GLOW_BERRIES), ValuedNode.Type.INTEGER);
        new ValuedNode(playerSettings, i++, "natural_regen", new ItemProperties<>(Material.CARROT), ValuedNode.Type.BOOLEAN);

        /* KIT SETTINGS */
        i = 0;
        new ActionNode(kitSettings, i++, "none", 
            new ItemProperties<Integer>(Material.BARRIER).style(TextColor.color(0xFFFFDF)), 
            p -> {
                plugin.getGameManager().kit(Kit.none());
            });
        new ActionNode(kitSettings, i++, "gone_fishing", 
            new ItemProperties<>(Material.FISHING_ROD).style(TextColor.color(0x3730FF)), 
            p -> {
                // Each player receives:
                // - An unbreakable maxed Fishing Rod
                // - 64 Anvils
                // - 1000 Levels
                List<ItemStack> storage = new ArrayList<>();
                storage.add(new ItemStack(Material.ANVIL, 64));

                ItemStack rod = new ItemStack(Material.FISHING_ROD);
                rod.addEnchantments(Map.ofEntries(
                    Map.entry(Enchantment.LURE, 5),
                    Map.entry(Enchantment.LUCK, 84)
                ));
                rod.editMeta(m -> m.setUnbreakable(true));
                storage.add(rod);

                plugin.getGameManager().kit(new Kit(storage.toArray(ItemStack[]::new), new ItemStack[4], null, 1000));
            });
        new ActionNode(kitSettings, i++, "always_elytra", 
            new ItemProperties<>(Material.ELYTRA).style(TextColor.color(0xB5B8FF)), 
            p -> {
                // Everyone is in a flying state 
                // for the entire game.
                // Each player receives:
                //     - Unbreakable Elytra w/ Curse of Binding
                ItemStack elytra = new ItemStack(Material.ELYTRA);
                elytra.editMeta(m -> m.setUnbreakable(true));
                elytra.addEnchantment(Enchantment.BINDING_CURSE, 1);
                ItemStack[] armor = new ItemStack[] {
                    null, elytra, null, null
                };
                plugin.getGameManager().kit(new Kit(new ItemStack[0], armor, null, 0));
            });
        new ActionNode(kitSettings, i++, "bomberman", 
            new ItemProperties<>(Material.GUNPOWDER).style(TextColor.color(0x800000)), 
            p -> {
                // Direct combat is disabled.
                // Each player receives:
                //     - 128 TNT
                // TODO bomberman "Direct combat is disabled."

                List<ItemStack> storage = new ArrayList<>();
                storage.add(new ItemStack(Material.TNT, 64));
                storage.add(new ItemStack(Material.TNT, 64));

                ItemStack fns = new ItemStack(Material.FLINT_AND_STEEL);
                fns.editMeta(m -> m.setUnbreakable(true));
                storage.add(fns);

                plugin.getGameManager().kit(new Kit(storage.toArray(ItemStack[]::new), new ItemStack[4], null, 0));
            });
        // TODO custom
        new ActionNode(kitSettings, i++, "custom", 
            new ItemProperties<>(Material.DIAMOND_PICKAXE).style(TextColor.color(0x7FCFCF)), 
            p -> {
                
            });

        /* PRESETS */
        // TODO, add more presets
        i = 0;
        new ActionNode(presetSettings, i++, "normal", new ItemProperties<>(), p -> {});
    
         return cfgRoot;
    }
}
