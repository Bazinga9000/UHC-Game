package xyz.baz9k.UHCGame;

import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
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
import java.util.function.Supplier;

import static xyz.baz9k.UHCGame.util.Utils.*;
import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

public class GameManager implements Listener {
    private final UHCGamePlugin plugin;

    private TeamManager teamManager;
    private HUDManager hudManager;
    private BossbarManager bbManager;
    private WorldManager worldManager;
    private Recipes recipes;
    private BukkitTask tick;
    
    
    private final HashMap<UUID, Component> prevDisplayNames = new HashMap<>();
    private final HashMap<UUID, Integer> kills = new HashMap<>();
    
    private GameStage stage = GameStage.NOT_IN_GAME;
    
    private Optional<Instant> startTime = Optional.empty();
    private Optional<Instant> lastStageInstant = Optional.empty();

    public GameManager(UHCGamePlugin plugin) {
        this.plugin = plugin;
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

    private enum GameInitFailure {
        TEAM_UNASSIGNED      (new Key("err.team.must_assigned")),
        WORLDS_NOT_REGENED   (new Key("err.world.must_regened"), new Key("err.world.must_regened_short")),
        GAME_NOT_STARTED     (new Key("err.not_started")       ),
        GAME_ALREADY_STARTED (new Key("err.already_started")   );

        private final Key errKey;
        private final Key panelErrKey;
        GameInitFailure(Key errKey) {
            this(errKey, errKey);
        }
        GameInitFailure(Key errKey, Key panelErrKey) {
            this.errKey = errKey;
            this.panelErrKey = panelErrKey;
        }

        public IllegalStateException exception() {
            return errKey.transErr(IllegalStateException.class);
        }
        public String panelErr() {
            return renderString(panelErrKey.trans());
        }
    }

    private List<GameInitFailure> checkStart() {
        worldManager = plugin.getWorldManager();
        teamManager = plugin.getTeamManager();
        
        var fails = new ArrayList<GameInitFailure>();

        if (hasUHCStarted()) fails.add(GameInitFailure.GAME_ALREADY_STARTED);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (teamManager.getPlayerState(p) == PlayerState.COMBATANT_UNASSIGNED) {
                fails.add(GameInitFailure.TEAM_UNASSIGNED);
                break;
            }
        }

        // if (!worldManager.worldsRegened()) {
        //     fails.add(GameInitFailure.WORLDS_NOT_REGENED);
        // }

        return Collections.unmodifiableList(fails);
    }

    private List<GameInitFailure> checkEnd() {
        var fails = new ArrayList<GameInitFailure>();

        if (!hasUHCStarted()) fails.add(GameInitFailure.GAME_NOT_STARTED);

        return Collections.unmodifiableList(fails);
    }

    public List<String> checkStartPanel() {
        return checkStart().stream()
            .map(GameInitFailure::panelErr)
            .toList();
    }
    public List<String> checkEndPanel() {
        return checkEnd().stream()
            .map(GameInitFailure::panelErr)
            .toList();
    }

    private void runEventWithChecks(String eventKey, Runnable event, Supplier<List<GameInitFailure>> checks, boolean skipChecks) {
        Key EVENT_TRY       = new Key("debug.%s.try", eventKey),
            EVENT_FORCE_TRY = new Key("debug.%s.force", eventKey),
            EVENT_COMPLETE  = new Key("debug.%s.complete", eventKey),
            EVENT_FAIL      = new Key("debug.%s.fail", eventKey);
        
        var prevStage = stage;

        Debug.printDebug(EVENT_TRY.trans());
        if (!skipChecks) {
            var fails = checks.get();
            if (fails.size() != 0) {
                throw fails.get(0).exception();
            }
        } else {
            Debug.printDebug(EVENT_FORCE_TRY.trans());
        }

        try {
            event.run();
            Debug.printDebug(EVENT_COMPLETE.trans());
        } catch (Exception e) {
            setStage(prevStage);
            Debug.printDebug(EVENT_FAIL.trans());
            Debug.printError(e);
        }
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
        runEventWithChecks("start", this::_startUHC, this::checkStart, skipChecks);
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
        runEventWithChecks("end", this::_endUHC, this::checkEnd, skipChecks);
    }

