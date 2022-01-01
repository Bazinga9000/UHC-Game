package xyz.baz9k.UHCGame.menu;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import static xyz.baz9k.UHCGame.util.ComponentUtils.*;

public class OptionValuedNode extends ValuedNode {

    private static final Key OPT_DESC_ID_FORMAT = new Key("menu.inv.%s.options");

    /**
     * @param parent Parent node
     * @param slot Slot of this node in parent's inventory
     * @param nodeName Name of the node
     * @param optMaterials Materials for the options supported
     */
    public OptionValuedNode(BranchNode parent, int slot, String nodeName, NodeItemStack.ItemProperties<Object> props, Material... optMaterials) {
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
    public boolean click(@NotNull Player p) {
        int currIndex = cfg.getInt(cfgKey());
        this.set(currIndex + 1);
        return true;
    }

    /**
     * @param i
     * @return the description for option i
     */
    private String optDesc(int i) {
        var langYaml = plugin.getLangManager().langYaml();
        var optDescs = langYaml.getStringList(OPT_DESC_ID_FORMAT.args(langKey()).key());
        if (optDescs.size() == 0) {
            return String.format(OPT_DESC_ID_FORMAT + "[%s]", langKey(), i);
        } else {
            return optDescs.get(i);
        }
    }
}
