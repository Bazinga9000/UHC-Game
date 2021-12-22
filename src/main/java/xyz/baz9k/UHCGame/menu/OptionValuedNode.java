package xyz.baz9k.UHCGame.menu;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

public class OptionValuedNode extends ValuedNode {

    private Material[] optMaterials;

    private static final String OPT_DESC_ID_FORMAT = "xyz.baz9k.uhc.menu.inv.%s.options";

    /**
     * @param parent Parent node
     * @param slot lot of this node in parent's inventory
     * @param nodeName Node name, which is used to determine the ID
     * @param optMaterials Materials for the options supported
     */
    public OptionValuedNode(BranchNode parent, int slot, String nodeName, NodeItemStack.ItemProperties props, Material... optMaterials) {
        super(parent, slot, nodeName, props.mat(v -> optMaterials[(int) v]), ValuedNode.Type.OPTION);
        props.formatter(v -> this.optDesc((int) v))
            .extraLore(v -> {
                int current = (int) v % optMaterials.length;

                var extraLore = new ArrayList<Component>();
                for (int i = 0; i < optMaterials.length; i++) {
                    var clr = i == current ? NamedTextColor.GREEN : NamedTextColor.RED;
                    extraLore.add(Component.text(optDesc(i), noDeco(clr)));

                }
                return new NodeItemStack.ExtraLore(extraLore);
            });
        this.optMaterials = optMaterials;
    }

    @Override
    public void click(Player p) {
        int currIndex = cfg.getInt(id());
        this.set((currIndex + 1) % optMaterials.length);
    }

    /**
     * @param i
     * @return the description for option i
     */
    private String optDesc(int i) {
        var optDescs = cfg.getStringList(String.format(OPT_DESC_ID_FORMAT, id()));
        if (optDescs.size() == 0) {
            return String.format(OPT_DESC_ID_FORMAT + "[%s]", id(), i);
        } else {
            return optDescs.get(i);
        }
    }
}
