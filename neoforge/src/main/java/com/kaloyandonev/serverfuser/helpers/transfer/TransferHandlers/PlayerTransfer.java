package com.kaloyandonev.serverfuser.helpers.transfer.TransferHandlers;

import com.kaloyandonev.serverfuser.Interconnect.InterconnectClient;
import com.kaloyandonev.serverfuser.Messages.Helpers.Packets;
import com.kaloyandonev.serverfuser.Messages.PlayerData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerTransfer {
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
}
