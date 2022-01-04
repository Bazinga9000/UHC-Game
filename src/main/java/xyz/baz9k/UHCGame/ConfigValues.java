package xyz.baz9k.UHCGame;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import xyz.baz9k.UHCGame.util.Path;

public class ConfigValues {
    private final UHCGamePlugin plugin;
    private final FileConfiguration cfg;

    public ConfigValues(UHCGamePlugin plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfig();
    }

    public Object get(String path) {
        return cfg.get(path);
    }

    /**
     * @param wbName name of world border to get diameter of
     * @return the diameter of the specified world border
     */
    public OptionalDouble wbDiameter(String wbName) {
        String path = Path.join("wb_size", wbName);
        if (cfg.isDouble(path)) {
            return OptionalDouble.of(cfg.getDouble(path));
        }
        return OptionalDouble.empty();
    }

    /**
     * @param wbName name of world border to get diameter of
     * @param def default value if there's no valid config present
     * @return the diameter of the specified world border
     */
    public double wbDiameter(String wbName, double def) {
        return cfg.getDouble(Path.join("wb_size", wbName), def);
    }

    /**
     * @param cfgStage name of stage (as it is labeled in config)
     * @return the duration of the stage
     */
    public Optional<Duration> stageDuration(String cfgStage) {
        String path = Path.join("intervals", cfgStage);
        if (cfg.isInt(path)) {
            var dur = Duration.ofSeconds(cfg.getInt(path));
            return Optional.of(dur);
        }
        return Optional.empty();
    }

    /// GLOBAL ///

    /**
     * @return if wither bonus round is enabled
     */
    public boolean witherBonus() {
        return cfg.getBoolean("global.wither_bonus");
    }
    
    /**
     * @return if players spawn in the nether
     */
    public boolean netherSpawn() {
        return cfg.getBoolean("global.nether_spawn");
    }

    /**
     * @return
     * <p> 0: 05:00 per cycle
     * <p> 1: 10:00 per cycle
     * <p> 2: 20:00 per cycle
     * <p> 3: Always Day
     * <p> 4: Always Night
     */
    public int dnCycle() {
        return cfg.getInt("global.dn_cycle");
    }

    /**
     * @return
     * <p> 0: Spread players by teams
     * <p> 1: Spread players individually
     */
    public int spreadPlayersMethod() {
        return cfg.getInt("global.spreadplayers");
    }

        
    /**
     * @return if iron, gold, copper autosmelt when mined
     */
    public boolean autoSmelt() {
        return cfg.getBoolean("global.auto_smelt");
    }

        
    /**
     * @return if mobs auto cook when killed
     */
    public boolean autoCook() {
        return cfg.getBoolean("global.auto_cook");
    }

        
    /**
     * @return if gravel always drops flint
     */
    public boolean alwaysFlint() {
        return cfg.getBoolean("global.always_flint");
    }

        
    /**
     * @return the drop rate multiplier of apple drops
     */
    public int appleDropRate() {
        return new int[]{0, 1, 2, 4, 8}[cfg.getInt("global.apple_drop_rate")];
    }

        
    /**
     * @return if shearing drops apples
     */
    public boolean shearApple() {
        return cfg.getBoolean("global.shear_apple");
    }

        
    /**
     * @return if all leaves drop apples
     */
    public boolean allLeaves() {
        return cfg.getBoolean("global.all_leaves");
    }
    /// TEAMS ///

    /**
     * @return
     * <p>0: Display all teams
     * <p>1: Display only your team
     * <p>2: Do not display teams
     */
    public int hideTeams() {
        return cfg.getInt("teams.hide_teams");
    }

    /**
     * @return whether friendly fire is enabled or not
     */
    public boolean allowFriendlyFire() {
        return cfg.getBoolean("team.friendly_fire");
    }

    public record BossMode(boolean enabled, int nPlayers, int bossHealth) {
        public BossMode {
            if (!enabled) {
                nPlayers = 0;
                bossHealth = 0;
            }
        }
        public static BossMode disabled() { return new BossMode(false, 0, 0); };
    };
    
