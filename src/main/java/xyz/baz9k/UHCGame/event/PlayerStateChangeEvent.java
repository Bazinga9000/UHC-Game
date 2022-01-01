package xyz.baz9k.UHCGame.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import xyz.baz9k.UHCGame.PlayerState;

public class PlayerStateChangeEvent extends Event {
    // boilerplate
    private static final HandlerList HANDLERS = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
    //

    private final Player p;
    private final PlayerState state;
    private final int team;

    public PlayerStateChangeEvent(Player p, PlayerState state, int team) {
        this.p = p;
        this.state = state;
        this.team = team;
    }

    public Player player() { return p; }
    public PlayerState state() { return state; }
    public int team() { return team; }
    
}
