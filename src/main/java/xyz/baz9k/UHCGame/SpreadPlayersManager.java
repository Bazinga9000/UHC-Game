package xyz.baz9k.UHCGame;

import xyz.baz9k.UHCGame.util.*;
import static xyz.baz9k.UHCGame.util.Utils.*;
import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class SpreadPlayersManager {
    private UHCGamePlugin plugin;

    public SpreadPlayersManager(UHCGamePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Stores information for determining what groups to spread together
     */
    private static record Grouping(Function<UHCGamePlugin, Collection<? extends Collection<Player>>> groupGen, Location def) {
        public Collection<? extends Collection<Player>> groups(UHCGamePlugin plugin) { return groupGen.apply(plugin); }
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
     private static boolean isLocationUnspawnable(Location l) {
        return isLocationOverLava(l);
    }
    private static boolean isLocationAvoidSpawn(Location l) {
        return isLocationOverWater(l);
    }

    private static boolean isLocationOverLava(Location l) {
        Location blockLocation = l.add(0,-1,0);
        return (blockLocation.getBlock().getType() == Material.LAVA);
    }

    private static boolean isLocationOverWater(Location l) {
        Location blockLocation = l.add(0,-1,0);
        return (blockLocation.getBlock().getType() == Material.WATER);
    }

    private record Index2D(int x, int y) {
        public Index2D translate(int X, int Y) { return new Index2D(x + X, y + Y); } 
    }
    private static Index2D gridPoint(Point2D topLeft, double cellSize, Point2D pointToGet) {
        Point2D diff = pointToGet.minus(topLeft);
        int x = (int) Math.floor(diff.x() / cellSize),
            y = (int) Math.floor(diff.z() / cellSize);
        return new Index2D(x, y);
    }
    private static boolean isOnGrid(int[][] bgGrid, Index2D gridPoint) {
        int x = gridPoint.x(),
            y = gridPoint.y();
        int w = bgGrid[0].length,
            h = bgGrid.length;
        return (0 <= x && x < w) && (0 <= y && y < h);
    }
    private static int getOnGrid(int[][] bgGrid, Index2D gridPoint) {
        int x = gridPoint.x(),
            y = gridPoint.y();
        return bgGrid[y][x];
    }
    private static void setOnGrid(int[][] bgGrid, Index2D gridPoint, int val) {
        int x = gridPoint.x(),
            y = gridPoint.y();
        bgGrid[y][x] = val;
    }
    private static Set<Integer> gridNeighbors(int[][] bgGrid, Index2D gridPoint) {
        Set<Integer> s = new HashSet<>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                var neighborIndex = gridPoint.translate(x, y);
                if (!isOnGrid(bgGrid, neighborIndex)) continue;
                
                var neighborSampleIndex = getOnGrid(bgGrid, neighborIndex);
                if (neighborSampleIndex == -1) continue;
                s.add(neighborSampleIndex);
            }
        }
        return s;
    }

    //poisson disk sampling https://www.cs.ubc.ca/~rbridson/docs/bridson-siggraph07-poissondisk.pdf
    private static List<Location> getRandomLocations(Location center, int numLocations, double sideLength, double minSeparation) {
        double cellSize = minSeparation / Math.sqrt(2);
        int bgGridSize = (int) Math.ceil(sideLength / cellSize); // number of cells in one length of the bg grid
        int[][] bgGrid = new int[bgGridSize][bgGridSize];
        for (int y = 0; y < bgGrid.length; y++) {
            for (int x = 0; x < bgGrid[0].length; x++) {
                bgGrid[y][x] = -1;
            }
        }

        Point2D center2 = Point2D.fromLocation(center);
        Point2D topLeft = center2.minus(sideLength / 2, sideLength / 2);
        List<Point2D> samples = new ArrayList<>();
        List<Point2D> activeList = new ArrayList<>();
        Random r = new Random();
        World w = center.getWorld();

        final int POINTS_PER_ITER = 30;
        
        Point2D firstLocation = Point2D.uniformRand(center2, sideLength);
        activeList.add(firstLocation);
        samples.add(firstLocation);
        setOnGrid(bgGrid, gridPoint(topLeft, cellSize, firstLocation), 0);

        while (!activeList.isEmpty()) {
            int index = r.nextInt(activeList.size());
            Point2D search = activeList.get(index);

            Stream.generate(() -> Point2D.ringRand(search, minSeparation, 2 * minSeparation))
                .limit(POINTS_PER_ITER)
                .filter(p -> p.inSquare(center2, sideLength))
                .filter(p -> {
                    OptionalDouble minDist = gridNeighbors(bgGrid, gridPoint(topLeft, cellSize, p)) // get the neighbor cells' sample points' indexes
                        .stream()
                        .mapToInt(Integer::intValue)
                        .mapToObj(samples::get) // get the neighbor cells' sample points
                        .mapToDouble(p::dist) // find distance of this point to the neighbor cells' points
                        .min();
                        
                        if (minDist.isPresent()) { // there was at least one neighbor. this is the minimum distance between every neighbor
                            return minDist.getAsDouble() >= minSeparation;
                        } else { // there was not at least one neighbor so this point is far enough
                            return true;
                        }
                })
                .filter(p -> !isLocationUnspawnable(getHighestLoc(w, p)))
                .findAny()
                .ifPresentOrElse(
                    p -> {
                        int i = samples.size();

                        activeList.add(p);
                        samples.add(p);
                        setOnGrid(bgGrid, gridPoint(topLeft, cellSize, p), i);
                    }, 
                    () -> {
                        activeList.remove(index);
                    }
                );
        }

        Debug.printDebug(trans("xyz.baz9k.uhc.debug.spreadplayers.generated", samples.size()));

        if (samples.size() < numLocations) {
            throw translatableErr(IllegalStateException.class, "xyz.baz9k.uhc.err.world.missing_spread_locs", samples.size(), numLocations);
        }

        Map<Boolean, List<Location>> spawns =
        samples.stream()
            .map(p -> getHighestLoc(w, p))
            .collect(Collectors.partitioningBy(SpreadPlayersManager::isLocationAvoidSpawn));


        List<Location> spawnableLocations = spawns.get(false);
        List<Location> avoidLocations = spawns.get(true);
        

        if (spawnableLocations.size() < numLocations) {
            int numAvoidLocations = numLocations - spawnableLocations.size();
            Collections.shuffle(avoidLocations);
            spawnableLocations.addAll(avoidLocations.subList(0, numAvoidLocations));
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
        
        var groups = grouping.groups(plugin);
        var locs = locGenerator.apply(groups.size());
        
        var groupIter = groups.iterator();
        var locsIter = locs.iterator();
        
        if (def != null) {
            teleportGroup(Bukkit.getOnlinePlayers(), def);
        }

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
