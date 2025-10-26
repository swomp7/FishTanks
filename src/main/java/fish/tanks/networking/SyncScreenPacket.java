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

// Sync the screen of a tank with the block
public record SyncScreenPacket(int syncId) implements CustomPayload {

    public static final Id<SyncScreenPacket> ID = new Id<>(Identifier.of(FishTanks.MOD_ID, "sync_screen_packet"));
    public static final PacketCodec<RegistryByteBuf, SyncScreenPacket> PACKET = PacketCodec.tuple(PacketCodecs.INTEGER, SyncScreenPacket::syncId, SyncScreenPacket::new);

    public static void receive(SyncScreenPacket payload, ServerPlayNetworking.Context context) {
        int syncId = payload.syncId;
        ServerPlayerEntity player = context.player();
    
        if (player.currentScreenHandler.syncId == syncId && player.currentScreenHandler instanceof FishTankScreenHandler tank) {
            tank.syncBlockToScreen();
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
