package xyz.baz9k.UHCGame;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;

import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import org.bukkit.Bukkit;
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
    /spectator <player>
    /combatant <player>
    /setteam <player> <team : int>
    /getteamdata <player>
    /stage next
    /stage set <n>
     */

    private void uhcStart() {
        new CommandAPICommand("uhc")
        .withPermission(CommandPermission.OP)
        .withArguments(
            new LiteralArgument("start")
        )
        .executes(
            (sender, args) -> {
                try {
                    Bukkit.broadcastMessage("UHC attempting start");
                    plugin.getGameManager().startUHC();
                    Bukkit.broadcastMessage("UHC started");
                } catch (IllegalStateException e) {
                    CommandAPI.fail("UHC has already started!");
                }
            }
        ).register();
    }

    private void uhcEnd() {
        new CommandAPICommand("uhc")
        .withPermission(CommandPermission.OP)
        .withArguments(
            new LiteralArgument("end")
        )
        .executes(
            (sender, args) -> {
                try {
                    Bukkit.broadcastMessage("UHC attempting end");
                    plugin.getGameManager().endUHC();
                    Bukkit.broadcastMessage("UHC ended");
                } catch (IllegalStateException e) {
                    CommandAPI.fail("UHC has not started!");
                }
            }
        ).register();
    }

    private void randomizeTeamsLiteral() {
        new CommandAPICommand("randomizeteams")
        .withArguments(
            new MultiLiteralArgument("solos", "duos", "trios", "quartets", "quintets")
        )
        .executes(
            (sender, args) -> {
                int pPerTeam = groupMap.get((String) args[0]);
                TeamManager tm = plugin.getGameManager().getTeamManager();
                List<Player> combatants = tm.getAllCombatants();

                if (combatants.size() % pPerTeam != 0) {
                    CommandAPI.fail("Cannot separate combatants into " + args[0] + ".");
                    return;
                }
                int nTeams = combatants.size() / pPerTeam;
                Collections.shuffle(combatants);
                int i = 1;
                for (Player p : combatants) {
                    tm.assignPlayerTeam(p, i);
                    i = (i + 1) % nTeams;
                }
                tm.setNumTeams(nTeams);

            }
        ).register();
    }

    private void randomizeTeamsNTeams() {
        new CommandAPICommand("randomizeteams")
        .withArguments(
            new IntegerArgument("n", 1)
        )
        .executes(
            (sender, args) -> {
                TeamManager tm = plugin.getGameManager().getTeamManager();
                List<Player> combatants = tm.getAllCombatants();

                int nTeams = (int) args[0];
                Collections.shuffle(combatants);
                int i = 1;
                for (Player p : combatants) {
                    tm.assignPlayerTeam(p, i);
                    i = (i + 1) % nTeams;
                }
                tm.setNumTeams(nTeams);
            }
        ).register();
    }

    private void spectator() {
        new CommandAPICommand("spectator")
        .withArguments(
            new EntitySelectorArgument("target", EntitySelector.MANY_PLAYERS)
        )
        .executes(
            (sender, args) -> {
                for (Player p : (Collection<Player>) args[0]) {
                    plugin.getGameManager().getTeamManager().setSpectator(p);
                    sender.sendMessage("Set " + p.getName() + " to state SPECTATOR.");
                }
            }
        ).register();
    }

    private void combatant() {
        new CommandAPICommand("combatant")
        .withArguments(
            new EntitySelectorArgument("target", EntitySelector.MANY_PLAYERS)
        )
        .executes(
            (sender, args) -> {
                for (Player p : (Collection<Player>) args[0]) {
                    plugin.getGameManager().getTeamManager().setUnassignedCombatant(p);
                    sender.sendMessage("Set " + p.getName() + " to state COMBATANT_UNASSIGNED.");
                }
            }
        ).register();
}

    private void setTeam() {
        int numTeams = plugin.getGameManager().getTeamManager().getNumTeams();

        new CommandAPICommand("setteam")
        .withArguments(
            new EntitySelectorArgument("target", EntitySelector.MANY_PLAYERS),
            new IntegerArgument("team", 1, numTeams)
        )
        .executes(
            (sender, args) -> {
                for (Player p : (Collection<Player>) args[0]) {
                    int t = (int) args[1];
                    plugin.getGameManager().getTeamManager().assignPlayerTeam(p, t);
                    sender.sendMessage("Set " + p.getName() + " to team " + t);
                }
            }
        ).register();
    }

    private void getTeamData() {
        new CommandAPICommand("getteamdata")
        .withArguments(
            new EntitySelectorArgument("target", EntitySelector.MANY_PLAYERS)
        )
        .executes(
            (sender, args) -> {
                TeamManager tm = plugin.getGameManager().getTeamManager();
                for (Player p : (Collection<Player>) args[0]) {
                    int team = tm.getTeam(p);
                    PlayerState state = tm.getPlayerState(p);
                    sender.sendMessage(p.getName() + " is a " + state + " on team " + team);
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

    void registerAll() {
        uhcStart();
        uhcEnd();
        spectator();
        combatant();
        setTeam();
        getTeamData();
        stageNext();
        stageSet();
        randomizeTeamsLiteral();
        randomizeTeamsNTeams();
    }
}
