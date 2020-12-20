package xyz.baz9k.UHCGame;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.time.*;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

public class GameManager {
    private UHCGame plugin;

    private boolean isUHCStarted = false;
    private final HashMap<Player, String> previousDisplayNames;
    private final TeamManager teamManager = new TeamManager();
    
    private HUDManager hudManager;
    private TickManager tickManager;

    private Instant startTime = null;
    private Duration timeElapsed = null;

    private World uhcWorld;

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

        }
        
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

}
