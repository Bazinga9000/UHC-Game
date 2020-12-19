package xyz.baz9k.UHCGame;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class UHCHUDManager implements Listener {
    private UHCGame plugin;
    UHCHUDManager(UHCGame plugin){
        this.plugin = plugin;
    }

    private String createEmptyName(char c){
        return ChatColor.translateAlternateColorCodes('&', "&"+c);
    }

    String formatState(Player p) {
        UHCTeamManager tm = plugin.getUHCManager().getTeamManager();

        PlayerState state = tm.getPlayerState(p);
        int team = tm.getTeam(p);

        if (state == PlayerState.SPECTATOR) return ChatColor.AQUA.toString() + ChatColor.ITALIC + "Spectator";
        if (state == PlayerState.COMBATANT_UNASSIGNED) return ChatColor.ITALIC + "Unassigned";
        return TeamColors.getTeamChatColor(team) + "Team " + team;
    }

    String formatPos(Location loc){
        return ChatColor.GREEN.toString() + loc.getBlockX() + ", " + loc.getBlockZ();
    }

    String formatRot(Location loc){
        double yaw = ((loc.getYaw() % 360) + 360) % 360;
        String xf = yaw < 180 ? "+" : "-";
        String zf = yaw < 90 || yaw > 270 ? "+" : "-";
        return ChatColor.RED + xf + "X " + ChatColor.BLUE + zf + "Z";
    }


    private void setupPlayerHUD(Player p){
        // give player scoreboard & objective
        Scoreboard newBoard = Bukkit.getScoreboardManager().getNewScoreboard();
        p.setScoreboard(newBoard);

        Objective hud = newBoard.registerNewObjective("hud", "dummy", p.getName());
        hud.setDisplaySlot(DisplaySlot.SIDEBAR);

        Team state = newBoard.registerNewTeam("state");
        Team position = newBoard.registerNewTeam("position");
        Team rotation = newBoard.registerNewTeam("rotation");
        
        // create entries
        String stateEntry = createEmptyName('a');
        String newLineEntry = createEmptyName('b');
        String posEntry = createEmptyName('c');
        String rotEntry = createEmptyName('d');

        state.addEntry(stateEntry);
        position.addEntry(posEntry);
        rotation.addEntry(rotEntry);
        
        // change state display
        state.setPrefix(formatState(p));

        // display entries
        hud.getScore(stateEntry).setScore(15);
        hud.getScore(newLineEntry).setScore(14);
        hud.getScore(posEntry).setScore(13);
        hud.getScore(rotEntry).setScore(12);

    }


    public void start(){
        for(Player p : plugin.getServer().getOnlinePlayers()){
            setupPlayerHUD(p);
            updateMovementHUD(p);
        }
    }

    public void cleanup(){
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for(Player p : plugin.getServer().getOnlinePlayers()){
            p.setScoreboard(main);
        }
    }



    private void updateMovementHUD(Player p){
        Location loc = p.getLocation();
        Scoreboard scoreboard = p.getScoreboard();

        Team position = scoreboard.getTeam("position");
        Team rotation = scoreboard.getTeam("rotation");

        position.setPrefix(formatPos(loc));
        rotation.setPrefix(formatRot(loc));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent join){
        if(plugin.getUHCManager().isUHCStarted()){
            setupPlayerHUD(join.getPlayer());
            updateMovementHUD(join.getPlayer());
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent movement){
        if(plugin.getUHCManager().isUHCStarted())
            updateMovementHUD(movement.getPlayer());
    }
}
