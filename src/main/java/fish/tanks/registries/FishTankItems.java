package fish.tanks.registries;

import fish.tanks.FishTanks;
import fish.tanks.items.FilterBagItem;
import fish.tanks.items.FishFoodItem;
import fish.tanks.items.TankBuilderItem;
import fish.tanks.tank.FilterDataStorage;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class FishTankItems {
    
    public static final Item FISH_FOOD = register("fish_food", new FishFoodItem(new Item.Settings().maxCount(1).component(FishTankComponents.FOOD_STATUS, 64).registryKey(FishTanks.makeKey(RegistryKeys.ITEM, "fish_food"))));
    public static final Item TANK_BUILDER = register("tank_builder", new TankBuilderItem(new Item.Settings().registryKey(FishTanks.makeKey(RegistryKeys.ITEM, "tank_builder")).maxCount(1).maxDamage(64).component(FishTankComponents.BUILDER_MODE, "REMOVE_FLOOR")));
    public static final Item FILTER_BAG = register("filter_bag", new FilterBagItem(new Item.Settings().registryKey(FishTanks.makeKey(RegistryKeys.ITEM, "filter_bag")).maxCount(1).component(FishTankComponents.FILTER_DATA, new FilterDataStorage(10000, 10000, 1))));
    public static final Item DIRTY_FILTER_BAG = register("dirty_filter_bag", new FilterBagItem(new Item.Settings().registryKey(FishTanks.makeKey(RegistryKeys.ITEM, "dirty_filter_bag")).maxCount(1).component(FishTankComponents.FILTER_DATA, new FilterDataStorage(0, 10000, 1))));

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(FishTanks.MOD_ID, name), item);
    }

    public static void register() {}
}
