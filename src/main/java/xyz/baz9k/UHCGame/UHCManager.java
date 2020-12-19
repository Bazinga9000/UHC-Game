package xyz.baz9k.UHCGame;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import java.util.HashMap;

public class UHCManager {
    private UHCGame uhcPlugin;

    private boolean isUHCStarted = false;
    private final HashMap<Player, String> previousDisplayNames;
    private final UHCTeamManager teamManager = new UHCTeamManager();

    public UHCManager(UHCGame plugin) {
        uhcPlugin = plugin;
        previousDisplayNames = new HashMap<>();
    }


    public void startUHC() {
        isUHCStarted = true;

        for (Player p : uhcPlugin.getServer().getOnlinePlayers()) {
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

    }

    public void endUHC() {
        isUHCStarted = false;

        //update display names
        for (Player p : previousDisplayNames.keySet()) {
            p.setDisplayName(previousDisplayNames.get(p));
            p.setGameMode(GameMode.SURVIVAL);
        }

        teamManager.resetAllPlayers();
    }

    public UHCTeamManager getTeamManager() {
        return teamManager;
    }

    public boolean isUHCStarted() {
        return isUHCStarted;
    }

}
