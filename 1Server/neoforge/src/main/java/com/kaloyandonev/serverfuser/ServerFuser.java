package com.kaloyandonev.serverfuser;


import com.kaloyandonev.serverfuser.Interconnect.InterconnectClient;
import com.kaloyandonev.serverfuser.helpers.transfer.ShardTransfer;
import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.UUID;

import static com.kaloyandonev.serverfuser.config.Config.loadOrCreateShardId;

@Mod(Constants.MOD_ID)
@EventBusSubscriber(modid = Constants.MOD_ID)
public class ServerFuser {

    public static InterconnectClient interconnectClient = new InterconnectClient("127.0.0.1", 6666);

    public ServerFuser(IEventBus eventBus) {

        // This method is invoked by the NeoForge mod loader when it is ready
        // to load your mod. You can access NeoForge and Common code in this
        // project.

        // Use NeoForge to bootstrap the Common mod.
        Constants.LOG.info("Hello NeoForge world!");
        CommonClass.init();

        try {
            byte[] modSetHash = ModSetHash.compute(FMLPaths.MODSDIR.get());
            interconnectClient.start(modSetHash);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("serverfuser")
                        .requires(src -> src.hasPermission(4))
                        .then(Commands.literal("sendPlayerFile")
                                .then(Commands.argument("uuid", UuidArgument.uuid())
                                        .executes(ctx -> {
                                            UUID uuid = UuidArgument.getUuid(ctx, "uuid");
                                            ShardTransfer.sendSaveFile(uuid);
                                            return Command.SINGLE_SUCCESS;
                                        })))
                        .then(Commands.literal("sendMob")
                                .then(Commands.argument("entity", EntityArgument.entity())
                                        .executes(ctx -> {
                                            UUID uuid = EntityArgument.getEntity(ctx, "entity").getUUID();
                                            ShardTransfer.sendEntity(uuid);
                                            return Command.SINGLE_SUCCESS;
                                        })))
        );
    }
}