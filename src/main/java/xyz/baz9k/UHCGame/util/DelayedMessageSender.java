package xyz.baz9k.UHCGame.util;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class DelayedMessageSender extends BukkitRunnable {
    private final String message;

    public DelayedMessageSender(String message) {
        this.message = message;
    }

    @Override
    public void run() {
        Bukkit.broadcastMessage(message);
    }
}
