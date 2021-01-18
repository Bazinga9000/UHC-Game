package xyz.baz9k.UHCGame;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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
    private final BitSet aliveTeams = new BitSet();
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
    public void assignPlayerToTeam(@NotNull Player p, int team) {
        if (team <= 0 || team > numTeams) {
            throw new IllegalArgumentException("Invalid team (Team must be positive and less than the team count.)");
        }

        Node n = getPlayerNode(p);
        n.state = PlayerState.COMBATANT_ALIVE;
        n.team = team;

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

    @NotNull
    public PlayerState getPlayerState(@NotNull Player p) {
        return getPlayerNode(p).state;
    }

    public int getTeam(@NotNull Player p) {
        return getPlayerNode(p).team;
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
        
        Node n = getPlayerNode(p);
        n.state = aliveStatus ? PlayerState.COMBATANT_ALIVE : PlayerState.COMBATANT_DEAD;
        updateTeamAliveStatus(getTeam(p));
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


    /**
     * @return the number of living teams in the team manager.
     */
    public int countLivingTeams() {
        return aliveTeams.cardinality();
    }
    
    /**
     * @return an array of the alive teams by int
     */
    public int[] getAliveTeams() {
        return aliveTeams.stream().toArray();
    }

    /**
     * On game start, this should be run to mark which teams are alive.
     */
    public void prepareAliveTeams() {
        aliveTeams.clear();
        aliveTeams.set(1, numTeams + 1);
    }

    /**
     * This should be run whenever the # of players on a team updates.
     */
    public void updateTeamAliveStatus(int t) {
        aliveTeams.set(t, countLivingCombatantsInTeam(t) == 0);
    }

    /**
     * @param t
     * @return if the specified team is eliminated
     */
    public boolean isTeamEliminated(int t) {
        return !aliveTeams.get(t);
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
