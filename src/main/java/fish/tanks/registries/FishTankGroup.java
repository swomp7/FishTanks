package fish.tanks.registries;

import fish.tanks.FishTanks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;

public class FishTankGroup {
    
    public static final RegistryKey<ItemGroup> FISH_TANK_GROUP = FishTanks.makeKey(RegistryKeys.ITEM_GROUP, "fish_tank_group");

    public static void register() {

        Registry.register(Registries.ITEM_GROUP, FISH_TANK_GROUP, FabricItemGroup.builder().icon(() -> new ItemStack(FishTankBlocks.FISH_TANK)).displayName(Text.translatable("group.fish-tanks.fish_tank_group")).build());

        ItemGroupEvents.modifyEntriesEvent(FISH_TANK_GROUP).register(content -> {
            content.add(FishTankBlocks.FISH_TANK);
            content.add(FishTankItems.TANK_BUILDER);
            content.add(FishTankItems.FISH_FOOD);
            content.add(FishTankBlocks.FILTER);
            content.add(FishTankItems.FILTER_BAG);
            content.add(FishTankItems.DIRTY_FILTER_BAG);
        });
    }
}
