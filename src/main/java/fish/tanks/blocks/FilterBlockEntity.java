package fish.tanks.blocks;

import org.jetbrains.annotations.Nullable;

import fish.tanks.networking.SyncIntPacketS2C;
import fish.tanks.registries.FishTankBlocks;
import fish.tanks.registries.FishTankComponents;
import fish.tanks.registries.FishTankItems;
import fish.tanks.screens.FilterData;
import fish.tanks.screens.FilterScreenHandler;
import fish.tanks.tank.FilterDataStorage;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class FilterBlockEntity extends LootableContainerBlockEntity implements ExtendedScreenHandlerFactory<FilterData> {

    private int filterStatus = 0;
    private int maxFilterStatus = 0;
    private int boost = 0;

    private DefaultedList<ItemStack> inventory = DefaultedList.ofSize(1, ItemStack.EMPTY);
    private final PropertyDelegate delegate;

    public FilterBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(FishTankBlocks.FILTER_BE, blockPos, blockState);
        this.delegate = new PropertyDelegate() {

            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> filterStatus / 100;
                    case 1 -> maxFilterStatus / 100;
                    case 2 -> filterStatus % 100;
                    case 3 -> maxFilterStatus % 100;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 1: filterStatus = value * 100; break;
                    case 2: maxFilterStatus = value * 100; break;
                }
            }

            @Override
            public int size() {
                return 4;
            }
            
        };
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FilterData getScreenOpeningData(ServerPlayerEntity player) {
        return new FilterData(getPos());
    }

    @Override
    protected Text getContainerName() {
        return Text.translatable("block.fish-tanks.filter");
    }

    @Override
    protected DefaultedList<ItemStack> getHeldStacks() {
        return inventory;
    }

    @Override
    protected void setHeldStacks(DefaultedList<ItemStack> inventory) {
        this.inventory = inventory;
        markDirty();
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return new FilterScreenHandler(syncId, playerInventory, this, delegate);
    }

    @Override
    protected void writeData(WriteView view) {
        Inventories.writeData(view, inventory);
        view.putInt("filterStatus", filterStatus);
        view.putInt("maxFilterStatus", maxFilterStatus);
        view.putInt("boost", boost);
        super.writeData(view);
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        Inventories.readData(view, inventory);
        filterStatus = view.getInt("filterStatus", 0);
        maxFilterStatus = view.getInt("maxFilterStatus", 0);
        boost = view.getInt("boost", 0);
    }

    @Override
    public void markDirty() {
        if (!world.isClient) for (ServerPlayerEntity serverPlayerEntity : PlayerLookup.tracking((ServerWorld)world, this.getPos())) ServerPlayNetworking.send(serverPlayerEntity, new SyncIntPacketS2C(filterStatus, "filterStatus", this.getPos()));
        if (world.getBlockState(pos).contains(FilterBlock.FACING) && world.getBlockEntity(pos.offset(world.getBlockState(pos).get(FilterBlock.FACING))) instanceof FishTankBlockEntity tank) tank.updateFilterBoost();
        super.markDirty();
    }
    
    public void tick(World world, BlockPos pos, BlockState state) {
        if (world.isReceivingRedstonePower(pos) && isTankConnected() && hasCleanFilterBag()) {
            if (world.isClient) spawnBubbles(world, pos, state);
            ItemStack stack = inventory.get(0);
            assert stack.contains(FishTankComponents.FILTER_DATA);
            FilterDataStorage data = stack.get(FishTankComponents.FILTER_DATA);
            if (maxFilterStatus == 0) {
                filterStatus = data.status() * 10;
                maxFilterStatus = data.maxStatus() * 10;
                boost = data.strength() * 10000;
                markDirty();
            }
            if (filterStatus > 0) {
                filterStatus--;
                if (filterStatus % 10 == 0) {
                    stack.set(FishTankComponents.FILTER_DATA, new FilterDataStorage(filterStatus / 10, data.maxStatus(), data.strength()));
                    markDirty();
                }
            } else {
                filterStatus = 0;
                ItemStack output = new ItemStack(FishTankItems.DIRTY_FILTER_BAG);
                output.set(FishTankComponents.FILTER_DATA, new FilterDataStorage(0, data.maxStatus(), data.strength()));
                inventory.set(0, output);
                markDirty();
            }
        } else {
            if (boost != 0 || filterStatus != 0 || maxFilterStatus != 0) {
                filterStatus = 0;
                maxFilterStatus = 0;
                boost = 0;
                markDirty();
            }
        }
    }

    public void setFilterStatus(int status) {
        filterStatus = status;
    }

    private boolean hasCleanFilterBag() {
        return inventory.get(0).getItem().equals(FishTankItems.FILTER_BAG);
    }

    // Custom bubble animation coming out of the filter
    private void spawnBubbles(World world, BlockPos pos, BlockState state) {
        if (world.isClient && state.contains(FilterBlock.FACING)) {
            Random random = Random.create();
            BlockPos particlePos = pos.offset(state.get(FilterBlock.FACING));
            double[] offset = {0, 0};
            switch (state.get(FilterBlock.FACING)) {
                case NORTH: offset[0] = 0.4; offset[1] = 0.68; break;
                case SOUTH: offset[0] = 0.4; offset[1] = 0.12; break;
                case EAST: offset[0] = 0.1; offset[1] = 0.4; break;
                case WEST: offset[0] = 0.7; offset[1] = 0.4; break;
                default: break;
            }
            double d = (double)particlePos.getX() + offset[0] + (double)random.nextFloat() * 0.2;
            double e = (double)particlePos.getY() + 0.25 + (double)random.nextFloat() * 0.3;
            double f = (double)particlePos.getZ() + offset[1] + (double)random.nextFloat() * 0.2;
            world.addParticleClient(ParticleTypes.BUBBLE, d, e, f, 0.0, 0.01, 0.0);
        } 
    }

    private boolean isTankConnected() {
        if (world.getBlockState(pos).contains(FilterBlock.FACING)) return world.getBlockEntity(pos.offset(world.getBlockState(pos).get(FilterBlock.FACING))) instanceof FishTankBlockEntity;
        return false;
    }

    public int getBoost() {
        return boost;
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }
}
