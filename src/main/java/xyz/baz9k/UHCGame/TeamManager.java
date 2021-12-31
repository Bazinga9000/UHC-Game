package xyz.baz9k.UHCGame;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import xyz.baz9k.UHCGame.event.PlayerStateChangeEvent;
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
import java.util.Objects;

public class TeamManager {
    private record Node(PlayerState state, int team) {
        public Node {
            if (!state.isAssignedCombatant()) team = 0;
        }
    }

    private int numTeams = 2;
    private final HashMap<UUID, Node> playerMap = new HashMap<>();
    private final HashMap<UUID, Player> cachedPlayers = new HashMap<>();

    /**
     * Gets the stored node of the player in the player map.
     * <p>
     * If the player is not found in the map, a new Node of state {@link PlayerState#COMBATANT_UNASSIGNED} is created.
     * @param p Player to get node of
     * @return the node
     */
    private @NotNull Node getNode(@NotNull Player p) {
        cachedPlayers.put(p.getUniqueId(), p);
        return playerMap.computeIfAbsent(p.getUniqueId(), 
            k -> new Node(PlayerState.COMBATANT_UNASSIGNED, 0)
        );
    }

    public void addPlayer(@NotNull Player p, boolean hasStarted) {
        Node n = getNode(p);
        if (isAssignedCombatant(p)) return;
        // this doesn't count as a state change, b/c their state before is not relevant
        PlayerState s = hasStarted ? PlayerState.SPECTATOR : PlayerState.COMBATANT_UNASSIGNED;
        setNode(p, s, n.team(), false);
    }

    private Node setNode(@NotNull Player p, @NotNull PlayerState s, int t, boolean callEvent) {
        Node n = getNode(p);
        if (callEvent && p.isOnline() && n.state() != s) {
            new PlayerStateChangeEvent(p, s).callEvent();
        }
        playerMap.put(p.getUniqueId(), new Node(s, t));
        return getNode(p);
    }
    private Node setNode(@NotNull Player p, @NotNull PlayerState s, int t) {
        return setNode(p, s, t, true);
    }

    private Node setState(@NotNull Player p, @NotNull PlayerState s) {
        Node n = getNode(p);
        return setNode(p, s, n.team());
    }

    /**
     * Sets a player's state to {@link PlayerState#SPECTATOR}
     * @param p Player to set
     */
    public void setSpectator(@NotNull Player p) {
        setNode(p, PlayerState.SPECTATOR, 0);
    }

    /**
     * Sets a player's state to {@link PlayerState#COMBATANT_UNASSIGNED}
     * @param p Player to set
     */
    public void setUnassignedCombatant(@NotNull Player p) {
        setNode(p, PlayerState.COMBATANT_UNASSIGNED, 0);
    }

    /**
     * Assigns a player to a combatant team.
     * @param p Player to assign team to
     * @param t Team to assign player to
     */
    public void assignPlayerToTeam(@NotNull Player p, int t) {
        if (t <= 0 || t > numTeams) {
            throw new Key("team.invalid").transErr(IllegalArgumentException.class);
        }

        setNode(p, PlayerState.COMBATANT_ALIVE, t);

    }

