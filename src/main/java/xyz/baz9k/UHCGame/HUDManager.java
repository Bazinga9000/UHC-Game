package xyz.baz9k.UHCGame;

import net.md_5.bungee.api.ChatColor;
import xyz.baz9k.UHCGame.util.ColoredStringBuilder;
import xyz.baz9k.UHCGame.util.TeamColors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class HUDManager implements Listener {
    private UHCGame plugin;
    HUDManager(UHCGame plugin){
        this.plugin = plugin;
    }

    private static String createEmptyName(char c){
        return ChatColor.translateAlternateColorCodes('&', "&"+c);
    }

    private String formatState(Player p) {
        TeamManager tm = plugin.getUHCManager().getTeamManager();

        PlayerState state = tm.getPlayerState(p);
        int team = tm.getTeam(p);

        if (state == PlayerState.SPECTATOR) return ChatColor.AQUA.toString() + ChatColor.ITALIC + "Spectator";
        if (state == PlayerState.COMBATANT_UNASSIGNED) return ChatColor.ITALIC + "Unassigned";
        return TeamColors.getTeamChatColor(team) + "Team " + team;
    }

    private String formatPos(Location loc){
        return ChatColor.GREEN.toString() + loc.getBlockX() + ", " + loc.getBlockZ();
    }

    private String formatRot(Location loc){
        double yaw = ((loc.getYaw() % 360) + 360) % 360;
        String xf = yaw < 180 ? "+" : "-";
        String zf = yaw < 90 || yaw > 270 ? "+" : "-";
        return ChatColor.RED + xf + "X " + ChatColor.BLUE + zf + "Z";
    }

    private String formatCombsAlive() {
        TeamManager tm = plugin.getUHCManager().getTeamManager();
        ColoredStringBuilder s = new ColoredStringBuilder();
        s.append("Combatants: ", ChatColor.WHITE);
        s.append(tm.countLivingCombatants());
        s.append(" / ");
        s.append(tm.countCombatants());
        return s.toString();
    }
    private String formatTeamsAlive() {
        TeamManager tm = plugin.getUHCManager().getTeamManager();
        ColoredStringBuilder s = new ColoredStringBuilder();
        s.append("Teams: ", ChatColor.WHITE);
        s.append(tm.countLivingTeams());
        s.append(" / ");
        s.append(tm.getNumTeams());
        return s.toString();
    }

    public static void createHUDScoreboard(Player p){
        // give player scoreboard & objective
        Scoreboard newBoard = Bukkit.getScoreboardManager().getNewScoreboard();
        p.setScoreboard(newBoard);

        Objective hud = newBoard.registerNewObjective("hud", "dummy", p.getName());
        hud.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    public static void addHUDLine(Player p, String name, int position){
        Scoreboard b = p.getScoreboard();
        Team team = b.getTeam(name);
        if (team == null) team = b.registerNewTeam(name);
        if (position < 1 || position > 15) throw new IllegalArgumentException("Position needs to be between 1 and 15.");
        String pname = createEmptyName(Integer.toString(position, 16).charAt(0));
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

        addHUDLine(p, "state",      15);
        addHUDLine(p, "newline",    14);
        addHUDLine(p, "newline",     9);
        addHUDLine(p, "position",    8);
        addHUDLine(p, "rotation",    7);
        addHUDLine(p, "newline",     6);
        addHUDLine(p, "combsalive",  5);
        addHUDLine(p, "teamsalive",  4);
        addHUDLine(p, "kills",       3);
        addHUDLine(p, "newline",     2);
        addHUDLine(p, "elapsedTime", 1);
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

    public void updateElapsedTimeHUD(Player p){
        String elapsed = plugin.getUHCManager().getTimeElapsedString();
        ColoredStringBuilder s = new ColoredStringBuilder();
        s.append("Elapsed Time: ",ChatColor.RED);
        s.append(elapsed);
        setHUDLine(p, "elapsedTime", s.toString());

    }

    private void updateCombatantsAliveHUD(Player p) {
        setHUDLine(p, "combsalive", formatCombsAlive());
    }
    
    private void updateTeamsAliveHUD(Player p) {
        setHUDLine(p, "teamsalive", formatTeamsAlive());
        
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

    @EventHandler
    public void onDeath(PlayerDeathEvent death) {
        if(plugin.getUHCManager().isUHCStarted()) {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                updateCombatantsAliveHUD(p);
                updateTeamsAliveHUD(p);
            
            }
        }
    }
}
