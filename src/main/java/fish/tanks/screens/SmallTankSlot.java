package fish.tanks.screens;

import fish.tanks.registries.FishTankComponents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;

public class SmallTankSlot extends Slot {

    public SmallTankSlot(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }
    
    @Override
    public void onTakeItem(PlayerEntity player, ItemStack stack) {
        if (stack.getItem().equals(Items.BUCKET) && stack.contains(FishTankComponents.FISH_STATUS)) {
            stack.remove(FishTankComponents.FISH_STATUS);
        }
        super.onTakeItem(player, stack);
    }
}
