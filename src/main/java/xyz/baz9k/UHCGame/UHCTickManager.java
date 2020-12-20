package xyz.baz9k.UHCGame;

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
        manager.updateElapsedTime();
    }
}
