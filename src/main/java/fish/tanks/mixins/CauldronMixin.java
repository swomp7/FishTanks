package fish.tanks.mixins;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import fish.tanks.registries.FishTankComponents;
import fish.tanks.registries.FishTankItems;
import fish.tanks.tank.FilterDataStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

// Allow filter bags to be cleaned by adding them to vanilla logic
@Mixin(CauldronBehavior.class)
public interface CauldronMixin {
    
    @Inject(method = "registerBehavior", at = @At("TAIL"))
    private static void registerNewBehavior(CallbackInfo cb, @Local(ordinal = 1) Map<Item, CauldronBehavior> map2) {
        map2.put(FishTankItems.DIRTY_FILTER_BAG, CauldronMixin::cleanFilterBag);
        map2.put(FishTankItems.FILTER_BAG, CauldronMixin::cleanFilterBag);
    }

    private static ActionResult cleanFilterBag(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, ItemStack stack) {
		if (stack.contains(FishTankComponents.FILTER_DATA)) {
            if (!world.isClient) {
                FilterDataStorage data = stack.get(FishTankComponents.FILTER_DATA);
                ItemStack newStack = new ItemStack(FishTankItems.FILTER_BAG);
                newStack.set(FishTankComponents.FILTER_DATA, new FilterDataStorage(data.maxStatus(), data.maxStatus(), data.strength()));
                player.setStackInHand(hand, newStack);
                LeveledCauldronBlock.decrementFluidLevel(state, world, pos);
            }
            return ActionResult.SUCCESS;
        } else return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
	}
}
