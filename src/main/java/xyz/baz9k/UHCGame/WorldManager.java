package xyz.baz9k.UHCGame;

import static xyz.baz9k.UHCGame.util.Utils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;

import xyz.baz9k.UHCGame.util.Point2D;

public class WorldManager {
    private UHCGame plugin;
    private boolean worldsRegened = false;
    private List<String> worldNames = new ArrayList<>();
    private final Point2D center = new Point2D(0.5, 0.5);

    public WorldManager(UHCGame plugin) {
        this.plugin = plugin;

        // create MV worlds if missing
        registerWorld("game", Environment.NORMAL);
        registerWorld("game_nether", Environment.NETHER);
    }

    public boolean worldsRegened() { return worldsRegened; }
    public void worldsRegenedOff() { worldsRegened = false; }


    private void registerWorld(String world, Environment env) {
        boolean succ = true;
        if (Bukkit.getWorld(world) == null) {
            var wm = plugin.getMVWorldManager();
            succ = wm.addWorld(world, env, String.valueOf(new Random().nextLong()), WorldType.NORMAL, true, null);
        }

        if (succ) worldNames.add(world);
    }
    
    /**
     * @return an Array of {@link World} which the UHC uses.
     */
    public World[] getUHCWorlds() {
        return worldNames.stream()
            .map(Bukkit::getWorld)
            .toArray(World[]::new);
    }

    /**
     * Get a specific game world.
     * @param i Index of world in registration
     * @return the world
     */
    public World getUHCWorld(int i) {
        return Bukkit.getWorld(worldNames.get(i));
    }

    public World getLobbyWorld() {
        World lobby = Bukkit.getWorld("lobby");
        if (lobby != null) return lobby;
        return Bukkit.getWorld("world");
    }
    
    public void initializeWorlds() {
        for (World w : getUHCWorlds()) {
            // set time to 0 and delete rain
            w.setTime(0);
            w.setClearWeatherDuration(Integer.MAX_VALUE); // there is NO rain. Ever again. [ :( ]
            
            w.getWorldBorder().setCenter(center.x(), center.z());
            w.getWorldBorder().setWarningDistance(25);
            w.getWorldBorder().setDamageBuffer(0);
            w.getWorldBorder().setDamageAmount(1);

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
            for (int y = 3; y <= w.getMaxHeight() - 1; y++) {
                w.getBlockAt(0, y, 0).setType(Material.BARRIER);
            }
        }
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


    /**
     * Sends all players back to the lobby world.
     * <p>
     * Accessible through /uhc escape
     */
    public void escapeAll() {
        World lobby = getLobbyWorld();
        Location spawn = lobby.getSpawnLocation();
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.teleport(spawn);
        };
    }

    /**
     * Kills all monsters in a world
     * @param w
     */
    public void purgeWorld(World w) {
        var wm = plugin.getMVWorldManager();
        var purger = wm.getTheWorldPurger();
        var mvWorld = wm.getMVWorld(w);

        purger.purgeWorld(mvWorld, Arrays.asList("MONSTERS"), false, false); // multiverse is stupid (purges all monsters, hopefully)
    }
    
    public Location getCenter() {
        return getCenterAtY(0);
    }
    
    public Location getCenterAtY(double y) {
        return center.loc(getUHCWorld(0), y);
    }

    public Location getHighCenter() {
        return getHighestLoc(getUHCWorld(0), center);
    }
}
