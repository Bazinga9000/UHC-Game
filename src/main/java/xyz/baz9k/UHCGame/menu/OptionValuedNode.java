package xyz.baz9k.UHCGame.menu;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

public class OptionValuedNode extends ValuedNode {

    private static final String OPT_DESC_ID_FORMAT = "xyz.baz9k.uhc.menu.inv.%s.options";

    /**
     * @param parent Parent node
     * @param slot lot of this node in parent's inventory
     * @param nodeName Node name, which is used to determine the ID
     * @param optMaterials Materials for the options supported
     */
    public OptionValuedNode(BranchNode parent, int slot, String nodeName, NodeItemStack.ItemProperties props, Material... optMaterials) {
        super(parent, slot, nodeName, props.mat(v -> optMaterials[(int) v]), ValuedNode.Type.OPTION, i -> (int) i % optMaterials.length);
        props.formatter(v -> this.optDesc((int) v))
            .extraLore(v -> {
                int current = (int) v;

                var extraLore = new ArrayList<Component>();
                for (int i = 0; i < optMaterials.length; i++) {
                    var clr = i == current ? NamedTextColor.GREEN : NamedTextColor.RED;
                    extraLore.add(Component.text(optDesc(i), noDeco(clr)));

                }
                return new NodeItemStack.ExtraLore(extraLore);
            });
    }

    @Override
    public void click(Player p) {
        int currIndex = cfg.getInt(cfgKey());
        this.set(currIndex + 1);
    }

    /**
     * @param i
     * @return the description for option i
     */
    private String optDesc(int i) {
        var langYaml = plugin.getLangManager().langYaml();
        var optDescs = langYaml.getStringList(String.format(OPT_DESC_ID_FORMAT, langKey()));
        if (optDescs.size() == 0) {
            return String.format(OPT_DESC_ID_FORMAT + "[%s]", langKey(), i);
        } else {
            return optDescs.get(i);
        }
    }
}
