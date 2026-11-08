package com.kaloyandonev.serverfuser.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class Config {
    public static UUID loadOrCreateShardId(Path file) throws IOException {
        if (Files.exists(file)) {
            try { return UUID.fromString(Files.readString(file).trim()); }
            catch (IllegalArgumentException ignored) { }   // corrupt file: regenerate
        }
        UUID id = UUID.randomUUID();
        Files.createDirectories(file.getParent());
        Files.writeString(file, id.toString());
        return id;
    }
}
