package fish.tanks.tank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import fish.tanks.tank.FishStatus.FishDescription;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

// Compressed version of a fish status, used for storage in items without updating
public record FishStatusDataCarrier(float health, float maxHealth, float hunger, float maxHunger, int happiness, FishDescription status) {
    
    public static final Codec<FishStatusDataCarrier> CODEC = RecordCodecBuilder.create(in -> in.group(
        Codec.FLOAT.fieldOf("health").forGetter(carrier -> carrier.health),
        Codec.FLOAT.fieldOf("maxHealth").forGetter(carrier -> carrier.maxHealth),
        Codec.FLOAT.fieldOf("hunger").forGetter(carrier -> carrier.hunger),
        Codec.FLOAT.fieldOf("maxHunger").forGetter(carrier -> carrier.maxHunger),
        Codec.INT.fieldOf("happiness").forGetter(carrier -> carrier.happiness),
        FishStatus.DESC_CODEC.fieldOf("status").forGetter(carrier -> carrier.status)
    ).apply(in, FishStatusDataCarrier::new));

    public static final PacketCodec<RegistryByteBuf, FishStatusDataCarrier> PACKET_CODEC = PacketCodec.tuple(PacketCodecs.FLOAT, FishStatusDataCarrier::health, PacketCodecs.FLOAT, FishStatusDataCarrier::maxHealth, PacketCodecs.FLOAT, FishStatusDataCarrier::hunger, PacketCodecs.FLOAT, FishStatusDataCarrier::maxHunger, PacketCodecs.INTEGER, FishStatusDataCarrier::happiness, FishStatus.DESC_PACKET_CODEC, FishStatusDataCarrier::status, FishStatusDataCarrier::new);
}
