package xyz.baz9k.UHCGame.util.tag;

import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class BooleanTagType implements PersistentDataType<Byte, Boolean> {

    @Override
    public @NotNull Boolean fromPrimitive(@NotNull Byte primitive, @NotNull PersistentDataAdapterContext ctx) {
        return primitive != 0;
    }

    @Override
    public @NotNull Class<Boolean> getComplexType() {
        return Boolean.class;
    }

    @Override
    public @NotNull Class<Byte> getPrimitiveType() {
        return Byte.class;
    }

    @Override
    public @NotNull Byte toPrimitive(@NotNull Boolean complex, @NotNull PersistentDataAdapterContext ctx) {
        return (byte) (complex ? 1 : 0);
    }
    
}
