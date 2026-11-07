package com.kaloyandonev.serverfuser;


import com.kaloyandonev.serverfuser.Interconnect.InterconnectClient;
import com.kaloyandonev.serverfuser.helpers.transfer.SendSavefile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.nio.ByteBuffer;
import java.util.UUID;

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


        interconnectClient.start();

        UUID uuid = UUID.randomUUID();

        byte[] body = ByteBuffer.allocate(17)
                .put(MessageTypes.HANDSHAKE)
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits())
                .array();

        interconnectClient.send(body);

    }

    @SubscribeEvent // on the game event bus
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("serverfuser")
                        .requires(src -> src.hasPermission(4))          // op-only; it moves player data
                        .then(Commands.literal("sendPlayerFile")
                                .then(Commands.argument("uuid", UuidArgument.uuid())
                                        .executes(ctx -> {
                                            UUID uuid = UuidArgument.getUuid(ctx, "uuid");
                                            SendSavefile.sendSaveFile(uuid);
                                            return Command.SINGLE_SUCCESS;
                                        })))
        );
    }
}