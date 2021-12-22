package xyz.baz9k.UHCGame;

import static xyz.baz9k.UHCGame.util.Utils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import xyz.baz9k.UHCGame.util.Debug;
import xyz.baz9k.UHCGame.util.Point2D;

public class WorldManager {
    private UHCGamePlugin plugin;
    private boolean worldsRegened = false;
    private List<String> worldNames = new ArrayList<>();
    private final Point2D center = new Point2D(0.5, 0.5);

    public WorldManager(UHCGamePlugin plugin) {
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
    public World[] getGameWorlds() {
        return worldNames.stream()
            .map(Bukkit::getWorld)
            .toArray(World[]::new);
    }

    /**
     * Get a specific game world.
     * @param i Index of world in registration
     * @return the world
     */
    public World getGameWorld(int i) {
        return Bukkit.getWorld(worldNames.get(i));
    }

    public World getLobbyWorld() {
        World lobby = Bukkit.getWorld("lobby");
        return lobby != null ? lobby : Bukkit.getWorld("world");
    }

    public Location gameSpawn() {
        return getGameWorld(0).getSpawnLocation();
    }
    public Location lobbySpawn() {
        return getLobbyWorld().getSpawnLocation();
    }
    
    public void initWorlds() {
        for (World w : getGameWorlds()) {
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

            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    w.getBlockAt(x, w.getMinHeight(), z).setType(Material.BEDROCK);
                }
            }
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    w.getBlockAt(x, w.getMinHeight(), z).setType(Material.NETHERITE_BLOCK);
                    w.getBlockAt(x, w.getMinHeight() + 1, z).setType(Material.BEDROCK);
                }
            }
            w.getBlockAt(0, w.getMinHeight() + 1, 0).setType(Material.BEACON);
            w.getBlockAt(0, w.getMinHeight() + 2, 0).setType(Material.BEDROCK);

            for (int y = 3; y < w.getMaxHeight(); y++) {
                w.getBlockAt(0, y, 0).setType(Material.BARRIER);
            }
        }
    }

    /**
     * Reseed worlds then mark worlds as reseeded.
     * <p>
     * Accessible through /uhc reseed
     */
    private void reseedWorld(World w, long seed) {
        var wc = new WorldCreator(w.getName())
            .environment(w.getEnvironment())
            .seed(seed);
        
        Bukkit.unloadWorld(w, false);
        plugin.getMVWorldManager().deleteWorld(wc.name(), false, true);
        Bukkit.createWorld(wc);
    }

    /**
     * Reseed worlds then mark worlds as reseeded.
     * <p>
     * Accessible through /uhc reseed
     */
    public void reseedWorlds() {
        Random r = new Random();
        long l;
        do {
            l = r.nextLong();
            Debug.printDebug(String.format("Checking seed %s", l));
            reseedWorld(getGameWorld(0), l);
        } while(!isGoodWorld(getGameWorld(0)));
        
        Debug.printDebug(String.format("Using seed %s", l));
        reseedWorlds(l, true);
    }

    /**
     * Reseed worlds then mark worlds as reseeded.
     * <p>
     * Accessible through /uhc reseed <seed>
     * @param seed
     */
    public void reseedWorlds(long seed, boolean ignoreOverworld) {
        World[] worlds = getGameWorlds();

        int init = ignoreOverworld ? 1 : 0;
        for (int i = init; i < worlds.length; i++) {
            reseedWorld(worlds[i], seed);
        }
        worldsRegened = true;
    }

    private static final List<Biome> rejectedBiomes = List.of(
        Biome.OCEAN,
        Biome.COLD_OCEAN,
        Biome.DEEP_COLD_OCEAN,
        Biome.DEEP_FROZEN_OCEAN,
        Biome.DEEP_LUKEWARM_OCEAN,
        Biome.DEEP_OCEAN,
        Biome.FROZEN_OCEAN,
        Biome.LUKEWARM_OCEAN,
        Biome.WARM_OCEAN
    );

    public boolean isGoodWorld(@NotNull World w) {
        var loc = getHighestLoc(w, 1, 1);
        Debug.printDebug(String.format("Checking %s's biome", w.getSeed()));
        Biome b = w.getBiome(1, (int) loc.getY() - 1, 1);
        Debug.printDebug(String.format("Checked %s's biome, it's %s", w.getSeed(), b.toString()));
        
        return !rejectedBiomes.contains(b);
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
        return center.loc(getGameWorld(0), y);
    }

    public Location getHighCenter() {
        return getHighestLoc(getGameWorld(0), center);
    }

    /**
     * Return player to lobby
     */
    public void escapePlayer(Player p) {
        plugin.getMVWorldManager().loadWorld("lobby");
        Location ls = lobbySpawn();
        p.setBedSpawnLocation(ls);
        p.teleport(ls);
        p.setGameMode(GameMode.ADVENTURE);
    }

    public boolean inGame(Player p) {
        return Arrays.asList(getGameWorlds()).contains(p.getWorld());
    }
}