    private void _startUHC() {
        worldManager.initWorlds();

        // do spreadplayers
        Debug.printDebug(new Key("debug.spreadplayers.start").trans());
        double max = GameStage.WB_STILL.wbDiameter(),
               min = max / Math.sqrt(3 * teamManager.getNumTeams());

        //    | # Groups | Min   | Max  |
        //    |----------|-------|------|
        //    |        1 | 692.8 | 1200 |
        //    |        2 | 489.9 | 1200 |
        //    |        3 | 400.0 | 1200 |
        //    |        4 | 346.4 | 1200 |
        //    |        5 | 309.8 | 1200 |
        //    |        6 | 282.8 | 1200 |
        //    |        7 | 261.9 | 1200 |
        //    |        8 | 244.9 | 1200 |
        //    |        9 | 230.9 | 1200 |
        //    |       10 | 219.1 | 1200 |
        //    |       11 | 208.9 | 1200 |
        //    |       12 | 200.0 | 1200 |
        //    |       13 | 192.2 | 1200 |
        //    |       14 | 185.2 | 1200 |
        //    |       15 | 178.9 | 1200 |

        // btw if you're reading this,
        // i sampled the average # of points generated at ratios of min/max
        // x: min / max
        // y: number of points generated
        // y = .65 * (1/x)^2

        // the min value calculation here is based on that 
        // but the constant has been adjusted to give margin of error
        // (in case SP produces less points than average)

        Location defaultLoc = worldManager.gameSpawn();

        plugin.spreadPlayers().random(SpreadPlayersManager.BY_TEAMS(defaultLoc), worldManager.getCenter(), max, min);
        Debug.printDebug(new Key("debug.spreadplayers.end").trans());

        // unload world
        plugin.getMVWorldManager().unloadWorld("lobby", true);

        setStage(GameStage.nth(0));
        startTime = lastStageInstant = Optional.of(Instant.now());


        kills.clear();
        

        for (Player p : Bukkit.getOnlinePlayers()) {
            prepareToGame(p, true);
        }

        // start ticking
        startTick();
    }

    private void _endUHC() {
        setStage(GameStage.NOT_IN_GAME);
        for (Player p : Bukkit.getOnlinePlayers()) {
            prepareToLobby(p, true);
        }

        // global stuff, affects all players incl offline ones
        teamManager.resetAllPlayers();
        bbManager.disable(Bukkit.getServer());
        hudManager.cleanup();
        kills.clear();
        endTick();

    }

    public boolean hasUHCStarted() {
        return stage != GameStage.NOT_IN_GAME;
    }

    public void requireStarted() {
        if (!hasUHCStarted()) {
            throw new Key("err.not_started").transErr(IllegalStateException.class);
        }
    }

