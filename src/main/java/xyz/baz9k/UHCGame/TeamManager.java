package xyz.baz9k.UHCGame;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
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

    public Node getPlayerNode(Player p) {
        return playerMap.get(p.getUniqueId());
    }
    public void addPlayer(Player p) {
        UUID uuid = p.getUniqueId();
        if (playerMap.containsKey(uuid)) {
            playerMap.get(uuid).player = p;
        };
        playerMap.put(uuid, new Node(0, PlayerState.SPECTATOR, p));
    }

    public void setSpectator(Player p) {
        Node n = getPlayerNode(p);
        n.team = 0;
        n.state = PlayerState.SPECTATOR;
    }

    public void setUnassignedCombatant(Player p) {
        Node n = getPlayerNode(p);
        n.team = 0;
        n.state = PlayerState.COMBATANT_UNASSIGNED;
    }

    public void assignPlayerTeam(Player p, int team) {
        if (team <= 0 || team > numTeams) {
            throw new IllegalArgumentException("Team must be positive and less than the team count.");
        }

        Node n = getPlayerNode(p);
        n.state = PlayerState.COMBATANT_ALIVE;
        n.team = team;

    }

    public void removePlayer(Player p) {
        playerMap.remove(p.getUniqueId());
    }

    public int getTeam(Player p) {
        return getPlayerNode(p).team;
    }

    public PlayerState getPlayerState(Player p) {
        return getPlayerNode(p).state;
    }

    public boolean isPlayerAlive(Player p) {
        return getPlayerNode(p).state == PlayerState.COMBATANT_ALIVE;
    }

    public void setCombatantAliveStatus(Player p, boolean aliveStatus) {
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
            throw new IllegalArgumentException("Team must be positive and less than the team count.");
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
    public List<Player> getAllSpectators() {
        return getAllPlayersMatching(n -> n.state == PlayerState.SPECTATOR);
    }

    public List<Player> getAllCombatants() {
        return getAllPlayersMatching(n -> n.state != PlayerState.SPECTATOR);
    }

    public List<Player> getAllCombatantsOnTeam(int team) {
        if (team <= 0 || team > numTeams) {
            throw new IllegalArgumentException("Team must be positive and less than the team count.");
        }

        return getAllPlayersMatching(n -> n.state != PlayerState.SPECTATOR && n.state != PlayerState.COMBATANT_UNASSIGNED && n.team == team);
    }

    public boolean isTeamEliminated(int team) {
        return countLivingCombatantsInTeam(team) == 0;
    }

    public boolean isSpectator(Player p) {
        return getPlayerNode(p).state == PlayerState.SPECTATOR;
    }

    public boolean isAssignedCombatant(Player p) {
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
