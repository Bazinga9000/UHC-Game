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

import xyz.baz9k.UHCGame.util.DelayedMessageSender;
import xyz.baz9k.UHCGame.util.TeamColors;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;

public class GameManager implements Listener {
    private UHCGame plugin;

    private boolean isUHCStarted = false;
    private final HashMap<Player, String> previousDisplayNames;
    
    private final TeamManager teamManager = new TeamManager(); // exists always
    private HUDManager hudManager;
    private TickManager tickManager; // exists while game started
    private BossbarManager bbManager;

    private Instant startTime = null;
    private Duration timeElapsed = null;
    
    private World uhcWorld;

    private HashMap<Player, Integer> kills;

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
        hudManager = new HUDManager(plugin, this); // exists always
        bbManager = new BossbarManager(plugin, this); // exists always
        plugin.getServer().getPluginManager().registerEvents(hudManager, plugin);
        uhcWorld = plugin.getServer().getWorld("world"); // TODO MULTIVERSE
    }

    public void startUHC() {
        if (isUHCStarted) throw new IllegalStateException("UHC has already started.");
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
        tickManager = new TickManager(plugin);
        tickManager.runTaskTimer(plugin, 0L, 1L);

        stage = 0;
        bbManager.enable();
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
        tickManager.cancel();

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

    public int getStage() {
        return stage;
    }

    public void incrementStage() {
        stage++;
        lastStageInstant = Instant.now();
        bbManager.updateBossbarStage();
    }
    public void setStage(int stage) {
        // probably just for debug purposes and not intended to be used in actual UHC
        this.stage = stage;
        lastStageInstant = Instant.now();
        bbManager.updateBossbarStage();
    }
    public Instant getLStageInstant() {
        return lastStageInstant;
    }
    public Duration getCurrentStageDuration() {
        return stageDurations[stage];
    }
    public boolean isStageComplete() {
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

    public String getTimeElapsedString() {
        long s = timeElapsed.getSeconds();

        return String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
    }

    public int getKills(Player p) {
        return kills.get(p);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        teamManager.addPlayer(event.getPlayer());
        if(isUHCStarted()) {
            hudManager.initializePlayerHUD(p);
            hudManager.addPlayerToTeams(p);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (this.isUHCStarted()) {
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
        if (this.isUHCStarted()) {
            event.getPlayer().setGameMode(GameMode.SPECTATOR);
        }
    }

    @EventHandler
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player p = (Player) event.getEntity();

        if (this.isUHCStarted()) {
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
}
