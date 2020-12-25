package xyz.baz9k.UHCGame;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import xyz.baz9k.UHCGame.util.DelayedMessageSender;
import xyz.baz9k.UHCGame.util.TeamColors;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;

public class GameManager implements Listener {
    private UHCGame plugin;

    private boolean isUHCStarted = false;
    private final HashMap<Player, String> previousDisplayNames;
    
    private final TeamManager teamManager;
    private HUDManager hudManager;
    private BossbarManager bbManager;
    private ConfigManager cfgManager;
    private GameTick tick;

    private Instant startTime = null;
    private Duration timeElapsed = null;
    
    private World uhcWorld;

    private HashMap<Player, Integer> kills;

    //TODO REMOVE THESE AND REPLACE WITH CONFIG MANAGER
    private final int WORLDBORDER = 1200;
    private final int WB2 = 25;
    private final int WB3 = 3;

    private int stage = -1;
    private Instant lastStageInstant = null;
    private Duration[] stageDurations = {
        Duration.ofMinutes(60), // Still border
        Duration.ofMinutes(15), // Border 1
        Duration.ofMinutes(5), // Border stops
        Duration.ofMinutes(10), // Border 2
        Duration.ofMinutes(5), // Waiting until DM
        ChronoUnit.FOREVER.getDuration() // deathmatch
    };

    public GameManager(UHCGame plugin) {
        this.plugin = plugin;
        previousDisplayNames = new HashMap<>();

        // init all managers (minus tick, b/c tick is created at uhc start)
        teamManager = new TeamManager();
        hudManager = new HUDManager(plugin, this);
        bbManager = new BossbarManager(plugin, this);
        cfgManager = new ConfigManager(plugin, this);

        plugin.getServer().getPluginManager().registerEvents(hudManager, plugin);
        uhcWorld = plugin.getServer().getWorld("world"); // TODO MULTIVERSE
    }

    public void startUHC() {
        if (isUHCStarted) throw new IllegalStateException("UHC has already started.");
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (teamManager.getPlayerState(p) == PlayerState.COMBATANT_UNASSIGNED) throw new IllegalStateException("Teams not assigned.");
        }

        isUHCStarted = true;
        startTime = lastStageInstant = Instant.now();
        updateElapsedTime();
        this.kills = new HashMap<>();
        
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            // archive previous display name
            previousDisplayNames.put(p, p.getDisplayName());
            
            // fully heal, adequately saturate, remove XP
            p.setHealth(20.0f);
            p.setFoodLevel(20);
            p.setSaturation(5.0f);
            p.setExp(0.0f);
            
            // clear all potion effects
            for (PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }

            // 60s grace period
            PotionEffectType.DAMAGE_RESISTANCE.createEffect(60 * 20 /* ticks */, /* lvl */ 5).apply(p);

            if (!teamManager.isSpectator(p)) {
                kills.put(p, 0);
            }

            if (teamManager.isSpectator(p)) {
                p.setGameMode(GameMode.SPECTATOR);
            }

            // set player display name
            p.setDisplayName(TeamColors.getTeamPrefixWithSpace(teamManager.getTeam(p)) + p.getName());