    public void requireNotStarted() {
        if (hasUHCStarted()) {
            throw new Key("err.already_started").transErr(IllegalStateException.class);
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

                // if (p.getGameMode() == GameMode.SPECTATOR && !p.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                //     PotionEffectType.NIGHT_VISION.createEffect(20000000, 1).apply(p);
                // }
            }
        }, 0L, 1L);
    }

    private void endTick() {
        if (tick != null) tick.cancel();
    }

    /**
     * Resets a player's statuses (health, food, sat, xp, etc.)
     * @param p the player
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

        // clear all advancements
        Iterable<Advancement> advancements = Bukkit::advancementIterator;
        for (var a : advancements) {
            var progress = p.getAdvancementProgress(a);
            for (String criterion : progress.getAwardedCriteria()) {
                progress.revokeCriteria(criterion);
            }
        }
    }

    /**
     * @return the {@link Duration} since the game has started.
     */
    public Optional<Duration> getElapsedTime() {
        return startTime.map(start ->
            Duration.between(start, Instant.now())
        );
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
        lastStageInstant = Optional.of(Instant.now());
        bbManager.updateBossbarStage();

        stage.sendMessage();
        stage.applyWBSize(worldManager.getGameWorlds());

        if (stage == GameStage.WB_STOP) {
            worldManager.setGamerule(GameRule.DO_MOB_SPAWNING, false);
        }

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

            for (Player p : teamManager.getCombatants()) {
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
    public Optional<Duration> getRemainingStageDuration() {
        requireStarted();
        Duration stageDur = getStageDuration();
        if (isDeathmatch()) return Optional.of(stageDur); // if deathmatch, just return ∞
        return lastStageInstant.map(instant -> 
            Duration.between(Instant.now(), instant.plus(stageDur))
        );
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
        return lastStageInstant.map(instant -> {
                var end = instant.plus(stage.duration());
                return !end.isAfter(Instant.now());
            })
            .orElse(false);
    }

    /**
     * Returns the number of kills that this combatant has dealt.
     * @param p
     * @return the number of kills in an OptionalInt. 
     * <p>
     * If the player is not registered in the kills map (i.e. they're a spec), return empty OptionalInt.
     */
    public OptionalInt getKills(@NotNull Player p) {
        if (!teamManager.isSpectator(p)) {
            return OptionalInt.of(kills.computeIfAbsent(p.getUniqueId(), uuid -> 0));
        }
        return OptionalInt.empty();
    }

    private Component includeGameTimestamp(Component c) {
        if (c == null) return null;
        String timeStr = getLongTimeString(getElapsedTime(), "?");
        return Component.text(String.format("[%s]", timeStr))
               .append(Component.space())
               .append(c);
    }

    private void winMessage() {
        if (teamManager.getAliveTeams().length > 1) return;
        endTick();
        int winner = teamManager.getAliveTeams()[0];
        Component winMsg = new Key("win").trans(TeamDisplay.getName(winner))
            .style(noDeco(NamedTextColor.WHITE));
        winMsg = includeGameTimestamp(winMsg);

        // this msg should be displayed after player death
        delayedMessage(winMsg, 1);
        
        var fwe = FireworkEffect.builder()
            .withColor(TeamDisplay.getBukkitColor(winner), org.bukkit.Color.WHITE)
            .with(FireworkEffect.Type.BURST)
            .withFlicker()
            .withTrail()
            .build();
        for (Player p : teamManager.getCombatantsOnTeam(winner)) {
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
            prepareToGame(p, false);
        } else {
            prepareToLobby(p, false);
        }

        
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (!hasUHCStarted()) return;
        Player dead = e.getEntity();
        
        e.deathMessage(includeGameTimestamp(e.deathMessage()));
        dead.setGameMode(GameMode.SPECTATOR);
        if (teamManager.getPlayerState(dead) == PlayerState.COMBATANT_ALIVE) {
            teamManager.setCombatantAliveStatus(dead, false);

            // check team death
            int t = teamManager.getTeam(dead);
            if (teamManager.isTeamEliminated(t)) {
                Component teamElimMsg = new Key("eliminated").trans(TeamDisplay.getName(t))
                    .style(noDeco(NamedTextColor.WHITE));
                teamElimMsg = includeGameTimestamp(teamElimMsg);
                // this msg should be displayed after player death
                delayedMessage(teamElimMsg, 1);
            }

            // check win condition
            if (teamManager.getAliveTeams().length == 1) {
                winMessage();
            }
        }
        
        // set bed spawn
        Location newSpawn = dead.getLocation();
        if (newSpawn.getY() < newSpawn.getWorld().getMinHeight()) {
            newSpawn = worldManager.gameSpawn();
        }
        dead.setBedSpawnLocation(newSpawn, true);

        Player killer = dead.getKiller();
        if (killer != null) {
            OptionalInt k = getKills(killer);
            if (k.isPresent()) {
                this.kills.put(killer.getUniqueId(), k.orElseThrow() + 1);
                hudManager.updateKillsHUD(killer);
            }
        }
    }

    @EventHandler
    public void onPlayerFight(EntityDamageByEntityEvent e) {
        if (!hasUHCStarted()) return;
        // cancel friendly fire
        if (e.getEntity() instanceof Player target) {
            if (e.getDamager() instanceof Player damager) {
                if (teamManager.getTeam(target) == teamManager.getTeam(damager)) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerAdvancement(PlayerAdvancementDoneEvent e) {
        if (!hasUHCStarted()) return;

        Player p = e.getPlayer();
        if (!teamManager.getPlayerState(p).isSpectating()) {
            e.message(includeGameTimestamp(e.message()));
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        if (!hasUHCStarted()) return;

        Player p = e.getPlayer();
        if (teamManager.getPlayerState(p).isSpectating()) {
            prepareToSpectate(p);
        }
    }

    private void prepareToGame(Player p, boolean onGameStart) {
        bbManager.enable(p);
        hudManager.initPlayerHUD(p);
        
        recipes.discoverFor(p);
        setDisplayName(p, TeamDisplay.prefixed(teamManager.getTeam(p), p.getName()));
        
        if (onGameStart || !worldManager.inGame(p)) {
            // if the player joins midgame and are in the lobby, then idk where to put them! put in spawn
            if (!onGameStart && !worldManager.inGame(p)) {
                p.teleport(worldManager.gameSpawn());
            }

            // handle display name
            // prevDisplayNames.put(p.getUniqueId(), p.displayName());
            
            if (teamManager.isSpectator(p)) {
                p.setGameMode(GameMode.SPECTATOR);
                prepareToSpectate(p);
            } else {
                p.setGameMode(GameMode.SURVIVAL);

                // set maximum health and movement speed according to esoteric options
                var cfg = plugin.getConfig();
                int max_health = new int[]{10, 20, 40, 60}[cfg.getInt("esoteric.max_health")];
                double movement_speed = new double[]{0.5,1,2,3}[cfg.getInt("esoteric.mv_speed")];
    
                p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(max_health);
                p.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.1 * movement_speed); // 0.1 is default value
            }

            resetStatuses(p);

            // 60s grace period
            PotionEffectType.DAMAGE_RESISTANCE.createEffect(60 * 20 /* ticks */, /* lvl */ 5).apply(p);
        }
    }

    private void prepareToLobby(Player p, boolean onGameEnd) {
        bbManager.disable(p);
        hudManager.prepareToLobby(p);
        if (onGameEnd || worldManager.inGame(p)) {
            worldManager.escapePlayer(p);
            resetStatuses(p);
            p.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20);
            p.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.1);
            
            // update display names
            UUID uuid = p.getUniqueId();
            setDisplayName(p, prevDisplayNames.get(uuid));
        }
    }

    private void prepareToSpectate(Player p) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, true, false)
                .apply(p);
        }, 1);
    }

}
