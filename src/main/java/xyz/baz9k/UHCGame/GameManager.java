package xyz.baz9k.UHCGame;

import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import xyz.baz9k.UHCGame.util.*;

import java.time.*;
import java.util.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import static xyz.baz9k.UHCGame.util.Utils.*;

public class GameManager implements Listener {
    private UHCGame plugin;

    private final HashMap<UUID, Component> previousDisplayNames;
    
    private TeamManager teamManager;
    private HUDManager hudManager;
    private BossbarManager bbManager;
    private WorldManager worldManager;
    private Recipes recipes;
    private BukkitTask tick;

    private Instant startTime = null;

    private HashMap<UUID, Integer> kills = new HashMap<>();

    private GameStage stage = GameStage.NOT_IN_GAME;
    private Instant lastStageInstant = null;

    public GameManager(UHCGame plugin) {
        this.plugin = plugin;
        previousDisplayNames = new HashMap<>();
    }

    /**
     * After all managers are initialized, this function is run to give gameManager references to all other managers.
     */
    public void loadManagerRefs() {
        teamManager = plugin.getTeamManager();
        hudManager = plugin.getHUDManager();
        bbManager = plugin.getBossbarManager();
        worldManager = plugin.getWorldManager();
        recipes = plugin.getRecipes();
    }

    /**
     * Starts UHC.
     * <p>
     * Accessible through /uhc start or /uhc start force.
     * <p>
     * /uhc start: Checks that teams are assigned, worlds have regenerated, and game has not started
     * <p>
     * /uhc start force: Skips checks
     * @param skipChecks If true, all checks are ignored.
     */
    public void startUHC(boolean skipChecks) {
        var prevStage = stage;
        requireNotStarted();

        Debug.printDebug(trans("xyz.baz9k.uhc.debug.start.try"));
        if (!skipChecks) {
            // require teams assigned
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (teamManager.getPlayerState(p) == PlayerState.COMBATANT_UNASSIGNED) {
                    throw translatableErr(IllegalStateException.class, "xyz.baz9k.uhc.err.team.must_assigned");
                }
            }

            // require world regened
            if (!worldManager.worldsRegened()) {
                throw translatableErr(IllegalStateException.class, "xyz.baz9k.uhc.err.world.must_regened");
            }
        } else {
            Debug.printDebug(trans("xyz.baz9k.uhc.debug.start.force"));
        }

