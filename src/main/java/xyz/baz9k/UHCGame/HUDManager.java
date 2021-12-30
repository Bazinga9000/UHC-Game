package xyz.baz9k.UHCGame;

import net.kyori.adventure.text.*;
import net.kyori.adventure.text.format.*;
import static net.kyori.adventure.text.format.TextDecoration.*;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Damageable;
import xyz.baz9k.UHCGame.event.PlayerStateChangeEvent;
import xyz.baz9k.UHCGame.util.ColorGradient;
import xyz.baz9k.UHCGame.util.Point2D;
import xyz.baz9k.UHCGame.util.TeamDisplay;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scoreboard.*;
import org.jetbrains.annotations.NotNull;

import static xyz.baz9k.UHCGame.util.Utils.*;
import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

import java.awt.Color;
import java.util.*;

public class HUDManager implements Listener {
    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final UHCGamePlugin plugin;

    public HUDManager(UHCGamePlugin plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager();
        this.teamManager = plugin.getTeamManager();
        cleanup(); // remove any extra uhc prefix teams in case
    }

    private static String createEmptyName(char c){
        return "\u00A7" + c;
    }

    /* FORMATTING */
    private Component formatState(@NotNull Player p) {

        PlayerState state = teamManager.getPlayerState(p);
        int team = teamManager.getTeam(p);

        if (state == PlayerState.COMBATANT_UNASSIGNED) {
            return new Key("team.unassigned").trans().style(Style.style(NamedTextColor.WHITE, ITALIC));
        }
        return TeamDisplay.getName(team);
    }

    /**
     * @param d Entity with health
     * @return the total health (main health plus absorption amount)
     */
    private double getTotalHealth(Damageable d) {
        return d.getHealth() + d.getAbsorptionAmount();
    }

    private @NotNull Component formatTeammate(@NotNull Player you, @NotNull Player teammate) {
        TextComponent.Builder s = Component.text();

        double teammateHP = getTotalHealth(teammate);
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
     * Prefix of the scoreboard team names that give each team their chat prefix
     */
    private static final String PREFIXING_TEAM_FORMAT = "uhc_prefix_";

    private Set<Scoreboard> scoreboardsInUse() {
        return scoreboardsInUse(true);
    }
    private Set<Scoreboard> scoreboardsInUse(boolean includeMain) {
        Set<Scoreboard> scoreboards = new HashSet<>();
        if (includeMain) scoreboards.add(Bukkit.getScoreboardManager().getMainScoreboard());

        for (Player p : Bukkit.getOnlinePlayers()) {
            scoreboards.add(p.getScoreboard());
        }
        return scoreboards;
    }

    /**
     * Register on a scoreboard the player's prefix team
     * @param s Scoreboard to register player prefix on
     * @param p Player to register prefix of
     */
    private void applyPrefixOnScoreboard(Scoreboard s, Player p) {
        int team = teamManager.getTeam(p);
        
        String teamName = PREFIXING_TEAM_FORMAT;
        int n_users = Bukkit.getOnlinePlayers().size();
        int n_users_mag = String.valueOf(n_users).length();
        
        teamName += team == 0 ? "s" : String.format("%0" + n_users_mag + "d", team);

        Team t = s.getTeam(teamName);
        if (t == null) {
            t = s.registerNewTeam(teamName);
            t.prefix(TeamDisplay.getPrefixWithSpace(team));
        }

        t.addPlayer(p);
    }

    /**
     * Update all prefixes on player's HUD
     * @param p Player to update scoreboard of
     */
    public void updatePrefixesOnHUD(Player p) {
        Scoreboard s = p.getScoreboard();
        for (Player pl : Bukkit.getOnlinePlayers()) {
            applyPrefixOnScoreboard(s, pl);
        }
    }

    /**
     * Update player's prefix on all HUDs
     * @param p Player whose prefix should be dispatched
     */
    public void dispatchPrefixUpdate(Player p) {
        for (Scoreboard s : scoreboardsInUse()) {
            applyPrefixOnScoreboard(s, p);
        }
    }

    /**
     * Creates the scoreboard for a player, adds necessary objectives (the main hud, hearts), registers all online players to it
     * @param p Player to create scoreboard for
     */
    private void createHUDScoreboard(@NotNull Player p){
        // give player scoreboard & objective
        Scoreboard newBoard = Bukkit.getScoreboardManager().getNewScoreboard();
        p.setScoreboard(newBoard);

        Objective hud = newBoard.registerNewObjective("hud", "dummy", Component.text(p.getName(), NamedTextColor.WHITE));
        hud.setDisplaySlot(DisplaySlot.SIDEBAR);

        // bukkit et al. apparently do not allow one obj in multiple display slots even though vanilla is 100% okay with that. no clue.
        Objective hearts1 = newBoard.registerNewObjective("hearts1", "health", Component.text("♥", NamedTextColor.RED), RenderType.HEARTS);
        Objective hearts2 = newBoard.registerNewObjective("hearts2", "health", Component.text("♥", NamedTextColor.RED), RenderType.HEARTS);
        hearts1.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        hearts2.setDisplaySlot(DisplaySlot.BELOW_NAME);
    }

    /* INITIALIZING HUD */

    /**
     * Reserves a space for a HUD line at a position in a player's scoreboard
     * @param p Player whose scoreboard will have a HUD line added
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
            throw new Key("err.hud.must_fit").transErr(IllegalArgumentException.class);
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
      * Create a new scoreboard to display just the prefixes (and not disrupt the main scoreboard) while in lobby
      * @param p Player 
      */
    public void initPlayerHUDLite(@NotNull Player p) {
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        p.setScoreboard(sb);

        updatePrefixesOnHUD(p);
        dispatchPrefixUpdate(p);
    }

    /**
     * Init a player's HUD (create scoreboard, reserve all the slots, load data onto all the slots)
     * @param p Player whose scoreboard should be initialized
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
        updateHealthHUD(p);
        updatePrefixesOnHUD(p);
        dispatchPrefixUpdate(p);
    }

    /**
     * Clean up any HUD setup when game ends.
     */
    public void cleanup() {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setScoreboard(main);
        }
        
        // remove all prefix teams from main
        main.getTeams().stream()
            .filter(t -> t.getName().startsWith(PREFIXING_TEAM_FORMAT))
            .forEach(Team::unregister);
    }

