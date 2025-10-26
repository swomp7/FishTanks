package fish.tanks.networking;

import fish.tanks.FishTanks;
import fish.tanks.blocks.FishTankBlockEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

// Sync the plant scale and position in a tank
public record SyncPlantPacket(float scale, float x, float y, float z, BlockPos pos) implements CustomPayload {
    
    public static final Id<SyncPlantPacket> ID = new Id<>(Identifier.of(FishTanks.MOD_ID, "sync_plant_packet"));
    public static final PacketCodec<RegistryByteBuf, SyncPlantPacket> PACKET = PacketCodec.tuple(PacketCodecs.FLOAT, SyncPlantPacket::scale, PacketCodecs.FLOAT, SyncPlantPacket::x, PacketCodecs.FLOAT, SyncPlantPacket::y, PacketCodecs.FLOAT, SyncPlantPacket::z, BlockPos.PACKET_CODEC, SyncPlantPacket::pos, SyncPlantPacket::new);

    public static void receive(SyncPlantPacket payload, ClientPlayNetworking.Context context) {
        float scale = payload.scale;
        float[] xyz = {payload.x, payload.y, payload.z};
        BlockPos pos = payload.pos;
    
        if (context.client().world.getBlockEntity(pos) instanceof FishTankBlockEntity tank) {
            tank.setPlantScale(scale);
            tank.setPlantPos(xyz);
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
