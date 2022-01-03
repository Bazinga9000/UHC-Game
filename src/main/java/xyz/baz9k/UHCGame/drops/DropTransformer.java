package xyz.baz9k.UHCGame.drops;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

public record DropTransformer(Material fromMat, Material toMat) {
    /**
     * If the item stack has the from material, convert it to the to material.
     * @param s the stack
     */
    public void transform(ItemStack s) {
        if (s.getType() == fromMat) s.setType(toMat);
    }

    /**
     * If the item drop has the "from" material, convert it to the "to" material.
     * @param it the item
     */
    public void transform(Item it) {
        transform(it.getItemStack());
    }
}