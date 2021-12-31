package xyz.baz9k.UHCGame.menu;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import xyz.baz9k.UHCGame.Kit;
import xyz.baz9k.UHCGame.menu.NodeItemStack.ItemProperties;
import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

public class KitNode extends InventoryNode {

    private boolean grantXP = false;
    private NodeItemStack xpStack;

    public KitNode(@Nullable BranchNode parent, int slot, String nodeName, ItemProperties<?> props) {
        super(parent, slot, nodeName, props, 5, new ReserveSlots(9 * 5 - 4));

        this.fillReserved = Material.GRAY_STAINED_GLASS_PANE;
        xpStack = new NodeItemStack("kit_xp", 
        new ItemProperties<Boolean>(b -> b ? Material.EXPERIENCE_BOTTLE : Material.GLASS_BOTTLE)
            .useObject(() -> this.grantXP)
        );
    }

    @Override
    void initInventory() {
        super.initInventory();
        inventory.setItem(rsLeft(), xpStack);

        ItemStack saveStack = new NodeItemStack("kit_save", new ItemProperties<>(Material.STRUCTURE_BLOCK));
        inventory.setItem(rsRight() - 2, saveStack);
        updateInventory();
    }

    @Override
    void updateInventory() {
        // load currently used kit
        ItemStack[] contents = inventory.getContents();
        Arrays.fill(contents, 0, rsLeft(), null);
        Kit k = kit();

        ItemStack[] storage = k.storage();
        System.arraycopy(storage, 0, contents, 0, storage.length);
        inventory.setContents(contents);
        
        ItemStack[] armor = k.armor();
        for (int i = 0; i < armor.length; i++) {
            inventory.setItem(9 * 4 + i, armor[i]);
        }
        inventory.setItem(9 * 4 + 4, k.offhand());

        grantXP(k.xpLevels() > 0);
    }

    @Override
    protected int clickHandler(@NotNull Player p, int slot) {
        if (slot == rsLeft()) {
            grantXP(!grantXP);
            return 1;
        } else if (slot == rsRight() - 2) {
            plugin.saveResource("new_kit.yml", true);
            File f = new File(plugin.getDataFolder(), "new_kit.yml");
            YamlConfiguration kitCfg = YamlConfiguration.loadConfiguration(f);
            kitCfg.set("new_kit", kit());
            try {
                kitCfg.save(f);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            p.sendMessage(new Key("menu.inv.kit_save.succ").trans());
        }
        return 0;
    }

    @Override
    public void onClose(@NotNull Player p) {
        // extract kit, save it
        List<ItemStack> contents = Arrays.asList(inventory.getContents()).subList(0, rsLeft());
        
        ItemStack[] storage = contents.subList(0, 9 * 4).stream()
            .filter(Objects::nonNull)
            .toArray(ItemStack[]::new);
        ItemStack[] armor = contents.subList(9 * 4, 9 * 4 + 4).toArray(ItemStack[]::new);
        ItemStack offhand = contents.get(9 * 4 + 4);

        int xpLevels;
        if (grantXP) {
            xpLevels = kit().xpLevels();
            if (xpLevels == 0) xpLevels = 100;
        } else {
            xpLevels = 0;
        }

        plugin.getGameManager().kit(new Kit(storage, armor, offhand, xpLevels));
    }

    private void grantXP(boolean gxp) {
        grantXP = gxp;
        inventory.setItem(rsRight() - 4, xpStack.updateAll());
    }

    private Kit kit() {
        return plugin.getGameManager().kit();
    }
}
