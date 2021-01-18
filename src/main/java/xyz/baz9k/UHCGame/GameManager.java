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
import org.jetbrains.annotations.Nullable;

import net.md_5.bungee.api.chat.BaseComponent;
import xyz.baz9k.UHCGame.util.ColoredStringBuilder;
import xyz.baz9k.UHCGame.util.DelayedMessage;
import xyz.baz9k.UHCGame.util.TeamDisplay;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import com.onarandombox.MultiverseCore.api.WorldPurger;

import xyz.baz9k.UHCGame.util.Utils;

public class GameManager implements Listener {
    private UHCGame plugin;

    private boolean isUHCStarted = false;
    private final HashMap<UUID, String> previousDisplayNames;
    
    private TeamManager teamManager;
    private HUDManager hudManager;
    private BossbarManager bbManager;
    private ConfigManager cfgManager;
    private GameTick tick;

    private Instant startTime = null;
    
    private boolean worldsRegened = false;

    private HashMap<UUID, Integer> kills = new HashMap<>();

    private final double[] center = {0.5, 0.5};

    //TODO REMOVE THESE AND REPLACE WITH CONFIG MANAGER
    private static final int WB_INIT = 1200;
    private static final int WB2 = 25;
    private static final int WB3 = 3;
    private static final int WB_DM = 20;

