package xyz.baz9k.UHCGame.config;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.md_5.bungee.api.ChatColor;

public class OptionValuedNode extends ValuedNode {

    private OptionData[] optData;
    public OptionValuedNode(BranchNode parent, int slot, ItemStack stack, String id, OptionData... optData) {
        super(parent, slot, stack, ValuedNodeType.OPTION, id);
        this.optData = optData;
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
        List<String> newDesc = new ArrayList<>(itemDesc);

        itemStack.setType(dat.getMaterial());
        for (int i = 0; i < optData.length; i++) {
            ChatColor clr;

            if (i == ind) {
                clr = ChatColor.GREEN;
            } else {
                clr = ChatColor.RED;
            }

            newDesc.add(clr + optData[i].getName());
        }

        m.setLore(newDesc);
        itemStack.setItemMeta(m);
        // since updating the item does not update it in the inventory, parent has to
        parent.updateSlot(parentSlot);
    }
    
}
