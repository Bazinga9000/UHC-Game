package xyz.baz9k.UHCGame;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class Listeners implements Listener {
    UHCGame plugin;

    public Listeners(UHCGame plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getGameManager().getTeamManager().addPlayer(event.getPlayer());
    }

    private class DelayedMessageSender extends BukkitRunnable {
        private final String message;
        public DelayedMessageSender(String message) {
            this.message = message;
        }

        @Override
        public void run() {
            Bukkit.broadcastMessage(message);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        GameManager gameManager = plugin.getGameManager();
        if (gameManager.isUHCStarted()) {
            TeamManager teamManager = gameManager.getTeamManager();
            Player deadPlayer = event.getEntity();
            if (teamManager.getPlayerState(deadPlayer) == PlayerState.COMBATANT_ALIVE) {
                teamManager.setCombatantAliveStatus(deadPlayer, false);
                int team = teamManager.getTeam(deadPlayer);
                if (teamManager.isTeamEliminated(team)) {
                    String teamEliminatedMessage = "Team " + team + " has been eliminated!"; // TODO FANCY
                    (new DelayedMessageSender(teamEliminatedMessage)).runTaskLater(plugin, 1);
                }

                deadPlayer.setBedSpawnLocation(deadPlayer.getLocation(), true);

                if (teamManager.countLivingTeams() == 1) {
                    String winnerMessage = "Only one team is left, this is when the game would end."; //TODO FANCY
                    (new DelayedMessageSender(winnerMessage)).runTaskLater(plugin, 1);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        GameManager gameManager = plugin.getGameManager();
        if (gameManager.isUHCStarted()) {
            event.getPlayer().setGameMode(GameMode.SPECTATOR);
        }
    }

}
