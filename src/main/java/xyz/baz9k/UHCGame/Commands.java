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
import org.bukkit.scoreboard.ScoreboardManager;

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
                    plugin.getUHCManager().startUHC();
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
                    plugin.getUHCManager().endUHC();
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
