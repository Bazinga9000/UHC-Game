package xyz.baz9k.UHCGame;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.attribute.Attribute;
import xyz.baz9k.UHCGame.util.ColorGradient;
import xyz.baz9k.UHCGame.util.TeamDisplay;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import static xyz.baz9k.UHCGame.util.Utils.*;

import java.awt.Color;
import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;

public class HUDManager implements Listener {
    private GameManager gameManager;
    private TeamManager teamManager;

    public HUDManager(UHCGame plugin) {
        this.gameManager = plugin.getGameManager();
        this.teamManager = plugin.getTeamManager();
    }

    private static String createEmptyName(char c){
        return "\u00A7" + c;
    }

    /* FORMATTING */
    private TextComponent formatState(@NotNull Player p) {

        PlayerState state = teamManager.getPlayerState(p);
        int team = teamManager.getTeam(p);

        if (state == PlayerState.COMBATANT_UNASSIGNED) {
            return Component.text("Unassigned", NamedTextColor.WHITE, TextDecoration.ITALIC);
        }
        return TeamDisplay.getName(team);
    }

    private Component formatTeammate(@NotNull Player you, @NotNull Player teammate) {
        TextComponent.Builder s = Component.text();

        double teammateHP = teammate.getHealth() + teammate.getAbsorptionAmount();
        double teammateMaxHP = teammate.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        Color FULL_HP = new Color(87, 232, 107);
        Color HALF_HP = new Color(254, 254, 105);
        Color NO_HP = new Color(232, 85, 85);
        Color OVER_HEAL = new Color(171, 85, 232);
        Color gradient;

        if (teammateHP > teammateMaxHP) {
            gradient = OVER_HEAL;
        } else {
            gradient = ColorGradient.multiColorGradient(teammateHP/teammateMaxHP, NO_HP, HALF_HP, FULL_HP);
        }
        TextColor tcGradient = TextColor.color(gradient.getRGB());

        // prefix if spectator
        if (teamManager.isSpectator(you)) {
            int team = teamManager.getTeam(teammate);
            s.append(TeamDisplay.getPrefixWithSpace(team));
        }

        // name and health
        if (teamManager.getPlayerState(teammate) == PlayerState.COMBATANT_DEAD) {
            s.append(Component.text(teammate.getName(), NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH))
             .append(Component.text(" 0♥", NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH));
            return s.asComponent();
        } else {
            s.append(Component.text(teammate.getName(), tcGradient));
            s.append(Component.text(" " + (int) Math.ceil(teammateHP) + "♥ ", tcGradient));
        }
        // direction
        Location youLoc = you.getLocation();
        Location teammateLoc = teammate.getLocation();
        if (youLoc.getWorld() == teammateLoc.getWorld()) {
            double dx = youLoc.getX() - teammateLoc.getX();
            double dz = youLoc.getZ() - teammateLoc.getZ();
    
            double angle = Math.toDegrees(Math.atan2(dz, dx)); // angle btwn you & teammate
            double yeYaw = youLoc.getYaw();
    
            double relAngle = mod(yeYaw - angle + 90, 360) - 180;
            String arrow;
            if (112.5 < relAngle && relAngle < 157.5) arrow = "↙";
            else if (67.5 < relAngle && relAngle < 112.5) arrow = "←";
            else if (22.5 < relAngle && relAngle < 67.5) arrow = "↖";
            else if (-22.5 < relAngle && relAngle < 22.5) arrow = "↑";
            else if (-67.5 < relAngle && relAngle < -22.5) arrow = "↗";
            else if (-112.5 < relAngle && relAngle < -67.5) arrow = "→";
            else if (-157.5 < relAngle && relAngle < -112.5) arrow = "↘";
            else arrow = "↓";
            
            TextColor clr = teammate.isOnline() ? NamedTextColor.GOLD : NamedTextColor.DARK_GRAY;
            s.append(Component.text(arrow, clr));
        }

        return s.asComponent();
    }

    /* HANDLE SCOREBOARD PARITY */
    private void addPlayerToScoreboardTeam(@NotNull Scoreboard s, @NotNull Player p, int team){
        Team t = s.getTeam(String.valueOf(team));
        if(t == null){
            t = s.registerNewTeam(String.valueOf(team));
            t.prefix(TeamDisplay.getPrefixWithSpace(team));
        }
        t.addEntry(p.getName());
    }

