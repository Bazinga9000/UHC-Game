package xyz.baz9k.UHCGame;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import net.md_5.bungee.api.ChatColor;
import xyz.baz9k.UHCGame.util.ColoredStringBuilder;
import xyz.baz9k.UHCGame.util.TeamDisplay;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;


import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("unchecked")
public class Commands {
    private final UHCGame plugin;
    private static HashMap<String, Integer> groupMap;
    static {
        groupMap = new HashMap<>();
        groupMap.put("solos", 1);
        groupMap.put("duos", 2);
        groupMap.put("trios", 3);
        groupMap.put("quartets", 4);
        groupMap.put("quintets", 5);
    }

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
    /uhc config
    /uhc config get <id>
    /uhc config set <type> <id> <value>
     */

    @Command
    private CommandAPICommand start() {
        return new CommandAPICommand("start")
        .executes(
            (sender, args) -> {
                try {
                    Bukkit.broadcastMessage("[DEBUG] UHC attempting start");
                    plugin.getGameManager().startUHC(false);
                    Bukkit.broadcastMessage("[DEBUG] UHC started");
                } catch (IllegalStateException e) {
                    CommandAPI.fail(e.getMessage());
                    throw e;
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
                    Bukkit.broadcastMessage("[DEBUG] UHC attempting end");
                    plugin.getGameManager().endUHC(false);
                    Bukkit.broadcastMessage("[DEBUG] UHC ended");
                } catch (IllegalStateException e) {
                    CommandAPI.fail(e.getMessage());
                    throw e;
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
                    Bukkit.broadcastMessage("[DEBUG] UHC attempting start");
                    Bukkit.broadcastMessage("[DEBUG] Skipping starting requirements");
                    plugin.getGameManager().startUHC(true);
                    Bukkit.broadcastMessage("[DEBUG] UHC started");
                } catch (IllegalStateException e) {
                    CommandAPI.fail(e.getMessage());
                    throw e;
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
                    Bukkit.broadcastMessage("[DEBUG] UHC attempting end");
                    Bukkit.broadcastMessage("[DEBUG] Skipping ending requirements");
                    plugin.getGameManager().endUHC(true);
                    Bukkit.broadcastMessage("[DEBUG] UHC ended");
                } catch (IllegalStateException e) {
                    CommandAPI.fail(e.getMessage());
                    throw e;
                }
            }
        );
    }

    private void _assignTeams(int n) {
        TeamManager tm = plugin.getTeamManager();
        List<Player> combatants = new ArrayList<>(tm.getAllOnlineCombatants());

        Collections.shuffle(combatants);
        tm.setNumTeams(n);

        int i = 1;
        for (Player p : combatants) {
            tm.assignPlayerTeam(p, i);
            i = i % n + 1;
        }
        _announceTeams();
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
            players = tm.getAllOnlineSpectators();
        } else {
            players = tm.getAllCombatantsOnTeam(t);
        }
        if (players.size() == 0) return;

        ColoredStringBuilder b;
        b = ColoredStringBuilder.of(TeamDisplay.getName(t))
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
                int pPerTeam = groupMap.get((String) args[0]);
                TeamManager tm = plugin.getTeamManager();
                int combSize = tm.getAllOnlineCombatants().size();

                if (combSize % pPerTeam != 0) {
                    CommandAPI.fail("Cannot separate combatants into " + args[0] + ".");
                    return;
                }
                _assignTeams(combSize / pPerTeam);

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
                _assignTeams((int) args[0]);
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

    private void _reseed(CommandSender sender, String seed) {
        MVWorldManager wm = plugin.getMVWorldManager();
        for (MultiverseWorld mvWorld : plugin.getGameManager().getMVUHCWorlds()) {
            wm.regenWorld(mvWorld.getName(), true, false, seed);
        }
        plugin.getGameManager().setWorldsRegenedStatus(true);
        sender.sendMessage(ChatColor.GREEN + "Both dimensions have been reseeded successfully.");

    }

    @Command
    private CommandAPICommand reseed() {
        // reseeds worlds
        return new CommandAPICommand("reseed")
        .executes(
            (sender, args) -> {
                long seed = new Random().nextLong();
                _reseed(sender, String.valueOf(seed));
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
                _reseed(sender, (String) args[0]);
            }
        );
    }

    private void _respawn(CommandSender sender, Player p, Location loc) {
        TeamManager tm = plugin.getTeamManager();
        if (tm.isSpectator(p)) {
            sender.sendMessage(ChatColor.RED + "Cannot respawn spectator " + p.getName() + ".");
            return;
        }

        p.teleport(loc);
        tm.setCombatantAliveStatus(p, true);
        p.setGameMode(GameMode.SURVIVAL);
    }

    @Command
    private CommandAPICommand respawn() {
        return new CommandAPICommand("respawn")
        .withArguments(
            new EntitySelectorArgument("target", EntitySelector.MANY_PLAYERS)
        )
        .executes(
            (sender, args) -> {
                if (!plugin.getGameManager().isUHCStarted()) {
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
                if (!plugin.getGameManager().isUHCStarted()) {
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
                    sender.sendMessage(p.getName() + " is a " + state + " on team " + team);
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
                    sender.sendMessage("Set " + p.getName() + " to state " + tm.getPlayerState(p) + ".");
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
                    plugin.getTeamManager().assignPlayerTeam(p, t);
                    sender.sendMessage("Set " + p.getName() + " to team " + t);
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
                gm.setStage((int)args[0]);
            }
        );
    }

    @Command
    private CommandAPICommand config() {
        return new CommandAPICommand("config")
        .executesPlayer(
            (sender, args) -> {
                ConfigManager cfgManager = plugin.getConfigManager();
                cfgManager.openMenu(sender);
            }
        );
    }
}
