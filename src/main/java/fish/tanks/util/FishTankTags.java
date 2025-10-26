package fish.tanks.util;

import fish.tanks.FishTanks;
import net.minecraft.item.Item;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class FishTankTags {

    public static final TagKey<Item> TANK_FLOOR_PLACEABLE = register("tank_floor_placeable", RegistryKeys.ITEM);
    public static final TagKey<Item> TANK_PLANTS_PLACEABLE = register("tank_plants_placeable", RegistryKeys.ITEM);
    public static final TagKey<Item> FISH_FOODS = register("fish_food_items", RegistryKeys.ITEM);
    
    private static <T> TagKey<T> register(String name, RegistryKey<Registry<T>> registry) {
        return TagKey.of(registry, Identifier.of(FishTanks.MOD_ID, name));
    }
}
