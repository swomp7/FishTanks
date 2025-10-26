package fish.tanks.screens;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.math.BlockPos;

public record FilterData(BlockPos pos) {
    public static final PacketCodec<RegistryByteBuf, FilterData> PACKET_CODEC = PacketCodec.tuple(BlockPos.PACKET_CODEC, FilterData::pos, FilterData::new);
}
