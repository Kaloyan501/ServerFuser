package com.kaloyandonev.serverfuser.helpers.transfer;

import com.kaloyandonev.serverfuser.Interconnect.InterconnectClient;
import com.kaloyandonev.serverfuser.MessageTypes;
import com.kaloyandonev.serverfuser.Messages.EntityData;
import com.kaloyandonev.serverfuser.Messages.Helpers.Packets;
import com.kaloyandonev.serverfuser.Messages.PlayerData;
import com.kaloyandonev.serverfuser.Transfers;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.kaloyandonev.serverfuser.MessageTypes.statusName;
import static com.kaloyandonev.serverfuser.ServerFuser.interconnectClient;
import com.kaloyandonev.serverfuser.config.Config;

public class ShardTransfer {

    private static Entity findEntity(MinecraftServer s, UUID uuid) {
        for (ServerLevel level : s.getAllLevels()) {
            Entity e = level.getEntity(uuid);
            if (e != null) return e;
        }
        return null;
    }

    public static void sendSaveFile(UUID playerUuid, UUID recieverUuid){
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        // Sender UUID is blank here as the ServerFuser interconnect will replace the UUID with the correct one and vouch for it on forward.
        // Avoids weird network shenanigans.
        UUID senderUuid = new UUID(0, 0);

        if(s == null) return;

        s.execute(() -> {
           ServerPlayer p = s.getPlayerList().getPlayer(playerUuid);
           if (p == null) return;
            try {
                CompoundTag tag = p.saveWithoutId(new CompoundTag());
                NbtUtils.addCurrentDataVersion(tag);
                var nbtOut = new ByteArrayOutputStream();
                NbtIo.writeCompressed(tag, nbtOut);
                byte[] nbt = nbtOut.toByteArray();

                byte[] sha = MessageDigest.getInstance("SHA-256").digest(nbt);   // 32 bytes

                long txId = ThreadLocalRandom.current().nextLong();

                InterconnectClient.send(Packets.encode(new PlayerData(senderUuid, recieverUuid, txId, playerUuid, sha, nbt.length, nbt)));
            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        });
    }

    public static void sendEntity(UUID entityUuid, UUID recieverUuid){
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        // Sender UUID is blank here as the ServerFuser interconnect will replace the UUID with the correct one and vouch for it on forward.
        // Avoids weird network shenanigans.
        UUID senderUuid = new UUID(0, 0);
        if (s == null) return;

        try {
            CompoundTag tag = new CompoundTag();

            Entity entity = null;
            for (ServerLevel level: s.getAllLevels()) {
                entity = level.getEntity(entityUuid);
                if (entity != null) break;
            }
            if (entity == null) return;

            entity = entity.getRootVehicle();
            if (!entity.save(tag)) return;

            NbtUtils.addCurrentDataVersion(tag);
            var nbtOut = new ByteArrayOutputStream();
            NbtIo.writeCompressed(tag, nbtOut);
            byte[] nbt = nbtOut.toByteArray();

            byte[] sha = MessageDigest.getInstance("SHA-256").digest(nbt);
            System.out.println("send: nbtLen=" + nbt.length + " sha=" + HexFormat.of().formatHex(sha));

            long txId = ThreadLocalRandom.current().nextLong();
            var result = Transfers.register(txId);

            result.thenAccept(status -> {
                if (status == MessageTypes.STATUS_OK) {
                    s.execute(() -> {                       // world access: server thread
                        Entity e = findEntity(s, entityUuid);  // same loop over s.getAllLevels() as before
                        if (e != null) e.discard();
                    });
                } else {
                    System.out.println("transfer " + txId + " failed: " + statusName(status));
                }
            }).exceptionally(t -> { System.out.println("transfer " + txId + " gave up: " + t); return null; });

            InterconnectClient.send(Packets.encode(new EntityData(senderUuid, recieverUuid, txId, entityUuid, sha, nbt.length, nbt)));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
