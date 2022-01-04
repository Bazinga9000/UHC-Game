package xyz.baz9k.UHCGame.util.drops;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

public final class BlockDropTransformer {
    private Set<Mapping> transforms = new HashSet<>();

    private record Mapping(Set<Material> blockMats, Set<Material> fromMats, Material toMat) {}
    
    private BlockDropTransformer add(Set<Material> b, Set<Material> f, Material t) {
        transforms.add(new Mapping(b, f, t));
        return this;
    }
    public BlockDropTransformer add(Material blockMat,      Material fromMat,      Material toMat) { return add(Set.of(blockMat),     Set.of(fromMat),     toMat); }
    public BlockDropTransformer add(Material blockMat,      Tag<Material> fromMat, Material toMat) { return add(Set.of(blockMat),     fromMat.getValues(), toMat); }
    public BlockDropTransformer add(Tag<Material> blockMat, Material fromMat,      Material toMat) { return add(blockMat.getValues(), Set.of(fromMat),     toMat); }
    public BlockDropTransformer add(Tag<Material> blockMat, Tag<Material> fromMat, Material toMat) { return add(blockMat.getValues(), fromMat.getValues(), toMat); }
    
    /**
     * Identify which transformer rule to use based on the block and item stack, 
     * then convert the stack to the "to" material.
     * @param b block material
     * @param s item stack
     */
    public void transform(Material b, ItemStack s) {
        for (Mapping m : transforms) {
            if (m.blockMats.contains(b) && m.fromMats().contains(s.getType())) {
                s.setType(m.toMat());
                break;
            }
        }
    }

    /**
     * Identify which transformer rule to use based on the block and item, 
     * then convert the item's stack to the "to" material.
     * @param b block material
     * @param it item drop
     */
    public void transform(Material b, Item it) {
        transform(b, it.getItemStack());
    }
}