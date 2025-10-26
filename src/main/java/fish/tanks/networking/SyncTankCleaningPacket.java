package fish.tanks.networking;

import fish.tanks.FishTanks;
import fish.tanks.blocks.FishTankBlockEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

// Sync when the tank has been cleaned
public record SyncTankCleaningPacket(BlockPos pos) implements CustomPayload {
    
    public static final Id<SyncTankCleaningPacket> ID = new Id<>(Identifier.of(FishTanks.MOD_ID, "sync_tank_cleaning_packet"));
    public static final PacketCodec<RegistryByteBuf, SyncTankCleaningPacket> PACKET = PacketCodec.tuple(BlockPos.PACKET_CODEC, SyncTankCleaningPacket::pos, SyncTankCleaningPacket::new);

    public static void receive(SyncTankCleaningPacket payload, ClientPlayNetworking.Context context) {
        BlockPos pos = payload.pos;
    
        if (context.client().world.getBlockEntity(pos) instanceof FishTankBlockEntity tank) {
           tank.clean();
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
