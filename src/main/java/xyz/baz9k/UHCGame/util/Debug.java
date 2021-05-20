package xyz.baz9k.UHCGame.util;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import static xyz.baz9k.UHCGame.util.Utils.*;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;

public final class Debug {
    private Debug() { }
    private static boolean debug = true;
    private static Logger logger = null;

    public static boolean isDebugging() {
        return debug && logger != null;
    }
    public static void setDebug(boolean d) {
        debug = d;
    }
    public static void setLogger(Logger log) {
        logger = log;
    }
    
    public static Audience onlinePlayers() {
        return Audience.audience(Bukkit.getOnlinePlayers());
    }
    /**
     * Display a stack trace error to an audience and also print a log
     * @param audience
     * @param e
     */
    public static void printError(Audience audience, Throwable err) {
        if (isDebugging()) {
            audience.sendMessage(Component.text(err.toString(), NamedTextColor.RED));
            for (var line : err.getStackTrace()) {
                audience.sendMessage(Component.text(line.toString(), NamedTextColor.RED));
            }
        }

        logger.log(Level.SEVERE, err.getMessage(), err);
    }

    /**
     * Display a stack trace error to chat and also print a log
     * @param e
     */
    public static void printError(Throwable e) {
        printError(onlinePlayers(), e);
    }

    private static Component fmtDebug(Component msg) {
        return Component.text("[DEBUG]", NamedTextColor.YELLOW)
            .append(Component.space())
            .append(msg);
    }

    /**
     * Broadcast a debug message in chat
     * @param msg
     */
    public static void broadcastDebug(String msg) {
        broadcastDebug(Component.text(msg));
    }

    /**
     * Broadcast a debug message in chat
     * @param msg
     */
    public static void broadcastDebug(Component msg) {
        if (isDebugging()) {
            logger.log(Level.INFO, componentString(Locale.US, fmtDebug(msg)));
            onlinePlayers().sendMessage(fmtDebug(msg));
        }
    }
}
