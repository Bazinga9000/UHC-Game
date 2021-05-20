package xyz.baz9k.UHCGame;

import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.World.Environment;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import xyz.baz9k.UHCGame.util.*;

import java.time.*;
import java.util.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.function.IntFunction;

import static xyz.baz9k.UHCGame.util.Utils.*;

public class GameManager implements Listener {
    private UHCGame plugin;

    private final HashMap<UUID, Component> previousDisplayNames;
    
    private TeamManager teamManager;
    private HUDManager hudManager;
    private BossbarManager bbManager;
    private Recipes recipes;
    private BukkitTask tick;

    private Instant startTime = null;
    
    private boolean worldsRegened = false;

    private HashMap<UUID, Integer> kills = new HashMap<>();

    private final Point2D center = new Point2D(0, 0);

    private GameStage stage = GameStage.NOT_IN_GAME;
    private Instant lastStageInstant = null;

    public GameManager(UHCGame plugin) {
        this.plugin = plugin;
        previousDisplayNames = new HashMap<>();

        // create MV worlds if missing
        createMVWorld("game", Environment.NORMAL);
        createMVWorld("game_nether", Environment.NETHER);
    }

    /**
     * After all managers are initialized, this function is run to give gameManager references to all other managers.
     */
    public void loadManagerRefs() {
        teamManager = plugin.getTeamManager();
        hudManager = plugin.getHUDManager();
        bbManager = plugin.getBossbarManager();
        recipes = plugin.getRecipes();
    }

    /**
     * Starts UHC.
     * <p>
     * Accessible through /uhc start or /uhc start force.
     * <p>
     * /uhc start: Checks that teams are assigned, worlds have regenerated, and game has not started
     * <p>
     * /uhc start force: Skips checks
     * @param skipChecks If true, all checks are ignored.
     */
    public void startUHC(boolean skipChecks) {
        Debug.printDebug(Component.translatable("xyz.baz9k.uhc.debug.start.try"));
        if (!skipChecks) {
            // check if game is OK to start
            requireNotStarted();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (teamManager.getPlayerState(p) == PlayerState.COMBATANT_UNASSIGNED) {
                    throw new IllegalStateException("Teams have not been assigned.");
                }
            }
            if (!worldsRegened) {
                throw new IllegalStateException("UHC worlds have not been regenerated. Run /uhc reseed to regenerate.");
            }
        } else {
            Debug.printDebug(Component.translatable("xyz.baz9k.uhc.debug.start.force"));
        }

