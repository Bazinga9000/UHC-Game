package xyz.baz9k.UHCGame;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class Commands {
    private final UHCGame plugin;

    public Commands(UHCGame plugin) {
        this.plugin = plugin;
    }
    /*
    /startuhc
    /enduhc
    /spectator <player>
    /combatant <player>
    /setTeam <player> <team : int>
    /getTeamData <player>
     */

    private void startUHC() {
        new CommandAPICommand("startuhc")
        .executes(
            (sender, args) -> {
                plugin.getUHCManager().startUHC();
            }
        ).register();
    }

    private void endUHC() {
        new CommandAPICommand("enduhc")
        .executes(
            (sender, args) -> {
                plugin.getUHCManager().endUHC();
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
                plugin.getUHCManager().getTeamManager().setSpectator((Player) args[0]);
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
                plugin.getUHCManager().getTeamManager().setUnassignedCombatant((Player) args[0]);
            }
        ).register();
}

    private void setTeam() {
        ArrayList<Argument> arguments = new ArrayList<>();
        arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));
        int numTeams = plugin.getUHCManager().getTeamManager().getNumTeams();
        arguments.add(new IntegerArgument("team",1,numTeams));

        new CommandAPICommand("setteam")
        .withArguments(arguments)
        .executes(
            (sender, args) -> {
                plugin.getUHCManager().getTeamManager().assignPlayerTeam((Player) args[0], (int) args[1]);
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
                UHCTeamManager tm = plugin.getUHCManager().getTeamManager();
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
