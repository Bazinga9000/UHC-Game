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

    public PlayerStateChangeEvent(Player p, PlayerState state) {
        this.p = p;
        this.state = state;
    }

    public Player player() { return p; }
    public PlayerState state() { return state; }

}
