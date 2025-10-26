package fish.tanks.networking;

import fish.tanks.FishTanks;
import fish.tanks.screens.FishTankScreenHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

// Sync scrolling the screen of a large tank system (22+)
public record ScrollPacket(float scroll, int syncId) implements CustomPayload {

    public static final Id<ScrollPacket> ID = new Id<>(Identifier.of(FishTanks.MOD_ID, "scroll_packet"));
    public static final PacketCodec<RegistryByteBuf, ScrollPacket> PACKET = PacketCodec.tuple(PacketCodecs.FLOAT, ScrollPacket::scroll, PacketCodecs.INTEGER, ScrollPacket::syncId, ScrollPacket::new);

    public static void receive(ScrollPacket payload, ServerPlayNetworking.Context context) {
        float scroll = payload.scroll;
        int syncId = payload.syncId;
        ServerPlayerEntity player = context.player();
    
        if (player.currentScreenHandler.syncId == syncId && player.currentScreenHandler instanceof FishTankScreenHandler tank) {
            tank.setScroll(scroll);
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
