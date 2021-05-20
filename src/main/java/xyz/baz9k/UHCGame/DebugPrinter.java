package xyz.baz9k.UHCGame;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import static xyz.baz9k.UHCGame.util.Utils.*;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;

public final class DebugPrinter {
    private UHCGame plugin;
    private Logger logger;

    private boolean debug = true;
    public DebugPrinter(UHCGame plugin) { 
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public boolean isDebugging() {
        return debug && logger != null;
    }
    public void setDebug(boolean d) {
        debug = d;
    }
    
    private static Audience onlinePlayers() {
        return Audience.audience(Bukkit.getOnlinePlayers());
    }

    /**
     * Display a stack trace error to an audience and also print a log
     * @param audience
     * @param e
     */
    public void printError(Audience audience, Throwable err) {
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
    public void printError(Throwable e) {
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
    public void broadcastDebug(String msg) {
        broadcastDebug(Component.text(msg));
    }

    /**
     * Broadcast a debug message in chat
     * @param msg
     */
    public void broadcastDebug(Component msg) {
        if (isDebugging()) {
            logger.log(Level.INFO, componentString(plugin.getLangManager().getLocale(), fmtDebug(msg)));
            onlinePlayers().sendMessage(fmtDebug(msg));
        }
    }
}
