package xyz.baz9k.UHCGame.util;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandExecutor;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import dev.jorel.commandapi.executors.PlayerResultingCommandExecutor;
import dev.jorel.commandapi.executors.ResultingCommandExecutor;
import xyz.baz9k.UHCGame.UHCGamePlugin;
import xyz.baz9k.UHCGame.exception.UHCException;

public final class CommandAPIUtils {
    private CommandAPIUtils() {};

    public static void requireNotStarted() throws WrapperCommandSyntaxException {
        try {
            JavaPlugin.getPlugin(UHCGamePlugin.class).getGameManager().requireNotStarted();
        } catch (UHCException e) {
            CommandAPI.fail(e.getMessage());
        }
    }
    public static void requireStarted() throws WrapperCommandSyntaxException {
        try {
            JavaPlugin.getPlugin(UHCGamePlugin.class).getGameManager().requireStarted();
        } catch (UHCException e) {
            CommandAPI.fail(e.getMessage());
        }
    }

    // buncha functional interfaces for allowing UHCException
    // i'm gonna hope i don't need to implement more than these
    @FunctionalInterface
    public interface UHCCommandExecutor extends CommandExecutor {
        void tryRun(CommandSender sender, Object[] args) throws UHCException, WrapperCommandSyntaxException;

        @Override
        default void run(CommandSender sender, Object[] args) throws WrapperCommandSyntaxException {
            try {
                tryRun(sender, args);
            } catch (UHCException e) {
                CommandAPI.fail(e.getMessage());
            }
        }

    }

    @FunctionalInterface
    public interface UHCResultingCommandExecutor extends ResultingCommandExecutor {
        int tryRun(CommandSender sender, Object[] args) throws UHCException, WrapperCommandSyntaxException;

        @Override
        default int run(CommandSender sender, Object[] args) throws WrapperCommandSyntaxException {
            try {
                return tryRun(sender, args);
            } catch (UHCException e) {
                CommandAPI.fail(e.getMessage());
            } catch (Exception e) {
                Debug.printError(e);
            }
            return 0; // unreachable
        }

    }

    @FunctionalInterface
    public interface UHCPlayerCommandExecutor extends PlayerCommandExecutor {
        void tryRun(Player sender, Object[] args) throws UHCException, WrapperCommandSyntaxException;

        @Override
        default void run(Player sender, Object[] args) throws WrapperCommandSyntaxException {
            try {
                tryRun(sender, args);
            } catch (UHCException e) {
                CommandAPI.fail(e.getMessage());
            }
        }

    }

    @FunctionalInterface
    public interface UHCPlayerResultingCommandExecutor extends PlayerResultingCommandExecutor {
        int tryRun(Player sender, Object[] args) throws UHCException, WrapperCommandSyntaxException;

        @Override
        default int run(Player sender, Object[] args) throws WrapperCommandSyntaxException {
            try {
                return tryRun(sender, args);
            } catch (UHCException e) {
                CommandAPI.fail(e.getMessage());
            } catch (Exception e) {
                Debug.printError(e);
            }
            return 0; // unreachable
        }

    }
}
