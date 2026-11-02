package com.kaloyandonev.serverfuser;


import ca.weblite.objc.Message;
import com.kaloyandonev.serverfuser.Interconnect.InterconnectClient;
import net.minecraft.network.protocol.PacketType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import java.nio.ByteBuffer;
import java.util.UUID;

@Mod(Constants.MOD_ID)
public class ExampleMod {

    public static InterconnectClient interconnectClient = new InterconnectClient("127.0.0.1", 6666);

    public ExampleMod(IEventBus eventBus) {

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
}