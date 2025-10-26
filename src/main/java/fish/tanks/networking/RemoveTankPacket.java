package fish.tanks.networking;

import fish.tanks.FishTanks;
import fish.tanks.blocks.FishTankBlockEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

// Sync removing a tank
public record RemoveTankPacket(BlockPos brain, BlockPos remove) implements CustomPayload {
    
    public static final Id<RemoveTankPacket> ID = new Id<>(Identifier.of(FishTanks.MOD_ID, "remove_tank_packet"));
    public static final PacketCodec<RegistryByteBuf, RemoveTankPacket> PACKET = PacketCodec.tuple(BlockPos.PACKET_CODEC, RemoveTankPacket::brain, BlockPos.PACKET_CODEC, RemoveTankPacket::remove, RemoveTankPacket::new);

    public static void receive(RemoveTankPacket payload, ClientPlayNetworking.Context context) {
        BlockPos brain = payload.brain;
        BlockPos remove = payload.remove;
    
        if (context.client().world.getBlockEntity(brain) instanceof FishTankBlockEntity tank) {
           tank.removeTank(remove);
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}