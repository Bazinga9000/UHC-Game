package xyz.baz9k.UHCGame.exception;

import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

import org.jetbrains.annotations.NotNull;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import xyz.baz9k.UHCGame.util.ComponentUtils.Key;

public class UHCException extends Exception implements ComponentLike {
    public UHCException(String message) {
        super(message);
    }

    public UHCException(Key key, Object... args) {
        super(renderString(key.trans(args)));
    }

    @Override
    public @NotNull Component asComponent() {
        return Component.text(getMessage(), noDeco(NamedTextColor.RED));
    }

}
