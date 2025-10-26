package fish.tanks.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.block.BlockState;
import net.minecraft.block.PlantBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

// For compatibility with other mods: see if a plant can be placed on the floor block in the tank
@Mixin(PlantBlock.class)
public interface PlantGetterMixin {
    
    @Invoker("canPlantOnTop")
    boolean callCanPlantOnTop(BlockState floor, BlockView world, BlockPos pos);
}
