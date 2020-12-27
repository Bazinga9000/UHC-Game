package xyz.baz9k.UHCGame;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
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

    int numTeams = 2;
    private final HashMap<UUID, Node> playerMap;

    public TeamManager() {
        playerMap = new HashMap<>();
    }

    @NotNull
    private Node getPlayerNode(@NotNull Player p) {
        return playerMap.get(p.getUniqueId());
    }

    public void addPlayer(@NotNull Player p) {
        UUID uuid = p.getUniqueId();
        if (playerMap.containsKey(uuid)) {
            playerMap.get(uuid).player = p;
            return;
        };
        playerMap.put(uuid, new Node(0, PlayerState.SPECTATOR, p));
    }

    public void setSpectator(@NotNull Player p) {
        Node n = getPlayerNode(p);
        n.team = 0;
        n.state = PlayerState.SPECTATOR;
    }

    public void setUnassignedCombatant(@NotNull Player p) {
        Node n = getPlayerNode(p);
        n.team = 0;
        n.state = PlayerState.COMBATANT_UNASSIGNED;
    }

    public void assignPlayerTeam(@NotNull Player p, int team) {
        if (team <= 0 || team > numTeams) {
            throw new IllegalArgumentException("Invalid team (Team must be positive and less than the team count.)");
        }

        Node n = getPlayerNode(p);
        n.state = PlayerState.COMBATANT_ALIVE;
        n.team = team;

    }

    public void removePlayer(@NotNull Player p) { // unused
        playerMap.remove(p.getUniqueId());
    }

    public int getTeam(@NotNull Player p) {
        return getPlayerNode(p).team;
    }

    @NotNull
    public PlayerState getPlayerState(@NotNull Player p) {
        return getPlayerNode(p).state;
    }

    public boolean isPlayerAlive(@NotNull Player p) {
        return getPlayerNode(p).state == PlayerState.COMBATANT_ALIVE;
    }

    public void setCombatantAliveStatus(@NotNull Player p, boolean aliveStatus) {
        if (!isAssignedCombatant(p)) {
            throw new IllegalArgumentException("Player must be an assigned combatant.");
        }
        
        Node n = getPlayerNode(p);
        n.state = aliveStatus ? PlayerState.COMBATANT_ALIVE : PlayerState.COMBATANT_DEAD;
    }

    public int countCombatants() {
        int count = 0;
        for (Node v : playerMap.values()) {
            if (v.state != PlayerState.SPECTATOR) {
                count++;
            }
        }
        return count;
    }

    public int countLivingCombatants() {
        int count = 0;
        for (Node v : playerMap.values()) {
            if (v.state == PlayerState.COMBATANT_ALIVE) {
                count++;
            }
        }
        return count;
    }

    public int countLivingTeams() {
        int count = 0;
        for (int i = 1; i <= numTeams; i++) {
            if (!isTeamEliminated(i)) {
                count++;
            }
        }
        return count;
    }
    public int countLivingCombatantsInTeam(int team) {
        if (team <= 0 || team > numTeams) {
            throw new IllegalArgumentException("Invalid team (Team must be positive and less than the team count.)");
        }

        int count = 0;
        for (Node v : playerMap.values()) {
            if (v.state == PlayerState.COMBATANT_ALIVE && v.team == team) {
                count++;
            }
        }
        return count;
    }

    private List<Player> getAllPlayersMatching(Predicate<Node> predicate) {
        ArrayList<Player> players = new ArrayList<>();
        for (Node n : playerMap.values()) {
            if (predicate.test(n)) {
                players.add(n.player);
            }
        }

        return players;
    }

    private List<Player> onlyOnline(@NotNull List<Player> pList) {
        return pList.stream()
                    .filter(p -> p.isOnline())
                    .collect(Collectors.toList());
    }
    @NotNull
    public List<Player> getAllSpectators() {
        return getAllPlayersMatching(n -> n.state == PlayerState.SPECTATOR);
    }

    @NotNull
    public List<Player> getAllCombatants() {
        return getAllPlayersMatching(n -> n.state != PlayerState.SPECTATOR);
    }

    @NotNull
    public List<Player> getAllCombatantsOnTeam(int team) {
        if (team <= 0 || team > numTeams) {
            throw new IllegalArgumentException("Invalid team (Team must be positive and less than the team count.)");
        }

        return getAllPlayersMatching(n -> n.state != PlayerState.SPECTATOR && n.state != PlayerState.COMBATANT_UNASSIGNED && n.team == team);
    }
    @NotNull
    public List<Player> getAllOnlineSpectators() {
        return onlyOnline(getAllSpectators());
    }

    @NotNull
    public List<Player> getAllOnlineCombatants() {
        return onlyOnline(getAllCombatants());
    }

    @NotNull
    public List<Player> getAllOnlineCombatantsOnTeam(int t) {
        return onlyOnline(getAllCombatantsOnTeam(t));
    }

    public boolean isTeamEliminated(int t) {
        return countLivingCombatantsInTeam(t) == 0;
    }

    public boolean isSpectator(@NotNull Player p) {
        return getPlayerNode(p).state == PlayerState.SPECTATOR;
    }

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

    public void resetAllPlayers() {
        for (Node v : playerMap.values()) {
            if (v.state != PlayerState.SPECTATOR) {
                v.state = PlayerState.COMBATANT_UNASSIGNED;
                v.team = 0;
            }
        }
    }
}
