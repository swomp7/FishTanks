package fish.tanks.networking;

import fish.tanks.FishTanks;
import fish.tanks.blocks.FishTankBlockEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

// Sync the floor or plant of a tank
public record SyncBlocksPacket(ItemStack stack, String type, BlockPos pos) implements CustomPayload {

    public static final Id<SyncBlocksPacket> ID = new Id<>(Identifier.of(FishTanks.MOD_ID, "sync_blocks_packet"));
    public static final PacketCodec<RegistryByteBuf, SyncBlocksPacket> PACKET = PacketCodec.tuple(ItemStack.OPTIONAL_PACKET_CODEC, SyncBlocksPacket::stack, PacketCodecs.STRING, SyncBlocksPacket::type, BlockPos.PACKET_CODEC, SyncBlocksPacket::pos, SyncBlocksPacket::new);

    public static void receive(SyncBlocksPacket payload, ClientPlayNetworking.Context context) {
        ItemStack stack = payload.stack;
        String type = payload.type;
        BlockPos pos = payload.pos;
    
        if (context.client().world.getBlockEntity(pos) instanceof FishTankBlockEntity tank) {
            if (type.equals("floor")) tank.setFloor(stack);
            else if (type.equals("plant")) tank.setPlant(stack);
            else FishTanks.LOGGER.error("Invalid type {}! Must be \"floor\" or \"plant\"", type);
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
