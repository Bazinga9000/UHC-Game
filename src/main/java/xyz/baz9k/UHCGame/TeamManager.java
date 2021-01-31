package xyz.baz9k.UHCGame;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.IntStream;
import java.util.HashMap;
import java.util.List;

public class TeamManager {
    private class Node {
        public int team;
        public PlayerState state;
        public Player player;
        public Node(PlayerState state, int team, Player player) {
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

    /**
     * Gets the stored node of the player in the player map.
     * <p>
     * If the player is not found in the map, a new Node of state {@link PlayerState#COMBATANT_UNASSIGNED} is created.
     * @param p
     * @return the node
     */
    @NotNull
    private Node getNode(@NotNull Player p) {
        return playerMap.compute(p.getUniqueId(), (k, v) -> {
            if (v == null) {
                return new Node(PlayerState.COMBATANT_UNASSIGNED, 0, p);
            } else {
                v.player = p;
                return v;
            }
        });
    }

    private void setNode(@NotNull Player p, @NotNull PlayerState s, int team) {
        Node n = getNode(p);
        n.state = s;
        n.team = team;
    }

    /**
     * Sets a player's state to {@link PlayerState#SPECTATOR}
     * @param p
     */
    public void setSpectator(@NotNull Player p) {
        setNode(p, PlayerState.SPECTATOR, 0);
    }

    /**
     * Sets a player's state to {@link PlayerState#COMBATANT_UNASSIGNED}
     * @param p
     */
    public void setUnassignedCombatant(@NotNull Player p) {
        setNode(p, PlayerState.COMBATANT_UNASSIGNED, 0);
    }

    /**
     * Assigns a player to a combatant team.
     * @param p
     * @param team
     */
    public void assignPlayerToTeam(@NotNull Player p, int t) {
        if (t <= 0 || t > numTeams) {
            throw new IllegalArgumentException("Invalid team (Team must be positive and less than the team count.)");
        }

        setNode(p, PlayerState.COMBATANT_ALIVE, t);

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

    /**
     * Assigns all registered players to a team.
     */
    public void assignTeams() {
        List<Player> combatants = new ArrayList<>(getOnlineCombatants());
        Collections.shuffle(combatants);

        int i = 1;
        for (Player p : combatants) {
            assignPlayerToTeam(p, i);
            i = i % numTeams + 1;
        }
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
        return getNode(p).state;
    }

    public int getTeam(@NotNull Player p) {
        return getNode(p).team;
    }

    private static boolean isOnline(Player p) {
        Player pl = Bukkit.getPlayer(p.getUniqueId());

        return pl != null;
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
        
        Node n = getNode(p);
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
     * @param team
     * @return the number of living combatants on the specified team.
     */
    public int countLivingCombatantsInTeam(int t) {
        return countPlayersMatching(n -> n.state == PlayerState.COMBATANT_ALIVE && n.team == t);
    }

    /* LIST OF PLAYERS */
    
    private Set<Player> getAllPlayersMatching(Predicate<Node> predicate) {
        return playerMap.values().stream()
                                 .filter(predicate)
                                 .map(n -> n.player)
                                 .collect(Collectors.toSet());
    }

    private Set<Player> filterOnline(@NotNull Set<Player> pSet) {
        return pSet.stream()
                    .filter(TeamManager::isOnline)
                    .collect(Collectors.toSet());
    }

    /**
     * @return a {@link Set} of all spectators
     */
    @NotNull
    public Set<Player> getAllSpectators() {
        return getAllPlayersMatching(n -> n.state == PlayerState.SPECTATOR);
    }

    /**
     * @return a {@link Set} of all combatants
     */
    @NotNull
    public Set<Player> getAllCombatants() {
        return getAllPlayersMatching(n -> n.state != PlayerState.SPECTATOR);
    }

    /**
     * @param team
     * @return a {@link Set} of all combatants on a specific team
     */
    @NotNull
    public Set<Player> getAllCombatantsOnTeam(int team) {
        if (team <= 0 || team > numTeams) {
            throw new IllegalArgumentException("Invalid team (Team must be positive and less than the team count.)");
        }

        return getAllPlayersMatching(n -> n.state != PlayerState.SPECTATOR && n.state != PlayerState.COMBATANT_UNASSIGNED && n.team == team);
    }

    /**
     * @return a {@link Set} of all online spectators
     */
    @NotNull
    public Set<Player> getOnlineSpectators() {
        return filterOnline(getAllSpectators());
    }

    /**
     * @return a {@link Set} of all online combatants
     */
    @NotNull
    public Set<Player> getOnlineCombatants() {
        return filterOnline(getAllCombatants());
    }

    /**
     * @param t
     * @return a {@link Set} of all online combatants on a specific team
     */
    @NotNull
    public Set<Player> getOnlineCombatantsOnTeam(int t) {
        return filterOnline(getAllCombatantsOnTeam(t));
    }

    private IntStream aliveTeamsStream() {
        return playerMap.values().stream()
                                 .filter(n -> n.state == PlayerState.COMBATANT_ALIVE)
                                 .mapToInt(n -> n.team)
                                 .distinct();
    }

    /**
     * @return the number of living teams in the team manager.
     */
    public int countLivingTeams() {
        return (int) aliveTeamsStream().count();
    }
    
    /**
     * @return an array of the alive teams by int
     */
    public int[] getAliveTeams() {
        return aliveTeamsStream().toArray();
    }

    /**
     * @param t
     * @return if the specified team is eliminated
     */
    public boolean isTeamEliminated(int t) {
        return playerMap.values().stream()
                                 .filter(n -> n.state == PlayerState.COMBATANT_ALIVE)
                                 .mapToInt(n -> n.team)
                                 .noneMatch(i -> i == t);
    }

    /**
     * @param p
     * @return if the specified player is a {@link PlayerState#SPECTATOR}
     */
    public boolean isSpectator(@NotNull Player p) {
        return getPlayerState(p) == PlayerState.SPECTATOR;
    }

    /**
     * @param p
     * @return if the specified player is an assigned combatant.
     */
    public boolean isAssignedCombatant(@NotNull Player p) {
        PlayerState state = getPlayerState(p);
        return state == PlayerState.COMBATANT_ALIVE || state == PlayerState.COMBATANT_DEAD;
    }

    public int getNumTeams() {
        return numTeams;
    }

    /**
     * Sets the number of players per team
     * @param s "solos", "duos", "trios", "quartets", "quintets", "sextets", "septets", "octets"
     */
    public void setTeamSize(String s) {
        int tsize;
        switch (s) {
            case "solos":
                tsize = 1;
                break;
            case "duos":
                tsize = 2;
                break;
            case "trios":
                tsize = 3;
                break;
            case "quartets":
                tsize = 4;
                break;
            case "quintets":
                tsize = 5;
                break;
            case "sextets":
                tsize = 6;
                break;
            case "septets":
                tsize = 7;
                break;
            case "octets":
                tsize = 8;
                break;
            default:
                throw new IllegalArgumentException();
        }
        setTeamSize(tsize);
    }

    /**
     * Sets the number of players per team
     * If the # of combatants is not easily divisible by the team size, try to even as much as possible
     * @param n
     */
    public void setTeamSize(int n) {
        setNumTeams((int) Math.round(countCombatants() / (double) n));
    }

    public void setNumTeams(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("Team count must be positive.");
        }

        numTeams = n;
    }
}
