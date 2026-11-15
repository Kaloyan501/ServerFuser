package com.kaloyandonev.serverfuser.Interconnect;

import com.kaloyandonev.serverfuser.MessageTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;


import java.io.ByteArrayInputStream;
import java.util.function.Consumer;

public class AfterTransferHandler {
    public static void spawnEntity(byte[] nbt, Consumer<Byte> ack, MinecraftServer s) {
        //MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        if (s == null) { ack.accept(MessageTypes.STATUS_INVALID_PLAYER); return; }   // pick/add a fitting status

        s.execute(() -> {
            byte status;
            try {
                CompoundTag tag = NbtIo.readCompressed(new ByteArrayInputStream(nbt), NbtAccounter.unlimitedHeap());
                ServerLevel level = s.overworld();                       // see the dimension note below
                Entity e = EntityType.loadEntityRecursive(tag, level, x -> x);
                status = (e != null && level.tryAddFreshEntityWithPassengers(e))
                        ? MessageTypes.STATUS_OK
                        : MessageTypes.STATUS_WRITE_FAILED;               // add a dedicated SPAWN_FAILED later
            } catch (Exception ex) {
                ex.printStackTrace();
                status = MessageTypes.STATUS_WRITE_FAILED;
            }
            ack.accept(status);                // send TRANSFER_ACK from here, after the result is known
        });
    }
}
