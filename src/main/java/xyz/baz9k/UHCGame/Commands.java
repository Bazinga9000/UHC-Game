package xyz.baz9k.UHCGame;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import net.md_5.bungee.api.ChatColor;
import xyz.baz9k.UHCGame.util.ColoredText;
import xyz.baz9k.UHCGame.util.Debug;
import xyz.baz9k.UHCGame.util.TeamDisplay;

import java.util.Set;
import java.util.stream.Collectors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;


import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("unchecked")
public class Commands {
    private final UHCGame plugin;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private static @interface Command { }

    public Commands(UHCGame plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {
        CommandAPICommand uhc = new CommandAPICommand("uhc")
                                    .withPermission(CommandPermission.OP);
        // register each @Command method
        Class<Command> annot = Command.class;
        Class<Commands> cls = Commands.class;
        try {
            for (Method m : cls.getDeclaredMethods()) {
                if (!m.isAnnotationPresent(annot)) continue;
                uhc.withSubcommand((CommandAPICommand) m.invoke(this));
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        uhc.register();
    }

    /*
    /uhc start
    /uhc end
    /uhc start force
    /uhc end force
    /uhc assignteams <solos|duos|trios|quartets|quintets|n: int>
    /uhc clearteams
    /uhc reseed (seed: string)
    /uhc respawn <target: players> (loc: location)
    /uhc state get <target: players>
    /uhc state set <target: players> <spectator|combatant>
    /uhc state set <target: players> combatant <team: int>
    /uhc stage next
    /uhc stage set <n: int>
    /uhc hasstarted ~ use w/ /execute store success
    /uhc escape
    /uhc debug
    /uhc debug <on|off>
     */

    @Command
    private CommandAPICommand start() {
        return new CommandAPICommand("start")
        .executes(
            (sender, args) -> {
                try {
                    Debug.broadcastDebug("UHC attempting start");
                    plugin.getGameManager().startUHC(false);
                    Debug.broadcastDebug("UHC started");
                } catch (IllegalStateException e) {
                    CommandAPI.fail(e.getMessage());
                    e.printStackTrace();
                }
            }
        );
    }

    @Command
    private CommandAPICommand end() {
        return new CommandAPICommand("end")
        .executes(
            (sender, args) -> {
                try {
                    Debug.broadcastDebug("UHC attempting end");
                    plugin.getGameManager().endUHC(false);
                    Debug.broadcastDebug("UHC ended");
                } catch (IllegalStateException e) {
                    CommandAPI.fail(e.getMessage());
                    Debug.printError(sender, e);
                }
            }
        );
    }

    @Command
    private CommandAPICommand startForce() {
        return new CommandAPICommand("start")
        .withArguments(
            new LiteralArgument("force")
        )
        .executes(
            (sender, args) -> {
                try {
                    Debug.broadcastDebug("UHC attempting start");
                    Debug.broadcastDebug("Skipping starting requirements");
                    plugin.getGameManager().startUHC(true);
                    Debug.broadcastDebug("UHC started");
                } catch (IllegalStateException e) {
                    CommandAPI.fail(e.getMessage());
                    Debug.printError(sender, e);
                }
            }
        );
    }

    @Command
    private CommandAPICommand endForce() {
        return new CommandAPICommand("end")
        .withArguments(
            new LiteralArgument("force")
        )
        .executes(
            (sender, args) -> {
                try {
                    Debug.broadcastDebug("UHC attempting end");
                    Debug.broadcastDebug("Skipping ending requirements");
                    plugin.getGameManager().endUHC(true);
                    Debug.broadcastDebug("UHC ended");
                } catch (IllegalStateException e) {
                    CommandAPI.fail(e.getMessage());
                    Debug.printError(sender, e);
                }
            }
        );
    }

    private void _announceTeams() {
        TeamManager tm = plugin.getTeamManager();
        for (int i = 1; i <= tm.getNumTeams(); i++) {
            _announceTeamsLine(i);
        }
        _announceTeamsLine(0);

    }

    private void _announceTeamsLine(int t) {
        TeamManager tm = plugin.getTeamManager();
        Set<Player> players;
        if (t == 0) {
            players = tm.getOnlineSpectators();
        } else {
            players = tm.getAllCombatantsOnTeam(t);
        }
        if (players.size() == 0) return;

        var b = new ColoredText()
                .appendColored(TeamDisplay.getName(t))
                .append(": ");

        String str = players.stream()
                            .map(p -> p.getName())
                            .collect(Collectors.joining(", "));
        b.append(str);
        Bukkit.broadcast(b.toComponents());
    }

    @Command
    private CommandAPICommand assignTeamsLiteral() {
        return new CommandAPICommand("assignteams")
        .withArguments(
            new MultiLiteralArgument("solos", "duos", "trios", "quartets", "quintets")
        )
        .executes(
            (sender, args) -> {
                TeamManager tm = plugin.getTeamManager();
                String s = (String) args[0];

                tm.setTeamSize(s);
                tm.assignTeams();
                _announceTeams();

            }
        );
    }

    @Command
    private CommandAPICommand assignTeamsNTeams() {
        return new CommandAPICommand("assignteams")
        .withArguments(
            new IntegerArgument("n", 1)
        )
        .executes(
            (sender, args) -> {
                TeamManager tm = plugin.getTeamManager();
                int n = (int) args[0];

                tm.setNumTeams(n);
                tm.assignTeams();
                _announceTeams();
            }
        );
    }

    @Command
    private CommandAPICommand clearTeams() {
        return new CommandAPICommand("clearteams")
        .executes(
            (sender, args) -> {
                TeamManager tm = plugin.getTeamManager();
                tm.resetAllPlayers();
                sender.sendMessage("All teams have been reset.");
            }
        );
    }

    @Command
    private CommandAPICommand reseed() {
        // reseeds worlds
        return new CommandAPICommand("reseed")
        .executes(
            (sender, args) -> {
                plugin.getGameManager().reseedWorlds();
                sender.sendMessage(ChatColor.GREEN + "Both dimensions have been reseeded successfully.");
            }
        );
    }

    @Command
    private CommandAPICommand reseedSpecified() {
        // reseeds worlds
        return new CommandAPICommand("reseed")
        .withArguments(
            new TextArgument("seed")
        )
        .executes(
            (sender, args) -> {
                plugin.getGameManager().reseedWorlds((String) args[0]);
                sender.sendMessage(ChatColor.GREEN + "Both dimensions have been reseeded successfully.");
            }
        );
    }

    private void _respawn(CommandSender sender, Player p, Location loc) {
        TeamManager tm = plugin.getTeamManager();
        if (tm.isSpectator(p)) {
            sender.sendMessage(String.format("%sCannot respawn spectator %s.", ChatColor.RED, p.getName()));
            return;
        }

        p.teleport(loc);
        tm.setCombatantAliveStatus(p, true);
        p.setGameMode(GameMode.SURVIVAL);
        sender.sendMessage(String.format("Respawned %s!", p.getName()));
    }

    @Command
    private CommandAPICommand respawn() {
        return new CommandAPICommand("respawn")
        .withArguments(
            new EntitySelectorArgument("target", EntitySelector.MANY_PLAYERS)
        )
        .executes(
            (sender, args) -> {
                if (!plugin.getGameManager().hasUHCStarted()) {
                    CommandAPI.fail("Game has not started.");
                    return;
                }
                for (Player p : (Collection<Player>) args[0]) {
                    _respawn(sender, p, p.getBedSpawnLocation());
                }
            }
        );
    }

    @Command
    private CommandAPICommand respawnLoc() {
        return new CommandAPICommand("respawn")
        .withArguments(
            new EntitySelectorArgument("target", EntitySelector.MANY_PLAYERS),
            new LocationArgument("location", LocationType.PRECISE_POSITION)
        )
        .executes(
            (sender, args) -> {
                if (!plugin.getGameManager().hasUHCStarted()) {
                    CommandAPI.fail("Game has not started.");
                    return;
                }
                for (Player p : (Collection<Player>) args[0]) {
                    _respawn(sender, p, (Location) args[1]);
                }
            }
        );
    }

    @Command
    private CommandAPICommand stateGet() {
        return new CommandAPICommand("state")
        .withArguments(
            new LiteralArgument("get"),
            new EntitySelectorArgument("target", EntitySelector.MANY_PLAYERS)
        )
        .executes(
            (sender, args) -> {
                TeamManager tm = plugin.getTeamManager();
                for (Player p : (Collection<Player>) args[0]) {
                    int team = tm.getTeam(p);
                    PlayerState state = tm.getPlayerState(p);
                    sender.sendMessage(String.format("%s is a %s on team %s.", p.getName(), state, team));
                }
            }
        );
    }

    @Command
    private CommandAPICommand stateSet() {
        return new CommandAPICommand("state")
        .withArguments(
            new LiteralArgument("set"),
            new EntitySelectorArgument("target", EntitySelector.MANY_PLAYERS),
            new MultiLiteralArgument("spectator", "combatant")
        )
        .executes(
            (sender, args) -> {
                TeamManager tm = plugin.getTeamManager();
                for (Player p : (Collection<Player>) args[0]) {
                    switch ((String) args[1]) {
                        case "spectator":
                            tm.setSpectator(p);
                            break;
                        case "combatant":
                            tm.setUnassignedCombatant(p);
                            break;
                    }
                    sender.sendMessage(String.format("Set %s to team %s.", p.getName(), tm.getPlayerState(p)));
                }
            }
        );
    }

    @Command
    private CommandAPICommand stateSetTeam() {
        return new CommandAPICommand("state")
        .withArguments(
            new LiteralArgument("set"),
            new EntitySelectorArgument("target", EntitySelector.MANY_PLAYERS),
            new LiteralArgument("combatant"),
            new IntegerArgument("team", 1)
        )
        .executes(
            (sender, args) -> {
                TeamManager tm = plugin.getTeamManager();
                int max = tm.getNumTeams();
                int t = (int) args[1];
                if (t > max) {
                    CommandAPI.fail("Team must not be greater than the number of teams.");
                    return;
                }
                for (Player p : (Collection<Player>) args[0]) {
                    plugin.getTeamManager().assignPlayerToTeam(p, t);
                    sender.sendMessage(String.format("Set %s to team %s.", p.getName(), tm.getPlayerState(p)));
                }
            }
        );
    }

    @Command
    private CommandAPICommand stageNext() {
        return new CommandAPICommand("stage")
        .withArguments(
            new LiteralArgument("next")
        )
        .executes(
            (sender, args) -> {
                GameManager gm = plugin.getGameManager();

                gm.incrementStage();
                sender.sendMessage("Set stage to " + gm.getStage());
            }
        );
    }

    @Command
    private CommandAPICommand stageSet() {
        return new CommandAPICommand("stage")
        .withArguments(
            new LiteralArgument("set"),
            new IntegerArgument("stage")
        )
        .executes(
            (sender, args) -> {
                GameManager gm = plugin.getGameManager();
                GameStage s = GameStage.fromIndex((int) args[0]);

                gm.setStage(s);
                sender.sendMessage("Set stage to " + s);
            }
        );
    }

    @Command
    private CommandAPICommand hasStarted() {
        return new CommandAPICommand("hasstarted")
        .executes(
            (sender, args) -> {
                boolean started = plugin.getGameManager().hasUHCStarted();
                if (started) {
                    sender.sendMessage("UHC has started");
                    return;
                } else {
                    CommandAPI.fail("UHC has not started");
                }
            }
        );
    }

    @Command
    private CommandAPICommand escape() {
        return new CommandAPICommand("escape")
        .executes(
            (sender, args) -> {
                plugin.getGameManager().escapeAll();
            }
        );
    }

    @Command
    private CommandAPICommand debug() {
        return new CommandAPICommand("debug")
        .withAliases("verbose")
        .executes(
            (sender, args) -> {
                Debug.setDebug(!Debug.getDebug());
                boolean d = Debug.getDebug();

                if (d) {
                    sender.sendMessage("Verbose messages on");
                } else {
                    sender.sendMessage("Verbose messages off");
                }
            }
        );
    }

    @Command
    private CommandAPICommand debugOnOff() {
        return new CommandAPICommand("debug")
        .withAliases("verbose")
        .withArguments(new BooleanArgument("status"))
        .executes(
            (sender, args) -> {
                boolean d = (boolean) args[0];
                Debug.setDebug(d);

                if (d) {
                    sender.sendMessage("Verbose messages on");
                } else {
                    sender.sendMessage("Verbose messages off");
                }
            }
        );
    }

}
