package xyz.baz9k.UHCGame;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class TeamManager {
    private class Node {
        public int team;
        public PlayerState state;
        public Node(int team, PlayerState state) {
            this.team = team;
            this.state = state;
        }
    }

    int numTeams = 2;
    private final HashMap<Player, Node> playerMap;

    public TeamManager() {
        playerMap = new HashMap<>();
    }

    public void addPlayer(Player player) {
        if (playerMap.containsKey(player)) return;
        playerMap.put(player, new Node(0, PlayerState.SPECTATOR));
    }

    public void setSpectator(Player player) {
        Node n = playerMap.get(player);
        n.team = 0;
        n.state = PlayerState.SPECTATOR;
    }

    public void setUnassignedCombatant(Player player) {
        Node n = playerMap.get(player);
        n.team = 0;
        n.state = PlayerState.COMBATANT_UNASSIGNED;
    }

    public void assignPlayerTeam(Player player, int team) {
        if (team <= 0 || team > numTeams) {
            throw new IllegalArgumentException("Team must be positive and less than the team count.");
        }

        Node n = playerMap.get(player);
        n.state = PlayerState.COMBATANT_ALIVE;
        n.team = team;

    }

    public void removePlayer(Player player) {
        playerMap.remove(player);
    }

    public int getTeam(Player player) {
        return playerMap.get(player).team;
    }

    public PlayerState getPlayerState(Player player) {
        return playerMap.get(player).state;
    }

    public boolean isPlayerAlive(Player player) {
        return (playerMap.get(player).state == PlayerState.COMBATANT_ALIVE);
    }

    public void setCombatantAliveStatus(Player player, boolean aliveStatus) {
        if (!isAssignedCombatant(player)) {
            throw new IllegalArgumentException("Player must be an assigned combatant.");
        }
        
        Node n = playerMap.get(player);
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

    public List<Player> getAllSpectators() {
        ArrayList<Player> players = new ArrayList<>();
        for (Player p : playerMap.keySet()) {
            if (playerMap.get(p).state == PlayerState.SPECTATOR) {
                players.add(p);
            }
        }

        return players;
    }

    public List<Player> getAllCombatants() {
        ArrayList<Player> players = new ArrayList<>();
        for (Player p : playerMap.keySet()) {
            if (playerMap.get(p).state != PlayerState.SPECTATOR) {
                players.add(p);
            }
        }

        return players;
    }

    public List<Player> getAllCombatantsOnTeam(int team) {
        if (team <= 0 || team > numTeams) {
            throw new IllegalArgumentException("Team must be positive and less than the team count.");
        }

        ArrayList<Player> players = new ArrayList<>();
        for (Player p : playerMap.keySet()) {
            Node n = playerMap.get(p);
            if (n.state != PlayerState.SPECTATOR && n.team == team) {
                players.add(p);
            }
        }

        return players;
    }

    public boolean isTeamEliminated(int team) {
        return countLivingCombatantsInTeam(team) == 0;
    }

    public boolean isSpectator(Player p) {
        return playerMap.get(p).state == PlayerState.SPECTATOR;
    }

    public boolean isAssignedCombatant(Player p) {
        PlayerState state = playerMap.get(p).state;
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
