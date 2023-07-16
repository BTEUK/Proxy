package me.bteuk.proxy.config;

import me.bteuk.proxy.Proxy;

import java.io.File;
import java.io.IOException;

public class Config {

    private final ConfigurationFile config = new YamlConfigurationFile(getClass().getClassLoader().getResourceAsStream("config.yml"),
            new File(Proxy.getInstance().getDataFolder(), "config.yml"));

    public Config() throws IOException {
    }

    public String getString(String path) {
        return config.getString(path);
    }

    public int getInt(String path) {
        return config.getInt(path);
    }

    public boolean getBoolean(String path) {
        return config.getBoolean(path);
    }
}
