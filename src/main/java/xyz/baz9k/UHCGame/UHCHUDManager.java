package xyz.baz9k.UHCGame;

import net.md_5.bungee.api.ChatColor;
import xyz.baz9k.UHCGame.util.TeamColors;

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

    private static String createEmptyName(char c){
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

    public static void createHUDScoreboard(Player p){
        // give player scoreboard & objective
        Scoreboard newBoard = Bukkit.getScoreboardManager().getNewScoreboard();
        p.setScoreboard(newBoard);

        Objective hud = newBoard.registerNewObjective("hud", "dummy", p.getName());
        hud.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    public static void addHUDLine(Player p, String name, int position, char c){
        Scoreboard b = p.getScoreboard();
        Team team = b.registerNewTeam(name);
        String pname = createEmptyName(c);
        team.addEntry(pname);

        Objective hud = b.getObjective("hud");
        if(hud == null) return;
        hud.getScore(pname).setScore(position);
    }

    public static void setHUDLine(Player p, String field, String text){
        Scoreboard b = p.getScoreboard();
        Team team = b.getTeam(field);
        if(team == null) return;
        team.setPrefix(text);
    }


    private void setupPlayerHUD(Player p){
        createHUDScoreboard(p);

        addHUDLine(p, "state", 15, 'a');
        addHUDLine(p, "newline", 14, 'b');
        addHUDLine(p, "position", 13, 'c');
        addHUDLine(p, "rotation", 12, 'd');
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
        setHUDLine(p, "position", formatPos(loc));
        setHUDLine(p, "rotation", formatRot(loc));
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
