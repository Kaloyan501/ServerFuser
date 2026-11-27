package com.kaloyandonev.serverfuser.helpers.transfer.TransferHandlers;

import com.kaloyandonev.serverfuser.Constants;
import com.kaloyandonev.serverfuser.Interconnect.InterconnectClient;
import com.kaloyandonev.serverfuser.MessageTypes;
import com.kaloyandonev.serverfuser.Messages.EntityData;
import com.kaloyandonev.serverfuser.Messages.Helpers.Packets;
import com.kaloyandonev.serverfuser.Transfers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.kaloyandonev.serverfuser.MessageTypes.statusName;
import static com.kaloyandonev.serverfuser.helpers.transfer.TransferHelper.findEntity;

public class EntityTransfer {
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

            Constants.LOG.info("send: nbtLen={} sha={}", nbt.length, HexFormat.of().formatHex(sha));

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
            }).exceptionally(t -> {
                Constants.LOG.error("transfer {} gave up: {}", txId, t);
                return null;
            });

            InterconnectClient.send(Packets.encode(new EntityData(senderUuid, recieverUuid, txId, entityUuid, sha, nbt.length, nbt)));
        } catch(Exception e) {
            Constants.LOG.error(e.toString());
        }
    }
}
