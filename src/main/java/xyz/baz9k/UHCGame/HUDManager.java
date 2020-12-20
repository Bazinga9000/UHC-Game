package xyz.baz9k.UHCGame;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.World;
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

import java.awt.*;

public class HUDManager implements Listener {
    private UHCGame plugin;
    private GameManager gameManager;
    HUDManager(UHCGame plugin, GameManager gameManager){
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    private static String createEmptyName(char c){
        return ChatColor.translateAlternateColorCodes('&', "&"+c);
    }

    String formatState(Player p) {
        TeamManager tm = gameManager.getTeamManager();

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

        setHUDLine(p, "state", formatState(p));
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
        String elapsed = gameManager.getTimeElapsedString();
        ColoredStringBuilder s = new ColoredStringBuilder();
        s.append("Game Time: ",ChatColor.RED);
        s.append(elapsed + " ");
        World world = gameManager.getUHCWorld();
        long time = world.getTime();
        boolean isDay = !(13188 <= time && time <= 22812);
        Color dayCharacterColor = isDay ? new Color(255, 245, 123) : new Color(43, 47, 119);
        String dayCharacterString = isDay ? "☀" : "☽";
        s.append(dayCharacterString, dayCharacterColor);

        setHUDLine(p, "elapsedTime", s.toString());

    }

    private void updateCombatantsAliveHUD(Player p) {
        TeamManager tm = gameManager.getTeamManager();
        ColoredStringBuilder s = new ColoredStringBuilder();
        s.append("Combatants: ", ChatColor.WHITE);
        s.append(tm.countLivingCombatants());
        s.append(" / ");
        s.append(tm.countCombatants());

        setHUDLine(p, "combsalive", s.toString());
    }

    private void updateTeamsAliveHUD(Player p) {
        TeamManager tm = gameManager.getTeamManager();
        ColoredStringBuilder s = new ColoredStringBuilder();
        s.append("Teams: ", ChatColor.WHITE);
        s.append(tm.countLivingTeams());
        s.append(" / ");
        s.append(tm.getNumTeams());

        setHUDLine(p, "teamsalive", s.toString());

    }

    @EventHandler
    public void onJoin(PlayerJoinEvent join){
        if(gameManager.isUHCStarted()){
            setupPlayerHUD(join.getPlayer());
            updateMovementHUD(join.getPlayer());
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent movement){
        if(gameManager.isUHCStarted())
            updateMovementHUD(movement.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent death) {
        if(gameManager.isUHCStarted()) {
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                updateCombatantsAliveHUD(p);
                updateTeamsAliveHUD(p);

            }
        }
    }
}
