package xyz.baz9k.UHCGame.config;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import static xyz.baz9k.UHCGame.util.Utils.*;

public class OptionValuedNode extends ValuedNode {

    private OptionData[] optData;

    /**
     * @param parent Parent node
     * @param slot lot of this node in parent's inventory
     * @param stack Item stack of this node in parent's inventory
     * @param id The config ID for this node
     * @param optData An array of options that this node supports 
     */
    public OptionValuedNode(BranchNode parent, int slot, ItemStack stack, String id, OptionData... optData) {
        super(parent, slot, stack, ValuedNodeType.OPTION, id, false);
        this.optData = optData;
        updateItemStack();
    }

    @Override
    public void click(Player p) {
        int currIndex = cfg.getInt(id);
        cfg.set(id, (currIndex + 1) % optData.length);
    }

    public void updateItemStack() {
        int ind = cfg.getInt(id) % optData.length;
        OptionData dat = optData[ind];

        ItemMeta m = itemStack.getItemMeta();
        m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        List<Component> newDesc = new ArrayList<>(itemDesc);

        itemStack.setType(dat.getMaterial());
        for (int i = 0; i < optData.length; i++) {

            TextColor clr = i == ind ? NamedTextColor.GREEN : NamedTextColor.RED;
            newDesc.add(Component.text(optData[i].getName(), noDecoStyle(clr)));

        }

        m.lore(newDesc);
        itemStack.setItemMeta(m);
        // since updating the item does not update it in the inventory, parent has to
        parent.updateSlot(parentSlot);
    }
    
}
