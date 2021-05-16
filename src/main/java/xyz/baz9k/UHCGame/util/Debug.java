package xyz.baz9k.UHCGame.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public final class Debug {
    private Debug() { }
    private static boolean debug = true;

    public static boolean getDebug() {
        return debug;
    }
    public static void setDebug(boolean d) {
        debug = d;
    }
    /**
     * Display a stack trace error to a sender and also print a log
     * @param sender
     * @param e
     */
    public static void printError(CommandSender sender, Throwable e) {
        if (debug) {
            sender.sendMessage(Component.text(e.toString(), NamedTextColor.RED));
            for (var el : e.getStackTrace()) {
                sender.sendMessage(Component.text(el.toString(), NamedTextColor.RED));
            }
        }

        e.printStackTrace();
    }

    /**
     * Display a stack trace error to chat and also print a log
     * @param e
     */
    public static void printError(Throwable e) {
        if (debug) {
            Bukkit.getServer().sendMessage(Component.text(e.toString(), NamedTextColor.RED));
            for (var el : e.getStackTrace()) {
                Bukkit.getServer().sendMessage(Component.text(el.toString(), NamedTextColor.RED));
            }
        } else {
            e.printStackTrace();
        }
    }

    private static TextComponent fmtDebug(String msg) {
        return Component.text(String.format("[DEBUG] %s", msg), NamedTextColor.YELLOW);
    }

    /**
     * Broadcast a debug message in chat
     * @param msg
     */
    public static void broadcastDebug(String msg) {
        if (debug) {
            Bukkit.getServer().sendMessage(fmtDebug(msg));
        }
    }
}
