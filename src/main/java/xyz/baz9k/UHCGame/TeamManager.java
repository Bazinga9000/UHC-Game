package xyz.baz9k.UHCGame;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import xyz.baz9k.UHCGame.util.TeamDisplay;

import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class TeamManager {
    private static class Node {
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
    private final HashMap<UUID, Node> playerMap = new HashMap<>();

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

    public void addPlayer(@NotNull Player p, boolean hasStarted) {
        Node n = getNode(p);
        if (isAssignedCombatant(p)) return;
        n.state = hasStarted ? PlayerState.SPECTATOR : PlayerState.COMBATANT_UNASSIGNED;
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
            throw translatableErr(IllegalArgumentException.class, "xyz.baz9k.uhc.team.invalid");
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

    public void announceTeams() {
        for (int i = 1; i <= getNumTeams(); i++) {
            announceTeamsLine(i);
        }
        announceTeamsLine(0);

    }

    private void announceTeamsLine(int t) {
        Set<Player> players;
        if (t == 0) {
            players = getOnlineSpectators();
        } else {
            players = getCombatantsOnTeam(t);
        }
        if (players.size() == 0) return;

        var b = Component.text()
            .append(TeamDisplay.getName(t))
            .append(Component.text(": ", noDeco(NamedTextColor.WHITE)));

        // list of players one a team, separated by commas
        String tPlayers = players.stream()
            .map(p -> p.getName())
            .collect(Collectors.joining(", "));
        
        b.append(Component.text(tPlayers, noDeco(NamedTextColor.WHITE)));
        Bukkit.getServer().sendMessage(b);
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
            throw translatableErr(IllegalArgumentException.class, "xyz.baz9k.uhc.err.team.must_assigned_comb");
        }
        
        Node n = getNode(p);
        n.state = aliveStatus ? PlayerState.COMBATANT_ALIVE : PlayerState.COMBATANT_DEAD;
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
    public Set<Player> getSpectators() {
        return getAllPlayersMatching(n -> n.state == PlayerState.SPECTATOR);
    }

    /**
     * @return a {@link Set} of all combatants
     */
    @NotNull
    public Set<Player> getCombatants() {
        return getAllPlayersMatching(n -> n.state != PlayerState.SPECTATOR);
    }

    /**
     * @return a {@link Set} of all combatants
     */
    @NotNull
    public Set<Player> getAliveCombatants() {
        return getAllPlayersMatching(n -> n.state == PlayerState.COMBATANT_ALIVE);
    }

    /**
     * @param team
     * @return a {@link Set} of all combatants on a specific team
     */
    @NotNull
    public Set<Player> getCombatantsOnTeam(int team) {
        if (team <= 0 || team > numTeams) {
            throw translatableErr(IllegalArgumentException.class, "xyz.baz9k.uhc.err.team.invalid", team, numTeams);
        }

        return getAllPlayersMatching(n -> n.state != PlayerState.SPECTATOR && n.state != PlayerState.COMBATANT_UNASSIGNED && n.team == team);
    }

    /**
     * @return a {@link Set} of all online spectators
     */
    @NotNull
    public Set<Player> getOnlineSpectators() {
        return filterOnline(getSpectators());
    }

    /**
     * @return a {@link Set} of all online combatants
     */
    @NotNull
    public Set<Player> getOnlineCombatants() {
        return filterOnline(getCombatants());
    }

    /**
     * @param t
     * @return a {@link Set} of all online combatants on a specific team
     */
    @NotNull
    public Set<Player> getOnlineCombatantsOnTeam(int t) {
        return filterOnline(getCombatantsOnTeam(t));
    }
    
    /**
     * @return an array of the alive teams by int
     */
    public int[] getAliveTeams() {
        return playerMap.values().stream()
            .filter(n -> n.state == PlayerState.COMBATANT_ALIVE)
            .mapToInt(n -> n.team)
            .distinct()
            .toArray();
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
        setTeamSize(switch (s) {
            case "solos" -> 1;
            case "duos" -> 2;
            case "trios" -> 3;
            case "quartets" -> 4;
            case "quintets" -> 5;
            case "sextets" -> 6;
            case "septets" -> 7;
            case "octets" -> 8;
            default -> throw translatableErr(IllegalArgumentException.class, "xyz.baz9k.uhc.err.team.size_name_invalid", s);
        });
    }

    /**
     * Sets the number of players per team
     * If the # of combatants is not easily divisible by the team size, try to even as much as possible
     * @param n
     */
    public void setTeamSize(int n) {
        setNumTeams((int) Math.round(getCombatants().size() / (double) n));
    }

    public void setNumTeams(int n) {
        if (n <= 0) {
            throw translatableErr(IllegalArgumentException.class, "xyz.baz9k.uhc.err.team.count_must_pos");
        }

        numTeams = n;
    }
}
