package xyz.baz9k.UHCGame;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.*;
import java.util.HashMap;

public class GameManager implements Listener {
    private UHCGame plugin;

    private boolean isUHCStarted = false;
    private final HashMap<Player, String> previousDisplayNames;
    private final TeamManager teamManager = new TeamManager();
    
    private HUDManager hudManager;
    private TickManager tickManager;

    private Instant startTime = null;
    private Duration timeElapsed = null;

    private World uhcWorld;

    private class DelayedMessageSender extends BukkitRunnable {
        private final String message;
        public DelayedMessageSender(String message) {
            this.message = message;
        }

        @Override
        public void run() {
            Bukkit.broadcastMessage(message);
        }
    }

    private HashMap<Player, Integer> kills;
    
    public GameManager(UHCGame plugin) {
        this.plugin = plugin;
        previousDisplayNames = new HashMap<>();
        hudManager = new HUDManager(plugin, this);
        plugin.getServer().getPluginManager().registerEvents(hudManager, plugin);
        uhcWorld = plugin.getServer().getWorld("world"); //TODO MULTIVERSE
    }


    public void startUHC() {
        if (isUHCStarted) throw new IllegalStateException("UHC has already started.");
        isUHCStarted = true;
        startTime = Instant.now();
        this.kills = new HashMap<>();
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            //archive previous display name
            previousDisplayNames.put(p, p.getDisplayName());
            
            //fully heal, adequately saturate, remove XP
            p.setHealth(20.0f);
            p.setSaturation(5.0f);
            p.setExp(0.0f);
            
            //clear all potion effects
            for (PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }
            
            if (teamManager.isPlayerSpectator(p)) {
                p.setGameMode(GameMode.SPECTATOR);
            }

            if (teamManager.getPlayerState(p) != PlayerState.SPECTATOR) {
                kills.put(p, 0);
            }

        }

        //set time to 0 and delete rain
        uhcWorld.setTime(0);
        uhcWorld.setClearWeatherDuration(Integer.MAX_VALUE); // there is NO rain. Ever again.

        //start hud things
        hudManager.start();

        // begin uhc tick events
        tickManager = new TickManager(plugin);
        tickManager.runTaskTimer(plugin, 0L, 1L);
    }
    
    public void endUHC() {
        if (!isUHCStarted) throw new IllegalStateException("UHC has not begun.");
        isUHCStarted = false;

        //update display names
        for (Player p : previousDisplayNames.keySet()) {
            p.setDisplayName(previousDisplayNames.get(p));
            p.setGameMode(GameMode.SURVIVAL);
        }

        teamManager.resetAllPlayers();
        hudManager.cleanup();
        kills.clear();
        tickManager.cancel();
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public HUDManager getHUDManager() {
        return hudManager;
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
        teamManager.addPlayer(event.getPlayer());
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
                    String winnerMessage = "Only one team is left, this is when the game would end."; //TODO FANCY
                    (new DelayedMessageSender(winnerMessage)).runTaskLater(plugin, 1);
                }
            }

            Player killer = deadPlayer.getKiller();
            if (killer != null) {
                if (teamManager.getPlayerState(killer) != PlayerState.SPECTATOR) {
                    int nKills = this.kills.get(killer);
                    this.kills.put(killer, nKills + 1);
                    hudManager.updateKillsHUD(killer);
                }
            }
            hudManager.updateDeathCounters();
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (this.isUHCStarted()) {
            event.getPlayer().setGameMode(GameMode.SPECTATOR);
        }
    }

}
