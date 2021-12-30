package xyz.baz9k.UHCGame;

public enum PlayerState {
    SPECTATOR,
    COMBATANT_UNASSIGNED,
    COMBATANT_ALIVE,
    COMBATANT_DEAD;

    public boolean isSpectator() { return this == SPECTATOR; }
    public boolean isCombatant() { return this != SPECTATOR; }
    public boolean isAssignedCombatant() { return this == COMBATANT_ALIVE || this == COMBATANT_DEAD; }
    public boolean isSpectating() { return this == COMBATANT_DEAD || this == SPECTATOR; }
}