package xyz.baz9k.UHCGame;

import xyz.baz9k.UHCGame.util.*;
import static xyz.baz9k.UHCGame.util.Utils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class SpreadPlayersManager {
    private UHCGame plugin;

    public SpreadPlayersManager(UHCGame plugin) {
        this.plugin = plugin;
    }

    /**
     * Stores information for determining what groups to spread together
     */
    private static record Grouping(Function<UHCGame, Collection<? extends Collection<Player>>> groupGen, Location def) {
        public Collection<? extends Collection<Player>> groups(UHCGame plugin) { return groupGen.apply(plugin); }
    }
    
    /**
     * Generates a collection with each player in separate groups, meaning that all players are spread separately.
     * @return a new generator that spreads all players separately
     */
    public static Grouping BY_PLAYERS() {
        return BY_PLAYERS(null);
    };
        
    /**
     * Generates a collection with each player in separate groups, meaning that all players are spread separately.
     * @param def All other online players (specs, etc) will be teleported here
     * @return a new generator that spreads all players separately
     */
    public static Grouping BY_PLAYERS(Location def) {
        return new Grouping(pl -> {
            return pl.getTeamManager().getAllCombatants()
                .stream()
                .map(Collections::singleton)
                .toList();
        }, def);
    };

    /**
     * Generates a collection with each team in separate groups, meaning that all teams are spread together.
     * <p> Completely dead teams do not spread with this generator.
     * @return a new generator that spreads based on teams
     */
    public static Grouping BY_TEAMS() {
        return BY_TEAMS(null);
    }
    
    /**
     * Generates a collection with each team in separate groups, meaning that all teams are spread together.
     * <p> Completely dead teams do not spread with this generator.
     * @param def All other online players (specs, etc) will be teleported here
     * @return a new generator that spreads based on teams
     */
    public static Grouping BY_TEAMS(Location def) {
        return new Grouping(pl -> {
            var tm = pl.getTeamManager();
            return Arrays.stream(tm.getAliveTeams())
                .mapToObj(tm::getAllCombatantsOnTeam)
                .toList();
        }, def);
    }
    
    /**
     * Generates a collection based on the provided key map. Players mapping to the same key are spread together.
     * <p>
     * If a player is mapped to null, they will not be spread.
     * @param <K> Key return type, can be int, can be enum, can be w/e.
     * @param key Function that maps a player to a key
     * @return a new generator that spreads based on provided key
     */
    public static <K> Grouping BY(Function<Player, K> key) {
        return BY(key, null);
    };
    /**
     * Generates a collection based on the provided key map. Players mapping to the same key are spread together.
     * <p>
     * If a player is mapped to null, they will not be spread.
     * @param <K> Key return type, can be int, can be enum, can be w/e.
     * @param key Function that maps a player to a key
     * @param def All other online players (specs, etc) will be teleported here
     * @return a new generator that spreads based on provided key
     */
    public static <K> Grouping BY(Function<Player, K> key, Location def) {
        return new Grouping(pl -> {
            Map<K, List<Player>> g = pl.getTeamManager().getAllCombatants()
                .stream()
                .collect(Collectors.groupingBy(key));
            g.remove(null);
            return g.values();
        }, def);
    };

    private List<Location> getRootsOfUnityLocations(Location center, int numLocations, double distance) {
        Point2D center2 = Point2D.fromLocation(center);
        List<Location> locations = new ArrayList<>();
        World w = center.getWorld();
        for (int i = 0; i < numLocations; i++) {
            double theta = i * 2 * Math.PI / numLocations;
            Point2D newPt = center2.addPolar(distance, theta);
            locations.add(getHighestLoc(w, newPt));
        }
        Collections.shuffle(locations);
        return Collections.unmodifiableList(locations);
    }


    //random location generation
    private static boolean isLocationSpawnable(Location l) {
        return (!isLocationOverLava(l) && !isLocationOverWater(l));
    }

    private static boolean isLocationOverLava(Location l) {
        Location blockLocation = l.add(0,-1,0);
        return (blockLocation.getBlock().getType() == Material.LAVA);
    }

    private static boolean isLocationOverWater(Location l) {
        Location blockLocation = l.add(0,-1,0);
        return (blockLocation.getBlock().getType() == Material.WATER);
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

        Debug.printDebug(trans("xyz.baz9k.uhc.debug.spreadplayers.generated", samples.size()));

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
            throw translatableErr(IllegalStateException.class, "xyz.baz9k.uhc.err.world.missing_spread_locs", totalSize, numLocations);
        }

        if (spawnableLocations.size() < numLocations) {
            int numOverWaterLocations = numLocations - spawnableLocations.size();
            Collections.shuffle(overWaterLocations);
            spawnableLocations.addAll(overWaterLocations.subList(0, numOverWaterLocations));
        }

        Collections.shuffle(spawnableLocations);
        return Collections.unmodifiableList(spawnableLocations.subList(0, numLocations));

    }

    /**
     * Spreads players to a list of locations by the given generator
     * @param grouping Should teams be separated together?
     * @param locGenerator Takes in int n, returns a list of locations of size n
     */
    private void spreadPlayers(Grouping grouping, IntFunction<List<Location>> locGenerator) {
        var def = grouping.def();
        if (def != null) {
            teleportGroup(Bukkit.getOnlinePlayers(), def);
        }

        var groups = grouping.groups(plugin);
        var locs = locGenerator.apply(groups.size());
        
        var groupIter = groups.iterator();
        var locsIter = locs.iterator();

        while (groupIter.hasNext() && locsIter.hasNext()) teleportGroup(groupIter.next(), locsIter.next());
    }

    /**
     * Spreads players randomly
     * @param grouping
     * @param center
     * @param maximumRange
     * @param minSeparation
     */
    public void random(Grouping grouping, Location center, double maximumRange, double minSeparation) {
        spreadPlayers(grouping, n -> getRandomLocations(center, n, maximumRange, minSeparation));
    }

    /**
     * Spreads players based on the roots of unity
     * @param grouping
     * @param center
     * @param distance
     */
    public void rootsOfUnity(Grouping grouping, Location center, double distance) {
        spreadPlayers(grouping, n -> getRootsOfUnityLocations(center, n, distance));
    }

    private void teleportGroup(Collection<? extends Player> group, Location location) {
        for (Player p : group) p.teleport(location);
    }

}
