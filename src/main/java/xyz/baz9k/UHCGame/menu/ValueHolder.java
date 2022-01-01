package xyz.baz9k.UHCGame.menu;

public interface ValueHolder {
    /**
     * @return the key in the config this node is linked to
     */
    public String cfgKey();

    /**
     * Sets the node's value to the object.
     * <p> This should make any necessary changes to the node and call the super method to set to config.
     * <p> If cfgKey() is null, this does not set to config.
     * @param o
     */
    public default void set(Object o) {
        String key = cfgKey();
        if (key != null) Node.cfg.set(cfgKey(), o);
    }

    /**
     * @return the value located in the config
     * <p> If cfgKey() is null, this returns null
     */
    public default Object get() {
        String key = cfgKey();
        return key != null ? Node.cfg.get(cfgKey()) : null;
    }
}
