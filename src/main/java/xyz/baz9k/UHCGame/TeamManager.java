package xyz.baz9k.UHCGame;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.HashMap;

public class TeamManager {
    private class Node {
        public int team;
        public PlayerState state;
        public Player player;
        public Node(int team, PlayerState state, Player player) {
            this.team = team;
            this.state = state;
            this.player = player;
        }
    }

    private int numTeams = 2;
    private final HashMap<UUID, Node> playerMap;

    public TeamManager() {
        playerMap = new HashMap<>();
    }

    @NotNull
    private Node getPlayerNode(@NotNull Player p) {
        return playerMap.get(p.getUniqueId());
    }

    /**
     * Add a player to the TeamManager
     * @param p
     */
    public void addPlayer(@NotNull Player p) {
        UUID uuid = p.getUniqueId();
        if (playerMap.containsKey(uuid)) {
            playerMap.get(uuid).player = p;
            return;
        };
        playerMap.put(uuid, new Node(0, PlayerState.SPECTATOR, p));
    }

    /**
     * Sets a player's state to {@link PlayerState#SPECTATOR}
     * @param p
     */
    public void setSpectator(@NotNull Player p) {
        Node n = getPlayerNode(p);
        n.team = 0;
        n.state = PlayerState.SPECTATOR;
    }

    /**
     * Sets a player's state to {@link PlayerState#COMBATANT_UNASSIGNED}
     * @param p
     */
    public void setUnassignedCombatant(@NotNull Player p) {
        Node n = getPlayerNode(p);
        n.team = 0;
        n.state = PlayerState.COMBATANT_UNASSIGNED;
    }

    /**
     * Assigns a player to a combatant team.
     * @param p
     * @param team
     */
    public void assignPlayerTeam(@NotNull Player p, int team) {
        if (team <= 0 || team > numTeams) {
            throw new IllegalArgumentException("Invalid team (Team must be positive and less than the team count.)");
        }

        Node n = getPlayerNode(p);
        n.state = PlayerState.COMBATANT_ALIVE;
        n.team = team;

    }

    /**
     * Removes a player from the TeamManager.
     * @param p
     */
    public void removePlayer(@NotNull Player p) { // unused
        playerMap.remove(p.getUniqueId());
    }

    @NotNull
    public PlayerState getPlayerState(@NotNull Player p) {
        return getPlayerNode(p).state;
    }

    public int getTeam(@NotNull Player p) {
        return getPlayerNode(p).team;
    }

    /**
     * Sets player to alive or dead.
     * @param p
     * @param aliveStatus
     */
    public void setCombatantAliveStatus(@NotNull Player p, boolean aliveStatus) {
        if (!isAssignedCombatant(p)) {
            throw new IllegalArgumentException("Player must be an assigned combatant.");
        }
        
        Node n = getPlayerNode(p);
        n.state = aliveStatus ? PlayerState.COMBATANT_ALIVE : PlayerState.COMBATANT_DEAD;
    }

    /* COUNTING */
    
    private int countPlayersMatching(Predicate<Node> predicate) {
        return (int) playerMap.values().stream()
                                       .filter(predicate)
                                       .count();
    }
    /**
     * @return the number of combatants in the team manager.
     */
    public int countCombatants() {
        return countPlayersMatching(n -> n.state != PlayerState.SPECTATOR);
    }

    /**
     * @return the number of living combatants in the team manager.
     */
    public int countLivingCombatants() {
        return countPlayersMatching(n -> n.state == PlayerState.COMBATANT_ALIVE);
    }

    /**
     * @return the number of living teams in the team manager.
     */
    public int countLivingTeams() {
        return (int) IntStream.range(1, numTeams + 1)
                              .filter(t -> !isTeamEliminated(t))
                              .count();
    }

    /**
     * @param team
     * @return the number of living combatants on the specified team.
     */
    public int countLivingCombatantsInTeam(int t) {
        return countPlayersMatching(n -> n.state == PlayerState.COMBATANT_ALIVE && n.team == t);
    }

    /* LIST OF PLAYERS */
    
    private List<Player> getAllPlayersMatching(Predicate<Node> predicate) {
        return playerMap.values().stream()
                                 .filter(predicate)
                                 .map(n -> n.player)
                                 .collect(Collectors.toList());
    }

    private boolean isOnline(Player p) {
        Player pl = Bukkit.getPlayer(p.getUniqueId());

        return pl != null;
    }

    private List<Player> filterOnline(@NotNull List<Player> pList) {
        return pList.stream()
                    .filter(p -> isOnline(p))
                    .collect(Collectors.toList());
    }

    /**
     * @return a {@link List} of all spectators
     */
    @NotNull
    public List<Player> getAllSpectators() {
        return getAllPlayersMatching(n -> n.state == PlayerState.SPECTATOR);
    }

    /**
     * @return a {@link List} of all combatants
     */
    @NotNull
    public List<Player> getAllCombatants() {
        return getAllPlayersMatching(n -> n.state != PlayerState.SPECTATOR);
    }

    /**
     * @param team
     * @return a {@link List} of all combatants on a specific team
     */
    @NotNull
    public List<Player> getAllCombatantsOnTeam(int team) {
        if (team <= 0 || team > numTeams) {
            throw new IllegalArgumentException("Invalid team (Team must be positive and less than the team count.)");
        }

        return getAllPlayersMatching(n -> n.state != PlayerState.SPECTATOR && n.state != PlayerState.COMBATANT_UNASSIGNED && n.team == team);
    }

    /**
     * @return a {@link List} of all online spectators
     */
    @NotNull
    public List<Player> getAllOnlineSpectators() {
        return filterOnline(getAllSpectators());
    }

    /**
     * @return a {@link List} of all online combatants
     */
    @NotNull
    public List<Player> getAllOnlineCombatants() {
        return filterOnline(getAllCombatants());
    }

    /**
     * @param t
     * @return a {@link List} of all online combatants on a specific team
     */
    @NotNull
    public List<Player> getAllOnlineCombatantsOnTeam(int t) {
        return filterOnline(getAllCombatantsOnTeam(t));
    }

    /**
     * @param t
     * @return if the specified team is eliminated
     */
    public boolean isTeamEliminated(int t) {
        return countLivingCombatantsInTeam(t) == 0;
    }

    /**
     * @param p
     * @return if the specified player is a {@link PlayerState#SPECTATOR}
     */
    public boolean isSpectator(@NotNull Player p) {
        return getPlayerNode(p).state == PlayerState.SPECTATOR;
    }

    /**
     * @param p
     * @return if the specified player is an assigned combatant.
     */
    public boolean isAssignedCombatant(@NotNull Player p) {
        PlayerState state = getPlayerNode(p).state;
        return state == PlayerState.COMBATANT_ALIVE || state == PlayerState.COMBATANT_DEAD;
    }

    public int getNumTeams() {
        return numTeams;
    }

    public void setNumTeams(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("Team count must be positive.");
        }

        numTeams = n;
    }

    /**
     * Resets all players.
     */
    public void resetAllPlayers() {
        for (Node v : playerMap.values()) {
            if (v.state != PlayerState.SPECTATOR) {
                v.state = PlayerState.COMBATANT_UNASSIGNED;
                v.team = 0;
            }
        }
    }
}
