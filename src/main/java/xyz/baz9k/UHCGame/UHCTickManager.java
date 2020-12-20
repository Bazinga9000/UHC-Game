package xyz.baz9k.UHCGame;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class UHCTickManager extends BukkitRunnable {
    private UHCGame plugin;
    private UHCManager manager;
    public UHCTickManager(UHCGame plugin) {
        this.plugin = plugin;
        manager = plugin.getUHCManager();
    }

    @Override
    public void run() {
        if (manager.isUHCStarted()) {
            manager.updateElapsedTime();
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                manager.getHUDManager().updateElapsedTimeHUD(p);
            }
        }
    }
}
