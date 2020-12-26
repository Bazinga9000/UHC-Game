package xyz.baz9k.UHCGame;

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
        if (gameManager.isUHCStarted()) {
            gameManager.updateElapsedTime();
            if (gameManager.isStageComplete()) gameManager.incrementStage();
            plugin.getBossbarManager().tick();
            
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                plugin.getHUDManager().updateElapsedTimeHUD(p);
                plugin.getHUDManager().updateWBHUD(p);
            }
        }
    }
}
