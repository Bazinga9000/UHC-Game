package xyz.baz9k.UHCGame.config;

import org.bukkit.entity.Player;

public class OptionValuedNode extends ValuedNode {

    private OptionData[] optData;
    public OptionValuedNode(BranchNode parent, int slot, String id, OptionData... optData) {
        super(parent, slot, optData[0].getItemStack(), ValuedNodeType.OPTION, id);
        this.optData = optData;
    }

    @Override
    public void click(Player p) {
        int currIndex = cfg.getInt(id);
        cfg.set(id, (currIndex + 1) % optData.length);
    }

    public void updateItemStack() {
        // TODO
    }
    
}
