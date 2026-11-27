package com.kaloyandonev.serverfuser.mixin;

import com.kaloyandonev.serverfuser.Constants;
import com.kaloyandonev.serverfuser.Interconnect.InterconnectClient;
import com.kaloyandonev.serverfuser.Messages.Chat;
import com.kaloyandonev.serverfuser.Messages.Helpers.Packets;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.UUID;
import java.util.function.Predicate;

//import com.kaloyandonev.serverfuser.MessageTypes;


@Mixin(PlayerList.class)
public abstract class BroadcastChatMessage {

    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(
            method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void serverfuser$replaceChat(PlayerChatMessage message,
                                         Predicate<ServerPlayer> shouldFilterMessageTo,
                                         @Nullable ServerPlayer sender,
                                         ChatType.Bound boundChatType,
                                         CallbackInfo ci) {

        UUID uuid = sender != null ? sender.getUUID() : new UUID(0L, 0L);
        String name = sender != null ? sender.getGameProfile().getName() : "Server";
        String chatTypeKey = boundChatType.chatType().unwrapKey()
                .map(k -> k.location().toString())
                .orElse("minecraft:chat");

        try {
            Chat packet = new Chat(uuid, name, chatTypeKey, Component.Serializer.toJson(message.decoratedContent(), this.server.registryAccess()));
            InterconnectClient.send(Packets.encode(packet));
        } catch (IOException e) {
            Constants.LOG.error(e.toString());
        }

        ci.cancel();
    }
}