    /**
     * Clean up any HUD setup for a player (if they join during lobby mode).
     * @param p Player to clean up
     */
    private void cleanup(Player p) {
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        p.setScoreboard(main);

        main.getTeams().stream()
            .filter(t -> t.getName().startsWith(PREFIXING_TEAM_FORMAT))
            .forEach(Team::unregister);

    }

    public void prepareToLobby(Player p) {
        if (teamManager.isAssignedCombatant(p)) {
            initPlayerHUDLite(p);
        } else {
            cleanup(p);
        }
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

        Comparator<Player> compareByAliveness = (t1, t2) -> {
            PlayerState t1s = teamManager.getPlayerState(t1),
                        t2s = teamManager.getPlayerState(t2);
            
            return -Boolean.compare(
                t1s == PlayerState.COMBATANT_ALIVE, 
                t2s == PlayerState.COMBATANT_ALIVE);
        };
        Comparator<Player> compareByHealth = Comparator.comparingDouble(Damageable::getHealth);
        Comparator<Player> compareByProximity = (t1, t2) -> {
            Location pl = p.getLocation(),
                    t1l = t1.getLocation(),
                    t2l = t2.getLocation();
            return Comparator.nullsLast(Double::compare).compare(dist2d(pl, t1l), dist2d(pl, t2l));
        };

        Comparator<Player> sorter;
        if (teamManager.isAssignedCombatant(p)) {
            teammateSet = teamManager.getCombatantsOnTeam(team);
            sorter = compareByAliveness.thenComparing(compareByHealth);
        } else {
            teammateSet = teamManager.getCombatants();
            sorter = compareByAliveness.thenComparing(compareByProximity);
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

        if ( 90 <= yaw && yaw < 225) xz.add(new Key("hud.neg_x").trans().color(NamedTextColor.RED));
        if (270 <= yaw || yaw <  45) xz.add(new Key("hud.pos_x").trans().color(NamedTextColor.RED));

        if (  0 <= yaw && yaw < 135) xz.add(new Key("hud.pos_z").trans().color(NamedTextColor.BLUE));
        if (180 <= yaw && yaw < 315) xz.add(new Key("hud.neg_z").trans().color(NamedTextColor.BLUE));

        Component pos = new Key("hud.position").trans(x, y, z).color(NamedTextColor.GREEN);
        Component rot = new Key("hud.rotation").trans(Component.join(JoinConfiguration.separator(Component.space()), xz))
            .color(NamedTextColor.WHITE);
        setHUDLine(p, "posrot", Component.join(JoinConfiguration.separator(Component.space()), pos, rot));
    }

    public void updateWBHUD(@NotNull Player p) {
        Location loc = p.getLocation();
        
        // world border radius format
        double r = (p.getWorld().getWorldBorder().getSize() / 2);
        Component wbrad = new Key("hud.wbradius").trans((int) r).color(NamedTextColor.AQUA);
        // distance format
        double distance = r - Math.max(Math.abs(loc.getX()), Math.abs(loc.getZ()));
        Component wbdist = new Key("hud.wbdistance").trans((int) distance).color(NamedTextColor.WHITE)
;
        setHUDLine(p, "wbpos", Component.join(JoinConfiguration.separator(Component.space()), wbrad, wbdist));
    }

    public void updateElapsedTimeHUD(@NotNull Player p){
        String elapsed = getLongTimeString(gameManager.getElapsedTime(), "?");

        World world = plugin.getWorldManager().getGameWorld(0);
        long time = world.getTime();
        boolean isDay = !(13188 <= time && time <= 22812);
        TextColor dayCharColor = isDay ? TextColor.color(255, 245, 123) : TextColor.color(43, 47, 119);
        String dayCharString = isDay ? "☀" : "☽";

        Component s = new Key("hud.gametime").trans(
            Component.text(elapsed, NamedTextColor.WHITE),
            Component.text(dayCharString, dayCharColor)
        ).color(NamedTextColor.RED);

        setHUDLine(p, "elapsedTime", s);

    }

    public void updateCombatantsAliveHUD(@NotNull Player p) {
        var s = new Key("hud.combcount").trans(
            Component.text(teamManager.getAliveCombatants().size(), NamedTextColor.WHITE),
            Component.text(teamManager.getAssignedCombatants().size(), NamedTextColor.WHITE)
        ).color(NamedTextColor.WHITE);
        setHUDLine(p, "combsalive", s);
    }

    public void updateTeamsAliveHUD(@NotNull Player p) {
        var s = new Key("hud.teamcount").trans(
            Component.text(teamManager.getAliveTeams().length, NamedTextColor.WHITE),
            Component.text(teamManager.getNumTeams(), NamedTextColor.WHITE)
        ).color(NamedTextColor.WHITE);

        setHUDLine(p, "teamsalive", s);
        
    }

    public void updateKillsHUD(@NotNull Player p) {
        OptionalInt k = gameManager.getKills(p);

        if (k.isPresent()) {
            var s = new Key("hud.killcount").trans(
                Component.text(k.orElseThrow(), NamedTextColor.WHITE)
            ).color(NamedTextColor.WHITE);
            
            setHUDLine(p, "kills", s);
        }
    }

    /**
     * Updates the player's health on specified heart scoreboard
     * @param s Scoreboard to update health on
     * @param p Player whose health needs to be updated
     */
    private void updateHealthOnScoreboard(Scoreboard s, Player p) {
        int hp;
        if (teamManager.getPlayerState(p) == PlayerState.COMBATANT_ALIVE) {
            hp = (int) Math.ceil(getTotalHealth(p));
        } else {
            hp = 0;
        }

        List.of("hearts1", "hearts2")
            .forEach(objName -> {
                    Objective obj = s.getObjective(objName);
                    Score score = obj.getScore(p);
                    score.setScore(hp);
            });
    }

    public void updateHealthHUD(Player p) {
        Scoreboard s = p.getScoreboard();

        for (Player pl : teamManager.getCombatants()) {
            updateHealthOnScoreboard(s, pl);
        }
    }

    /**
     * Updates the teammate hud for everyone who could see this player's health / position
     * @param p Player whose health/position should be updated
     */
    public void dispatchTeammateHUDUpdate(@NotNull Player p) {

        int t = teamManager.getTeam(p);
        Set<Player> viewers = new HashSet<>(teamManager.getOnlineSpectators());
        if (t != 0) viewers.addAll(teamManager.getOnlineCombatantsOnTeam(t));

        for (Player viewer : viewers) {
            updateTeammateHUD(viewer);
        }
    }

    /**
     * Updates the player's health on everyone's heart scoreboard
     * @param p Player whose health should be updated
     */
    public void dispatchHealthHUDUpdate(Player p) {
        for (var s : scoreboardsInUse(false)) {
            updateHealthOnScoreboard(s, p);
        }
    }

    /* HANDLERS */
    @EventHandler
    public void onMove(PlayerMoveEvent e){
        if (!gameManager.hasUHCStarted()) return;
        Player p = e.getPlayer();

        updateMovementHUD(p);
        dispatchTeammateHUDUpdate(p);
    }

    private final Runnable getHealthChangeHandler(EntityEvent e) {
        return () -> {
            if (!gameManager.hasUHCStarted()) return;

            if (e.getEntity() instanceof Player p) {
                // update hud if dmg taken
                dispatchHealthHUDUpdate(p);
                dispatchTeammateHUDUpdate(p);
            }
        };
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent e) {
        Runnable handler = getHealthChangeHandler(e);
        Bukkit.getScheduler().runTaskLater(plugin, handler, 1);
    }

    @EventHandler
    public void onPlayerHeal(EntityRegainHealthEvent e) {
        Runnable handler = getHealthChangeHandler(e);
        Bukkit.getScheduler().runTaskLater(plugin, handler, 1);
    }

    @EventHandler
    public void onPlayerStateChange(PlayerStateChangeEvent e) {
        Runnable handler = () -> {
            Player pl = e.player();

            if (gameManager.hasUHCStarted()) {
                if (teamManager.isAssignedCombatant(pl)) {
                    // if they change state in game, then they just died or just respawned
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        updateCombatantsAliveHUD(p);
                        updateTeamsAliveHUD(p);
                    }
                }
            } else {
                prepareToLobby(pl);
            }
        };
        Bukkit.getScheduler().runTaskLater(plugin, handler, 1);
    }
}