    /**
     * @return a record of all information relating to boss mode.
     * <p> If boss mode is enabled
     * <p> If enabled, the number of players in the boss team (0 if disabled)
     * <p> If enabled, the health of players in boss team (0 if disabled)
     */
    public BossMode bossMode() {
        int bossN = cfg.getInt("team.boss_team");
        
        int normalHealth = maxHealth();
        int normalN = plugin.getTeamManager().getCombatantsOnTeam(2).size();
        
        int bossHealth = bossN > 0 ? normalHealth * normalN / bossN : 0;
        return new BossMode(bossN > 0, bossN, bossHealth);
    }

    /**
     * @return if sardines is enabled
     */
    public boolean sardines() {
        return cfg.getBoolean("team.sardines");
    }

    /// PLAYER ///

    /**
     * @return amount of health to assign (based on player.max_health)
     */
    public int maxHealth() {
        return new int[]{10, 20, 40, 60}[cfg.getInt("player.max_health")];
    }

    /**
     * @return speed of players (based on player.mv_speed)
     */
    public double movementSpeed() {
        return new double[]{0.5,1,2,3}[cfg.getInt("player.mv_speed")];
    }

    /**
     * @return length of time before grace period ends, or empty if grace period is not enabled
     */
    public Optional<Duration> gracePeriod() {
        int grace = cfg.getInt("player.grace_period");

        if (grace < 0) return Optional.empty();
        return Optional.of(Duration.ofSeconds(grace));
    }

    /**
     * @return length of time before final heal occurs, or empty if final heal is not enabled
     */
    public Optional<Duration> finalHealPeriod() {
        int fh = cfg.getInt("player.final_heal");

        if (fh < 0) return Optional.empty();
        return Optional.of(Duration.ofSeconds(fh));
    }

    /**
     * @return if natural regen is enabled
     */
    public boolean naturalRegen() {
        return cfg.getBoolean("player.natural_regen");
    }

    /**
     * @return if drowning damage is enabled
     */
    public boolean drowningDamage() {
        return cfg.getBoolean("player.drowning_damage");
    }

    /**
     * @return if fall damage is enabled
     */
    public boolean fallDamage() {
        return cfg.getBoolean("player.fall_damage");
    }

    /**
     * @return if fire damage is enabled
     */
    public boolean fireDamage() {
        return cfg.getBoolean("player.fire_damage");
    }

    /**
     * @return if freeze damage is enabled
     */
    public boolean freezeDamage() {
        return cfg.getBoolean("player.freeze_damage");
    }

    /**
     * @return the level of Hasty Boys, if enabled
     */
    public OptionalInt hastyBoys() {
        int v = cfg.getInt("player.hasty_boys");
        
        if (v > 0) return OptionalInt.of(v);
        return OptionalInt.empty();
    }

    /**
     * @return the level set for Lucky Boys, if enabled
     */
    public OptionalInt luckyBoys() {
        int v = cfg.getInt("player.lucky_boys");
        
        if (v > 0) return OptionalInt.of(v);
        return OptionalInt.empty();
    }

    /**
     * @return if compasses track nearest non-team player
     */
    public boolean proxTrack() {
        return cfg.getBoolean("player.prox_track");
    }

    /**
     * @param p Player who just died
     * @return extra item stack to give on player death
     */
    public Optional<ItemStack> playerDrops(Player p) {
        return switch (cfg.getInt("player.player_drops")) {
            case 1  -> Optional.of(new ItemStack(Material.GOLDEN_APPLE));
            case 2  -> {
                ItemStack stack = new ItemStack(Material.PLAYER_HEAD);
                stack.editMeta(SkullMeta.class, m -> {
                    m.setOwningPlayer(p);
                });
                yield Optional.of(stack);
            }
            default -> Optional.empty();
        };
    }
}