        try {
            _startUHC();
            Debug.printDebug(trans("xyz.baz9k.uhc.debug.start.complete"));
        } catch (Exception e) {
            setStage(prevStage);
            Debug.printDebug(trans("xyz.baz9k.uhc.debug.start.fail"));
            Debug.printError(e);
        }
    }


    private void _startUHC() {
        setStage(GameStage.nth(0));
        worldManager.worldsRegenedOff();

        startTime = lastStageInstant = Instant.now();
        kills.clear();
        
        worldManager.initializeWorlds();

        FileConfiguration cfg = plugin.getConfig();
        int max_health = new int[]{10, 20, 40, 60}[cfg.getInt("esoteric.max_health")];
        double movement_speed = new double[]{0.5,1,2,3}[cfg.getInt("esoteric.mv_speed")];


        for (Player p : Bukkit.getOnlinePlayers()) {
            // activate hud things for all
            hudManager.initializePlayerHUD(p);
            hudManager.addPlayerToTeams(p);

            // handle display name
            previousDisplayNames.put(p.getUniqueId(), p.displayName());
            p.displayName(TeamDisplay.prefixed(teamManager.getTeam(p), p.getName()));

            // set maximum health and movement speed according to esoteric options
            p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(max_health);
            p.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.1 * movement_speed); // 0.1 is default value

            resetStatuses(p);
            recipes.discoverFor(p);

            // 60s grace period
            PotionEffectType.DAMAGE_RESISTANCE.createEffect(60 * 20 /* ticks */, /* lvl */ 5).apply(p);

            if (teamManager.isSpectator(p)) {
                p.setGameMode(GameMode.SPECTATOR);
            } else {
                p.setGameMode(GameMode.SURVIVAL);
                kills.put(p.getUniqueId(), 0);
            }
        }

        bbManager.enable(Bukkit.getServer());

        // do spreadplayers
        Debug.printDebug(trans("xyz.baz9k.uhc.debug.spreadplayers.start"));

        double max = GameStage.WB_STILL.wbDiameter(),
               min = GameStage.WB_STILL.wbDiameter() / (1 + teamManager.getNumTeams());
        Location defaultLoc = worldManager.gameSpawn();

        plugin.spreadPlayers().random(SpreadPlayersManager.BY_TEAMS(defaultLoc), worldManager.getCenter(), max, min);
        Debug.printDebug(trans("xyz.baz9k.uhc.debug.spreadplayers.end"));

        Bukkit.unloadWorld(worldManager.getLobbyWorld(), true);

        startTick();
    }

    /**
     * Ends UHC.
     * <p>
     * Accessible through /uhc end or /uhc end force.
     * <p>
     * /uhc end: Checks that the game has started
     * <p>
     * /uhc end force: Forcibly starts game
     * @param skipChecks If true, started game checks are ignored.
     */
    public void endUHC(boolean skipChecks) {
        var prevStage = stage;
        requireStarted();
        Debug.printDebug(trans("xyz.baz9k.uhc.debug.end.try"));
        // if (!skipChecks) {
        // } else {
        //     Debug.printDebug(trans("xyz.baz9k.uhc.debug.end.force"));
        // }
        
        try {
            _endUHC();
            Debug.printDebug(trans("xyz.baz9k.uhc.debug.end.complete"));
        } catch (Exception e) {
            setStage(prevStage);
            Debug.printDebug(trans("xyz.baz9k.uhc.debug.end.fail"));
            Debug.printError(e);
        }
    }

    private void _endUHC() {
        setStage(GameStage.NOT_IN_GAME);
        for (Player p : Bukkit.getOnlinePlayers()) {
            resetStatuses(p);
            p.setGameMode(GameMode.SURVIVAL);
            worldManager.escapePlayer(p);
            p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
            p.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.1);
        }
        
        // update display names
        for (UUID uuid : previousDisplayNames.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            p.displayName(previousDisplayNames.get(uuid));
        }

        teamManager.resetAllPlayers();
        hudManager.cleanup();
        bbManager.disable(Bukkit.getServer());
        kills.clear();
        if (tick != null) tick.cancel();

    }

    public void requireStarted() {
        if (!hasUHCStarted()) {
            throw translatableErr(IllegalStateException.class, "xyz.baz9k.uhc.err.not_started");
        }
    }

    public void requireNotStarted() {
        if (hasUHCStarted()) {
            throw translatableErr(IllegalStateException.class, "xyz.baz9k.uhc.err.already_started");
        }
    }

    private void startTick() {
        tick = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!hasUHCStarted()) return;
    
            bbManager.tick();
            
            if (isStageComplete()) {
                incrementStage();
            }
            
            for (Player p : Bukkit.getOnlinePlayers()) {
                hudManager.updateElapsedTimeHUD(p);
                hudManager.updateWBHUD(p);
            }
        }, 0L, 1L);
    }

    /**
     * Resets a player's statuses (health, food, sat, xp, etc.)
     * @param p
     */
    public void resetStatuses(@NotNull Player p) {
        // fully heal, adequately saturate, remove XP
        p.setHealth(p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        p.setFoodLevel(20);
        p.setSaturation(5.0f);
        p.setExp(0.0f);
        p.getInventory().clear();
        
        // clear all potion effects
        for (PotionEffect effect : p.getActivePotionEffects()) {
            p.removePotionEffect(effect.getType());
        }
    }

    public boolean hasUHCStarted() {
        return stage != GameStage.NOT_IN_GAME;
    }

    /**
     * @return the {@link Duration} since the game has started.
     */
    @NotNull
    public Duration getElapsedTime() {
        return Duration.between(startTime, Instant.now());
    }

    /**
     * @return the current stage of the game.
     */
    public GameStage getStage() {
        return stage;
    }

    public void incrementStage() {
        setStage(stage.next());
    }

    public void setStage(GameStage stage) {
        if (stage == null) return;
        this.stage = stage;
        updateStage();
    }

    private void updateStage() {
        if (!hasUHCStarted()) return;
        lastStageInstant = Instant.now();
        bbManager.updateBossbarStage();

        stage.sendMessage();
        stage.applyWBSize(worldManager.getGameWorlds());

        // deathmatch
        if (isDeathmatch()) {
            World w = worldManager.getGameWorld(0);

            int radius = (int) GameStage.DEATHMATCH.wbRadius();
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    w.getBlockAt(x, w.getMaxHeight() - 1, z).setType(Material.AIR);
                    w.getBlockAt(x, w.getMaxHeight() - 2, z).setType(Material.BARRIER);
                }
            }

            for (int x = -radius; x <= radius; x++) {
                w.getBlockAt(x, w.getMaxHeight() - 1, -radius - 1).setType(Material.BARRIER);
                w.getBlockAt(x, w.getMaxHeight() - 1, radius + 1).setType(Material.BARRIER);
            }

            for (int z = -radius; z <= radius; z++) {
                w.getBlockAt(-radius - 1, w.getMaxHeight() - 1, z).setType(Material.BARRIER);
                w.getBlockAt(radius + 1, w.getMaxHeight() - 1, z).setType(Material.BARRIER);
            }

            for (Player p : teamManager.getAllCombatants()) {
                p.addPotionEffects(Arrays.asList(
                    PotionEffectType.DAMAGE_RESISTANCE.createEffect(10 * 20, 10),
                    PotionEffectType.SLOW.createEffect(10 * 20, 10),
                    PotionEffectType.JUMP.createEffect(10 * 20, 128),
                    PotionEffectType.BLINDNESS.createEffect(10 * 20, 10)
                ));
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.teleport(worldManager.getCenterAtY(255));
            }

            double rad = GameStage.DEATHMATCH.wbRadius() - 1;
            plugin.spreadPlayers().rootsOfUnity(SpreadPlayersManager.BY_TEAMS(worldManager.getHighCenter()), worldManager.getCenter(), rad);

        }

    }

    /**
     * @return the {@link Duration} that the current stage lasts.
     */
    @NotNull
    public Duration getStageDuration() {
        requireStarted();
        return stage.duration();
    }

    /**
     * @return the {@link Duration} until the current stage ends.
     */
    @NotNull
    public Duration getRemainingStageDuration() {
        requireStarted();
        Duration stageDur = getStageDuration();
        if (isDeathmatch()) return stageDur; // if deathmatch, just return ∞
        return Duration.between(Instant.now(), lastStageInstant.plus(stageDur));
    }

    /**
     * @return if deathmatch (last stage) has started.
     */
    public boolean isDeathmatch() {
        return stage == GameStage.DEATHMATCH;
    }

    /**
     * @return if the stage has completed and needs to be incremented.
     */
    public boolean isStageComplete() {
        if (isDeathmatch()) return false;
        Instant end = lastStageInstant.plus(stage.duration());
        return !end.isAfter(Instant.now());
    }

    /**
     * Returns the number of kills that this combatant has dealt.
     * @param p
     * @return the number of kills in an OptionalInt. 
     * <p>
     * If the player is not registered in the kills map (i.e. they're a spec), return empty OptionalInt.
     */
    public OptionalInt getKills(@NotNull Player p) {
        Integer k = kills.get(p.getUniqueId());
        if (k == null) return OptionalInt.empty();
        return OptionalInt.of(k);
    }

    private void winMessage() {
        if (teamManager.countLivingTeams() > 1) return;
        int winner = teamManager.getAliveTeams()[0];
        Component winMsg = trans("xyz.baz9k.uhc.win", TeamDisplay.getName(winner))
            .style(noDeco(NamedTextColor.WHITE));

        // this msg should be displayed after player death
        delayedMessage(winMsg, plugin, 1);
        
        var fwe = FireworkEffect.builder()
                                .withColor(TeamDisplay.getBukkitColor(winner), org.bukkit.Color.WHITE)
                                .with(FireworkEffect.Type.BURST)
                                .withFlicker()
                                .withTrail()
                                .build();
        for (Player p : teamManager.getAllCombatantsOnTeam(winner)) {
            Firework fw = p.getWorld().spawn(p.getLocation(), Firework.class);
            FireworkMeta meta = fw.getFireworkMeta();
            meta.addEffect(fwe);
            meta.setPower(1);
            fw.setFireworkMeta(meta);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        teamManager.addPlayer(p, hasUHCStarted());

        // set this perm for crossdim travel
        p.addAttachment(plugin, "mv.bypass.gamemode.*", true);
        p.recalculatePermissions();

        if (hasUHCStarted()) {
            bbManager.enable(p);
            hudManager.initializePlayerHUD(p);
            hudManager.addPlayerToTeams(p);

            if (!worldManager.inGame(p)) p.teleport(worldManager.gameSpawn());
        } else {
            if (worldManager.inGame(p)) worldManager.escapePlayer(p);
        }

        
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (!hasUHCStarted()) return;
        Player dead = e.getEntity();
        
        dead.setGameMode(GameMode.SPECTATOR);
        if (teamManager.getPlayerState(dead) == PlayerState.COMBATANT_ALIVE) {
            teamManager.setCombatantAliveStatus(dead, false);

            // check team death
            int t = teamManager.getTeam(dead);
            if (teamManager.isTeamEliminated(t)) {
                Component teamElimMsg = trans("xyz.baz9k.uhc.eliminated", TeamDisplay.getName(t))
                    .style(noDeco(NamedTextColor.WHITE));
                // this msg should be displayed after player death
                delayedMessage(teamElimMsg, plugin, 1);
            }

            // check win condition
            if (teamManager.countLivingTeams() == 1) {
                winMessage();
            }
        }
        
        // set bed spawn
        Location newSpawn = dead.getLocation();
        if (newSpawn.getY() < 0) {
            dead.setBedSpawnLocation(worldManager.gameSpawn(), true);
        } else {
            dead.setBedSpawnLocation(newSpawn, true);
        }

        Player killer = dead.getKiller();
        if (killer != null) {
            OptionalInt k = getKills(killer);
            if (k.isPresent()) {
                this.kills.put(killer.getUniqueId(), k.orElseThrow() + 1);
                hudManager.updateKillsHUD(killer);
            }
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            hudManager.updateCombatantsAliveHUD(p);
            hudManager.updateTeamsAliveHUD(p);
        }
    }

    @EventHandler
    public void onPlayerFight(EntityDamageByEntityEvent e) {
        if (!hasUHCStarted()) return;
        // friendly fire
        if (e.getEntity() instanceof Player target) {
            if (e.getDamager() instanceof Player damager) {
                if (teamManager.getTeam(target) == teamManager.getTeam(damager)) {
                    e.setCancelled(true);
                }
            }
        }
    }
}
