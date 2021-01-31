package xyz.baz9k.UHCGame.util;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.chat.BaseComponent;

public class DelayedMessage extends BukkitRunnable {
    private String message;
    private BaseComponent[] msgc;

    public DelayedMessage(String message) {
        this.message = message;
    }
    public DelayedMessage(BaseComponent[] message) {
        this.msgc = message;
    }

    @Override
    public void run() {
        if (msgc != null) {
            Bukkit.broadcast(msgc);
            return;
        }
        Bukkit.broadcastMessage(message);
    }
}
