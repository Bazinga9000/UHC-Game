package xyz.baz9k.UHCGame.config;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

public class OptionValuedNode extends ValuedNode {

    private Material[] optMaterials;

    private static final String OPT_DESC_ID_FORMAT = "xyz.baz9k.uhc.config.inv.%s.options";

    /**
     * @param parent Parent node
     * @param slot lot of this node in parent's inventory
     * @param nodeName Node name, which is used to determine the ID
     * @param optMaterials Materials for the options supported
     */
    public OptionValuedNode(BranchNode parent, int slot, String nodeName, NodeItemStack.ItemProperties props, Material... optMaterials) {
        super(parent, slot, nodeName, props.mat(v -> optMaterials[(int) v]), ValuedNode.Type.OPTION);
        this.optMaterials = optMaterials;
        updateItemStack();
    }

    @Override
    public void click(Player p) {
        int currIndex = cfg.getInt(id());
        this.set((currIndex + 1) % optMaterials.length);
    }

    /**
     * @return the currently selected option index
     */
    public int selectedIndex() {
        return cfg.getInt(id()) % optMaterials.length;
    }

    /**
     * @param i
     * @return the material for option i
     */
    public Material optMaterial(int i) {
        return optMaterials[i];
    }

    /**
     * @param i
     * @return the description for option i
     */
    public String optDesc(int i) {
        var optDescs = cfg.getStringList(String.format(OPT_DESC_ID_FORMAT, id()));
        if (optDescs.size() == 0) {
            return String.format(OPT_DESC_ID_FORMAT + "[%s]", id(), i);
        } else {
            return optDescs.get(i);
        }
    }

    public void updateItemStack() {
        if (optMaterials == null) return;
        int current = selectedIndex();
        itemStack.desc(optDesc(current));
        itemStack.setType(optMaterial(current));

        var extraLore = new ArrayList<Component>();
        for (int i = 0; i < optMaterials.length; i++) {
            TextColor clr = i == current ? NamedTextColor.GREEN : NamedTextColor.RED;
            extraLore.add(Component.text(optDesc(i), noDeco(clr)));

        }
        itemStack.extraLore(extraLore);
    }
    
}
