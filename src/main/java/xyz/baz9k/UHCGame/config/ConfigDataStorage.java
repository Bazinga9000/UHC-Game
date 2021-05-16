package xyz.baz9k.UHCGame.config;

public class ConfigDataStorage {
    private ValuedNodeType type;
    private String data;

    public ConfigDataStorage(ValuedNodeType type, String data) {
        this.type = type;
        this.data = data;
    }

    public ValuedNodeType getType() {
        return type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
