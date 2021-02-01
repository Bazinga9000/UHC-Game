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
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import net.md_5.bungee.api.chat.BaseComponent;
import xyz.baz9k.UHCGame.util.ColoredText;
import xyz.baz9k.UHCGame.util.Debug;
import xyz.baz9k.UHCGame.util.TeamDisplay;

import java.time.*;
import java.util.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static xyz.baz9k.UHCGame.util.Utils.*;

public class GameManager implements Listener {
    private UHCGame plugin;

    private final HashMap<UUID, String> previousDisplayNames;
    
    private TeamManager teamManager;
    private HUDManager hudManager;
    private BossbarManager bbManager;
    private Recipes recipes;
    private BukkitRunnable tick;

    private Instant startTime = null;
    
    private boolean worldsRegened = false;

    private HashMap<UUID, Integer> kills = new HashMap<>();

    private final double[] center = {0.5, 0.5};

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
        Debug.broadcastDebug("UHC attempting start");
        if (!skipChecks) {
            // check if game is OK to start
            requireNotStarted();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (teamManager.getPlayerState(p) == PlayerState.COMBATANT_UNASSIGNED) {
                    throw new IllegalStateException("Teams have not been assigned.");
                }
            }
            if (!worldsRegened) {
                throw new IllegalStateException("UHC worlds have not been regenerated. Run /reseed to regenerate.");
            }
        } else {
            Debug.broadcastDebug("Skipping starting requirements");
        }

        try {
            _startUHC();
            Debug.broadcastDebug("UHC started");
        } catch (Exception e) {
            setStage(GameStage.NOT_IN_GAME);
            Debug.broadcastDebug("UHC cancelling start due to error");
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
            previousDisplayNames.put(p.getUniqueId(), p.getDisplayName());
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
            p.setDisplayName(TeamDisplay.prefixed(teamManager.getTeam(p),  p.getName()));

            // activate hud things for all
            hudManager.initializePlayerHUD(p);
        }
        
        for (World w : getUHCWorlds()) {
            // set time to 0 and delete rain
            w.setTime(0);
            w.setClearWeatherDuration(Integer.MAX_VALUE); // there is NO rain. Ever again. [ :( ]
            
            w.getWorldBorder().setCenter(center[0], center[1]);
            w.getWorldBorder().setWarningDistance(25);

            Gamerules.set(w);
            purgeWorld(w);

            // create beacon in worlds
            w.getBlockAt(0, 1, 0).setType(Material.BEACON);
            w.getBlockAt(0, 2, 0).setType(Material.BEDROCK);
            for (int x = -1; x < 2; x++) {
                for (int z = -1; z < 2; z++) {
                    w.getBlockAt(x, 0, z).setType(Material.NETHERITE_BLOCK);
                }
            }
            for (int y = 3; y < w.getMaxHeight() - 1; y++) {
                w.getBlockAt(0, y, 0).setType(Material.BARRIER);
            }
        }

        Debug.broadcastDebug("Generating Spawn Locations");
        spreadPlayersRandom(true, getCenter(), GameStage.WB_STILL.getWBRadius(), GameStage.WB_STILL.getWBDiameter() / (1 + teamManager.getNumTeams()));
        Debug.broadcastDebug("Done!");
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
        Debug.broadcastDebug("UHC attempting end");
        if (!skipChecks) {
            // check if game is OK to end
            requireStarted();
        } else {
            Debug.broadcastDebug("Skipping ending requirements");
        }
        
        try {
            _endUHC();
            Debug.broadcastDebug("UHC ended");
        } catch (Exception e) {
            setStage(GameStage.WB_STILL);
            Debug.broadcastDebug("UHC cancelling end due to error");
            Debug.printError(e);
        }
    }

    private void _endUHC() {
        setStage(GameStage.NOT_IN_GAME);
        // update display names
        escapeAll();
        for (Player p : Bukkit.getOnlinePlayers()) resetStatuses(p);

        for (UUID uuid : previousDisplayNames.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            p.setDisplayName(previousDisplayNames.get(uuid));
            p.setGameMode(GameMode.SURVIVAL);
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
        tick = new BukkitRunnable() {
            public void run() {
                if (!hasUHCStarted()) return;

                bbManager.tick();
                
                if (isStageComplete()) {
                    incrementStage();
                }
                
                for (Player p : Bukkit.getOnlinePlayers()) {
                    hudManager.updateElapsedTimeHUD(p);
                    hudManager.updateWBHUD(p);
                }
            }
        };

        tick.runTaskTimer(plugin, 0L, 1L);
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
        World world;
        switch (env) {
            case NORMAL:
                world = worlds[0];
                break;
            case NETHER:
                world = worlds[1];
                break;
            case THE_END:
            default:
                world = null;
                break;
        }
        return world;
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
        return new Location(getUHCWorld(Environment.NORMAL), center[0], y, center[1]);
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
        ArrayList<Location> locations = new ArrayList<Location>();
        World w = getUHCWorld(Environment.NORMAL);
        for (int i = 0; i < numLocations; i++) {
            double X = center.getX() + (distance * Math.cos(i * 2 * Math.PI / numLocations));
            double Z = center.getZ() + (distance * Math.sin(i * 2 * Math.PI / numLocations));
            double Y = 2 + w.getHighestBlockYAt((int) X, (int) Z);
            locations.add(new Location(w, X, Y, Z));
        }
        Collections.shuffle(locations);
        return locations;
    }

    //poisson disk sampling
    private List<Location> getRandomLocations(Location center, int numLocations, double squareEdgeLength, double minimumSeparation) {
        ArrayList<Location> samples = new ArrayList<>();
        ArrayList<Location> activeList = new ArrayList<>();
        Random r = new Random();

        double minX = center.getX() - (squareEdgeLength/2);
        double maxX = center.getX() + (squareEdgeLength/2);
        double minZ = center.getZ() - (squareEdgeLength/2);
        double maxZ = center.getZ() + (squareEdgeLength/2);

        final int numPointsPerIteration = 30;
        World w = getUHCWorld(Environment.NORMAL);
        Location firstLocation = uniformRandomSpawnableLocation(w, minX, maxX, minZ, maxZ);
        activeList.add(firstLocation);
        samples.add(firstLocation);

        int count = 0;
        while (!activeList.isEmpty()) {
            count++;
            if (count % 5 == 0) {
                Debug.broadcastDebug(samples.size() + " Samples, " + activeList.size() + " Active");
            }
            int index = r.nextInt(activeList.size());
            Location search = activeList.get(index);
            Location toCheck = ringRandomSpawnableLocation(w, search.getX(), search.getZ(), minimumSeparation, 2 * minimumSeparation);
            boolean success = false;
            for (int i = 0; i < numPointsPerIteration; i++) {
                toCheck = ringRandomSpawnableLocation(w, search.getX(), search.getZ(), minimumSeparation, 2 * minimumSeparation);
                if (!isLocationInSquare(toCheck, center, squareEdgeLength)) {
                    continue;
                }
                double x = toCheck.getX();
                double z = toCheck.getZ();
                double minimumDistance = samples.stream()
                        .map(l -> euclideanDistance(x, z, l.getX(), l.getZ()))
                        .min(Double::compareTo)
                        .orElseThrow();

                if (minimumDistance < minimumSeparation) {
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

        Collections.shuffle(samples);
        return samples.subList(0, numLocations);

    }

    /*
    private List<Location> getRandomLocations(Location center, int numLocations, double maximumRange, double minimumSeparation) {
        ArrayList<Location> locations = new ArrayList<>();
        Random r = new Random();
        World w = getUHCWorld(Environment.NORMAL);

        for (int i = 0; i < numLocations; i++) {
            Location newLocation = null;
            while (true) {
                double x = center.getX() + (r.nextDouble() - 0.5) * maximumRange;
                double z = center.getZ() + (r.nextDouble() - 0.5) * maximumRange;

                if (!locations.isEmpty()) {
                    double minimumDistance = locations.stream()
                                                      .map(l -> euclideanDistance(x, z, l.getX(), l.getZ()))
                                                      .min(Double::compareTo)
                                                      .orElseThrow();
                    if (minimumDistance < minimumSeparation) {
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

    /**
     * Spreads players randomly
     * @param respectTeams
     * @param center
     * @param maximumRange
     * @param minimumSeparation
     */
    public void spreadPlayersRandom(boolean respectTeams, Location center, double maximumRange, double minimumSeparation) {
        if (respectTeams) {
            List<Collection<Player>> groups = new ArrayList<>();
            for (int i : teamManager.getAliveTeams()) {
                groups.add(teamManager.getAllCombatantsOnTeam(i));
            }
            List<Location> locations = getRandomLocations(center, groups.size(), maximumRange, minimumSeparation);
            teleportPlayersToLocations(groups, locations);
        } else {
            Collection<Player> players = teamManager.getAllCombatants();
            List<Location> locations = getRandomLocations(center, players.size(), maximumRange, minimumSeparation);
            teleportPlayersToLocations(players, locations);
        }
    }

    /**
     * Spreads players based on the roots of unity
     * @param respectTeams
     * @param center
     * @param distance
     */
    public void spreadPlayersRootsOfUnity(boolean respectTeams, Location center, double distance) {
        List<Location> locations;
        if (respectTeams) {
            List<Collection<Player>> groups = new ArrayList<>();
            for (int i : teamManager.getAliveTeams()) {
                groups.add(teamManager.getAllCombatantsOnTeam(i));
            }
            locations = getRootsOfUnityLocations(center, groups.size(), distance);
            teleportPlayersToLocations(groups, locations);
        } else {
            Collection<Player> players = teamManager.getAllCombatants();
            locations = getRootsOfUnityLocations(center, players.size(), distance);
            teleportPlayersToLocations(players, locations);
        }
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
        BaseComponent[] winMsg = new ColoredText()
            .append(TeamDisplay.getName(winner))
            .append(" has won!")
            .toComponents();

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
        teamManager.addPlayer(p);
        if(!hasUHCStarted()) return; 
        if (!teamManager.isAssignedCombatant(p)) teamManager.setSpectator(p);
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
                BaseComponent[] teamEliminatedMessage;
                teamEliminatedMessage = new ColoredText()
                                            .appendColored(TeamDisplay.getName(t))
                                            .append(" has been eliminated!")
                                            .toComponents();
                // this msg should be displayed after player death
                delayedMessage(teamEliminatedMessage, plugin, 1);
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
        if (!(e.getEntity() instanceof Player)) return;
        if (!(e.getDamager() instanceof Player)) return;
        
        // friendly fire
        Player target = (Player) e.getEntity();
        Player damager = (Player) e.getDamager();
        if (teamManager.getTeam(target) == teamManager.getTeam(damager)) {
            e.setCancelled(true);
        }
    }
}
