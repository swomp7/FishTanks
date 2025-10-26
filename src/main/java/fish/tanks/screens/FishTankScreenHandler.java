package fish.tanks.screens;

import fish.tanks.blocks.FishTankBlockEntity;
import fish.tanks.networking.ScrollPacket;
import fish.tanks.networking.SyncScreenPacket;
import fish.tanks.registries.FishTankComponents;
import fish.tanks.registries.FishTankItems;
import fish.tanks.registries.FishTankScreens;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.MathHelper;

public class FishTankScreenHandler extends ScreenHandler {

    private final Inventory tankInventory;
    private final Inventory inventory;
    public final FishTankBlockEntity blockEntity;
    private final int size;
    private float scrollPosition;
    private int slotsCount;
    private final PropertyDelegate delegate;
    
    public FishTankScreenHandler(int syncId, PlayerInventory inventory, TankData data) {
        this(syncId, inventory, inventory.player.getWorld().getBlockEntity(data.pos()), data.size(), new ArrayPropertyDelegate(2));
    }

    public FishTankScreenHandler(int syncId, PlayerInventory inventory2, BlockEntity blockEntity, int size, PropertyDelegate delegate) {
        super(FishTankScreens.FISH_TANK_SCREEN_HANDLER, syncId);
        checkSize(((Inventory)blockEntity), size + 2);
        this.blockEntity = (FishTankBlockEntity)blockEntity;
        this.size = size;
        this.tankInventory = (Inventory)this.blockEntity;
        this.inventory = new SimpleInventory(21);
        this.delegate = delegate;

        if (size > 21) {
            syncBlockToScreen();
            scrollPosition = 0.0F;
            this.scrollItems(0.0F);
        }

        addTankSlots(inventory2);
        addPlayerInventoryAndHotbar(inventory2);
        addProperties(delegate);
    }

    public boolean isFeeding() {
        return delegate.get(0) > 0;
    }

    public int getScaledFoodStatus() {
        int foodStatus = this.delegate.get(0);
        int foodLasts = this.delegate.get(1);
        int progressArrowSize = 14;

        return foodLasts != 0 && foodStatus != 0 ? foodStatus * progressArrowSize / foodLasts : 0;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int invSlot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(invSlot);
        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();
            if (originalStack.getItem().equals(Items.BUCKET) && originalStack.contains(FishTankComponents.FISH_STATUS) && slot instanceof SmallTankSlot) originalStack.remove(FishTankComponents.FISH_STATUS);
            if (invSlot == 1) {
                if (!this.insertItem(originalStack, slotsCount, slotsCount + 36, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickTransfer(originalStack, newStack);
            } else if (invSlot == 0 && isFeeding()) {
                return ItemStack.EMPTY;
            } else if (invSlot == 0 || (invSlot >= 2 && invSlot < slotsCount) ? !this.insertItem(originalStack, slotsCount, slotsCount + 36, true) : originalStack.getItem().equals(FishTankItems.FISH_FOOD) ? !this.insertItem(originalStack, 0, 1, false) ? !this.insertItem(originalStack, 2, slotsCount, false) : false : !this.insertItem(originalStack, 2, slotsCount, false)) {
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
    public boolean canUse(PlayerEntity var1) {
        return this.inventory.canPlayerUse(var1);
    }

    private void addPlayerInventoryAndHotbar(PlayerInventory playerInventory) {
        if (size <= 21) {
            for (int i = 0; i < 3; ++i) {
                for (int l = 0; l < 9; ++l) {
                    this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
                }
            }
            for (int i = 0; i < 9; ++i) {
                this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
            }
        } else {
            for (int i = 0; i < 3; ++i) {
                for (int l = 0; l < 9; ++l) {
                    this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18 + 1, 84 + i * 18));
                }
            }
            for (int i = 0; i < 9; ++i) {
                this.addSlot(new Slot(playerInventory, i, 8 + i * 18 + 1, 142));
            }
        }
    }

    // Add slots depending on the size of the tank
    private void addTankSlots(PlayerInventory playerInventory) {
        slotsCount += 2;
        if (size <= 21) {
            int rows = size <= 7 ? 1 : size <= 14 ? 2 : 3;
            this.addSlot(new TankFoodSlot(tankInventory, 0, 11, 18));
            this.addSlot(new TankOutputSlot(playerInventory.player, tankInventory, 1, 11, 54));
            for (int i = 0; i < rows - 1; i++) {
                for (int j = 0; j < 7; j++) {
                    this.addSlot(new SmallTankSlot(tankInventory, 2 + (i * 7) + j, 41 + (j * 18), 18 + (i * 18)));
                }
                slotsCount += 7;
            }
            for (int i = 0; i < size - ((rows - 1) * 7); i++) {
                this.addSlot(new SmallTankSlot(tankInventory, 2 + (rows - 1) * 7 + i, 41 + (i * 18), 18 + (rows - 1) * 18));
                slotsCount++;
            }
        } else {
            this.addSlot(new TankFoodSlot(tankInventory, 0, 12, 18));
            this.addSlot(new TankOutputSlot(playerInventory.player, tankInventory, 1, 12, 54));
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 7; j++) {
                    this.addSlot(new TankSlot(inventory, tankInventory, this, (i * 7) + j, 42 + (j * 18), 18 + (i * 18)));
                }
            }
            slotsCount += 21;
        }
    }

