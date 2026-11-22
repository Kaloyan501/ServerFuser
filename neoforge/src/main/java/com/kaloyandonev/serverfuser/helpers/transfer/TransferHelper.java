package com.kaloyandonev.serverfuser.helpers.transfer;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public class TransferHelper {

    private static Entity findEntity(MinecraftServer s, UUID uuid) {
        for (ServerLevel level : s.getAllLevels()) {
            Entity e = level.getEntity(uuid);
            if (e != null) return e;
        }
        return null;
    }
}
