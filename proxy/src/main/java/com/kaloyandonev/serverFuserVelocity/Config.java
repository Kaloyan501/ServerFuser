package com.kaloyandonev.serverFuserVelocity;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    public static ConfigurationNode loadConfig(Path dataDir) throws IOException {
        Files.createDirectories(dataDir);

        Path configFile = dataDir.resolve("serverfuser-proxy-config.yml");

        YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(configFile)
                .build();

        ConfigurationNode config;

        if (Files.exists(configFile)) {
            config = loader.load();
        } else {
            config = loader.createNode();
        }

        if (config.node("serverfuser-proxy", "port").virtual()) {
            config.node("serverfuser-proxy", "port").set(6666);
            loader.save(config);
        }

        return config;
    }
}