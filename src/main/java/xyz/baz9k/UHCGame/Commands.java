package xyz.baz9k.UHCGame;

import dev.jorel.commandapi.*;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.arguments.ScoreHolderArgument.*;
import dev.jorel.commandapi.arguments.CustomArgument.*;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.exceptions.*;
import dev.jorel.commandapi.wrappers.*;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import xyz.baz9k.UHCGame.util.Debug;

import static xyz.baz9k.UHCGame.util.CommandAPIUtils.*;
import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.*;

@SuppressWarnings("unchecked")
public final class Commands {
    private final UHCGamePlugin plugin;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private @interface RegisterUHCSubCommand { }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private @interface RegisterCommand { }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    private @interface RegisterBrigadierCommand { }

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
        var subAnnot  = RegisterUHCSubCommand.class;
        var cmdAnnot  = RegisterCommand.class;
        var bCmdAnnot = RegisterBrigadierCommand.class;
        var cls      = Commands.class;
        try {
            for (Method m : cls.getDeclaredMethods()) {
                if (m.isAnnotationPresent(subAnnot)) {
                    uhc.withSubcommand((CommandAPICommand) m.invoke(this));
                } else if (m.isAnnotationPresent(cmdAnnot)) {
                    ((CommandAPICommand) m.invoke(this)).register();
                } else if (m.isAnnotationPresent(bCmdAnnot)) {
                    m.invoke(this);
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        uhc.register();
    }

    private void fail(Key key, Object... args) throws WrapperCommandSyntaxException {
        CommandAPI.fail(renderString(key.trans(args)));
    }

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
        .executes((UHCCommandExecutor)
            (sender, args) -> {
                plugin.getGameManager().startUHC(false);
            }
        );
    }

    // uhc end
    @RegisterUHCSubCommand
    private CommandAPICommand end() {
        return new CommandAPICommand("end")
        .executes((UHCCommandExecutor)
            (sender, args) -> {
                plugin.getGameManager().endUHC(false);

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
        .executes((UHCCommandExecutor)
            (sender, args) -> {
                plugin.getGameManager().startUHC(true);
            }
        );
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
                tm.announceTeams();

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
                tm.announceTeams();
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
                sender.sendMessage(new Key("cmd.clearteams.succ").trans());
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
                Bukkit.getServer().sendMessage(new Key("cmd.reseed.start").trans().color(NamedTextColor.YELLOW));
                plugin.getWorldManager().reseedWorlds();
                Bukkit.getServer().sendMessage(new Key("cmd.reseed.succ").trans().color(NamedTextColor.YELLOW));
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
                Bukkit.getServer().sendMessage(new Key("cmd.reseed.start").trans().color(NamedTextColor.YELLOW));
                plugin.getWorldManager().reseedWorlds((long) args[0], false);
                Bukkit.getServer().sendMessage(new Key("cmd.reseed.succ").trans().color(NamedTextColor.YELLOW));
            }
        );
    }

    private void _respawn(CommandSender sender, Player p, Location loc) throws WrapperCommandSyntaxException {
        TeamManager tm = plugin.getTeamManager();
        if (tm.isSpectator(p)) {
            fail(new Key("cmd.respawn.fail.spectator"), p.getName());
            return;
        }

        p.teleport(loc);
        tm.setCombatantAliveStatus(p, true);
        p.setGameMode(GameMode.SURVIVAL);
        
        // clear all potion effects
        for (PotionEffect effect : p.getActivePotionEffects()) {
            p.removePotionEffect(effect.getType());
        }

        sender.sendMessage(new Key("cmd.respawn.succ").trans(p.getName()));
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
                    sender.sendMessage(new Key("cmd.state_get.succ").trans(p.getName(), state, team));
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
                    sender.sendMessage(new Key("cmd.state_get.succ").trans(p.getName(), tm.getPlayerState(p), tm.getTeam(p)));
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
                        sender.sendMessage(new Key("cmd.state_set.succ").trans(p.getName(), tm.getPlayerState(p), tm.getTeam(p)));
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
                sender.sendMessage(new Key("cmd.stage_set.succ").trans(gm.getStage()));
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
                sender.sendMessage(new Key("cmd.stage_set.succ").trans(gm.getStage()));
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
                    sender.sendMessage(new Key("cmd.has_started.succ").trans());
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

                Key onOff = new Key("cmd.debug.%s", Debug.isDebugging() ? "on" : "off");
                sender.sendMessage(onOff.trans());
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

                Key onOff = new Key("cmd.debug.%s", Debug.isDebugging() ? "on" : "off");
                sender.sendMessage(onOff.trans());
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
                    Object o = plugin.getConfig().get((String) args[0]);
                    sender.sendMessage(Component.text(Objects.toString(o)));
                }
        );
    }

    @RegisterCommand
    private CommandAPICommand invSee() {
        return new CommandAPICommand("invsee")
        .withArguments(new EntitySelectorArgument("target", EntitySelector.ONE_PLAYER))
        .executesPlayer((sender, args) -> {
            requireStarted();
            if (!plugin.getTeamManager().getPlayerState(sender).isSpectating()) {
                fail(new Key("cmd.invsee.fail.combatant"));
            }

            plugin.getMenuManager().invSee(sender, (Player) args[0]);
        });
    }

    @RegisterCommand
    private CommandAPICommand teamChannel() {
        return new CommandAPICommand("teamchannel")
            .withAliases("tc", "teamchan")
            .withArguments(new GreedyStringArgument("msg"))
            .executesPlayer((sender, args) -> {
                requireStarted();
                String msg = (String) args[0];

                var tm = plugin.getTeamManager();
                var buds = tm.getChatBuddies(sender);

                Component groupName = buds.groupName();
                Audience aud = buds.audience();
                
                Component recipientText = Component.translatable("chat.type.team.text", 
                    groupName, sender.displayName(), Component.text(msg));
                Component senderText = Component.translatable("chat.type.team.sent", 
                    groupName, sender.displayName(), Component.text(msg));
                
                sender.sendMessage(senderText);
                aud.sendMessage(recipientText);
            });
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

    @RegisterBrigadierCommand
    private void ifPlayerState() {
        var playerState = Brigadier.fromLiteralArgument(new LiteralArgument("playerstate")).build();
        for (PlayerState state : PlayerState.values()) {
            List<Argument> arguments = List.of(
                new EntitySelectorArgument("target", EntitySelector.ONE_PLAYER)
            );
            var playerNode = Brigadier.fromArgument(arguments, "target");
            var stateNode = Brigadier.fromLiteralArgument(new LiteralArgument(state.name()))
            //Fork redirecting to "execute" and state our predicate
            .fork(Brigadier.getRootNode().getChild("execute"), Brigadier.fromPredicate((sender, args) -> {
                Player p = (Player) args[0];
                return plugin.getTeamManager().getPlayerState(p) == state;
            }, arguments));

            playerState.addChild(playerNode.then(stateNode).build());
        }
        Brigadier.getRootNode().getChild("execute").getChild("if").addChild(playerState);
        
    }

    @RegisterBrigadierCommand
    private void ifPlayerTeam() {
        var playerState = Brigadier.fromLiteralArgument(new LiteralArgument("playerteam")).build();
        List<Argument> arguments = List.of(
            new EntitySelectorArgument("target", EntitySelector.ONE_PLAYER),
            new IntegerArgument("team")
        );
        var playerNode = Brigadier.fromArgument(arguments, "target");
        var teamNode = Brigadier.fromArgument(arguments, "team")
        //Fork redirecting to "execute" and state our predicate
        .fork(Brigadier.getRootNode().getChild("execute"), Brigadier.fromPredicate((sender, args) -> {
            Player p = (Player) args[0];
            int t = (int) args[1];
            return plugin.getTeamManager().getTeam(p) == t;
        }, arguments));

        playerState.addChild(playerNode.then(teamNode).build());
        Brigadier.getRootNode().getChild("execute").getChild("if").addChild(playerState);
        
    }

    ////// END FUNCTOOLS //////
}
