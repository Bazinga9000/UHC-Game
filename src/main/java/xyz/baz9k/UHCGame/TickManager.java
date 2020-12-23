package xyz.baz9k.UHCGame;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TickManager extends BukkitRunnable {
    private UHCGame plugin;
    private GameManager manager;
    public TickManager(UHCGame plugin) {
        this.plugin = plugin;
        manager = plugin.getGameManager();
    }

    @Override
    public void run() {
        if (manager.isUHCStarted()) {
            manager.updateElapsedTime();
            if (manager.isStageComplete()) manager.incrementStage();
            manager.getBossbarManager().tick();
            
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                manager.getHUDManager().updateElapsedTimeHUD(p);
                manager.getHUDManager().updateWBHUD(p);
            }
        }
    }
}
