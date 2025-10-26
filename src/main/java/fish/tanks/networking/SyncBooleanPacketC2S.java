package fish.tanks.networking;

import fish.tanks.FishTanks;
import fish.tanks.blocks.FishTankBlockEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

// Sync a boolean, client to server
public record SyncBooleanPacketC2S(boolean bool, String name, BlockPos pos) implements CustomPayload {

    public static final Id<SyncBooleanPacketC2S> ID = new Id<>(Identifier.of(FishTanks.MOD_ID, "sync_boolean_packet_c2s"));
    public static final PacketCodec<RegistryByteBuf, SyncBooleanPacketC2S> PACKET = PacketCodec.tuple(PacketCodecs.BOOLEAN, SyncBooleanPacketC2S::bool, PacketCodecs.STRING, SyncBooleanPacketC2S::name, BlockPos.PACKET_CODEC, SyncBooleanPacketC2S::pos, SyncBooleanPacketC2S::new);

    public static void receive(SyncBooleanPacketC2S payload, ServerPlayNetworking.Context context) {
        Boolean bool = payload.bool;
        String name = payload.name;
        BlockPos pos = payload.pos;
    
        if (context.player().getWorld().getBlockEntity(pos) instanceof FishTankBlockEntity tank) {
            if (name.equals("shouldUpdateShape")) tank.setShouldUpdateShape(bool);
            else FishTanks.LOGGER.warn("Boolean name not found when syncing!");
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
