package xyz.baz9k.UHCGame;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import net.md_5.bungee.api.ChatColor;
import xyz.baz9k.UHCGame.util.ColoredStringBuilder;
import xyz.baz9k.UHCGame.util.TeamColors;

import java.util.List;
import java.util.Random;

import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;

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

    public Commands(UHCGame plugin) {
        this.plugin = plugin;
    }
    /*
    /uhc start
    /uhc end
    /assignteams <solos|duos|trios|quartets|quintets|n: int>
    /clearteams
    /reseed (seed: string)
    /respawn <target: players> (loc: location)
    /state get <target: players>
    /state set <target: players> <spectator|combatant>
    /state set <target: players> combatant <team: int>
    /stage next
    /stage set <n: int>
    /config - WIP
     */

    private void uhcStartEnd() {
        new CommandAPICommand("uhc")
        .withPermission(CommandPermission.OP)
        .withArguments(
            new MultiLiteralArgument("start", "end")
        )
        .executes(
            (sender, args) -> {
                try {
                    switch ((String) args[0]) {
                        case "start":
                        Bukkit.broadcastMessage("[DEBUG] UHC attempting start");
                        plugin.getGameManager().startUHC();
                        Bukkit.broadcastMessage("[DEBUG] UHC started");
                        break;
                        case "end":
                        Bukkit.broadcastMessage("[DEBUG] UHC attempting end");
                        plugin.getGameManager().endUHC();
                        Bukkit.broadcastMessage("[DEBUG] UHC ended");
                    }
                } catch (IllegalStateException e) {
                    CommandAPI.fail(e.getMessage());
                    throw e;
                }
            }
        ).register();
    }

    private void _assignTeams(int n) {
        TeamManager tm = plugin.getTeamManager();
        List<Player> combatants = tm.getAllOnlineCombatants();

        Collections.shuffle(combatants);
        tm.setNumTeams(n);

        int i = 1;
        for (Player p : combatants) {
            tm.assignPlayerTeam(p, i);
            i = (i + 1) % n;
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
        List<Player> players;
        if (t == 0) {
            players = tm.getAllOnlineSpectators();
        } else {
            players = tm.getAllCombatantsOnTeam(t);
        }
        if (players.size() == 0) return;

        ColoredStringBuilder s = new ColoredStringBuilder();
        if (t == 0) {
            s.append("Spectators", TeamColors.getTeamChatColor(0), ChatColor.ITALIC);
        } else {
            s.append("Team " + t, TeamColors.getTeamChatColor(t), ChatColor.BOLD);
        }
        s.append(": ");
        List<String> plStrs = new ArrayList<>();
        for (Player p : players) {
            plStrs.add(p.toString());
        }
        s.append(String.join(", ", plStrs));
        Bukkit.broadcastMessage(s.toString());
    }

    private void assignTeamsLiteral() {
        new CommandAPICommand("assignteams")
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
        ).register();
    }

    private void assignTeamsNTeams() {
        new CommandAPICommand("assignteams")
        .withArguments(
            new IntegerArgument("n", 1)
        )
        .executes(
            (sender, args) -> {
                _assignTeams((int) args[0]);
            }
        ).register();
    }

    private void clearTeams() {
        new CommandAPICommand("clearteams")
        .executes(
            (sender, args) -> {
                TeamManager tm = plugin.getTeamManager();
                tm.resetAllPlayers();
                sender.sendMessage("All teams have been reset.");
            }
        )
    }
    private void _reseed(CommandSender sender, String seed) {
        MVWorldManager wm = plugin.getMVWorldManager();
        for (MultiverseWorld mvWorld : plugin.getGameManager().getMVUHCWorlds()) {
            wm.regenWorld(mvWorld.getName(), true, false, seed);
        }
        sender.sendMessage(ChatColor.GREEN + "Both dimensions have been reseeded successfully.");

    }
    private void reseed() {
        // reseeds worlds
        new CommandAPICommand("reseed")
        .executes(
            (sender, args) -> {
                long seed = new Random().nextLong();
                _reseed(sender, String.valueOf(seed));
            }
        ).register();
    }
    private void reseedSpecified() {
        // reseeds worlds
        new CommandAPICommand("reseed")
        .withArguments(
            new TextArgument("seed")
        )
        .executes(
            (sender, args) -> {
                _reseed(sender, (String) args[0]);
            }
        ).register();
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
    private void respawn() {
        new CommandAPICommand("respawn")
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
        ).register();
    }

    private void respawnLoc() {
        new CommandAPICommand("respawn")
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
        ).register();
    }

    private void stateGet() {
        new CommandAPICommand("state")
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
        ).register();
    }

    private void stateSet() {
        new CommandAPICommand("state")
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
        ).register();
    }

    private void stateSetTeam() {
        new CommandAPICommand("state")
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
        ).register();
    }

    private void stageNext() {
        new CommandAPICommand("stage")
        .withArguments(
            new LiteralArgument("next")
        )
        .withPermission(CommandPermission.OP)
        .executes(
            (sender, args) -> {
                GameManager gm = plugin.getGameManager();
                gm.incrementStage();
            }
        ).register();
    }
    private void stageSet() {
        new CommandAPICommand("stage")
        .withArguments(
            new LiteralArgument("set"),
            new IntegerArgument("stage")
        )
        .withPermission(CommandPermission.OP)
        .executes(
            (sender, args) -> {
                GameManager gm = plugin.getGameManager();
                gm.setStage((int)args[0]);
            }
        ).register();
    }

    private void config() {
        new CommandAPICommand("config")
        .executesPlayer(
            (sender, args) -> {
                ConfigManager cfgManager = plugin.getConfigManager();
                cfgManager.openMenu(sender);
            }
        ).register();
    }

    public void registerAll() {
        uhcStartEnd();
        assignTeamsLiteral();
        assignTeamsNTeams();
        clearTeams();
        reseed();
        reseedSpecified();
        respawn();
        respawnLoc();
        stateGet();
        stateSet();
        stateSetTeam();
        stageNext();
        stageSet();
        config();
    }
}