    public int getSlots() {
        return size;
    }

    // Allow scrolling if there are more slots in the tank than can be represented in the inventory at once (more than 21)

    protected float getScrollPosition(int row) {
        return MathHelper.clamp((float)row / (float)this.getOverflowRows(), 0.0F, 1.0F);
    }

    protected float getScrollPosition(float current, double amount) {
        return MathHelper.clamp(current - (float)(amount / (double)this.getOverflowRows()), 0.0F, 1.0F);
    }

    protected int getOverflowRows() {
        return MathHelper.ceilDiv(size, 7) - 3;
    }

    protected int getRow(float scroll) {
        return Math.max((int)((double)(scroll * (float)this.getOverflowRows()) + 0.5), 0);
    }

    public void scrollItems(float position) {
        setScroll(position);
        syncBlockToScreen();
        if (blockEntity.hasWorld()) {
            if (blockEntity.getWorld().isClient) {
                ClientPlayNetworking.send(new ScrollPacket(position, syncId));
                ClientPlayNetworking.send(new SyncScreenPacket(syncId));
            }
        }
        markDirty();
    }

    // Sync items to the block when slots are updated -- block and screen inventories are separate due to scrolling making them out of sync
    public void syncScreenToBlock() {
        int i = this.getRow(scrollPosition);
        for (int j = 0; j < 3; j++) {
            for (int k = 0; k < 7; k++) {
                int currentSlot = k + (j + i) * 7 + 2;
                if (currentSlot >= 0 && currentSlot < size + 2) {
                    tankInventory.setStack(currentSlot, inventory.getStack(k + j * 7));
                }
            }
        }
        markDirty();
    }

    // Sync updates of the block inventory to the screen inventory
    public void syncBlockToScreen() {
        int i = this.getRow(scrollPosition);
        for (int j = 0; j < 3; j++) {
            for (int k = 0; k < 7; k++) {
                int currentSlot = k + (j + i) * 7 + 2;
                if (currentSlot >= 0 && currentSlot < size + 2) {
                    inventory.setStack(k + j * 7, tankInventory.getStack(currentSlot));
                } else {
                    inventory.setStack(k + j * 7, ItemStack.EMPTY);
                }
            }
        }
        markDirty();
    }

    public int getRepresentative(int index) {
        return index + getRow(scrollPosition) * 7;
    }

    public void setScroll(float scroll) {
        scrollPosition = scroll;
    }

    public void markDirty() {
        tankInventory.markDirty();
        inventory.markDirty();
    }
}
