package com.kaloyandonev.serverfuser;


import com.kaloyandonev.serverfuser.Interconnect.InterconnectClient;
import com.kaloyandonev.serverfuser.config.ConfigNeo;
import com.kaloyandonev.serverfuser.helpers.transfer.TransferHandlers.EntityTransfer;
import com.kaloyandonev.serverfuser.helpers.transfer.TransferHandlers.PlayerTransfer;
import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;

@Mod(Constants.MOD_ID)
@EventBusSubscriber(modid = Constants.MOD_ID)
public class ServerFuser {
    static volatile boolean fatal = false;

    public static volatile InterconnectClient interconnectClient;


    public ServerFuser(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, ConfigNeo.SPEC);
        modEventBus.addListener(this::onCommonSetup);
    }

    private void onCommonSetup(FMLCommonSetupEvent event){
        try {
            interconnectClient =
                    new InterconnectClient(ConfigNeo.HOST.get(), ConfigNeo.PORT.get(), () -> {
                        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
                        if (s != null) s.halt(false);
                        else fatal = true;
                    });
            byte[] modSetHash = ModSetHash.compute(FMLPaths.MODSDIR.get());
            interconnectClient.start(modSetHash, ServerLifecycleHooks::getCurrentServer);
        } catch (Exception e) {
            Constants.LOG.error(e.toString());
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("serverfuser")
                        .requires(src -> src.hasPermission(4))
                        .then(Commands.literal("sendPlayerFile")
                                .then(Commands.argument("recieverUuid", UuidArgument.uuid())
                                    .then(Commands.argument("playerUuid", UuidArgument.uuid())
                                        .executes(ctx -> {
                                            UUID playerUuid = UuidArgument.getUuid(ctx, "playerUuid");
                                            UUID recieverUuid = UuidArgument.getUuid(ctx, "recieverUuid");
                                            PlayerTransfer.sendSaveFile(playerUuid, recieverUuid);
                                            return Command.SINGLE_SUCCESS;
                                        }))))
                        .then(Commands.literal("sendMob")
                                .then(Commands.argument("recieverUuid", UuidArgument.uuid())
                                    .then(Commands.argument("entity", EntityArgument.entity())
                                        .executes(ctx -> {
                                            UUID entityUuid = EntityArgument.getEntity(ctx, "entity").getUUID();
                                            UUID recieverUuid = UuidArgument.getUuid(ctx, "recieverUuid");
                                            EntityTransfer.sendEntity(entityUuid, recieverUuid);
                                            return Command.SINGLE_SUCCESS;
                                        }))))
        );
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        if (fatal) throw new IllegalStateException("ServerFuser: rejected by the interconnect, refusing to start");
    }
}