            // activate hud things for all
            hudManager.initializePlayerHUD(p);
        }

        // set time to 0 and delete rain
        uhcWorld.setTime(0);
        uhcWorld.setClearWeatherDuration(Integer.MAX_VALUE); // there is NO rain. Ever again.
        
        // begin uhc tick events
        tick = new GameTick(plugin);
        tick.runTaskTimer(plugin, 0L, 1L);
        
        setStage(0);
        bbManager.enable();

        // set wb center in case it was in the wrong position
        uhcWorld.getWorldBorder().setCenter(0.5, 0.5);
    }

    public void endUHC() {
        if (!isUHCStarted)
            throw new IllegalStateException("UHC has not begun.");
        isUHCStarted = false;

        // update display names
        for (Player p : previousDisplayNames.keySet()) {
            p.setDisplayName(previousDisplayNames.get(p));
            p.setGameMode(GameMode.SURVIVAL);
        }

        teamManager.resetAllPlayers();
        hudManager.cleanup();
        kills.clear();
        tick.cancel();

        stage = -1;
        bbManager.disable();

    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public HUDManager getHUDManager() {
        return hudManager;
    }

    public BossbarManager getBossbarManager() {
        return bbManager;
    }

    public ConfigManager getConfigManager() {
        return cfgManager;
    }

    public int getStage() {
        return Math.max(0, Math.min(stage, stageDurations.length));
    }

    public void incrementStage() {
        stage++;
        updateStage();
    }

    public void setStage(int stage) {
        this.stage = stage;
        this.stage = getStage();
        updateStage();
    }

    private void updateStage() {
        lastStageInstant = Instant.now();
        bbManager.updateBossbarStage();

        //worldborder
        switch (stage) {
            case 0: // start of game (still border)
                uhcWorld.getWorldBorder().setSize(WORLDBORDER);
                break;
            case 1: //worldborder starts moving the first time (border 1)
                uhcWorld.getWorldBorder().setSize(WB2, stageDurations[1].toSeconds());
                break;
            case 2: // border stop
                uhcWorld.getWorldBorder().setSize(WB2);
            case 3: //worldborder starts moving the second time (border 2)
                uhcWorld.getWorldBorder().setSize(WB3, stageDurations[3].toSeconds());
                break;
            case 4:
                uhcWorld.getWorldBorder().setSize(WB3);
                break;
            //TODO DEATHMATCH
        }
    }

    public Instant getLStageInstant() {
        return lastStageInstant;
    }

    public Duration getCurrentStageDuration() {
        return stageDurations[stage];
    }

    public boolean isDeathmatch() {
        return stage == stageDurations.length - 1;
    }

    public boolean isStageComplete() {
        if (isDeathmatch()) return false;
        Instant end = lastStageInstant.plus(stageDurations[stage]);
        return !end.isAfter(Instant.now());
    }

    public World getUHCWorld() {
        return uhcWorld;
    }

    public boolean isUHCStarted() {
        return isUHCStarted;
    }

    public void updateElapsedTime() {
        if (!isUHCStarted) {
            throw new IllegalStateException("UHC has not started.");
        }
        timeElapsed = Duration.between(startTime, Instant.now());
    }

    public int getKills(Player p) {
        return kills.get(p);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        teamManager.addPlayer(p);
        if(isUHCStarted) {
            bbManager.enable(p);
            hudManager.initializePlayerHUD(p);
            hudManager.addPlayerToTeams(p);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (isUHCStarted) {
            Player deadPlayer = event.getEntity();
            if (teamManager.getPlayerState(deadPlayer) == PlayerState.COMBATANT_ALIVE) {
                teamManager.setCombatantAliveStatus(deadPlayer, false);
                int team = teamManager.getTeam(deadPlayer);
                if (teamManager.isTeamEliminated(team)) {
                    String teamEliminatedMessage = "Team " + team + " has been eliminated!"; // TODO FANCY
                    (new DelayedMessageSender(teamEliminatedMessage)).runTaskLater(plugin, 1);
                }

                deadPlayer.setBedSpawnLocation(deadPlayer.getLocation(), true);

                if (teamManager.countLivingTeams() == 1) {
                    String winnerMessage = "Only one team is left, this is when the game would end."; // TODO FANCY
                    (new DelayedMessageSender(winnerMessage)).runTaskLater(plugin, 1);
                }
            }

            Player killer = deadPlayer.getKiller();
            if (killer != null) {
                if (!teamManager.isSpectator(killer)) {
                    int nKills = this.kills.get(killer);
                    this.kills.put(killer, nKills + 1);
                    hudManager.updateKillsHUD(killer);
                }
            }

            for (Player p : plugin.getServer().getOnlinePlayers()) {
                hudManager.updateCombatantsAliveHUD(p);
                hudManager.updateTeamsAliveHUD(p);
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (isUHCStarted) {
            event.getPlayer().setGameMode(GameMode.SPECTATOR);
        }
    }

    @EventHandler
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player p = (Player) event.getEntity();

        if (isUHCStarted) {
            // cancel friendlyFire
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent entDmgEvent = (EntityDamageByEntityEvent) event;
                Entity damager = entDmgEvent.getDamager(); 
                if (damager instanceof Player) {
                    Player playerDamager = (Player) damager;
                    if (teamManager.getTeam(p) == teamManager.getTeam(playerDamager)) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
            // update hud if dmg taken
            hudManager.updateTeammateHUD(p);
        }
    }

    public Duration getElapsedTime() {
        return timeElapsed;
    }
}