    private void setTeams(@NotNull Player player){
        Scoreboard s = player.getScoreboard();
        for(Player p : Bukkit.getOnlinePlayers()){
            int team = teamManager.getTeam(p);
            addPlayerToScoreboardTeam(s, p, team);
        }
    }

    public void addPlayerToTeams(@NotNull Player player){
        int team = teamManager.getTeam(player);
        for(Player p : Bukkit.getOnlinePlayers()){
            Scoreboard s = p.getScoreboard();
            addPlayerToScoreboardTeam(s, player, team);
        }
    }

    private void createHUDScoreboard(@NotNull Player p){
        // give player scoreboard & objective
        Scoreboard newBoard = Bukkit.getScoreboardManager().getNewScoreboard();
        p.setScoreboard(newBoard);

        Objective hud = newBoard.registerNewObjective("hud", "dummy", Component.text(p.getName(), NamedTextColor.WHITE));
        hud.setDisplaySlot(DisplaySlot.SIDEBAR);

        setTeams(p);

    }

    /* INITIALIZING HUD */
    private void addHUDLine(@NotNull Player p, @NotNull String name, int position){
        Scoreboard b = p.getScoreboard();
        Team team = b.getTeam(name);
        if (team == null) {
            team = b.registerNewTeam(name);
        }
        if (position < 1 || position > 15) {
            throw new IllegalArgumentException("Position needs to be between 1 and 15.");
        }
        String pname = createEmptyName(Integer.toString(position, 16).charAt(0));
        team.addEntry(pname);

        Objective hud = b.getObjective("hud");
        if(hud == null) return;
        hud.getScore(pname).setScore(position);
    }

    private void setHUDLine(@NotNull Player p, @NotNull String field, @NotNull ComponentLike text) {
        Scoreboard b = p.getScoreboard();
        Team team = b.getTeam(field);
        if(team == null) return;
        team.prefix(text.asComponent());
    }

    /**
     * Setup a player's HUD on join.
     * @param p
     */
    public void initializePlayerHUD(@NotNull Player p) {
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
        updateWBHUD(p);
        updateCombatantsAliveHUD(p);
        updateTeamsAliveHUD(p);
        updateKillsHUD(p);
        updateElapsedTimeHUD(p);
    }

