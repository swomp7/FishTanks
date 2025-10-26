package fish.tanks.networking;

import fish.tanks.FishTanks;
import fish.tanks.blocks.FilterBlockEntity;
import fish.tanks.blocks.FishTankBlockEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

// Sync an integer, server to client
public record SyncIntPacketS2C(int num, String name, BlockPos pos) implements CustomPayload {

    public static final Id<SyncIntPacketS2C> ID = new Id<>(Identifier.of(FishTanks.MOD_ID, "sync_int_packet_s2c"));
    public static final PacketCodec<RegistryByteBuf, SyncIntPacketS2C> PACKET = PacketCodec.tuple(PacketCodecs.INTEGER, SyncIntPacketS2C::num, PacketCodecs.STRING, SyncIntPacketS2C::name, BlockPos.PACKET_CODEC, SyncIntPacketS2C::pos, SyncIntPacketS2C::new);

    public static void receive(SyncIntPacketS2C payload, ClientPlayNetworking.Context context) {
        int num = payload.num;
        String name = payload.name;
        BlockPos pos = payload.pos;
    
        if (context.client().world.getBlockEntity(pos) instanceof FishTankBlockEntity tank) {
            if (name.equals("dirtyTime")) tank.setDirtyTime(num);
            else if (name.equals("floorLevel")) tank.setFloorLevel(num);
            else FishTanks.LOGGER.warn("Integer name not found when syncing!");
        } else if (context.client().world.getBlockEntity(pos) instanceof FilterBlockEntity filter) {
            if (name.equals("filterStatus")) filter.setFilterStatus(num);
            else FishTanks.LOGGER.warn("Integer name not found when syncing!");
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
