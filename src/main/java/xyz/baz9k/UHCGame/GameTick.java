package xyz.baz9k.UHCGame;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class GameTick extends BukkitRunnable {
    private UHCGame plugin;
    private GameManager gameManager;
    public GameTick(UHCGame plugin) {
        this.plugin = plugin;
        gameManager = plugin.getGameManager();
    }

    @Override
    public void run() {
        if (gameManager.hasUHCStarted()) {
            if (gameManager.isStageComplete()) {
                gameManager.incrementStage();
            }
            plugin.getBossbarManager().tick();
            
            for (Player p : Bukkit.getOnlinePlayers()) {
                plugin.getHUDManager().updateElapsedTimeHUD(p);
                plugin.getHUDManager().updateWBHUD(p);
            }
        }
    }
}
