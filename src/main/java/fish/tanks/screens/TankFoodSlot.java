package fish.tanks.screens;

import fish.tanks.registries.FishTankItems;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class TankFoodSlot extends Slot {

    public TankFoodSlot(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }
    
    @Override
    public boolean canInsert(ItemStack stack) {
        return stack.getItem().equals(FishTankItems.FISH_FOOD);
    }
}
