package fish.tanks.registries;

import com.mojang.serialization.Codec;

import fish.tanks.FishTanks;
import fish.tanks.tank.FilterDataStorage;
import fish.tanks.tank.FishStatusDataCarrier;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class FishTankComponents {
    
    public static final ComponentType<FishStatusDataCarrier> FISH_STATUS = register("fish_status", new ComponentType.Builder<FishStatusDataCarrier>().codec(FishStatusDataCarrier.CODEC).packetCodec(FishStatusDataCarrier.PACKET_CODEC).build());
    public static final ComponentType<String> BUILDER_MODE = register("builder_mode", new ComponentType.Builder<String>().codec(Codec.STRING).packetCodec(PacketCodecs.STRING).build());
    public static final ComponentType<Integer> FOOD_STATUS = register("food_status", new ComponentType.Builder<Integer>().codec(Codec.INT).packetCodec(PacketCodecs.INTEGER).build());
    public static final ComponentType<Boolean> MOSTLY_FILLED = register("mostly_filled", new ComponentType.Builder<Boolean>().codec(Codec.BOOL).packetCodec(PacketCodecs.BOOLEAN).build());
    public static final ComponentType<Boolean> HALF_FILLED = register("half_filled", new ComponentType.Builder<Boolean>().codec(Codec.BOOL).packetCodec(PacketCodecs.BOOLEAN).build());
    public static final ComponentType<Boolean> PARTIALLY_FILLED = register("partially_filled", new ComponentType.Builder<Boolean>().codec(Codec.BOOL).packetCodec(PacketCodecs.BOOLEAN).build());
    public static final ComponentType<FilterDataStorage> FILTER_DATA = register("filter_data", new ComponentType.Builder<FilterDataStorage>().codec(FilterDataStorage.CODEC).packetCodec(FilterDataStorage.PACKET_CODEC).build());

    private static <T> ComponentType<T> register(String name, ComponentType<T> type) {
        return Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of(FishTanks.MOD_ID, name), type);
    }

    public static void register() {}
}
