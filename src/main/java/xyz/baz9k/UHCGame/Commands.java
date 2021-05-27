package xyz.baz9k.UHCGame;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.arguments.CustomArgument.CustomArgumentException;
import dev.jorel.commandapi.arguments.CustomArgument.MessageBuilder;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import xyz.baz9k.UHCGame.util.Debug;
import xyz.baz9k.UHCGame.util.TeamDisplay;
import static xyz.baz9k.UHCGame.util.Utils.*;

import java.util.Set;
import java.util.stream.Collectors;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
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

    private void requireNotStarted() throws WrapperCommandSyntaxException {
        try {
            plugin.getGameManager().requireNotStarted();
        } catch (IllegalStateException e) {
            CommandAPI.fail(e.getMessage());
        }
    }
    private void requireStarted() throws WrapperCommandSyntaxException {
        try {
            plugin.getGameManager().requireStarted();
        } catch (IllegalStateException e) {
            CommandAPI.fail(e.getMessage());
        }
    }

    // private void fail(String key, Object... args) throws WrapperCommandSyntaxException {
    //     CommandAPI.fail(componentString(trans(key, args)));
    // }

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
                    plugin.getGameManager().startUHC(false);
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
                    plugin.getGameManager().endUHC(false);
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
                    plugin.getGameManager().startUHC(true);
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
                    plugin.getGameManager().endUHC(true);
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

        var b = Component.text()
                         .append(TeamDisplay.getName(t))
                         .append(Component.text(": ", noDeco(NamedTextColor.WHITE)));

        // list of players one a team, separated by commas
        String tPlayers = players.stream()
                                 .map(p -> p.getName())
                                 .collect(Collectors.joining(", "));
        
        b.append(Component.text(tPlayers, noDeco(NamedTextColor.WHITE)));
        Bukkit.getServer().sendMessage(b);
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
                sender.sendMessage(trans("xyz.baz9k.uhc.cmd.clearteams.succ"));
            }
        );
    }

    @Command
    private CommandAPICommand reseed() {
        // reseeds worlds
        return new CommandAPICommand("reseed")
        .executes(
            (sender, args) -> {
                sender.sendMessage(trans("xyz.baz9k.uhc.cmd.reseed.start").color(NamedTextColor.YELLOW));
                plugin.getWorldManager().reseedWorlds();
                sender.sendMessage(trans("xyz.baz9k.uhc.cmd.reseed.succ").color(NamedTextColor.YELLOW));
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
                sender.sendMessage(trans("xyz.baz9k.uhc.cmd.reseed.start").color(NamedTextColor.YELLOW));
                plugin.getWorldManager().reseedWorlds((String) args[0]);
                sender.sendMessage(trans("xyz.baz9k.uhc.cmd.reseed.succ").color(NamedTextColor.YELLOW));
            }
        );
    }

    private void _respawn(CommandSender sender, Player p, Location loc) {
        TeamManager tm = plugin.getTeamManager();
        if (tm.isSpectator(p)) {
            sender.sendMessage(trans("xyz.baz9k.uhc.cmd.respawn.fail.spectator", p.getName()).color(NamedTextColor.RED));
            return;
        }

        p.teleport(loc);
        tm.setCombatantAliveStatus(p, true);
        p.setGameMode(GameMode.SURVIVAL);
        sender.sendMessage(trans("xyz.baz9k.uhc.cmd.respawn.succ", p.getName()));
    }

    @Command
    private CommandAPICommand respawn() {
        return new CommandAPICommand("respawn")
        .withArguments(
            new EntitySelectorArgument("target", EntitySelector.MANY_PLAYERS)
        )
        .executes(
            (sender, args) -> {
                requireStarted();
                
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
                requireStarted();
                
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
                    sender.sendMessage(trans("xyz.baz9k.uhc.cmd.state_get.succ", p.getName(), state, team));
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
                        case "spectator" -> tm.setSpectator(p);
                        case "combatant" -> tm.setUnassignedCombatant(p);
                    }
                    sender.sendMessage(trans("xyz.baz9k.uhc.cmd.state_get.succ", p.getName(), tm.getPlayerState(p), tm.getTeam(p)));
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
                int t = (int) args[1];
                try {
                    for (Player p : (Collection<Player>) args[0]) {
                        plugin.getTeamManager().assignPlayerToTeam(p, t);
                        sender.sendMessage(trans("xyz.baz9k.uhc.cmd.state_set.succ", p.getName(), tm.getPlayerState(p), tm.getTeam(p)));
                    }
                } catch (IllegalArgumentException e) {
                    CommandAPI.fail(e.getMessage());
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
                sender.sendMessage(trans("xyz.baz9k.uhc.cmd.stage_set.succ", gm.getStage()));
            }
        );
    }

    private Argument gameStageArgument(String nodeName) {
        return new CustomArgument<GameStage>(nodeName, input -> {
            try {
                return GameStage.valueOf(input);
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new CustomArgumentException(new MessageBuilder("Unknown stage: ").appendArgInput());
            }
        }).overrideSuggestions(sender -> Arrays.stream(GameStage.values()).map(GameStage::toString).toArray(String[]::new));
    }

    @Command
    private CommandAPICommand stageSet() {
        return new CommandAPICommand("stage")
        .withArguments(
            new LiteralArgument("set"),
            gameStageArgument("stage")
        )
        .executes(
            (sender, args) -> {
                GameManager gm = plugin.getGameManager();
                GameStage s = (GameStage) args[0];

                gm.setStage(s);
                sender.sendMessage(trans("xyz.baz9k.uhc.cmd.stage_set.succ", gm.getStage()));
            }
        );
    }

    @Command
    private CommandAPICommand hasStarted() {
        return new CommandAPICommand("hasstarted")
        .executes(
            (sender, args) -> {
                if (plugin.getGameManager().hasUHCStarted()) {
                    sender.sendMessage(trans("xyz.baz9k.uhc.cmd.has_started.succ"));
                    return;
                }
                requireStarted();
            }
        );
    }

    @Command
    private CommandAPICommand escape() {
        return new CommandAPICommand("escape")
        .executes(
            (sender, args) -> {
                Location lobbySpawn = plugin.getWorldManager().lobbySpawn();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.teleport(lobbySpawn);
                }
            }
        );
    }

    @Command
    private CommandAPICommand debug() {
        return new CommandAPICommand("debug")
        .withAliases("verbose")
        .executes(
            (sender, args) -> {
                Debug.setDebug(!Debug.isDebugging());

                String onOff = Debug.isDebugging() ? "xyz.baz9k.uhc.cmd.debug.on" : "xyz.baz9k.uhc.cmd.debug.off";
                sender.sendMessage(trans(onOff));
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
                Debug.setDebug((boolean) args[0]);

                String onOff = Debug.isDebugging() ? "xyz.baz9k.uhc.cmd.debug.on" : "xyz.baz9k.uhc.cmd.debug.off";
                sender.sendMessage(trans(onOff));
            }
        );
    }

    @Command
    private CommandAPICommand config() {
        return new CommandAPICommand("config")
        .executesPlayer(
                (sender, args) -> {
                    requireNotStarted();
                    
                    ConfigManager cfgManager = plugin.getConfigManager();
                    cfgManager.openMenu(sender);
                }
        );
    }

    @Command
    private CommandAPICommand configWipe() {
        return new CommandAPICommand("config")
        .withArguments(new LiteralArgument("wipe"))
        .executes(
                (sender, args) -> {
                    this.plugin.saveResource("config.yml", true);
                    this.plugin.reloadConfig();
                }
        );
    }

    @Command
    private CommandAPICommand configGet() {
        return new CommandAPICommand("config")
        .withArguments(new LiteralArgument("get"))
        .withArguments(new StringArgument("path"))
        .executes(
                (sender, args) -> {
                    sender.sendMessage(Component.text(plugin.getConfig().getString((String) args[0])));
                }
        );
    }

}
