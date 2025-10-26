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

// Sync a boolean, server to client
public record SyncBooleanPacketS2C(boolean bool, String name, BlockPos pos) implements CustomPayload {

    public static final Id<SyncBooleanPacketS2C> ID = new Id<>(Identifier.of(FishTanks.MOD_ID, "sync_boolean_packet_s2c"));
    public static final PacketCodec<RegistryByteBuf, SyncBooleanPacketS2C> PACKET = PacketCodec.tuple(PacketCodecs.BOOLEAN, SyncBooleanPacketS2C::bool, PacketCodecs.STRING, SyncBooleanPacketS2C::name, BlockPos.PACKET_CODEC, SyncBooleanPacketS2C::pos, SyncBooleanPacketS2C::new);

    public static void receive(SyncBooleanPacketS2C payload, ClientPlayNetworking.Context context) {
        Boolean bool = payload.bool;
        String name = payload.name;
        BlockPos pos = payload.pos;
    
        if (context.client().world.getBlockEntity(pos) instanceof FishTankBlockEntity tank) {
            if (name.equals("hasTop")) tank.setHasTop(bool);
            else if (name.equals("connectedTextures")) tank.setConnectedTextures(bool);
            else FishTanks.LOGGER.warn("Boolean name not found when syncing!");
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
