package xyz.baz9k.UHCGame;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.HashMap;

public class UHCManager {
    private UHCGame plugin;

    private boolean isUHCStarted = false;
    private final HashMap<Player, String> previousDisplayNames;
    private final UHCTeamManager teamManager = new UHCTeamManager();
    
    private UHCHUDManager hudManager;
    private UHCTickManager tickManager;

    public UHCManager(UHCGame plugin) {
        this.plugin = plugin;
        previousDisplayNames = new HashMap<>();
        hudManager = new UHCHUDManager(plugin);
        plugin.getServer().getPluginManager().registerEvents(hudManager, plugin);
    }


    public void startUHC() {
        if (isUHCStarted) throw new IllegalStateException("UHC has already started.");
        isUHCStarted = true;
        
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
        tickManager = new UHCTickManager(plugin);
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

    public UHCTeamManager getTeamManager() {
        return teamManager;
    }

    public boolean isUHCStarted() {
        return isUHCStarted;
    }

}
