package xyz.baz9k.UHCGame;

import xyz.baz9k.UHCGame.util.*;
import static xyz.baz9k.UHCGame.util.Utils.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.IntFunction;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class SpreadPlayersManager {
    private UHCGame plugin;

    public SpreadPlayersManager(UHCGame plugin) {
        this.plugin = plugin;
    }

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
            throw translatableErr(IllegalStateException.class, "xyz.baz9k.uhc.err.world.missing_spread_locs", totalSize);
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
            for (int i : plugin.getTeamManager().getAliveTeams()) {
                groups.add(plugin.getTeamManager().getAllCombatantsOnTeam(i));
            }
            
            List<Location> loc = locGenerator.apply(groups.size());
            teleportPlayersToLocations(groups, loc);
        } else {
            Collection<Player> players = plugin.getTeamManager().getAllCombatants();

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
    public void random(boolean respectTeams, Location center, double maximumRange, double minSeparation) {
        spreadPlayers(respectTeams, n -> getRandomLocations(center, n, maximumRange, minSeparation));
    }

    /**
     * Spreads players based on the roots of unity
     * @param respectTeams
     * @param center
     * @param distance
     */
    public void rootsOfUnity(boolean respectTeams, Location center, double distance) {
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
}
