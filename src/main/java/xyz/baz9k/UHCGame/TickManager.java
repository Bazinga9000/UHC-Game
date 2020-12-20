package xyz.baz9k.UHCGame;

import org.bukkit.scheduler.BukkitRunnable;

public class TickManager extends BukkitRunnable {
    private UHCGame plugin;
    private GameManager manager;
    public TickManager(UHCGame plugin) {
        this.plugin = plugin;
        manager = plugin.getUHCManager();
    }

    @Override
    public void run() {
        manager.updateElapsedTime();
    }
}
