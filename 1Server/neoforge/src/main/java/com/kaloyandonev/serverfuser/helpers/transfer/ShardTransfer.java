package com.kaloyandonev.serverfuser.helpers.transfer;

import com.kaloyandonev.serverfuser.Interconnect.InterconnectClient;
import com.kaloyandonev.serverfuser.MessageTypes;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class ShardTransfer {
    public static void sendSaveFile(UUID uuid){
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();

        if(s == null) return;

        s.execute(() -> {
           ServerPlayer p = s.getPlayerList().getPlayer(uuid);
           if (p == null) return;
            try {
                // 1. player NBT -> bytes
                CompoundTag tag = p.saveWithoutId(new CompoundTag());
                NbtUtils.addCurrentDataVersion(tag);
                var nbtOut = new ByteArrayOutputStream();
                NbtIo.writeCompressed(tag, nbtOut);
                byte[] nbt = nbtOut.toByteArray();

                // 2. checksum
                byte[] sha = MessageDigest.getInstance("SHA-256").digest(nbt);   // 32 bytes

                // 3. packet body: [ID][uuid][sha256][int nbtLen][nbt]
                var buf = new ByteArrayOutputStream(1 + 16 + 32 + 4 + nbt.length);
                var d = new DataOutputStream(buf);
                d.writeByte(MessageTypes.PLAYERDATA);
                d.writeLong(uuid.getMostSignificantBits());
                d.writeLong(uuid.getLeastSignificantBits());
                d.write(sha);
                d.writeInt(nbt.length);
                d.write(nbt);

                InterconnectClient.send(buf.toByteArray());
            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        });
    }

    public static void sendMob(UUID uuid){
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        if (s == null) return;

        try {
            CompoundTag tag = new CompoundTag();

            Entity entity = null;
            for (ServerLevel level: s.getAllLevels()) {
                entity = level.getEntity(uuid);
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

            var buf = new ByteArrayOutputStream(1 + 16 + 32 + 4 + nbt.length);
            var d = new DataOutputStream(buf);
            d.writeByte(MessageTypes.MOBDATA);
            d.writeLong(uuid.getMostSignificantBits());
            d.writeLong(uuid.getLeastSignificantBits());
            d.write(sha);
            d.writeInt(nbt.length);
            d.write(nbt);

            InterconnectClient.send(buf.toByteArray());
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
