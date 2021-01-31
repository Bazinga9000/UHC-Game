package xyz.baz9k.UHCGame;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.World.Environment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import net.md_5.bungee.api.chat.BaseComponent;
import xyz.baz9k.UHCGame.util.ColoredText;
import xyz.baz9k.UHCGame.util.DelayedMessage;
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

import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import com.onarandombox.MultiverseCore.api.WorldPurger;

import xyz.baz9k.UHCGame.util.Utils;

public class GameManager implements Listener {
    private UHCGame plugin;

    private final HashMap<UUID, String> previousDisplayNames;
    
    private TeamManager teamManager;
    private HUDManager hudManager;
    private BossbarManager bbManager;
    private GameTick tick;

    private Instant startTime = null;
    
    private boolean worldsRegened = false;

    private HashMap<UUID, Integer> kills = new HashMap<>();

    private final double[] center = {0.5, 0.5};

    private GameStage stage = GameStage.NOT_IN_GAME;
    private Instant lastStageInstant = null;
    //private static Duration[] stageDurations = {
    //    Duration.ofMinutes(60), // Still border
    //    Duration.ofMinutes(15), // Border 1
    //    Duration.ofMinutes(5), // Border stops
    //    Duration.ofMinutes(10), // Border 2
    //    Duration.ofMinutes(5), // Waiting until DM
    //    ChronoUnit.FOREVER.getDuration() // deathmatch
    //};

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
    }

    /**
     * Starts UHC.
     * Accessible through /uhc start or /uhc start force.
     * /uhc start: Checks that teams are assigned, worlds have regenerated, and game has not started
     * /uhc start force: Skips checks
     * @param skipChecks If true, all checks are ignored.
     */
    public void startUHC(boolean skipChecks) {
        if (!skipChecks) {
            // check if game is OK to start
            if (hasUHCStarted()) {
                throw new IllegalStateException("UHC has already started.");
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (teamManager.getPlayerState(p) == PlayerState.COMBATANT_UNASSIGNED) {
                    throw new IllegalStateException("Teams have not been assigned.");
                }
            }
            if (!worldsRegened) {
                throw new IllegalStateException("UHC worlds have not been regenerated. Run /reseed to regenerate.");
            }
        }

        try {
            _startUHC();
        } catch (Exception e) {
            Bukkit.broadcastMessage("[DEBUG] UHC cancelling start due to error");
            throw e;
        }
    }
    private void _startUHC() {
        worldsRegened = false;

        startTime = lastStageInstant = Instant.now();
        kills.clear();
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            // archive previous display name
            previousDisplayNames.put(p.getUniqueId(), p.getDisplayName());
            
            // fully heal, adequately saturate, remove XP
            p.setHealth(20.0f);
            p.setFoodLevel(20);
            p.setSaturation(5.0f);
            p.setExp(0.0f);
            
            // clear all potion effects
            for (PotionEffect effect : p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }

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
        
        WorldPurger purger = plugin.getMVWorldManager().getTheWorldPurger();
        for (MultiverseWorld mvWorld : getMVUHCWorlds()) {
            World w = mvWorld.getCBWorld();
            // set time to 0 and delete rain
            w.setTime(0);
            w.setClearWeatherDuration(Integer.MAX_VALUE); // there is NO rain. Ever again. [ :( ]
            
            w.getWorldBorder().setCenter(center[0], center[1]);
            w.getWorldBorder().setWarningDistance(25);

            Gamerules.set(w);
            purger.purgeWorld(mvWorld, Arrays.asList("MONSTERS"), false, false); // multiverse is stupid (purges all monsters, hopefully)

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

        spreadPlayersByTeam(getCenter(), GameStage.WB_STILL.getWBSize() / 2.0, GameStage.WB_STILL.getWBSize() / 8.0);

        // begin uhc tick events
        tick = new GameTick(plugin);
        tick.runTaskTimer(plugin, 0L, 1L);
        
        setStage(GameStage.fromIndex(0));
        bbManager.enable();
    }

    /**
     * Ends UHC.
     * Accessible through /uhc end or /uhc end force.
     * /uhc end: Checks that the game has started
     * /uhc end force: Forcibly starts game
     * @param skipChecks If true, started game checks are ignored.
     */
    public void endUHC(boolean skipChecks) {
        if (!skipChecks) {
            // check if game is OK to end
            if (!hasUHCStarted()) {
                throw new IllegalStateException("UHC has not begun.");
            }
        }
        
        try {
            _endUHC();
        } catch (Exception e) {
            Bukkit.broadcastMessage("[DEBUG] UHC cancelling end due to error");
            throw e;
        }
    }

    private void _endUHC() {

        // update display names
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

        setStage(GameStage.NOT_IN_GAME);
        bbManager.disable();

    }

    public boolean hasUHCStarted() {
        return stage != GameStage.NOT_IN_GAME;
    }

    public boolean haveWorldsRegened() {
        return worldsRegened;
    }

    public void setWorldsRegenedStatus(boolean status) {
        worldsRegened = status;
    }

    /**
     * @return the {@link Duration} since the game has started.
     */
    @NotNull
    public Duration getElapsedTime() {
        if (!hasUHCStarted()) {
            throw new IllegalStateException("UHC has not started.");
        }

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
        lastStageInstant = Instant.now();
        bbManager.updateBossbarStage();

        stage.sendMessage();
        stage.applyWBSize(getUHCWorlds());

        // deathmatch
        if (isDeathmatch()) {
            World w = getUHCWorld(Environment.NORMAL);
            for (int x = -10; x < 11; x++) {
                for (int z = -10; z < 11; z++) {
                    w.getBlockAt(x, 254, z).setType(Material.BARRIER);
                }
            }
            List<PotionEffect> effs = Arrays.asList(
                PotionEffectType.DAMAGE_RESISTANCE.createEffect(10 * 20 /* ticks */, /* lvl */ 10),
                PotionEffectType.SLOW.createEffect(10 * 20 /* ticks */, /* lvl */ 10),
                PotionEffectType.JUMP.createEffect(10 * 20 /* ticks */, /* lvl */ 128),
                PotionEffectType.BLINDNESS.createEffect(10 * 20 /* ticks */, /* lvl */ 10)
            );
            for (Player p : plugin.getServer().getOnlinePlayers()) {
                p.teleport(getCenterAtY(255));
            }
            for (Player p : teamManager.getAllCombatants()) {
                p.addPotionEffects(effs);
            }

            spreadPlayersByTeam(getCenter(), GameStage.DEATHMATCH.getWBSize() / 2.0 - 1, GameStage.DEATHMATCH.getWBSize() / 4.0);

        }

    }

    /**
     * @return the {@link Duration} that the current stage lasts.
     */
    @NotNull
    public Duration getStageDuration() {
        if (!hasUHCStarted()) {
            throw new IllegalStateException("UHC has not started.");
        }
        return stage.getDuration();
    }

    /**
     * @return the {@link Duration} until the current stage ends.
     */
    @NotNull
    public Duration getRemainingStageDuration() {
        if (!hasUHCStarted()) {
            throw new IllegalStateException("UHC has not started.");
        }
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
        MVWorldManager wm = plugin.getMVWorldManager();
        MultiverseWorld w = wm.getMVWorld(world);
        if (w != null) return;
        
        Random temp = new Random();
        wm.addWorld(world, env, String.valueOf(temp.nextLong()), WorldType.NORMAL, true, null);
    }

    /**
     * @return an Array of {@link MultiverseWorld} which the UHC uses.
     */
    @NotNull
    public MultiverseWorld[] getMVUHCWorlds() {
        MVWorldManager wm = plugin.getMVWorldManager();
        return new MultiverseWorld[]{
            wm.getMVWorld("game"),
            wm.getMVWorld("game_nether")
        };
    }

    /**
     * @return an Array of {@link World} which the UHC uses.
     */
    @NotNull
    public World[] getUHCWorlds() {
        MultiverseWorld[] mvWorlds = getMVUHCWorlds();
        World[] worlds = new World[mvWorlds.length];
        for (int i = 0; i < worlds.length; i++) {
            worlds[i] = mvWorlds[i].getCBWorld();
        }

        return worlds;
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

    private Location getCenter() {
        return new Location(getUHCWorld(Environment.NORMAL), center[0], 0, center[1]);
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
     * @return the number of kills in an OptionalInt. If the player is not registered in the kills map (i.e. they're a spec), return empty OptionalInt.
     */
    public OptionalInt getKills(@NotNull Player p) {
        Integer k = kills.get(p.getUniqueId());
        if (k == null) return OptionalInt.empty();
        return OptionalInt.of(k);
    }
    
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
                                                      .map(l -> Utils.euclideanDistance(x, z, l.getX(), l.getZ()))
                                                      .min(Double::compareTo)
                                                      .orElseThrow();
                    if (minimumDistance < minimumSeparation) {
                        continue;
                    }
                }
                newLocation = new Location(w, x, w.getHighestBlockYAt((int) x, (int) z), z);

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

    /**
     * Spreads players with respect to teams
     * @param center
     * @param maximumRange
     * @param minimumSeparation
     */
    public void spreadPlayersByTeam(Location center, double maximumRange, double minimumSeparation) {
        List<Collection<Player>> groups = new ArrayList<>();
        for (int i = 1; i <= teamManager.getNumTeams(); i++) {
            groups.add(teamManager.getAllCombatantsOnTeam(i));
        }

        spreadPlayers(groups, center, maximumRange, minimumSeparation);
    }

    /**
     * Spreads players
     * @param players
     * @param center
     * @param maximumRange
     * @param minimumSeparation
     */
    public void spreadPlayers(Collection<Player> players, Location center, double maximumRange, double minimumSeparation) {
        List<Location> locations = getRandomLocations(center, players.size(), maximumRange, minimumSeparation);
        int index = 0;
        for (Player p : players) {
            p.teleport(locations.get(index));
            index++;
        }

    }

    /**
     * Spreads players with respect to specified groups
     * @param groups
     * @param center
     * @param maximumRange
     * @param minimumSeparation
     */
    public void spreadPlayers(List<Collection<Player>> groups, Location center, double maximumRange, double minimumSeparation) {
        List<Location> locations = getRandomLocations(center, groups.size(), maximumRange, minimumSeparation);
        for (int i = 0; i < groups.size(); i++) {
            for (Player p : groups.get(i)) {
                p.teleport(locations.get(i));
            }
        }

    }

    private void winMessage() {
        if (teamManager.countLivingTeams() > 1) return;
        int winner = teamManager.getAliveTeams()[0];
        String winnerMessage = "Only one team is left, this is when the game would end. Winner: " + winner; // TODO FANCY
        // this msg should be displayed after player death
        (new DelayedMessage(winnerMessage)).runTaskLater(plugin, 1);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        teamManager.addPlayer(p);
        if(hasUHCStarted()) {
            bbManager.addPlayer(p);
            hudManager.initializePlayerHUD(p);
            hudManager.addPlayerToTeams(p);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (hasUHCStarted()) {
            Player deadPlayer = event.getEntity();
            if (teamManager.getPlayerState(deadPlayer) == PlayerState.COMBATANT_ALIVE) {
                teamManager.setCombatantAliveStatus(deadPlayer, false);

                // check team death
                int t = teamManager.getTeam(deadPlayer);
                if (teamManager.isTeamEliminated(t)) {
                    BaseComponent[] teamEliminatedMessage;
                    teamEliminatedMessage = ColoredText.of(TeamDisplay.getName(t))
                                                       .append(" has been eliminated!")
                                                       .toComponents();
                    // this msg should be displayed after player death
                    (new DelayedMessage(teamEliminatedMessage)).runTaskLater(plugin, 1);
                }

                // set bed spawn
                Location newSpawn = deadPlayer.getLocation();
                if (newSpawn.getY() < 0) {
                    deadPlayer.setBedSpawnLocation(getUHCWorld(Environment.NORMAL).getSpawnLocation(), true);
                } else {
                    deadPlayer.setBedSpawnLocation(newSpawn, true);
                }

                // check win condition
                if (teamManager.countLivingTeams() == 1) {
                    winMessage();
                }
            }

            Player killer = deadPlayer.getKiller();
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
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (hasUHCStarted()) {
            event.getPlayer().setGameMode(GameMode.SPECTATOR);
        }
    }

    @EventHandler
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player p = (Player) event.getEntity();

        if (hasUHCStarted()) {
            // update hud if dmg taken
            hudManager.updateTeammateHUD(p);
            
            // cancel friendlyFire
            if (event instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent entDmgEvent = (EntityDamageByEntityEvent) event;
                Entity damager = entDmgEvent.getDamager(); 
                if (damager instanceof Player) {
                    Player playerDamager = (Player) damager;
                    if (teamManager.getTeam(p) == teamManager.getTeam(playerDamager)) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }
}