        try {
            _startUHC();
            Debug.printDebug(Component.translatable("xyz.baz9k.uhc.debug.start.complete"));
        } catch (Exception e) {
            setStage(GameStage.NOT_IN_GAME);
            Debug.printDebug(Component.translatable("xyz.baz9k.uhc.debug.start.fail"));
            Debug.printError(e);
        }
    }
    private void _startUHC() {
        setStage(GameStage.fromIndex(0));
        worldsRegened = false;

        startTime = lastStageInstant = Instant.now();
        kills.clear();
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            // archive previous display name
            previousDisplayNames.put(p.getUniqueId(), p.displayName());
            resetStatuses(p);
            recipes.discoverFor(p);

            // 60s grace period
            PotionEffectType.DAMAGE_RESISTANCE.createEffect(60 * 20 /* ticks */, /* lvl */ 5).apply(p);

            if (teamManager.isSpectator(p)) {
                p.setGameMode(GameMode.SPECTATOR);
            } else {
                p.setGameMode(GameMode.SURVIVAL);
                kills.put(p.getUniqueId(), 0);
            }

            // set player display name
            p.displayName(TeamDisplay.prefixed(teamManager.getTeam(p), p.getName()));

            // activate hud things for all
            hudManager.initializePlayerHUD(p);
        }
        
        for (World w : getUHCWorlds()) {
            // set time to 0 and delete rain
            w.setTime(0);
            w.setClearWeatherDuration(Integer.MAX_VALUE); // there is NO rain. Ever again. [ :( ]
            
            w.getWorldBorder().setCenter(center.x(), center.z());
            w.getWorldBorder().setWarningDistance(25);

            Gamerules.set(w);
            purgeWorld(w);

            // create beacon in worlds
            w.getBlockAt(0, 1, 0).setType(Material.BEACON);
            w.getBlockAt(0, 2, 0).setType(Material.BEDROCK);
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    w.getBlockAt(x, 0, z).setType(Material.NETHERITE_BLOCK);
                }
            }
            for (int y = 3; y <= w.getMaxHeight() - 2; y++) {
                w.getBlockAt(0, y, 0).setType(Material.BARRIER);
            }
        }

        Debug.printDebug(Component.translatable("xyz.baz9k.uhc.debug.spreadplayers.start"));
        spreadPlayersRandom(true, getCenter(), GameStage.WB_STILL.getWBDiameter(), GameStage.WB_STILL.getWBDiameter() / (1 + teamManager.getNumTeams()));
        Debug.printDebug(Component.translatable("xyz.baz9k.uhc.debug.spreadplayers.end"));
        Bukkit.unloadWorld(getLobbyWorld(), true);

        startTick();
        bbManager.enable();
    }

    /**
     * Ends UHC.
     * <p>
     * Accessible through /uhc end or /uhc end force.
     * <p>
     * /uhc end: Checks that the game has started
     * <p>
     * /uhc end force: Forcibly starts game
     * @param skipChecks If true, started game checks are ignored.
     */
    public void endUHC(boolean skipChecks) {
        Debug.printDebug(Component.translatable("xyz.baz9k.uhc.debug.end.try"));
        if (!skipChecks) {
            // check if game is OK to end
            requireStarted();
        } else {
            Debug.printDebug(Component.translatable("xyz.baz9k.uhc.debug.end.force"));
        }
        
        try {
            _endUHC();
            Debug.printDebug(Component.translatable("xyz.baz9k.uhc.debug.end.complete"));
        } catch (Exception e) {
            setStage(GameStage.WB_STILL);
            Debug.printDebug(Component.translatable("xyz.baz9k.uhc.debug.end.fail"));
            Debug.printError(e);
        }
    }

    private void _endUHC() {
        setStage(GameStage.NOT_IN_GAME);
        escapeAll();
        for (Player p : Bukkit.getOnlinePlayers()) {
            resetStatuses(p);
            p.setGameMode(GameMode.SURVIVAL);
        }
        
        // update display names
        for (UUID uuid : previousDisplayNames.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            p.displayName(previousDisplayNames.get(uuid));
        }

        teamManager.resetAllPlayers();
        hudManager.cleanup();
        kills.clear();
        tick.cancel();

        bbManager.disable();

    }

    public void requireStarted() {
        if (!hasUHCStarted()) {
            throw new IllegalStateException("UHC has not started");
        }
    }

    public void requireNotStarted() {
        if (hasUHCStarted()) {
            throw new IllegalStateException("UHC has already started");
        }
    }

    private void startTick() {
        tick = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!hasUHCStarted()) return;
    
            bbManager.tick();
            
            if (isStageComplete()) {
                incrementStage();
            }
            
            for (Player p : Bukkit.getOnlinePlayers()) {
                hudManager.updateElapsedTimeHUD(p);
                hudManager.updateWBHUD(p);
            }
        }, 0L, 1L);
    }

    /**
     * Resets a player's statuses (health, food, sat, xp, etc.)
     * @param p
     */
    public void resetStatuses(@NotNull Player p) {
        // fully heal, adequately saturate, remove XP
        p.setHealth(20.0f);
        p.setFoodLevel(20);
        p.setSaturation(5.0f);
        p.setExp(0.0f);
        p.getInventory().clear();
        
        // clear all potion effects
        for (PotionEffect effect : p.getActivePotionEffects()) {
            p.removePotionEffect(effect.getType());
        }
    }

    /**
     * Sends all players back to the lobby world.
     * <p>
     * Accessible through /uhc escape
     */
    public void escapeAll() {
        World lobby = getLobbyWorld();
        Location spawn = new Location(lobby, 0, 10, 0);
        Material mat = spawn.getBlock().getType();

        // check if 0 10 0 is valid spawn place, else teleport to highest 0,0
        if (mat != Material.AIR && mat != Material.CAKE) {
            spawn = lobby.getHighestBlockAt(0, 0).getLocation();
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setBedSpawnLocation(spawn);
            p.teleport(spawn);
        };
    }

    public boolean hasUHCStarted() {
        return stage != GameStage.NOT_IN_GAME;
    }

    /**
     * Reseed worlds then mark worlds as reseeded.
     * <p>
     * Accessible through /uhc reseed
     */
    public void reseedWorlds() {
        long l = new Random().nextLong();
        reseedWorlds(String.valueOf(l));
    }

    /**
     * Reseed worlds then mark worlds as reseeded.
     * <p>
     * Accessible through /uhc reseed <seed>
     * @param seed
     */
    public void reseedWorlds(String seed) {
        var wm = plugin.getMVWorldManager();
        for (World w : getUHCWorlds()) {
            wm.regenWorld(w.getName(), true, false, seed);
        }
        worldsRegened = true;
    }

    private void purgeWorld(World w) {
        var wm = plugin.getMVWorldManager();
        var purger = wm.getTheWorldPurger();
        var mvWorld = wm.getMVWorld(w);

        purger.purgeWorld(mvWorld, Arrays.asList("MONSTERS"), false, false); // multiverse is stupid (purges all monsters, hopefully)
    }

    /**
     * @return the {@link Duration} since the game has started.
     */
    @NotNull
    public Duration getElapsedTime() {
        return Duration.between(startTime, Instant.now());
    }

    /**
     * @return the current stage of the game.
     */
    public GameStage getStage() {
        return stage;
    }

    public void incrementStage() {
        setStage(stage.next());
    }

    public void setStage(GameStage stage) {
        if (stage == null) return;
        this.stage = stage;
        updateStage();
    }

    private void updateStage() {
        if (!hasUHCStarted()) return;
        lastStageInstant = Instant.now();
        bbManager.updateBossbarStage();

        stage.sendMessage();
        stage.applyWBSize(getUHCWorlds());

        // deathmatch
        if (isDeathmatch()) {
            World w = getUHCWorld(Environment.NORMAL);

            int radius = (int) GameStage.DEATHMATCH.getWBRadius();
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    w.getBlockAt(x, w.getMaxHeight() - 2, z).setType(Material.BARRIER);
                }
            }
            for (Player p : teamManager.getAllCombatants()) {
                p.addPotionEffects(Arrays.asList(
                    PotionEffectType.DAMAGE_RESISTANCE.createEffect(10 * 20, 10),
                    PotionEffectType.SLOW.createEffect(10 * 20, 10),
                    PotionEffectType.JUMP.createEffect(10 * 20, 128),
                    PotionEffectType.BLINDNESS.createEffect(10 * 20, 10)
                ));
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.teleport(getCenterAtY(255));
            }

            spreadPlayersRootsOfUnity(true, getCenter(), GameStage.DEATHMATCH.getWBRadius() - 1);

        }

    }

    /**
     * @return the {@link Duration} that the current stage lasts.
     */
    @NotNull
    public Duration getStageDuration() {
        requireStarted();
        return stage.getDuration();
    }

    /**
     * @return the {@link Duration} until the current stage ends.
     */
    @NotNull
    public Duration getRemainingStageDuration() {
        requireStarted();
        Duration stageDur = getStageDuration();
        if (isDeathmatch()) return stageDur; // if deathmatch, just return ∞
        return Duration.between(Instant.now(), lastStageInstant.plus(stageDur));
    }

    /**
     * @return if deathmatch (last stage) has started.
     */
    public boolean isDeathmatch() {
        return stage == GameStage.DEATHMATCH;
    }

    /**
     * @return if the stage has completed and needs to be incremented.
     */
    public boolean isStageComplete() {
        if (isDeathmatch()) return false;
        Instant end = lastStageInstant.plus(stage.getDuration());
        return !end.isAfter(Instant.now());
    }

    private void createMVWorld(@NotNull String world, @NotNull Environment env) {
        var wm = plugin.getMVWorldManager();
        if (wm.isMVWorld(world)) return;
        wm.addWorld(world, env, String.valueOf(new Random().nextLong()), WorldType.NORMAL, true, null);
    }

    /**
     * @return an Array of {@link World} which the UHC uses.
     */
    @NotNull
    public World[] getUHCWorlds() {
        return new World[] {
            Bukkit.getWorld("game"),
            Bukkit.getWorld("game_nether")
        };
    }

    /**
     * Get a specific game world based on its environment.
     * @param env NORMAL|NETHER
     * @return the world. If there is no respective game world for the environment (e.g. THE_END) return null.
     */
    public World getUHCWorld(@NotNull Environment env) {
        World[] worlds = getUHCWorlds();
        return switch (env) {
            case NORMAL -> worlds[0];
            case NETHER -> worlds[1];
            default -> null;
        };
    }

    public World getLobbyWorld() {
        World lobby = Bukkit.getWorld("lobby");
        if (lobby != null) return lobby;
        return Bukkit.getWorld("world");
    }

    private Location getCenter() {
        return getCenterAtY(0);
    }
    private Location getCenterAtY(double y) {
        return center.loc(getUHCWorld(Environment.NORMAL), y);
    }

    /*
    private void setCenter(double x, double z) {
        center[0] = x;
        center[1] = z;
    }
    */

    /**
     * Returns the number of kills that this combatant has dealt.
     * @param p
     * @return the number of kills in an OptionalInt. 
     * <p>
     * If the player is not registered in the kills map (i.e. they're a spec), return empty OptionalInt.
     */
    public OptionalInt getKills(@NotNull Player p) {
        Integer k = kills.get(p.getUniqueId());
        if (k == null) return OptionalInt.empty();
        return OptionalInt.of(k);
    }

    private List<Location> getRootsOfUnityLocations(Location center, int numLocations, double distance) {
        Point2D center2 = Point2D.fromLocation(center);
        List<Location> locations = new ArrayList<>();
        World w = center.getWorld();

        for (int i = 0; i < numLocations; i++) {
            double theta = i * 2 * Math.PI / numLocations;
            center2.addPolar(distance, theta);
            locations.add(getHighestLoc(w, center2));
        }
        Collections.shuffle(locations);
        return Collections.unmodifiableList(locations);
    }

    //poisson disk sampling
    private List<Location> getRandomLocations(Location center, int numLocations, double sideLength, double minSeparation) {
        Point2D center2 = Point2D.fromLocation(center);
        List<Point2D> samples = new ArrayList<>();
        List<Point2D> activeList = new ArrayList<>();
        Random r = new Random();

        final int POINTS_PER_ITER = 30;
        
        Point2D firstLocation = Point2D.uniformRand(center2, sideLength);
        activeList.add(firstLocation);
        samples.add(firstLocation);

        while (!activeList.isEmpty()) {
            int index = r.nextInt(activeList.size());
            Point2D search = activeList.get(index);
            Point2D toCheck = new Point2D(0,0);
            boolean success = false;

            for (int i = 0; i < POINTS_PER_ITER; i++) {
                toCheck = Point2D.ringRand(search, minSeparation, 2 * minSeparation);
                if (!toCheck.inSquare(center2, sideLength)) {
                    continue;
                }

                double minDist = Double.MAX_VALUE;
                for (Point2D point : samples) {
                    double d = toCheck.dist(point);
                    if (d < minDist) {
                        minDist = d;
                    }
                }

                if (minDist < minSeparation) {
                    continue;
                }

                success = true;
                break;

            }

            if (success) {
                activeList.add(toCheck);
                samples.add(toCheck);
            } else {
                activeList.remove(index);
            }
        }

        Debug.printDebug(Component.translatable("xyz.baz9k.uhc.debug.spreadplayers.generated").args(Component.text(samples.size())));

        List<Location> spawnableLocations = new ArrayList<>();
        List<Location> overWaterLocations = new ArrayList<>();
        World w = center.getWorld();
        for (Point2D samplePoint : samples) {
            Location sample = getHighestLoc(w, samplePoint);
            if (isLocationSpawnable(sample)) {
                spawnableLocations.add(sample);
            } else if (isLocationOverWater(sample)) {
                overWaterLocations.add(sample);
            }
        }
        int totalSize = spawnableLocations.size() + overWaterLocations.size();
        if (totalSize < numLocations) {
            throw new IllegalStateException("Not enough locations (" + totalSize + ") were generated.");
        }

        if (spawnableLocations.size() < numLocations) {
            int numOverWaterLocations = numLocations - spawnableLocations.size();
            Collections.shuffle(overWaterLocations);
            spawnableLocations.addAll(overWaterLocations.subList(0, numOverWaterLocations));
        }

        Collections.shuffle(spawnableLocations);
        return Collections.unmodifiableList(spawnableLocations.subList(0, numLocations));

    }

    /*
    private List<Location> getRandomLocations(Location center, int numLocations, double maximumRange, double minSeparation) {
        ArrayList<Location> locations = new ArrayList<>();
        Random r = new Random();
        World w = getUHCWorld(Environment.NORMAL);

        for (int i = 0; i < numLocations; i++) {
            Location newLocation = null;
            while (true) {
                double x = center.getX() + (r.nextDouble() - 0.5) * maximumRange;
                double z = center.getZ() + (r.nextDouble() - 0.5) * maximumRange;

                if (!locations.isEmpty()) {
                    double minDist = locations.stream()
                                                      .map(l -> euclideanDistance(x, z, l.getX(), l.getZ()))
                                                      .min(Double::compareTo)
                                                      .orElseThrow();
                    if (minDist < minSeparation) {
                        continue;
                    }
                }
                newLocation = new Location(w, x, 2 + w.getHighestBlockYAt((int) x, (int) z), z);

                Material blockType = w.getBlockAt(newLocation).getType();

                if (blockType == Material.LAVA) {
                    continue;
                }

                if (blockType == Material.WATER) {
                    continue;
                }

                break;
            }

            locations.add(newLocation);
        }
        return locations;
    }
    */

    // TODO: spreadPlayers groups, for groups not aligning with teams
    /**
     * Spreads players to a list of locations by the given generator
     * @param respectTeams Should teams be separated together?
     * @param locGenerator Takes in int n, returns a list of locations of size n
     */
    private void spreadPlayers(boolean respectTeams, IntFunction<List<Location>> locGenerator) {
        if (respectTeams) {
            List<Collection<Player>> groups = new ArrayList<>();
            for (int i : teamManager.getAliveTeams()) {
                groups.add(teamManager.getAllCombatantsOnTeam(i));
            }
            
            List<Location> loc = locGenerator.apply(groups.size());
            teleportPlayersToLocations(groups, loc);
        } else {
            Collection<Player> players = teamManager.getAllCombatants();

            List<Location> loc = locGenerator.apply(players.size());
            teleportPlayersToLocations(players, loc);
        }
    }

    /**
     * Spreads players randomly
     * @param respectTeams
     * @param center
     * @param maximumRange
     * @param minSeparation
     */
    public void spreadPlayersRandom(boolean respectTeams, Location center, double maximumRange, double minSeparation) {
        spreadPlayers(respectTeams, n -> getRandomLocations(center, n, maximumRange, minSeparation));
    }

    /**
     * Spreads players based on the roots of unity
     * @param respectTeams
     * @param center
     * @param distance
     */
    public void spreadPlayersRootsOfUnity(boolean respectTeams, Location center, double distance) {
        spreadPlayers(respectTeams, n -> getRootsOfUnityLocations(center, n, distance));
    }

    private void teleportPlayersToLocations(Collection<Player> players, List<Location> locations) {
        int index = 0;
        for (Player p : players) {
            p.teleport(locations.get(index));
            index++;
        }
    }

    private void teleportPlayersToLocations(List<Collection<Player>> groups, List<Location> locations) {
        for (int i = 0; i < groups.size(); i++) {
            for (Player p : groups.get(i)) {
                p.teleport(locations.get(i));
            }
        }
    }

    private void winMessage() {
        if (teamManager.countLivingTeams() > 1) return;
        int winner = teamManager.getAliveTeams()[0];
        Component winMsg = Component.translatable("xyz.baz9k.uhc.win", noDeco(NamedTextColor.WHITE))
            .args(TeamDisplay.getName(winner));

        // this msg should be displayed after player death
        delayedMessage(winMsg, plugin, 1);
        
        var fwe = FireworkEffect.builder()
                                .withColor(TeamDisplay.getBukkitColor(winner), org.bukkit.Color.WHITE)
                                .with(FireworkEffect.Type.BURST)
                                .withFlicker()
                                .withTrail()
                                .build();
        for (Player p : teamManager.getAllCombatantsOnTeam(winner)) {
            Firework fw = p.getWorld().spawn(p.getLocation(), Firework.class);
            FireworkMeta meta = fw.getFireworkMeta();
            meta.addEffect(fwe);
            meta.setPower(1);
            fw.setFireworkMeta(meta);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        teamManager.addPlayer(p, hasUHCStarted());
        if(!hasUHCStarted()) return;
        bbManager.addPlayer(p);
        hudManager.initializePlayerHUD(p);
        hudManager.addPlayerToTeams(p);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (!hasUHCStarted()) return;
        Player dead = e.getEntity();
        
        dead.setGameMode(GameMode.SPECTATOR);
        if (teamManager.getPlayerState(dead) == PlayerState.COMBATANT_ALIVE) {
            teamManager.setCombatantAliveStatus(dead, false);

            // check team death
            int t = teamManager.getTeam(dead);
            if (teamManager.isTeamEliminated(t)) {
                Component teamElimMsg = Component.translatable("xyz.baz9k.uhc.eliminated", noDeco(NamedTextColor.WHITE))
                    .args(TeamDisplay.getName(t));
                // this msg should be displayed after player death
                delayedMessage(teamElimMsg, plugin, 1);
            }

            // set bed spawn
            Location newSpawn = dead.getLocation();
            if (newSpawn.getY() < 0) {
                dead.setBedSpawnLocation(getUHCWorld(Environment.NORMAL).getSpawnLocation(), true);
            } else {
                dead.setBedSpawnLocation(newSpawn, true);
            }

            // check win condition
            if (teamManager.countLivingTeams() == 1) {
                winMessage();
            }
        }

        Player killer = dead.getKiller();
        if (killer != null) {
            OptionalInt k = getKills(killer);
            if (k.isPresent()) {
                this.kills.put(killer.getUniqueId(), k.orElseThrow() + 1);
                hudManager.updateKillsHUD(killer);
            }
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            hudManager.updateCombatantsAliveHUD(p);
            hudManager.updateTeamsAliveHUD(p);
        }
    }

    @EventHandler
    public void onPlayerFight(EntityDamageByEntityEvent e) {
        if (!hasUHCStarted()) return;
        // friendly fire
        if (e.getEntity() instanceof Player target) {
            if (e.getDamager() instanceof Player damager) {
                if (teamManager.getTeam(target) == teamManager.getTeam(damager)) {
                    e.setCancelled(true);
                }
            }
        }
    }
}
