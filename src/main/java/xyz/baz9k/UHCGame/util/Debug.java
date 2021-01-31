package xyz.baz9k.UHCGame.util;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public final class Debug {
    private Debug() { }
    private static boolean debug = false;

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
            sender.sendMessage(ChatColor.RED + e.toString());
            for (var el : e.getStackTrace()) {
                sender.sendMessage(ChatColor.RED + el.toString());
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
            Bukkit.broadcastMessage(ChatColor.RED + e.toString());
            for (var el : e.getStackTrace()) {
                Bukkit.broadcastMessage(ChatColor.RED + el.toString());
            }
        } else {
            e.printStackTrace();
        }
    }

    private static String fmtDebug(String msg) {
        return String.format("%s[DEBUG] %s", ChatColor.YELLOW, msg);
    }

    /**
     * Broadcast a debug message in chat
     * @param msg
     */
    public static void broadcastDebug(String msg) {
        if (debug) {
            Bukkit.broadcastMessage(fmtDebug(msg));
        }
    }
}
