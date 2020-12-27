package xyz.baz9k.UHCGame;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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

import xyz.baz9k.UHCGame.util.DelayedMessageSender;
import xyz.baz9k.UHCGame.util.TeamColors;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

import com.onarandombox.MultiverseCore.api.MVWorldManager;
import com.onarandombox.MultiverseCore.api.MultiverseWorld;
import com.onarandombox.MultiverseCore.api.WorldPurger;

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
    private Duration timeElapsed = null;
    
    private MultiverseWorld[] mvUHCWorlds;
    private boolean worldsRegened = false;

    private HashMap<UUID, Integer> kills;

    //TODO REMOVE THESE AND REPLACE WITH CONFIG MANAGER
    private final int WORLDBORDER = 1200;
    private final int WB2 = 25;
    private final int WB3 = 3;

    private int stage = -1;
    private Instant lastStageInstant = null;
    private Duration[] stageDurations = {
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

        // get MV worlds or create if missing
        mvUHCWorlds = new MultiverseWorld[2]; // overworld, nether
        mvUHCWorlds[0] = getOrCreateMVWorld("game", Environment.NORMAL);
        mvUHCWorlds[1] = getOrCreateMVWorld("game_nether", Environment.NETHER);
    }

    public void loadManagerRefs() {
        teamManager = plugin.getTeamManager();
        hudManager = plugin.getHUDManager();
        bbManager = plugin.getBossbarManager();
        cfgManager = plugin.getConfigManager();
    }

    public void startUHC() {
        // check if game is OK to start
        if (isUHCStarted) {
            throw new IllegalStateException("UHC has already started.");
        }
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (teamManager.getPlayerState(p) == PlayerState.COMBATANT_UNASSIGNED) {
                throw new IllegalStateException("Teams have not been assigned.");
            }
        }
        if (!worldsRegened) {
            throw new IllegalStateException("UHC worlds have not been regenerated. Run /reseed to regenerate.");
        }

        isUHCStarted = true;
        worldsRegened = false;

        startTime = lastStageInstant = Instant.now();
        updateElapsedTime();
        this.kills = new HashMap<>();
        
        for (Player p : plugin.getServer().getOnlinePlayers()) {
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
            p.setDisplayName(TeamColors.getTeamPrefixWithSpace(teamManager.getTeam(p)) + p.getName());

            // activate hud things for all
            hudManager.initializePlayerHUD(p);
        }
        
        WorldPurger purger = plugin.getMVWorldManager().getTheWorldPurger();
        for (MultiverseWorld mvWorld : mvUHCWorlds) {
            World w = mvWorld.getCBWorld();
            // set time to 0 and delete rain
            w.setTime(0);
            w.setClearWeatherDuration(Integer.MAX_VALUE); // there is NO rain. Ever again. [ :( ]
            
            w.getWorldBorder().setCenter(0.5, 0.5);
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
            for (int y = 3; y < 255; y++) {
                w.getBlockAt(0, y, 0).setType(Material.BARRIER);
            }
        }
        // TODO teleport, spreadplayers

        // begin uhc tick events
        tick = new GameTick(plugin);
        tick.runTaskTimer(plugin, 0L, 1L);
        
        setStage(0);
        bbManager.enable();
    }

    public void endUHC() {
        // check if game is OK to end
        if (!isUHCStarted) {
            throw new IllegalStateException("UHC has not begun.");
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

    public int getStage() {
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

        //worldborder
        for (MultiverseWorld mvWorld : mvUHCWorlds) {
            World w = mvWorld.getCBWorld();
            switch (stage) {
                case 0: // start of game (still border)
                    w.getWorldBorder().setSize(WORLDBORDER);
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
                //TODO DEATHMATCH
            }
        }
    }

    public Instant getLStageInstant() {
        return lastStageInstant;
    }

    @NotNull
    public Duration getCurrentStageDuration() {
        return stageDurations[getStage()];
    }

    public boolean isDeathmatch() {
        return stage == stageDurations.length - 1;
    }

    public boolean isStageComplete() {
        if (isDeathmatch()) return false;
        Instant end = lastStageInstant.plus(stageDurations[stage]);
        return !end.isAfter(Instant.now());
    }

    @NotNull
    public MultiverseWorld[] getMVUHCWorlds() {
        return mvUHCWorlds;
    }

    @Nullable
    public World getUHCWorld(@NotNull Environment env) {
        MultiverseWorld mvWorld;
        switch (env) {
            case NORMAL:
                mvWorld = mvUHCWorlds[0];
                break;
            case NETHER:
                mvWorld = mvUHCWorlds[1];
                break;
            case THE_END:
            default:
                mvWorld = null;
                break;
        }
        return mvWorld == null ? null : mvWorld.getCBWorld();
    }

    public boolean isUHCStarted() {
        return isUHCStarted;
    }

    public void updateElapsedTime() {
        if (!isUHCStarted) {
            throw new IllegalStateException("UHC has not started.");
        }
        timeElapsed = Duration.between(startTime, Instant.now());
    }

    public Duration getElapsedTime() {
        return timeElapsed;
    }

    public int getKills(@NotNull Player p) {
        return kills.get(p.getUniqueId());
    }

    @NotNull
    public MultiverseWorld getOrCreateMVWorld(@NotNull String world, @NotNull Environment env) {
        MVWorldManager wm = plugin.getMVWorldManager();
        MultiverseWorld w = wm.getMVWorld(world);
        if (w != null) return w;
        
        Random temp = new Random();
        wm.addWorld(world, env, String.valueOf(temp.nextLong()), WorldType.NORMAL, true, null);
        return wm.getMVWorld(world);
    }

    public boolean haveWorldsRegened() {
        return worldsRegened;
    }

    public void setWorldsRegenedStatus(boolean status) {
        worldsRegened = status;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        teamManager.addPlayer(p);
        if(isUHCStarted) {
            bbManager.enable(p);
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
                int team = teamManager.getTeam(deadPlayer);
                if (teamManager.isTeamEliminated(team)) {
                    String teamEliminatedMessage = "Team " + team + " has been eliminated!"; // TODO FANCY
                    (new DelayedMessageSender(teamEliminatedMessage)).runTaskLater(plugin, 1);
                }

                if (deadPlayer.getLocation().getY() < 0) {
                    deadPlayer.setBedSpawnLocation(getUHCWorld(Environment.NORMAL).getSpawnLocation(), true);
                } else {
                    deadPlayer.setBedSpawnLocation(deadPlayer.getLocation(), true);
                }

                if (teamManager.countLivingTeams() == 1) {
                    String winnerMessage = "Only one team is left, this is when the game would end."; // TODO FANCY
                    (new DelayedMessageSender(winnerMessage)).runTaskLater(plugin, 1);
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

            for (Player p : plugin.getServer().getOnlinePlayers()) {
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
