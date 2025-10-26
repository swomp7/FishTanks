package fish.tanks.registries;

import fish.tanks.FishTanks;
import fish.tanks.blocks.FilterBlock;
import fish.tanks.blocks.FilterBlockEntity;
import fish.tanks.blocks.FishTankBlock;
import fish.tanks.blocks.FishTankBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class FishTankBlocks {

    public static final Block FISH_TANK = registerBlock("fish_tank", new FishTankBlock(AbstractBlock.Settings.copy(Blocks.GLASS).strength(0.6f).registryKey(FishTanks.makeKey(RegistryKeys.BLOCK, "fish_tank"))));
    public static final BlockEntityType<FishTankBlockEntity> FISH_TANK_BE = registerBE("fish_tank", FishTankBlockEntity::new, FISH_TANK);

    public static final Block FILTER = registerBlock("filter", new FilterBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).registryKey(FishTanks.makeKey(RegistryKeys.BLOCK, "filter")).nonOpaque()));
    public static final BlockEntityType<FilterBlockEntity> FILTER_BE = registerBE("filter", FilterBlockEntity::new, FILTER);

    private static Block registerBlock(String name, Block block) {
        Registry.register(Registries.ITEM, Identifier.of(FishTanks.MOD_ID, name), new BlockItem(block, new Item.Settings().registryKey(FishTanks.makeKey(RegistryKeys.ITEM, name))));
        return Registry.register(Registries.BLOCK, Identifier.of(FishTanks.MOD_ID, name), block);
    }

    private static <T extends BlockEntity> BlockEntityType<T> registerBE(String name, FabricBlockEntityTypeBuilder.Factory<T> factory, Block block  ) {
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(FishTanks.MOD_ID, name), FabricBlockEntityTypeBuilder.create(factory, block).build());
    }

    public static void register() {}
}