    /**
     * Resets all players.
     */
    public void resetAllPlayers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            setNode(p, PlayerState.COMBATANT_UNASSIGNED, 0);
        }

        playerMap.replaceAll((k, v) -> new Node(PlayerState.COMBATANT_UNASSIGNED, 0));
    }

    /**
     * Assigns all registered players to a team.
     */
    public void assignTeams() {
        List<Player> combatants = new ArrayList<>(getOnlineCombatants());
        
        if (combatants.size() == numTeams) { // solos
            combatants.sort((p1, p2) -> p1.getName().compareTo(p2.getName()));
            for (int i = 0; i < numTeams; i++) {
                assignPlayerToTeam(combatants.get(i), i + 1);
            }
        } else {
            Collections.shuffle(combatants);
    
            int i = 1;
            for (Player p : combatants) {
                assignPlayerToTeam(p, i);
                i = i % numTeams + 1;
            }
        }
    }

    public void announceTeams() {
        for (int i = 1; i <= numTeams; i++) {
            announceTeamsLine(PlayerState.COMBATANT_ALIVE, i);
        }

        announceTeamsLine(PlayerState.COMBATANT_ALIVE, 0);
        announceTeamsLine(PlayerState.COMBATANT_UNASSIGNED, 0);
        announceTeamsLine(PlayerState.SPECTATOR, 0);

    }

    private void announceTeamsLine(PlayerState s, int t) {
        Set<? extends OfflinePlayer> players;
        if (s.isAssignedCombatant()) {
            players = getCombatantsOnTeam(t);
        } else {
            players = filterOnline(getAllPlayersMatching(n -> n.state == s && n.team == t));
        }
        if (players.size() == 0) return;

        var b = Component.text()
            .append(TeamDisplay.getName(s, t))
            .append(Component.text(": ", noDeco(NamedTextColor.WHITE)));

        // list of players one a team, separated by commas
        String tPlayers = players.stream()
            .map(OfflinePlayer::getName)
            .collect(Collectors.joining(", "));
        
        b.append(Component.text(tPlayers, noDeco(NamedTextColor.WHITE)));
        Bukkit.getServer().sendMessage(b);
    }

    public @NotNull PlayerState getPlayerState(@NotNull Player p) {
        return getNode(p).state;
    }

    public int getTeam(@NotNull Player p) {
        return getNode(p).team;
    }

    /**
     * Sets player to alive or dead.
     * @param p Player to set
     * @param alive Status to set
     */
    public void setCombatantAliveStatus(@NotNull Player p, boolean alive) {
        if (!isAssignedCombatant(p)) {
            throw new Key("err.team.must_assigned_comb").transErr(IllegalArgumentException.class);
        }

        PlayerState s = alive ? PlayerState.COMBATANT_ALIVE : PlayerState.COMBATANT_DEAD;
        setState(p, s);
    }

    /* LIST OF PLAYERS */
    
    private Set<OfflinePlayer> getAllPlayersMatching(Predicate<Node> predicate) {
        return playerMap.entrySet().stream()
            .filter(e -> predicate.test(e.getValue()))
            .map(e -> Bukkit.getOfflinePlayer(e.getKey()))
            .collect(Collectors.toSet());
    }

    private Set<Player> filterOnline(@NotNull Set<? extends OfflinePlayer> pSet) {
        return pSet.stream()
            .map(OfflinePlayer::getPlayer)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private Set<Player> useCache(@NotNull Set<? extends OfflinePlayer> pSet) {
        return pSet.stream()
            .map(op -> {
                Player p = op.getPlayer();
                if (p != null) return p;
                return cachedPlayers.get(op.getUniqueId());
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    /**
     * @return a {@link Set} of all spectators
     */
    public @NotNull Set<OfflinePlayer> getSpectators() {
        return getAllPlayersMatching(n -> n.state.isSpectator());
    }

    /**
     * @return a {@link Set} of all combatants
     */
    public @NotNull Set<OfflinePlayer> getCombatants() {
        return getAllPlayersMatching(n -> n.state.isCombatant());
    }

    /**
     * @return a {@link Set} of all living combatants
     */
    public @NotNull Set<OfflinePlayer> getAliveCombatants() {
        return getAllPlayersMatching(n -> n.state == PlayerState.COMBATANT_ALIVE);
    }
    /**
     * @return a {@link Set} of all assigned combatants
     */
    public @NotNull Set<OfflinePlayer> getAssignedCombatants() {
        return getAllPlayersMatching(n -> n.state.isAssignedCombatant());
    }

    /**
     * @param team Team to inspect
     * @return a {@link Set} of all combatants on a specific team
     */
    public @NotNull Set<OfflinePlayer> getCombatantsOnTeam(int team) {
        if (team <= 0 || team > numTeams) {
            throw new Key("err.team.invalid").transErr(IllegalArgumentException.class, team, numTeams);
        }

        return getAllPlayersMatching(n -> n.state.isAssignedCombatant() && n.team == team);
    }

    /**
     * @return a {@link Set} of all combatants, using cached snapshots of offline players
     * <p>This method should only be used to query information from offline players.
     */
    public @NotNull Set<Player> getCachedCombatants() {
        return useCache(getCombatants());
    }

    /**
     * @return a {@link Set} of all combatants, using cached snapshots of offline players
     * <p>This method should only be used to query information from offline players.
     */
    public @NotNull Set<Player> getCachedCombatantsOnTeam(int t) {
        return useCache(getCombatantsOnTeam(t));
    }

    /**
     * @return a {@link Set} of all online spectators
     */
    public @NotNull Set<Player> getOnlineSpectators() {
        return filterOnline(getSpectators());
    }

    /**
     * @return a {@link Set} of all online combatants
     */
    public @NotNull Set<Player> getOnlineCombatants() {
        return filterOnline(getCombatants());
    }

    /**
     * @param t Team to inspect
     * @return a {@link Set} of all online combatants on a specific team
     */
    public @NotNull Set<Player> getOnlineCombatantsOnTeam(int t) {
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
     * @param t Team to inspect
     * @return if the specified team is eliminated
     */
    public boolean isTeamEliminated(int t) {
        return playerMap.values().stream()
            .filter(n -> n.state == PlayerState.COMBATANT_ALIVE)
            .mapToInt(n -> n.team)
            .noneMatch(i -> i == t);
    }

    /**
     * @param p Player to inspect
     * @return if the specified player is a {@link PlayerState#SPECTATOR}
     */
    public boolean isSpectator(@NotNull Player p) {
        return getPlayerState(p).isSpectator();
    }

    /**
     * @param p Player to inspect
     * @return if the specified player is an assigned combatant.
     */
    public boolean isAssignedCombatant(@NotNull Player p) {
        return getPlayerState(p).isAssignedCombatant();
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
            default -> throw new Key("err.team.size_name_invalid").transErr(IllegalArgumentException.class, s);
        });
    }

    /**
     * Sets the number of players per team
     * If the # of combatants is not easily divisible by the team size, try to even as much as possible
     * @param n Number of members in each team
     */
    public void setTeamSize(int n) {
        setNumTeams((int) Math.round(getOnlineCombatants().size() / (double) n));
    }

    /**
     * Sets the number of teams
     * @param n Number of teams
     */
    public void setNumTeams(int n) {
        if (n <= 0) {
            throw new Key("err.team.count_must_pos").transErr(IllegalArgumentException.class);
        }

        numTeams = n;
    }
}
