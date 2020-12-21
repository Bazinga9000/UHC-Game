package xyz.baz9k.UHCGame;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import xyz.baz9k.UHCGame.util.ColorGradient;
import xyz.baz9k.UHCGame.util.ColoredStringBuilder;
import xyz.baz9k.UHCGame.util.TeamColors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.awt.*;
import java.util.List;
import java.util.Collections;

public class HUDManager implements Listener {
    private UHCGame plugin;
    private GameManager gameManager;
    HUDManager(UHCGame plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    private static String createEmptyName(char c){
        return ChatColor.translateAlternateColorCodes('&', "&"+c);
    }

    private String formatState(Player p) {
        TeamManager tm = gameManager.getTeamManager();

        PlayerState state = tm.getPlayerState(p);
        int team = tm.getTeam(p);

        if (state == PlayerState.SPECTATOR) return ChatColor.AQUA.toString() + ChatColor.ITALIC + "Spectator";
        if (state == PlayerState.COMBATANT_UNASSIGNED) return ChatColor.ITALIC + "Unassigned";
        return TeamColors.getTeamChatColor(team) + "Team " + team;
    }

    private String formatTeammate(Player you, Player teammate) {
        ColoredStringBuilder s = new ColoredStringBuilder();
        TeamManager tm = gameManager.getTeamManager();

        double teammateHP = teammate.getHealth();
        double teammateMaxHP = teammate.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        Color FULL_HP = new Color(204, 246, 200);
        Color NO_HP = new Color(249, 192, 192);
        Color gradient = ColorGradient.twoColorGradient(teammateHP/teammateMaxHP, NO_HP, FULL_HP);

        // username
        if (tm.isSpectator(you)) {
            int team = tm.getTeam(teammate);
            s.append("[" + team + "] ", TeamColors.getTeamChatColor(team), ChatColor.BOLD);
        }

        s.append(teammate.getName(), gradient);
        // health
        if (tm.getPlayerState(teammate) == PlayerState.COMBATANT_DEAD) {
            s.append("0♥ ",ChatColor.GRAY, ChatColor.STRIKETHROUGH);
        } else {
            s.append((int) Math.ceil(teammate.getHealth()) + "♥ ", gradient);
        }
        // direction
        Location youLoc = you.getLocation();
        Location teammateLoc = teammate.getLocation();
        double dx = youLoc.getX() - teammateLoc.getX();
        double dz = youLoc.getZ() - teammateLoc.getZ();

        double angle = Math.toDegrees(Math.atan2(dz, dx)); // angle btwn you & teammate
        double yeYaw = youLoc.getYaw();

        double relAngle = (((yeYaw - angle + 90) % 360) + 360) % 360 - 180;
        String arrow;
        if (112.5 < relAngle && relAngle < 157.5) arrow = "↙";
        else if (67.5 < relAngle && relAngle < 112.5) arrow = "←";
        else if (22.5 < relAngle && relAngle < 67.5) arrow = "↖";
        else if (-22.5 < relAngle && relAngle < 22.5) arrow = "↑";
        else if (-67.5 < relAngle && relAngle < -22.5) arrow = "↗";
        else if (-112.5 < relAngle && relAngle < -67.5) arrow = "→";
        else if (-157.5 < relAngle && relAngle < -112.5) arrow = "↘";
        else arrow = "↓";
        s.append(arrow, ChatColor.GOLD);

        return s.toString();
    }

    private void addPlayerToScoreboardTeam(Scoreboard s, Player p, int team){
        String name = "team_" + team;
        Team t = s.getTeam(String.valueOf(team));
        if(t == null){
            t = s.registerNewTeam(String.valueOf(team));
            if(team != 0)
                t.setPrefix(TeamColors.getTeamChatColor(team) + "" + ChatColor.BOLD + "["+team+"] ");
            else
                t.setPrefix(ChatColor.AQUA + "" + ChatColor.ITALIC + "Spectator ");
        }
        t.addEntry(p.getName());
    }

    private void setTeams(Player player){
        Scoreboard s = player.getScoreboard();
        TeamManager tm = gameManager.getTeamManager();
        for(Player p : plugin.getServer().getOnlinePlayers()){
            int team = tm.getTeam(p);
            addPlayerToScoreboardTeam(s, p, team);
        }
    }

    public void addPlayerToTeams(Player player){
        TeamManager tm = gameManager.getTeamManager();
        int team = tm.getTeam(player);
        for(Player p : plugin.getServer().getOnlinePlayers()){
            Scoreboard s = p.getScoreboard();
            addPlayerToScoreboardTeam(s, player, team);
        }
    }

    public void createHUDScoreboard(Player p){
        // give player scoreboard & objective
        Scoreboard newBoard = Bukkit.getScoreboardManager().getNewScoreboard();
        p.setScoreboard(newBoard);

        Objective hud = newBoard.registerNewObjective("hud", "dummy", p.getName());
        hud.setDisplaySlot(DisplaySlot.SIDEBAR);

        setTeams(p);

    }

    public void addHUDLine(Player p, String name, int position){
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

    public void setHUDLine(Player p, String field, String text){
        Scoreboard b = p.getScoreboard();
        Team team = b.getTeam(field);
        if(team == null) return;
        team.setPrefix(text);
    }

    public void initializePlayerHUD(Player p) {
        createHUDScoreboard(p);

        addHUDLine(p, "state",      15);
        // 14 - 10 are tmate
        addHUDLine(p, "newline",     9);
        addHUDLine(p, "posrot",      8);
        addHUDLine(p, "wbpos",       7);
        addHUDLine(p, "newline",     6);
        addHUDLine(p, "combsalive",  5);
        addHUDLine(p, "teamsalive",  4);
        addHUDLine(p, "kills",       3);
        addHUDLine(p, "newline",     2);
        addHUDLine(p, "elapsedTime", 1);

        setHUDLine(p, "state", formatState(p));
        updateTeammateHUD(p);
        updateMovementHUD(p);
        // update WB POS hud //
        updateCombatantsAliveHUD(p);
        updateTeamsAliveHUD(p);
        updateKillsHUD(p);
        updateElapsedTimeHUD(p);
    }

    public void cleanup(){
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for(Player p : plugin.getServer().getOnlinePlayers()){
            p.setScoreboard(main);
        }
    }

    public void updateTeammateHUD(Player p) {
        Scoreboard b = p.getScoreboard();
        TeamManager tm = gameManager.getTeamManager();

        int team = tm.getTeam(p);
        List<Player> teammates;
        if (tm.isAssignedCombatant(p)) teammates = tm.getAllCombatants();
        else teammates = tm.getAllCombatantsOnTeam(team);
        teammates.remove(p);
        Collections.sort(teammates, (t1, t2) -> (int)Math.ceil(t1.getHealth()) - (int)Math.ceil(t2.getHealth()));

        int len = Math.min(5, teammates.size());
        for (int i = 0; i < len; i++) {
            String rowName = "tmate" + i;
            Team row = b.getTeam(rowName);
            Player teammate = teammates.get(i);

            if (row == null)  { // if row has not been created before
                addHUDLine(p, rowName, 14 - i);
                row = b.getTeam(rowName);
            }
            setHUDLine(p, rowName, formatTeammate(p, teammate));
        }
    }
    public void updateMovementHUD(Player p){
        Location loc = p.getLocation();

        ColoredStringBuilder s = new ColoredStringBuilder();
        // position format
        s.append(loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ(), ChatColor.GREEN);
        
        // rotation format
        s.append(" (", ChatColor.WHITE);
        double yaw = ((loc.getYaw() % 360) + 360) % 360;
        String xf = yaw < 180 ? "+" : "-";
        String zf = yaw < 90 || yaw > 270 ? "+" : "-";
        s.append(ChatColor.RED + xf + "X " + ChatColor.BLUE + zf + "Z");
        s.append(")", ChatColor.WHITE);

        setHUDLine(p, "posrot", s.toString());
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

    public void updateCombatantsAliveHUD(Player p) {
        TeamManager tm = gameManager.getTeamManager();
        ColoredStringBuilder s = new ColoredStringBuilder();
        s.append("Combatants: ", ChatColor.WHITE);
        s.append(tm.countLivingCombatants());
        s.append(" / ");
        s.append(tm.countCombatants());

        setHUDLine(p, "combsalive", s.toString());
    }

    public void updateTeamsAliveHUD(Player p) {
        TeamManager tm = gameManager.getTeamManager();
        ColoredStringBuilder s = new ColoredStringBuilder();
        s.append("Teams: ", ChatColor.WHITE);
        s.append(tm.countLivingTeams());
        s.append(" / ");
        s.append(tm.getNumTeams());

        setHUDLine(p, "teamsalive", s.toString());
        
    }

    public void updateKillsHUD(Player p) {
        TeamManager tm = gameManager.getTeamManager();
        if (tm.isSpectator(p)) return;
        ColoredStringBuilder s = new ColoredStringBuilder();
        s.append("Kills: ", ChatColor.WHITE);
        s.append(gameManager.getKills(p));
        
        setHUDLine(p, "kills", s.toString());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent movement){
        TeamManager tm = gameManager.getTeamManager();
        Player p = movement.getPlayer();
        if(gameManager.isUHCStarted())
            updateMovementHUD(p);
            // when someone moves, everyone who can see it (specs, ppl on team) should be able to see them move
            for (Player spec : tm.getAllSpectators()) {
                updateTeammateHUD(spec);
            }
            int team = tm.getTeam(p);
            for (Player tmate : tm.getAllCombatantsOnTeam(team)) {
                updateTeammateHUD(tmate);
            }
    }

}
