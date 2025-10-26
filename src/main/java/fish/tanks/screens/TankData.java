package fish.tanks.screens;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.math.BlockPos;

public record TankData(BlockPos pos, int size) {

    public static final PacketCodec<RegistryByteBuf, TankData> PACKET_CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, (data) -> data.pos(),
            PacketCodecs.INTEGER, (data) -> data.size(),
            TankData::new
        );
}
