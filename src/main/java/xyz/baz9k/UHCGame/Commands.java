package xyz.baz9k.UHCGame;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class Commands {
    private final UHCGame plugin;

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
     */

    private void startUHC() {
        new CommandAPICommand("uhc")
        .withPermission(CommandPermission.OP)
        .withArguments(new LiteralArgument("start"))
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

    private void endUHC() {
        new CommandAPICommand("uhc")
        .withPermission(CommandPermission.OP)
        .withArguments(new LiteralArgument("end"))
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

    private void spectator() {
        ArrayList<Argument> arguments = new ArrayList<>();
        arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));

        new CommandAPICommand("spectator")
        .withArguments(arguments)
        .executes(
            (sender, args) -> {
                Player p = (Player) args[0];
                plugin.getGameManager().getTeamManager().setSpectator(p);
                sender.sendMessage("Set " + p + " to state SPECTATOR.");
            }
        ).register();
    }

    private void combatant() {
        ArrayList<Argument> arguments = new ArrayList<>();
        arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));

        new CommandAPICommand("combatant")
        .withArguments(arguments)
        .executes(
            (sender, args) -> {
                Player p = (Player) args[0];
                plugin.getGameManager().getTeamManager().setUnassignedCombatant(p);
                sender.sendMessage("Set " + p + " to state COMBATANT_UNASSIGNED.");
            }
        ).register();
}

    private void setTeam() {
        ArrayList<Argument> arguments = new ArrayList<>();
        arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));
        int numTeams = plugin.getGameManager().getTeamManager().getNumTeams();
        arguments.add(new IntegerArgument("team",1,numTeams));

        new CommandAPICommand("setteam")
        .withArguments(arguments)
        .executes(
            (sender, args) -> {
                Player p = (Player) args[0];
                int t = (int) args[1];
                plugin.getGameManager().getTeamManager().assignPlayerTeam(p, t);
                sender.sendMessage("Set " + p + " to team " + t);
            }
        ).register();
    }

    private void getTeamData() {
        ArrayList<Argument> arguments = new ArrayList<>();
        arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));

        new CommandAPICommand("getteamdata")
        .withArguments(arguments)
        .executes(
            (sender, args) -> {
                TeamManager tm = plugin.getGameManager().getTeamManager();
                Player p = (Player) args[0];
                int team = tm.getTeam(p);
                PlayerState state = tm.getPlayerState(p);
                sender.sendMessage(p.getName() + " is a " + state + " on team " + team);
            }
        ).register();
    }

    void registerAll() {
        startUHC();
        endUHC();
        spectator();
        combatant();
        setTeam();
        getTeamData();
    }
}
