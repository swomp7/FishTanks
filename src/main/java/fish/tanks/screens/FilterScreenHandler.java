package fish.tanks.screens;

import fish.tanks.registries.FishTankScreens;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class FilterScreenHandler extends ScreenHandler {

    private final Inventory inventory;
    private final PropertyDelegate delegate;
    private static final int[] BUBBLE_PROGRESS = new int[]{29, 22, 18, 13, 9, 4, 0};

    public FilterScreenHandler(int syncId, PlayerInventory inventory, FilterData data) {
        this(syncId, inventory, inventory.player.getWorld().getBlockEntity(data.pos()), new ArrayPropertyDelegate(4));
    }

    public FilterScreenHandler(int syncId, PlayerInventory inventory2, BlockEntity blockEntity, PropertyDelegate delegate) {
        super(FishTankScreens.FILTER_SCREEN_HANDLER, syncId);
        this.inventory = (Inventory)blockEntity;
        checkSize(inventory, 1);
        this.delegate = delegate;

        this.addSlot(new Slot(inventory, 0, 80, 25));
        this.addPlayerSlots(inventory2, 8, 62);

        addProperties(delegate);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = (Slot)this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (invSlot == 0 ? !this.insertItem(originalStack, 1, 37, false) : !this.insertItem(originalStack, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
            if (originalStack.getCount() == newStack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTakeItem(player, originalStack);
        }
        
        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }
    
    @Override
    public void onClosed(PlayerEntity player) {
		super.onClosed(player);
		this.inventory.onClose(player);
	}

    public boolean isFiltering() {
        return delegate.get(0) != 0;
    }

    // Scale progress and status indicators to filter data: 
    
    public int getScaledFilterStatus() {
        int status = delegate.get(0) * 100 + delegate.get(2);
        int maxStatus = delegate.get(1) * 100 + delegate.get(3);
        int statusBarSize = 20;

        return maxStatus != 0 && status != 0 ? status * statusBarSize / maxStatus : 0;
    }

    public int[] getFilterStatus() {
        int status = delegate.get(0) * 100 + delegate.get(2);
        int maxStatus = delegate.get(1) * 100 + delegate.get(3);
        return new int[]{status, maxStatus};
    }

    public int getScaledBubblingProgress() {
        int status = delegate.get(0) * 100 + delegate.get(2);
        int maxStatus = delegate.get(1) * 100 + delegate.get(3);
        return BUBBLE_PROGRESS[(maxStatus - status) / 3 % 7];
    }
}
