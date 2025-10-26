package fish.tanks.tank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record FilterDataStorage(int status, int maxStatus, int strength) {
    
    public static final Codec<FilterDataStorage> CODEC = RecordCodecBuilder.create(in -> in.group(
        Codec.INT.fieldOf("status").forGetter(data -> data.status),
        Codec.INT.fieldOf("maxStatus").forGetter(data -> data.maxStatus),
        Codec.INT.fieldOf("strength").forGetter(data -> data.strength)
    ).apply(in, FilterDataStorage::new));

    public static final PacketCodec<RegistryByteBuf, FilterDataStorage> PACKET_CODEC = PacketCodec.tuple(
        PacketCodecs.INTEGER, FilterDataStorage::status,
        PacketCodecs.INTEGER, FilterDataStorage::maxStatus,
        PacketCodecs.INTEGER, FilterDataStorage::strength,
        FilterDataStorage::new
    );
}
