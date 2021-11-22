package xyz.baz9k.UHCGame;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import static net.kyori.adventure.text.format.TextDecoration.*;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import xyz.baz9k.UHCGame.util.ColorGradient;
import xyz.baz9k.UHCGame.util.Point2D;
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
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import static xyz.baz9k.UHCGame.util.Utils.*;
import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

public class HUDManager implements Listener {
    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final UHCGamePlugin plugin;

    public HUDManager(UHCGamePlugin plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
        this.teamManager = plugin.getTeamManager();
    }

    private static String createEmptyName(char c){
        return "\u00A7" + c;
    }

    /* FORMATTING */
    private Component formatState(@NotNull Player p) {

        PlayerState state = teamManager.getPlayerState(p);
        int team = teamManager.getTeam(p);

        if (state == PlayerState.COMBATANT_UNASSIGNED) {
            return Component.translatable("xyz.baz9k.uhc.team.unassigned", NamedTextColor.WHITE, ITALIC);
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
            s.append(Component.text(teammate.getName(), NamedTextColor.GRAY, STRIKETHROUGH))
             .append(Component.space())
             .append(Component.text("0♥", NamedTextColor.GRAY, STRIKETHROUGH));
            return s.asComponent();
        } else {
            s.append(Component.text(teammate.getName(), tcGradient))
             .append(Component.space())
             .append(Component.text((int) Math.ceil(teammateHP) + "♥", tcGradient))
             .append(Component.space());
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
    
    /**
     * Adds a player onto a {@link TeamManager} team on a scoreboard
     * @param s the scoreboard
     * @param p the player
     * @param team the TeamManager team (not Minecraft {@link Team})
     */
    private void addPlayerToScoreboardTeam(@NotNull Scoreboard s, @NotNull Player p, int team){
        Team t = s.getTeam(String.valueOf(team));
        if(t == null){
            t = s.registerNewTeam(String.valueOf(team));
            t.prefix(TeamDisplay.getPrefixWithSpace(team));
        }
        t.addEntry(p.getName());
    }

    /**
     * Registers all online users' teams to a player's scoreboard
     * @param player the player to register to
     */
    private void setTeams(@NotNull Player player){
        Scoreboard s = player.getScoreboard();
        for(Player p : Bukkit.getOnlinePlayers()){
            int team = teamManager.getTeam(p);
            addPlayerToScoreboardTeam(s, p, team);
        }
    }

    /**
     * Registers a player's team to all online users' scoreboards
     * @param player the player to register
     */
    public void addPlayerToTeams(@NotNull Player player){
        int team = teamManager.getTeam(player);
        for(Player p : Bukkit.getOnlinePlayers()){
            Scoreboard s = p.getScoreboard();
            addPlayerToScoreboardTeam(s, player, team);
        }
    }

    /**
     * Creates the scoreboard for a player, adds necessary objectives (the main hud, hearts), registers all online players to it
     * @param p
     */
    private void createHUDScoreboard(@NotNull Player p){
        // give player scoreboard & objective
        Scoreboard newBoard = Bukkit.getScoreboardManager().getNewScoreboard();
        p.setScoreboard(newBoard);

        Objective hud = newBoard.registerNewObjective("hud", "dummy", Component.text(p.getName(), NamedTextColor.WHITE));
        hud.setDisplaySlot(DisplaySlot.SIDEBAR);

        // bukkit et al apparently do not allow one obj in multiple display slots even though vanilla is 100% okay with that. no clue.
        Objective hearts1 = newBoard.registerNewObjective("hearts1", "health", Component.text("♥", NamedTextColor.RED), RenderType.HEARTS);
        Objective hearts2 = newBoard.registerNewObjective("hearts2", "health", Component.text("♥", NamedTextColor.RED), RenderType.HEARTS);
        hearts1.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        hearts2.setDisplaySlot(DisplaySlot.BELOW_NAME);
        
        setTeams(p);

    }

    /* INITIALIZING HUD */

    /**
     * Reserves a space for a HUD line at a position in a player's scoreboard
     * @param p the player whose scoreboard will have a HUD line added
     * @param name Identifier to reference the HUD line again (to add stuff to it)
     * @param position Position to reserve
     */
    private void addHUDLine(@NotNull Player p, @NotNull String name, int position){
        Scoreboard b = p.getScoreboard();
        Team team = b.getTeam(name);
        if (team == null) {
            team = b.registerNewTeam(name);
        }
        if (position < 1 || position > 15) {
            throw translatableErr(IllegalArgumentException.class, trans("xyz.baz9k.uhc.err.hud.must_fit"));
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
     * Setup a player's HUD (create scoreboard, reserve all the slots, load data onto all the slots)
     * @param p
     */
    public void initPlayerHUD(@NotNull Player p) {
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
     * Trash the player's scoreboard and return to the main scoreboard.
     */
    public void cleanup(Player p) {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        p.setScoreboard(main);
    }

    /**
     * 2D distance between two 3D locations, two different worlds results in null
     */
    private static Double dist2d(Location pl, Location ql) {
        if (pl.getWorld() != ql.getWorld()) return null;
        Point2D p = Point2D.fromLocation(pl),
                q = Point2D.fromLocation(ql);
        return p.dist(q);
    }

    /* UPDATING SECTIONS OF HUD */
    private void updateTeammateHUD(@NotNull Player p) {
        Scoreboard b = p.getScoreboard();

        int team = teamManager.getTeam(p);
        Set<Player> teammateSet;
        Comparator<? super Player> sorter;
        if (teamManager.isAssignedCombatant(p)) {
            teammateSet = teamManager.getAllCombatantsOnTeam(team);
            sorter = (t1, t2) -> Double.compare(t1.getHealth(), t2.getHealth());
        } else {
            teammateSet = teamManager.getAllCombatants();
            sorter = (t1, t2) -> {
                Location pl = p.getLocation(),
                        t1l = t1.getLocation(),
                        t2l = t2.getLocation();
                return Comparator.nullsLast(Double::compare).compare(dist2d(pl, t1l), dist2d(pl, t2l));
            };
        }

        Iterable<Player> tmates = teammateSet.stream()
            .filter(e -> !e.equals(p))
            .sorted(sorter)
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

        double yaw = mod(loc.getYaw() + 67.5, 360);
        List<Component> xz = new ArrayList<>();
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

        if ( 90 <= yaw && yaw < 225) xz.add(trans("xyz.baz9k.uhc.hud.pos_x").color(NamedTextColor.RED));
        if (270 <= yaw || yaw <  45) xz.add(trans("xyz.baz9k.uhc.hud.neg_x").color(NamedTextColor.RED));

        if (  0 <= yaw && yaw < 135) xz.add(trans("xyz.baz9k.uhc.hud.pos_z").color(NamedTextColor.BLUE));
        if (180 <= yaw && yaw < 315) xz.add(trans("xyz.baz9k.uhc.hud.neg_z").color(NamedTextColor.BLUE));

        Component pos = trans("xyz.baz9k.uhc.hud.position", x, y, z).color(NamedTextColor.GREEN);
        Component rot = trans("xyz.baz9k.uhc.hud.rotation", Component.join(Component.space(), xz))
            .color(NamedTextColor.WHITE);
        setHUDLine(p, "posrot", Component.join(Component.space(), pos, rot));
    }

    public void updateWBHUD(@NotNull Player p) {
        Location loc = p.getLocation();
        
        // world border radius format
        double r = (p.getWorld().getWorldBorder().getSize() / 2);
        Component wbrad = trans("xyz.baz9k.uhc.hud.wbradius", (int) r).color(NamedTextColor.AQUA);
        // distance format
        double distance = r - Math.max(Math.abs(loc.getX()), Math.abs(loc.getZ()));
        Component wbdist = trans("xyz.baz9k.uhc.hud.wbdistance", (int) distance).color(NamedTextColor.WHITE)
;
        setHUDLine(p, "wbpos", Component.join(Component.space(), wbrad, wbdist));
    }

    public void updateElapsedTimeHUD(@NotNull Player p){
        String elapsed = getLongTimeString(gameManager.getElapsedTime());

        World world = plugin.getWorldManager().getGameWorld(0);
        long time = world.getTime();
        boolean isDay = !(13188 <= time && time <= 22812);
        TextColor dayCharColor = isDay ? TextColor.color(255, 245, 123) : TextColor.color(43, 47, 119);
        String dayCharString = isDay ? "☀" : "☽";

        Component s = trans("xyz.baz9k.uhc.hud.gametime",
            Component.text(elapsed, NamedTextColor.WHITE),
            Component.text(dayCharString, dayCharColor)
        ).color(NamedTextColor.RED);

        setHUDLine(p, "elapsedTime", s);

    }

    public void updateCombatantsAliveHUD(@NotNull Player p) {
        var s = trans("xyz.baz9k.uhc.hud.combcount",
            Component.text(teamManager.countLivingCombatants(), NamedTextColor.WHITE),
            Component.text(teamManager.countCombatants(), NamedTextColor.WHITE)
        ).color(NamedTextColor.WHITE);
        setHUDLine(p, "combsalive", s);
    }

    public void updateTeamsAliveHUD(@NotNull Player p) {
        var s = trans("xyz.baz9k.uhc.hud.teamcount",
            Component.text(teamManager.countLivingTeams(), NamedTextColor.WHITE),
            Component.text(teamManager.getNumTeams(), NamedTextColor.WHITE)
        ).color(NamedTextColor.WHITE);

        setHUDLine(p, "teamsalive", s);
        
    }

    public void updateKillsHUD(@NotNull Player p) {
        OptionalInt k = gameManager.getKills(p);

        if (k.isPresent()) {
            var s = trans("xyz.baz9k.uhc.hud.killcount", Component.text(k.orElseThrow(), NamedTextColor.WHITE)) 
                .color(NamedTextColor.WHITE);
            
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

        if (e.getEntity() instanceof Player damaged) {
            // update hud if dmg taken
            updateTeammateHUDForViewers((Player) e.getEntity());
        }
    }
}