    /**
     * Remove player HUD (for the main HUD)
     */
    public void cleanup() {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(main);
        }
    }

    /* UPDATING SECTIONS OF HUD */
    private void updateTeammateHUD(@NotNull Player p) {
        Scoreboard b = p.getScoreboard();

        int team = teamManager.getTeam(p);
        Set<Player> teammateSet;
        if (teamManager.isAssignedCombatant(p)) {
            teammateSet = teamManager.getAllCombatantsOnTeam(team);
        } else {
            teammateSet = teamManager.getAllCombatants();
        }

        Iterable<Player> tmates = teammateSet.stream()
            .filter(e -> !e.equals(p))
            .sorted((t1, t2) -> Double.compare(t1.getHealth(), t2.getHealth()))
            .limit(5)
            ::iterator;

        int i = 0;
        for (Player tmate : tmates) {
            String rowName = "tmate" + i;

            Team row = b.getTeam(rowName);
            if (row == null) {
                addHUDLine(p, rowName, 14 - i);
            }

            setHUDLine(p, rowName, formatTeammate(p, tmate));
            i++;
        }
    }
    public void updateMovementHUD(@NotNull Player p){
        Location loc = p.getLocation();

        int x = loc.getBlockX(),
            y = loc.getBlockY(),
            z = loc.getBlockZ();

        TextComponent.Builder s;
        s = Component.text()
            .append(Component.text(x + " " + y + " " + z, NamedTextColor.GREEN)) // position format
            .append(Component.text(" ( ", NamedTextColor.WHITE)); // rotation format

        double yaw = mod(loc.getYaw() + 67.5, 360);
        /*
         * +Z =   0 -  67.5 - 135
         * -X =  90 - 157.5 - 225
         * -Z = 180 - 247.5 - 315
         * +X = 270 - 337.5 -  45
         * 
         *   0 -  45: -X +Z
         *  45 -  90: +Z
         *  90 - 135: +X +Z
         * 135 - 180: +X
         * 180 - 225: +X -Z
         * 225 - 270: -Z
         * 270 - 315: -X -Z
         * 315 - 360: -X
         */

        if ( 90 <= yaw && yaw < 225) s.append(Component.text("-X ", NamedTextColor.RED));
        if (270 <= yaw || yaw <  45) s.append(Component.text("+X ", NamedTextColor.RED));

        if (  0 <= yaw && yaw < 135) s.append(Component.text("+Z ", NamedTextColor.BLUE));
        if (180 <= yaw && yaw < 315) s.append(Component.text("-Z ", NamedTextColor.BLUE));

        s.append(Component.text(")", NamedTextColor.WHITE));

        setHUDLine(p, "posrot", s);
    }

    public void updateWBHUD(@NotNull Player p) {
        Location loc = p.getLocation();
        
        // world border radius format
        double r = (p.getWorld().getWorldBorder().getSize() / 2);
        TextComponent.Builder s = Component.text()
            .append(Component.text("World Border: ±", NamedTextColor.AQUA))
            .append(Component.text((int) r, NamedTextColor.AQUA));

        // distance format
        double distance = r - Math.max(Math.abs(loc.getX()), Math.abs(loc.getZ()));
        s.append(Component.text(" (" + (int) distance + ")", NamedTextColor.WHITE));

        setHUDLine(p, "wbpos", s);
    }

    public void updateElapsedTimeHUD(@NotNull Player p){
        String elapsed = getLongTimeString(gameManager.getElapsedTime());
        var s = Component.text()
            .append(Component.text("Game Time: ", NamedTextColor.RED))
            .append(Component.text(elapsed + " ", NamedTextColor.WHITE));

        World world = gameManager.getUHCWorld(Environment.NORMAL);
        long time = world.getTime();
        boolean isDay = !(13188 <= time && time <= 22812);
        TextColor dayCharColor = isDay ? TextColor.color(255, 245, 123) : TextColor.color(43, 47, 119);
        String dayCharString = isDay ? "☀" : "☽";
        s.append(Component.text(dayCharString, dayCharColor));

        setHUDLine(p, "elapsedTime", s);

    }

    public void updateCombatantsAliveHUD(@NotNull Player p) {
        var s = Component.text() 
            .append(Component.text("Combatants: ", NamedTextColor.WHITE))
            .append(Component.text(teamManager.countLivingCombatants() + " / " + teamManager.countCombatants(), NamedTextColor.WHITE));

        setHUDLine(p, "combsalive", s);
    }

    public void updateTeamsAliveHUD(@NotNull Player p) {
        var s = Component.text() 
            .append(Component.text("Teams: ", NamedTextColor.WHITE))
            .append(Component.text(teamManager.countLivingTeams() + " / " + teamManager.getNumTeams(), NamedTextColor.WHITE));

        setHUDLine(p, "teamsalive", s);
        
    }

    public void updateKillsHUD(@NotNull Player p) {
        OptionalInt k = gameManager.getKills(p);

        if (k.isPresent()) {
            var s = Component.text() 
                .append(Component.text("Kills: ", NamedTextColor.WHITE))
                .append(Component.text(k.orElseThrow(), NamedTextColor.WHITE));
            
            setHUDLine(p, "kills", s);
        }
    }

    /**
     * Updates the teammate hud for everyone who could see this player's health / position
     * @param p
     */
    public void updateTeammateHUDForViewers(@NotNull Player p) {
        Set<Player> viewers = new HashSet<>();

        int t = teamManager.getTeam(p);
        viewers.addAll(teamManager.getOnlineSpectators());
        if (t != 0) viewers.addAll(teamManager.getOnlineCombatantsOnTeam(t));

        for (Player viewer : viewers) {
            updateTeammateHUD(viewer);
        }
    }
    /* HANDLERS */
    @EventHandler
    public void onMove(PlayerMoveEvent e){
        if (!gameManager.hasUHCStarted()) return;
        updateMovementHUD(e.getPlayer());
        updateTeammateHUDForViewers(e.getPlayer());
    }

    @EventHandler
    public void onPlayerDamaged(EntityDamageEvent e) {
        if (!gameManager.hasUHCStarted()) return;
        if (!(e.getEntity() instanceof Player)) return;

        // update hud if dmg taken
        updateTeammateHUDForViewers((Player) e.getEntity());
    }

}
