package xyz.baz9k.UHCGame;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.baz9k.UHCGame.ConfigValues.BossMode;
import xyz.baz9k.UHCGame.event.PlayerStateChangeEvent;
import xyz.baz9k.UHCGame.exception.UHCException;
import xyz.baz9k.UHCGame.util.TeamDisplay;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class TeamManager {
    private record Node(PlayerState state, int team) {
        public Node {
            if (!state.isAssignedCombatant()) team = 0;
        }
    }

    public class UnresolvedPlayerSet extends AbstractSet<OfflinePlayer> {
        private Set<OfflinePlayer> pSet;
        private UnresolvedPlayerSet(Set<OfflinePlayer> pSet) {
            this.pSet = pSet;
        }

        @Override
        public Iterator<OfflinePlayer> iterator() {
            return pSet.iterator();
        }

        @Override
        public int size() {
            return pSet.size();
        }

        private Set<Player> resolveOfflines(Function<UUID, Player> resolver) {
            return this.stream()
                .map(op -> {
                    Player p = op.getPlayer();
                    if (p != null) return p;
                    return resolver.apply(op.getUniqueId());
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        }

        public Set<Player> online() { return resolveOfflines(u -> null); }
        public Set<Player> cached() { return resolveOfflines(cachedPlayers::get); }

    }

    private final UHCGamePlugin plugin;
    private int numTeams = 2;
    private final HashMap<UUID, Node> playerMap = new HashMap<>();
    private final HashMap<UUID, Player> cachedPlayers = new HashMap<>();

    public TeamManager(UHCGamePlugin plugin) {
        this.plugin = plugin;
    }

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
        Node oldNode = getNode(p);
        Node newNode = new Node(s, t);
        if (callEvent && p.isOnline() && oldNode != newNode) {
            new PlayerStateChangeEvent(p, s, t).callEvent();
        }
        playerMap.put(p.getUniqueId(), newNode);
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
        assignPlayerToTeam(p, t, true);
    }

    /**
     * Assigns a player to a combatant team.
     * @param p Player to assign team to
     * @param t Team to assign player to
     * @param bounded Bound the team to the number of teams allowed. If true, an IllegalArgumentException will occur if bound is not met.
     */
    public void assignPlayerToTeam(@NotNull Player p, int t, boolean bounded) {
        var invalidExc = new Key("team.invalid").transErr(IllegalArgumentException.class);

        if (t < 0) throw invalidExc;
        if (bounded && t > numTeams) {
            throw invalidExc;
        } else {
            numTeams = Math.max(numTeams, t);
        }

        // set to comb alive unless they're dead then set to dead
        Node n = getNode(p);
        PlayerState s = PlayerState.COMBATANT_ALIVE;
        if (n.state.isAssignedCombatant()) s = n.state;
        setNode(p, s, t);

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

    private void shuffleAssign(List<Player> combatants, int first, int last) {
        int n = last - first + 1; // n of ints in interval [first, last]
        
        if (n < 0) {
            throw new IllegalArgumentException(String.format("Cannot assign players between %s and %s", first, last));
        }
        if (n == 0) { // optimization
            for (Player p : combatants) assignPlayerToTeam(p, first);
            return;
        }

        var shufCombs = new ArrayList<>(combatants);
        Collections.shuffle(shufCombs);

        int i = 0;
        for (Player p : shufCombs) {
            assignPlayerToTeam(p, i + first);
            i++;
            i %= n;
        }
    }

    private <T> List<T> removeNRandom(List<T> list, int n) {
        if (n > list.size()) throw new IllegalArgumentException("n is larger than the size of the list");
        if (n == list.size()) {
            var newList = List.copyOf(list);
            list.clear();
            return newList;
        }
        
        Random r = new Random();
        var newList = new ArrayList<T>();
        for (int i = 0; i < n; i++) {
            int j = r.nextInt(list.size());
            newList.add(list.remove(j));
        }
        return Collections.unmodifiableList(newList);
    }
    /**
     * Assigns all registered players to a team.
     */
    private void assignTeams() {
        List<Player> combatants = new ArrayList<>(getCombatants().online());
        
        var boss = plugin.configValues().bossMode();
        boolean sardines = plugin.configValues().sardines();

        if (boss.enabled()) {
            // boss team shuffling
            boolean t1Assigned = IntStream.of(getAliveTeams()).anyMatch(t -> t == 1);

            List<Player> combatantsToAssign;
            if (t1Assigned) {
                combatantsToAssign = combatants.stream()
                    .filter(p -> getTeam(p) == 1)
                    .toList();
            } else {
                combatantsToAssign = new ArrayList<>(combatants);
                var t1 = removeNRandom(combatantsToAssign, boss.nPlayers());

                for (Player p : t1) assignPlayerToTeam(p, 1);
            }

            shuffleAssign(combatantsToAssign, 2, numTeams);

        } else if (sardines) {
            for (Player p : combatants) assignPlayerToTeam(p, 0);
        } else {
            if (combatants.size() == numTeams) { 
                // solos assigning
                combatants.sort((p1, p2) -> p1.getName().compareTo(p2.getName()));
                for (int i = 0; i < numTeams; i++) {
                    assignPlayerToTeam(combatants.get(i), i + 1);
                }
            } else {
                // regular assigning
                shuffleAssign(combatants, 1, numTeams);
            }
        }


        // set numTeams to actual number of teams
        numTeams = getAliveTeams().length;
    }

    public void announceTeams() {
        int hideTeams = plugin.configValues().hideTeams();
        var boss = plugin.configValues().bossMode();

        // 0: Display all teams
        // 1: Display only your team
        // 2: Do not display teams
        if (hideTeams != 0) return;
        
        int i;
        if (boss.enabled()) {
            announceTeamsLine(PlayerState.COMBATANT_ALIVE, 1, boss);
            i = 2;
        } else {
            i = 1;
        }
        for (; i <= numTeams; i++) {
            announceTeamsLine(PlayerState.COMBATANT_ALIVE, i);
        }

        announceTeamsLine(PlayerState.COMBATANT_ALIVE, 0);
        announceTeamsLine(PlayerState.COMBATANT_UNASSIGNED, 0);
        announceTeamsLine(PlayerState.SPECTATOR, 0);

    }

    private void announceTeamsLine(PlayerState s, int t) {
        announceTeamsLine(s, t, BossMode.disabled());
    }

    private void announceTeamsLine(PlayerState s, int t, BossMode boss) {
        Set<? extends OfflinePlayer> players;
        if (s.isAssignedCombatant()) {
            players = getCombatantsOnTeam(t);
        } else {
            players = getAllPlayersMatching(n -> n.state == s && n.team == t).online();
        }
        if (players.size() == 0) return;

        var b = Component.text()
            .append(TeamDisplay.getName(s, t));
        
        if (boss.enabled()) {
            b.append(Component.text(" (", noDeco(NamedTextColor.WHITE)))
             .append(Component.text(String.format("%s\u2665", boss.bossHealth()), TextColor.color(0xA100FF), TextDecoration.BOLD))
             .append(Component.text(")", noDeco(NamedTextColor.WHITE)));
        }
        
        b.append(Component.text(": ", noDeco(NamedTextColor.WHITE)));

        // list of players one a team, separated by commas
        String tPlayers = players.stream()
            .map(OfflinePlayer::getName)
            .collect(Collectors.joining(", "));
        
        b.append(Component.text(tPlayers, noDeco(NamedTextColor.WHITE)));
        Bukkit.getServer().sendMessage(b);
    }

    public void tryAssignTeams() throws UHCException {
        assignTeams();

        // verify teams are valid
        var boss = plugin.configValues().bossMode();
        if (boss.enabled() && numTeams < 2) {
            resetAllPlayers();
            throw new UHCException(new Key("err.team.boss_must_2"));
        }
        
        // announce teams if valid
        announceTeams();
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
    
    private UnresolvedPlayerSet getAllPlayersMatching(Predicate<Node> predicate) {
        Set<OfflinePlayer> pSet = playerMap.entrySet().stream()
            .filter(e -> predicate.test(e.getValue()))
            .map(e -> Bukkit.getOfflinePlayer(e.getKey()))
            .collect(Collectors.toSet());
        
        return new UnresolvedPlayerSet(pSet);
    }

    /**
     * @return a {@link Set} of all spectators
     */
    public @NotNull UnresolvedPlayerSet getSpectators() {
        return getAllPlayersMatching(n -> n.state.isSpectator());
    }

    /**
     * @return a {@link Set} of all combatants
     */
    public @NotNull UnresolvedPlayerSet getCombatants() {
        return getAllPlayersMatching(n -> n.state.isCombatant());
    }

    /**
     * @return a {@link Set} of all wildcards (players that do not have a known team while in game)
     */
    public @NotNull UnresolvedPlayerSet getWildcards() {
        return getAllPlayersMatching(n -> n.state.isAssignedCombatant() && n.team == 0);
    }

    /**
     * @return a {@link Set} of all living combatants
     */
    public @NotNull UnresolvedPlayerSet getAliveCombatants() {
        return getAllPlayersMatching(n -> n.state == PlayerState.COMBATANT_ALIVE);
    }
    /**
     * @return a {@link Set} of all assigned combatants
     */
    public @NotNull UnresolvedPlayerSet getAssignedCombatants() {
        return getAllPlayersMatching(n -> n.state.isAssignedCombatant());
    }

    /**
     * @param team Team to inspect
     * @return a {@link Set} of all combatants on a specific team
     */
    public @NotNull UnresolvedPlayerSet getCombatantsOnTeam(int team) {
        if (team < 0 || team > numTeams) {
            throw new Key("err.team.invalid").transErr(IllegalArgumentException.class, team, numTeams);
        }

        if (team == 0) return new UnresolvedPlayerSet(Set.of()); // wildcard
        return getAllPlayersMatching(n -> n.state.isAssignedCombatant() && n.team == team);
    }

    /**
     * @return a collection of spread groups if spreading by teams.
     * <p>Each team is spread together and each wild card is spread separately.
     */
    public @NotNull Collection<Collection<Player>> getSpreadGroups() {
        // break up online assigned combatants to their teams
        Map<Integer, List<Player>> teamDivs = getAssignedCombatants().online()
            .stream()
            .collect(Collectors.groupingBy(this::getTeam));
        
        // add teams to the final group list, but add each wildcard individually
        return teamDivs.entrySet().stream()
            .<Collection<Player>>mapMulti((e, acceptor) -> {
                if (e.getKey() > 0) {
                    // add teams as a group
                    acceptor.accept(e.getValue());
                } else {
                    // add all wildcards individually
                    e.getValue().forEach(wild -> acceptor.accept(Collections.singleton(wild)));
                }
            })
            .toList();
    }
    /**
     * @return an array of the alive teams by int
     */
    public int[] getAliveTeams() {
        return playerMap.values().stream()
            .mapToInt(Node::team)
            .filter(n -> n > 0)
            .distinct()
            .toArray();
    }

    public record AliveGroup(int team, UUID uuid) {
        @Override
        public boolean equals(Object o) {
            // wildcards are distinguished by UUIDs, teams are not
            if (o instanceof AliveGroup g) {
                if (this.team() == 0 && g.team() == 0) {
                    return this.uuid().equals(g.uuid());
                } else {
                    return this.team() == g.team();
                }
            }

            return false;
        }

        public Component getName() {
            if (team > 0) return TeamDisplay.getPrefix(PlayerState.COMBATANT_ALIVE, team);
            return Component.text(Bukkit.getOfflinePlayer(uuid).getName());
        }
    }

    /**
     * @return an array of alive teams and wildcards
     */
    public AliveGroup[] getAliveGroups() {
        return playerMap.entrySet().stream()
            .filter(e -> e.getValue().state == PlayerState.COMBATANT_ALIVE)
            .map(e -> new AliveGroup(e.getValue().team(), e.getKey()))
            .distinct()
            .toArray(AliveGroup[]::new);
    }

    public record ChatGroup(Component groupName, Audience audience) {}
    public ChatGroup getChatBuddies(Player p) {
        Node pn = getNode(p);
        PlayerState s = pn.state;
        int t = pn.team;

        Component groupName = Component.empty(); 
        Set<? extends Audience> aud = Set.of();

        if (s == PlayerState.COMBATANT_ALIVE && t != 0) {
            // team chat
            groupName = TeamDisplay.getPrefix(s, t);
            aud = getAllPlayersMatching(n -> n.state == PlayerState.COMBATANT_ALIVE && n.team == t).online();
        } else if (s == PlayerState.COMBATANT_ALIVE && t == 0) {
            // wildcard chat
            groupName = Component.text("[:(]");
            aud = Set.of();
        } else if (s.isSpectating()) {
            // dead chat
            groupName = TeamDisplay.getDeadPrefix();
            aud = getAllPlayersMatching(n -> n.state.isSpectating()).online();
        } 

        aud = new HashSet<>(aud);
        aud.remove(p);
        
        return new ChatGroup(groupName, Audience.audience(aud));
    }
    /**
     * @param t Team to inspect
     * @return if the specified team is eliminated
     */
    public boolean isTeamEliminated(int t) {
        if (t == 0) return false; // wildcard d/n have team

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

    /**
     * Check if two players are on the same team
     * <p> Both players need to be non-wildcard assigned combatants for this method to return true.
     * @param p1 Player 1 to check
     * @param p2 Player 2 to check
     * @return if the two players are on the same team
     */
    public boolean onSameTeam(@NotNull Player p1, @NotNull Player p2) {
        Node n1 = getNode(p1),
             n2 = getNode(p2);
        
        // check both players are assigned
        if (n1.state().isAssignedCombatant() && n2.state().isAssignedCombatant()) {
            // check both players are not wildcards
            if (n1.team() != 0 && n2.team() != 0) {
                return n1.team() == n2.team();
            }
        }

        return false;
    }

    /**
     * @return get number of wildcards in game
     */
    public int getNumWildcards() {
        return getWildcards().size();
    }

    /*
     * @param p Player to inspect
     * @return if the specified player is a wildcard.
     */
    public boolean isWildcard(@NotNull Player p) {
        Node n = getNode(p);
        return n.state.isAssignedCombatant() && n.team == 0;
    }

    /**
     * @return the number of teams in game
     */
    public int getNumTeams() {
        return numTeams;
    }

    /**
     * @return the number of spawn locations Spreadplayers needs to generate with these teams
     */
    public int getNumSpreadGroups() {
        return getNumWildcards() + getNumTeams();
    }

    /**
     * Sets the number of players per team
     * @param s "solos", "duos", "trios", "quartets", "quintets", "sextets", "septets", "octets"
     */
    public void setTeamSize(String s) throws IllegalArgumentException {
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
        setNumTeams((int) Math.round(getCombatants().online().size() / (double) n));
    }

    /**
     * Sets the number of teams
     * @param n Number of teams
     */
    public void setNumTeams(int n) {
        numTeams = Math.max(1, n);
    }
}
