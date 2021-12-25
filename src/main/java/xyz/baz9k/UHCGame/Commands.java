package xyz.baz9k.UHCGame;

import dev.jorel.commandapi.*;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.arguments.ScoreHolderArgument.*;
import dev.jorel.commandapi.arguments.CustomArgument.*;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.exceptions.*;
import dev.jorel.commandapi.wrappers.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import xyz.baz9k.UHCGame.util.Debug;
import xyz.baz9k.UHCGame.util.TeamDisplay;
import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

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
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

@SuppressWarnings("unchecked")
public class Commands {
    private final UHCGamePlugin plugin;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private static @interface RegisterUHCSubCommand { }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private static @interface RegisterCommand { }

    public Commands(UHCGamePlugin plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {
        CommandAPICommand uhc = new CommandAPICommand("uhc")
            .withPermission(CommandPermission.OP)
            .executesPlayer((sender, args) -> {
                plugin.getMenuManager().openMenu(sender);
            });
            
        // register each @RegisterUHCSubCommand method
        var subAnnot = RegisterUHCSubCommand.class;
        var cmdAnnot = RegisterCommand.class;
        var cls      = Commands.class;
        try {
            for (Method m : cls.getDeclaredMethods()) {
                if (m.isAnnotationPresent(subAnnot)) {
                    uhc.withSubcommand((CommandAPICommand) m.invoke(this));
                } else if (m.isAnnotationPresent(cmdAnnot)) {
                    ((CommandAPICommand) m.invoke(this)).register();
                }
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
    /uhc teams assign <solos|duos|trios|quartets|quintets>
    /uhc teams assign <n: int>
    /uhc teams clear
    /uhc reseed <seed: str>
    /uhc respawn <target: players>
    /uhc respawn <target: players> <loc: location>
    /uhc state get <target: players>
    /uhc state set <target: players> <spectator|combatant>
    /uhc state set <target: players> <combatant> <team: int>
    /uhc stage next
    /uhc stage set <stage: stage>
    /uhc hasstarted
    /uhc escape
    /uhc debug
    /uhc debug <true|false>
    /uhc config
    /uhc config wipe
    /uhc config get <path: str>
     */

    // uhc start
    @RegisterUHCSubCommand
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

    // uhc end
    @RegisterUHCSubCommand
    private CommandAPICommand end() {
        return new CommandAPICommand("end")
        .executes(
            (sender, args) -> {
                try {
                    plugin.getGameManager().endUHC(false);
                } catch (IllegalStateException e) {
                    CommandAPI.fail(e.getMessage());
                    Debug.printError(e);
                }
            }
        );
    }

    // uhc start force
    @RegisterUHCSubCommand
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
                    Debug.printError(e);
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

    // uhc teams assign <solos|duos|trios|quartets|quintets>
    @RegisterUHCSubCommand
    private CommandAPICommand assignTeamsLiteral() {
        return new CommandAPICommand("teams")
        .withArguments(
            new LiteralArgument("assign"),
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

    // uhc teams assign <n: int>
    @RegisterUHCSubCommand
    private CommandAPICommand assignTeamsNTeams() {
        return new CommandAPICommand("teams")
        .withArguments(
            new LiteralArgument("assign"),
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

    // uhc teams clear
    @RegisterUHCSubCommand
    private CommandAPICommand clearTeams() {
        return new CommandAPICommand("teams")
        .withArguments(
            new LiteralArgument("clear")
        )
        .executes(
            (sender, args) -> {
                TeamManager tm = plugin.getTeamManager();
                tm.resetAllPlayers();
                sender.sendMessage(trans("xyz.baz9k.uhc.cmd.clearteams.succ"));
            }
        );
    }

    // uhc reseed
    @RegisterUHCSubCommand
    private CommandAPICommand reseed() {
        // reseeds worlds
        return new CommandAPICommand("reseed")
        .executes(
            (sender, args) -> {
                Bukkit.getServer().sendMessage(trans("xyz.baz9k.uhc.cmd.reseed.start").color(NamedTextColor.YELLOW));
                plugin.getWorldManager().reseedWorlds();
                Bukkit.getServer().sendMessage(trans("xyz.baz9k.uhc.cmd.reseed.succ").color(NamedTextColor.YELLOW));
            }
        );
    }

    // uhc reseed <seed: str>
    @RegisterUHCSubCommand
    private CommandAPICommand reseedSpecified() {
        // reseeds worlds
        return new CommandAPICommand("reseed")
        .withArguments(
            new LongArgument("seed")
        )
        .executes(
            (sender, args) -> {
                Bukkit.getServer().sendMessage(trans("xyz.baz9k.uhc.cmd.reseed.start").color(NamedTextColor.YELLOW));
                plugin.getWorldManager().reseedWorlds((long) args[0], false);
                Bukkit.getServer().sendMessage(trans("xyz.baz9k.uhc.cmd.reseed.succ").color(NamedTextColor.YELLOW));
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

    // uhc respawn <target: players>
    @RegisterUHCSubCommand
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

    // uhc respawn <target: players> <loc: location>
    @RegisterUHCSubCommand
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

    // uhc state get <target: players>
    @RegisterUHCSubCommand
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

    // uhc state set <target: players> <spectator|combatant>
    @RegisterUHCSubCommand
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

    // uhc state set <target: players> <combatant> <team: int>
    @RegisterUHCSubCommand
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

    // uhc stage next
    @RegisterUHCSubCommand
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
        return new CustomArgument<GameStage>(nodeName, info -> {
            try {
                return GameStage.valueOf(info.input());
            } catch (IllegalArgumentException | NullPointerException e) {
                throw new CustomArgumentException(new MessageBuilder("Unknown stage: ").appendArgInput());
            }
            // cmd requires getting the exact GS name, so use GameStage::name, not GameStage::toString
        }).replaceSuggestions(info -> Arrays.stream(GameStage.values()).map(GameStage::name).toArray(String[]::new));
    }

    // uhc stage set <stage: stage>
    @RegisterUHCSubCommand
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

    // uhc hasstarted
    @RegisterUHCSubCommand
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

    // uhc escape
    @RegisterUHCSubCommand
    private CommandAPICommand escape() {
        return new CommandAPICommand("escape")
        .executes(
            (sender, args) -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    plugin.getWorldManager().escapePlayer(p);
                }
            }
        );
    }

    // uhc debug
    @RegisterUHCSubCommand
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

    // uhc debug <true|false>
    @RegisterUHCSubCommand
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

    // uhc config
    @RegisterUHCSubCommand
    private CommandAPICommand config() {
        return new CommandAPICommand("config")
        .executesPlayer(
                (sender, args) -> {
                    requireNotStarted();
                    
                    plugin.getMenuManager().openSubMenu("config", sender);
                }
        );
    }

    // uhc config wipe
    @RegisterUHCSubCommand
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

    // uhc config get <path: str>
    @RegisterUHCSubCommand
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

    ////// BEGIN FUNCTOOLS //////
    // delete as soon as possible //

    @RegisterCommand
    private CommandAPICommand dispatch() {
        // /dispatch, /cmd: runs a command, used to run any plugin command in /execute and functions
        return new CommandAPICommand("dispatch")
        .withArguments(new GreedyStringArgument("command"))
        .withPermission(CommandPermission.OP)
        .withAliases("cmd")
        .executes((sender, args) -> {
            String cmd = (String) args[0];
            Bukkit.dispatchCommand(sender, cmd);
        });
    }

    @RegisterCommand
    private CommandAPICommand forCmd() {
        // /for <var> in m..n run <cmd>
        // /for i in 0..10 run say $i         # (0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)

        return new CommandAPICommand("for")
        .withArguments(
            new StringArgument("var"),
            new LiteralArgument("in"),
            new IntegerRangeArgument("range"),
            new LiteralArgument("run"),
            new GreedyStringArgument("cmd")
        )
        .withPermission(CommandPermission.OP)
        .executes((sender, args) -> {
            String vname = (String) args[0];
            IntegerRange vrange = (IntegerRange) args[1];
            String cmd = (String) args[2];

            try {
                for (int i = vrange.getLowerBound(); i <= vrange.getUpperBound(); i++) {
                    String itercmd = cmd;
                    itercmd = itercmd.replaceAll("(?<!\\\\)\\$" + vname, Integer.toString(i));
                    itercmd = itercmd.replaceAll("\\\\\\$", "\\$");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), itercmd);
                }
            } catch (CommandException e) {
                CommandAPI.fail("An error occurred while running the commands: " + e.getMessage());
            }
        });
    }

    @RegisterCommand
    private CommandAPICommand forStepCmd() {
        // /for <var> in m..n step s run <cmd>
        // /for i in 0..10 step 2 run say $i  # (0, 2, 4, 6, 8, 10)

        return new CommandAPICommand("for")
            .withArguments(
                new StringArgument("var"),
                new LiteralArgument("in"),
                new IntegerRangeArgument("range"),
                new LiteralArgument("step"),
                new IntegerArgument("stepamt", 1),
                new LiteralArgument("run"),
                new GreedyStringArgument("cmd")
            )
            .withPermission(CommandPermission.OP)
            .executes((sender, args) -> {
                String vname = (String) args[0];
                IntegerRange vrange = (IntegerRange) args[1];
                int step = (int) args[2];
                String cmd = (String) args[3];

                try {
                    for (int i = vrange.getLowerBound(); i <= vrange.getUpperBound(); i += step) {
                        String itercmd = cmd;
                        itercmd = itercmd.replaceAll("(?<!\\\\)\\$" + vname, Integer.toString(i));
                        itercmd = itercmd.replaceAll("\\\\\\$", "\\$");
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), itercmd);
                    }
                } catch (CommandException e) {
                    CommandAPI.fail("An error occurred while running the commands: " + e.getMessage());
                }
            });
    }

    @RegisterCommand
    private CommandAPICommand runFnValue() {
        // /runfn <fn> with <player> <objective> as <value> || /runfn <fn> with <player> <objective> as <player> <objective>
        // this fn sets scoreboard entry to <value> / <player> <objective>, runs the function, and returns the value on said scoreboard entry.
        // this simplifies syntax for functions with args and allows them to be used with /for
        // uhhhh, if you want to use multiple arguments just curry I guess

        return new CommandAPICommand("runfunction")
        .withArguments(
            new FunctionArgument("function"),
            new LiteralArgument("with"),
            new ScoreHolderArgument("player", ScoreHolderType.SINGLE),
            new ObjectiveArgument("objective"),
            new LiteralArgument("as"),
            new IntegerArgument("value")
        )
        .withPermission(CommandPermission.OP)
        .withAliases("runfn")
        .executes((sender, args) -> {
            FunctionWrapper[] fns = (FunctionWrapper[]) args[0];
            String inputPl = (String) args[1];
            String inputOb = (String) args[2];
            int inputVal = (int) args[3];
            Score inputScore = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(inputOb).getScore(inputPl);
            inputScore.setScore(inputVal);
            // run fns after arg has been set
            for (FunctionWrapper fn : fns) {
                fn.run();
            }
            // return score for use w/ execute store
            try {
                int retVal = inputScore.getScore();
                if (sender instanceof Player && !(sender instanceof ProxiedCommandSender)) sender.sendMessage("Function returned " + Integer.toString(retVal));
                return retVal; 
            } catch (IllegalArgumentException | IllegalStateException e) {
                CommandAPI.fail("Entry no longer exists");
            }
            return 0;
        });
    }

    @RegisterCommand
    private CommandAPICommand runFnScore() {
        return new CommandAPICommand("runfunction")
        .withArguments(
            new FunctionArgument("function"),
            new LiteralArgument("with"),
            new ScoreHolderArgument("player", ScoreHolderType.SINGLE),
            new ObjectiveArgument("objective"),
            new LiteralArgument("as"),
            new ScoreHolderArgument("player value", ScoreHolderType.SINGLE),
            new ObjectiveArgument("objective value")
        )
        .withPermission(CommandPermission.OP)
        .withAliases("runfn")
        .executes((sender, args) -> {
            FunctionWrapper[] fns = (FunctionWrapper[]) args[0];
            String inputPl = (String) args[1];
            String inputOb = (String) args[2];
            String valuePl = (String) args[3];
            String valueOb = (String) args[4];
            
            // get (pl obj) slot and set it to val, effectively acting as the arg
            Score inputScore = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(inputOb).getScore(inputPl);
            int inputVal = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(valueOb).getScore(valuePl).getScore();
            
            inputScore.setScore(inputVal);
            // run fns after arg has been set
            for (FunctionWrapper fn : fns) {
                fn.run();
            }
            // return score for use w/ execute store
            try {
                int retVal = inputScore.getScore();
                if (sender instanceof Player && !(sender instanceof ProxiedCommandSender)) sender.sendMessage("Function returned " + Integer.toString(retVal));
                return retVal; 
            } catch (IllegalArgumentException | IllegalStateException e) {
                CommandAPI.fail("Entry no longer exists");
            }
            return 0;
        });
    }

    @RegisterCommand
    private CommandAPICommand let() {
        // /let <var> = <player> <objective> run <cmd>: substs var with entry in cmd, same syntax as /for
        return new CommandAPICommand("let")
        .withArguments(
            new StringArgument("var"),
            new LiteralArgument("="),
            new ScoreHolderArgument("player", ScoreHolderType.SINGLE),
            new ObjectiveArgument("objective"),
            new LiteralArgument("run"),
            new GreedyStringArgument("cmd")
        )
        .withPermission(CommandPermission.OP)
        .executes((sender, args) -> {
            String vname = (String) args[0];
            String valPl = (String) args[1];
            String valOb = (String) args[2];
            String cmd = (String) args[3];

            int v = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(valOb).getScore(valPl).getScore();
            cmd = cmd.replaceAll("(?<!\\\\)\\$" + vname, Integer.toString(v));
            cmd = cmd.replaceAll("\\\\\\$", "\\$");
            Bukkit.dispatchCommand(sender, cmd);
        });
    }

    @RegisterCommand
    private CommandAPICommand letScaled() {
        // /let <var> = <player> <objective> <scale: double> run <cmd>: multiplies by scale

        return new CommandAPICommand("let")
        .withArguments(
            new StringArgument("var"),
            new LiteralArgument("="),
            new ScoreHolderArgument("player", ScoreHolderType.SINGLE),
            new ObjectiveArgument("objective"),
            new DoubleArgument("scale"),
            new LiteralArgument("run"),
            new GreedyStringArgument("cmd")
        )
        .withPermission(CommandPermission.OP)
        .executes((sender, args) -> {
            String vname = (String) args[0];
            String valPl = (String) args[1];
            String valOb = (String) args[2];
            double scale = (Double) args[3];
            String cmd = (String) args[4];

            int u = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(valOb).getScore(valPl).getScore();
            double v = (double) u * scale;
            cmd = cmd.replaceAll("(?<!\\\\)\\$" + vname, Double.toString(v));
            cmd = cmd.replaceAll("\\\\\\$", "\\$");
            Bukkit.dispatchCommand(sender, cmd);
        });
    }

    ////// END FUNCTOOLS //////
}
