package fish.tanks.screens;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

public class TankSlot extends SmallTankSlot {

    private final Inventory blockInventory;
    private final FishTankScreenHandler handler;

    private final int index;

    public TankSlot(Inventory inventory, Inventory blockInventory, FishTankScreenHandler handler, int index, int x, int y) {
        super(inventory, index, x, y);
        this.blockInventory = blockInventory;
        this.handler = handler;
        this.index = index;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        if (handler.getRepresentative(index) >= blockInventory.size() - 2) return false;
        return super.canInsert(stack);
    }

    @Override
    public void markDirty() {
        handler.syncScreenToBlock();
        super.markDirty();
        blockInventory.markDirty();
    }

    @Override
    public boolean canBeHighlighted() {
        return handler.getRepresentative(index) >= 0 && handler.getRepresentative(index) < blockInventory.size() - 2;
    }
}