    private int stage = -1;
    private Instant lastStageInstant = null;
    private static Duration[] stageDurations = {
        Duration.ofMinutes(60), // Still border
        Duration.ofMinutes(15), // Border 1
        Duration.ofMinutes(5), // Border stops
        Duration.ofMinutes(10), // Border 2
        Duration.ofMinutes(5), // Waiting until DM
        ChronoUnit.FOREVER.getDuration() // deathmatch
    };

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
        cfgManager = plugin.getConfigManager();
    }

    /**
     * Starts UHC.
     * Accessible through /uhc start or /uhc start force.
     * /uhc start: Checks that teams are assigned, worlds have regenerated, and game has not started
     * /uhc start force: Skips checks
     * @param skipChecks If true, all checks are ignored.
     */
    public void startUHC(boolean skipChecks) {
        try {
            _startUHC(skipChecks);
        } catch (Exception e) {
            Bukkit.broadcastMessage("[DEBUG] UHC cancelling start due to error");
            isUHCStarted = false;
            throw e;
        }
    }
    private void _startUHC(boolean skipChecks) {
        if (!skipChecks) {
            // check if game is OK to start
            if (isUHCStarted) {
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

        isUHCStarted = true;
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

        spreadPlayersByTeam(getCenter(), WB_INIT/2.0, WB_INIT/8.0);

        // begin uhc tick events
        tick = new GameTick(plugin);
        tick.runTaskTimer(plugin, 0L, 1L);
        
        setStage(0);
        teamManager.prepareAliveTeams();
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
        try {
            _endUHC(skipChecks);
        } catch (Exception e) {
            Bukkit.broadcastMessage("[DEBUG] UHC cancelling end due to error");
            isUHCStarted = true;
            throw e;
        }
    }

    private void _endUHC(boolean skipChecks) {
        if (!skipChecks) {
            // check if game is OK to end
            if (!isUHCStarted) {
                throw new IllegalStateException("UHC has not begun.");
            }
        }
        
        isUHCStarted = false;

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

        stage = -1;
        bbManager.disable();

    }

    public boolean isUHCStarted() {
        return isUHCStarted;
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
        if (!isUHCStarted) {
            throw new IllegalStateException("UHC has not started.");
        }

        return Duration.between(startTime, Instant.now());
    }

    /**
     * @return the current stage of the game. -1 if game has not started.
     */
    public int getStage() {
        if (!isUHCStarted) return -1;
        return Math.max(0, Math.min(stage, stageDurations.length));
    }

    public void incrementStage() {
        stage++;
        updateStage();
    }

    public void setStage(int stage) {
        this.stage = stage;
        this.stage = getStage();
        updateStage();
    }

    private void updateStage() {
        lastStageInstant = Instant.now();
        bbManager.updateBossbarStage();

        // TODO messages when next stage starts
        //worldborder
        for (World w : getUHCWorlds()) {
            switch (stage) {
                case 0: // start of game (still border)
                    w.getWorldBorder().setSize(WB_INIT);
                    break;
                case 1: //worldborder starts moving the first time (border 1)
                    w.getWorldBorder().setSize(WB2, stageDurations[1].toSeconds());
                    break;
                case 2: // border stop
                    w.getWorldBorder().setSize(WB2);
                case 3: //worldborder starts moving the second time (border 2)
                    w.getWorldBorder().setSize(WB3, stageDurations[3].toSeconds());
                    break;
                case 4: // waiting till DM
                    w.getWorldBorder().setSize(WB3);
                    break;
                case 5: // DEATHMATCH
                    w.getWorldBorder().setSize(WB_DM);
            }
        }

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
            for (Player p : Bukkit.getOnlinePlayers()) p.teleport(getCenter(255));
            for (Player p : teamManager.getAllCombatants()) {
                p.addPotionEffects(effs);
            }

            spreadPlayersByTeam(getCenter(), WB_DM / 2.0 - 1, WB_DM / 4.0);

        }

    }

    /**
     * @return the {@link Duration} that the current stage lasts.
     */
    @NotNull
    public Duration getStageDuration() {
        if (!isUHCStarted) {
            throw new IllegalStateException("UHC has not started.");
        }
        return stageDurations[getStage()];
    }

    /**
     * @return the {@link Duration} until the current stage ends.
     */
    @NotNull
    public Duration getRemainingStageDuration() {
        if (!isUHCStarted) {
            throw new IllegalStateException("UHC has not started.");
        }
        Duration stageDur = getStageDuration();
        if (stageDur.equals(ChronoUnit.FOREVER.getDuration())) return stageDur; // if deathmatch, just return ∞
        return Duration.between(Instant.now(), lastStageInstant.plus(stageDur));
    }

    /**
     * @return if deathmatch (last stage) has started.
     */
    public boolean isDeathmatch() {
        return stage == stageDurations.length - 1;
    }

    /**
     * @return if the stage has completed and needs to be incremented.
     */
    public boolean isStageComplete() {
        if (isDeathmatch()) return false;
        Instant end = lastStageInstant.plus(stageDurations[stage]);
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
    private Location getCenter(double y) {
        return new Location(getUHCWorld(Environment.NORMAL), center[0], y, center[1]);
    }

    private void setCenter(double x, double z) {
        center[0] = x;
        center[1] = z;
    }

    /**
     * Returns the number of kills that this combatant has dealt.
     * @param p
     * @return the number of kills
     */
    public int getKills(@NotNull Player p) {
        return kills.get(p.getUniqueId());
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
        if(isUHCStarted) {
            bbManager.addPlayer(p);
            hudManager.initializePlayerHUD(p);
            hudManager.addPlayerToTeams(p);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (isUHCStarted) {
            Player deadPlayer = event.getEntity();
            if (teamManager.getPlayerState(deadPlayer) == PlayerState.COMBATANT_ALIVE) {
                teamManager.setCombatantAliveStatus(deadPlayer, false);
                int t = teamManager.getTeam(deadPlayer);
                teamManager.updateTeamAliveStatus(t);

                if (teamManager.isTeamEliminated(t)) {
                    BaseComponent[] teamEliminatedMessage;
                    teamEliminatedMessage = ColoredStringBuilder.of(TeamDisplay.getName(t))
                                                                .append(" has been eliminated!")
                                                                .toComponents();
                    // this msg should be displayed after player death
                    (new DelayedMessage(teamEliminatedMessage)).runTaskLater(plugin, 1);
                }

                Location newSpawn = deadPlayer.getLocation();
                if (newSpawn.getY() < 0) {
                    deadPlayer.setBedSpawnLocation(getUHCWorld(Environment.NORMAL).getSpawnLocation(), true);
                } else {
                    deadPlayer.setBedSpawnLocation(newSpawn, true);
                }

                if (teamManager.countLivingTeams() == 1) {
                    winMessage();
                }
            }

            Player killer = deadPlayer.getKiller();
            if (killer != null) {
                if (!teamManager.isSpectator(killer)) {
                    int nKills = this.kills.get(killer.getUniqueId());
                    this.kills.put(killer.getUniqueId(), nKills + 1);
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
        if (isUHCStarted) {
            event.getPlayer().setGameMode(GameMode.SPECTATOR);
        }
    }

    @EventHandler
    public void onPlayerDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player p = (Player) event.getEntity();

        if (isUHCStarted) {
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
            // update hud if dmg taken
            hudManager.updateTeammateHUD(p);
        }
    }
}
