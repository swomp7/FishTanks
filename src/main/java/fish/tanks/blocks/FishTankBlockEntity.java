package fish.tanks.blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import fish.tanks.FishTanks;
import fish.tanks.mixins.FishGetterMixin;
import fish.tanks.networking.SyncBooleanPacketC2S;
import fish.tanks.networking.SyncIntPacketS2C;
import fish.tanks.networking.SyncListsPacket;
import fish.tanks.registries.FishTankBlocks;
import fish.tanks.registries.FishTankComponents;
import fish.tanks.registries.FishTankItems;
import fish.tanks.screens.FishTankScreenHandler;
import fish.tanks.screens.TankData;
import fish.tanks.tank.FishStatus;
import fish.tanks.tank.FishStatusDataCarrier;
import fish.tanks.tank.FishStatus.FishDescription;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Bucketable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.EntityBucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.Uuids;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

public class FishTankBlockEntity extends LootableContainerBlockEntity implements ExtendedScreenHandlerFactory<TankData>, SidedInventory {

    // Tank System
    private int connected = 1;
    private boolean isBrain = false, hasBrain = false;
    private BlockPos brainPos;
    private List<BlockPos> tanks = new ArrayList<>();
    private boolean init;
    private List<BlockPos> pendingRemovals = new ArrayList<>(); // Which tanks should be removed
    private List<BlockPos> floorables = new ArrayList<>(); // Which tanks can have a floor
    private List<BlockPos> filters = new ArrayList<>(); // Where there are filters
    private int startup = 0;

    // Brain Tank Functionality
    private int foodLasts = 5000;
    private int foodStatus = 0;
    private int fishCount = 0;
    private int dirtyStatus = 0;
    private int dirtyTime = -1, dirtyLowerBound = 120000, dirtyUpperBound = 160000, dirtyTimeStored = -1;
    private boolean tankIsDirty = false;
    private int tickCounter = 0;

    // Rendering
    private boolean connectedTextures = false;
    private boolean[] topRender = new boolean[8];
    private boolean[] sideRender = new boolean[8];
    private boolean hasTop = true;
    private boolean shouldUpdateShape = true;
    private VoxelShape shape = FishTankBlock.SHAPE_SINGLE_NO_FLOOR_WT;
    private ItemStack floorBlock = ItemStack.EMPTY;
    private int floorLevel = 1;
    private ItemStack plantBlock = ItemStack.EMPTY;
    private float plantScale = 0;
    private float[] plantXYZ = {0, 0, 0};

    // Networking and NBT
    private DefaultedList<ItemStack> inventory = DefaultedList.ofSize(connected + 2, ItemStack.EMPTY); // Tank Inventory
    private final PropertyDelegate delegate; 
    private DefaultedList<Optional<UUID>> spawned = DefaultedList.ofSize(connected, Optional.empty()); // Store what has been spawned
    private DefaultedList<ItemStack> buckets = DefaultedList.ofSize(connected, ItemStack.EMPTY); // Store where there are buckets in the inventory and how many
    private DefaultedList<Optional<UUID>> alreadySpawned = DefaultedList.ofSize(connected, Optional.empty()); // Backup copy of what has been spawned
    private DefaultedList<Optional<FishStatus>> statuses = DefaultedList.ofSize(connected, Optional.empty()); // Store the status of each fish in each slot

    public FishTankBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(FishTankBlocks.FISH_TANK_BE, blockPos, blockState);

        this.delegate = new PropertyDelegate() {

            @Override
            public int get(int index) {
                return switch(index) {
                    case 0 -> foodStatus;
                    case 1 -> foodLasts;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> foodStatus = value;
                    case 1 -> foodLasts = value;
                }
            }

            @Override
            public int size() {
                return 2;
            }
            
        };

        connected = 1;
        init = true;
    }


    // Brain Tank Functionality

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        if (isBrain) return new FishTankScreenHandler(syncId, playerInventory, this, connected, delegate);
        else if (brainPos != null && world.getBlockEntity(brainPos) instanceof FishTankBlockEntity tank) return new FishTankScreenHandler(syncId, playerInventory, tank, tank.size() - 2, tank.getDelegate());
        else return null;
    }

    @Override
    protected Text getContainerName() {
        return Text.translatable("block.fish-tanks.fish_tank");
    }

    @Override
    public DefaultedList<ItemStack> getHeldStacks() {
        if (isBrain) return inventory;
        return DefaultedList.ofSize(0, ItemStack.EMPTY);
    }

    @Override
    protected void setHeldStacks(DefaultedList<ItemStack> inventory) {
        this.inventory = inventory;
        markDirty();
    }

    @Override
    public TankData getScreenOpeningData(ServerPlayerEntity player) {
        if (isBrain) return new TankData(this.getPos(), connected);
        else if (brainPos != null && world.getBlockEntity(brainPos) instanceof FishTankBlockEntity tank) {
            return new TankData(brainPos, tank.size() - 2);
        } else return null;
    }

    // Store data on log out
    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);

        if (init) return;
        view.putInt("init", 1);

        // Individual
        view.put("FloorBlock", ItemStack.OPTIONAL_CODEC, floorBlock);
        view.putInt("FloorLevel", floorLevel);
        view.put("PlantBlock", ItemStack.OPTIONAL_CODEC, plantBlock);
        view.putFloat("plantScale", plantScale);
        view.putFloat("plantX", plantXYZ[0]);
        view.putFloat("plantY", plantXYZ[1]);
        view.putFloat("plantZ", plantXYZ[2]);
        view.putBoolean("IsBrain", isBrain);
        view.putBoolean("HasBrain", hasBrain);
        if (brainPos != null) view.put("BrainPos", BlockPos.CODEC, brainPos);
        view.putBoolean("IsDirty", tankIsDirty);
        view.putBoolean("ConnectedTextures", connectedTextures);
        view.putBoolean("HasTop", hasTop);

        // Brain Only
        if (isBrain) {
            view.putInt("Connected", connected);
            view.putInt("FoodLasts", foodLasts);
            view.putInt("FoodStatus", foodStatus);
            view.putInt("FishCount", fishCount);
            view.putInt("DirtyTime", dirtyTime);
            view.putInt("DirtyTimeStored", dirtyTimeStored);
            view.putInt("DirtyStatus", dirtyStatus);

            writeList(view, inventory, "tankInventory", ItemStack.OPTIONAL_CODEC);
            writeList(view, listToDefaultedList(tanks, getPos()), "tanks", BlockPos.CODEC);
            writeList(view, listToDefaultedList(floorables, getPos()), "floorables", BlockPos.CODEC);
            writeList(view, listToDefaultedList(filters, getPos()), "filters", BlockPos.CODEC);
            if (!pendingRemovals.isEmpty()) {
                view.putInt("prCheck", 1);
                writeList(view, listToDefaultedList(pendingRemovals, getPos()), "pendingRemovals", BlockPos.CODEC);
            }
            writeOptionalList(view, spawned, "spawned", Uuids.CODEC);
            writeList(view, buckets, "buckets", ItemStack.OPTIONAL_CODEC);
            writeOptionalList(view, alreadySpawned, "alreadySpawned", Uuids.CODEC);
            writeOptionalList(view, statuses, "statuses", FishStatus.CODEC);
        }
    }

    // Recover data on login
    @Override
    protected void readData(ReadView view) {
        if (view.getInt("init", -1) == -1) return;
        // Individual
        floorBlock = view.read("FloorBlock", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        floorLevel = view.getInt("FloorLevel", 1);
        plantBlock = view.read("PlantBlock", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        plantScale = view.getFloat("plantScale", 0);
        plantXYZ[0] = view.getFloat("plantX", 0);
        plantXYZ[1] = view.getFloat("plantY", 0);
        plantXYZ[2] = view.getFloat("plantZ", 0);
        isBrain = view.getBoolean("IsBrain", false);
        hasBrain = view.getBoolean("HasBrain", false);
        brainPos = view.read("BrainPos", BlockPos.CODEC).orElse(null);
        tankIsDirty = view.getBoolean("IsDirty", false);
        connectedTextures = view.getBoolean("ConnectedTextures", false);
        hasTop = view.getBoolean("HasTop", true);

        // Brain Only
        if (isBrain) {
            connected = view.getInt("Connected", 1);
            foodLasts = view.getInt("FoodLasts", 5000);
            foodStatus = view.getInt("FoodStatus", 0);
            fishCount = view.getInt("FishCount", 0);
            dirtyTime = view.getInt("DirtyTime", -1);
            dirtyTimeStored = view.getInt("DirtyTimeStored", -1);
            dirtyStatus = view.getInt("DirtyStatus", 0);

            inventory = readList(view, "tankInventory", ItemStack.EMPTY, ItemStack.OPTIONAL_CODEC);
            tanks = defaultedListToList(readList(view, "tanks", getPos(), BlockPos.CODEC));
            floorables = defaultedListToList(readList(view, "floorables", getPos(), BlockPos.CODEC));
            filters = defaultedListToList(readList(view, "filters", getPos(), BlockPos.CODEC));
            if (view.getInt("prCheck", -1) != -1) pendingRemovals = defaultedListToList(readList(view, "pendingRemovals", getPos(), BlockPos.CODEC));
            spawned = readOptionalList(view, "spawned", Uuids.CODEC);
            buckets = readList(view, "buckets", ItemStack.EMPTY, ItemStack.OPTIONAL_CODEC);
            alreadySpawned = readOptionalList(view, "alreadySpawned", Uuids.CODEC);
            statuses = readOptionalList(view, "statuses", FishStatus.CODEC);
        }

        init = false;
        super.readData(view);
    }

    public void tick(World world, BlockPos pos, BlockState state) {
        // Individual

        if (init) initTankData(null); // Init tank data (separate to avoid null issues)

        // If shape should update, make a new custom shape based on surroundings
        if (shouldUpdateShape && world != null) {
            if (world.isClient) ClientPlayNetworking.send(new SyncBooleanPacketC2S(true, "shouldUpdateShape", pos));
            makeShape(world, this.getPos());
            shouldUpdateShape = false;
        }
        BlockState state2 = world.getBlockState(pos);
        if (state2.getBlock() instanceof FishTankBlock) world.setBlockState(pos, state2.with(FishTankBlock.IS_DIRTY, tankIsDirty));

        if (!isBrain) return;

        // Counters fish being removed on startup by accident
        if (startup < 10) startup++;

        // Brain Tank only (to reduce lag and unnecessary processing)

        tickCounter++;
        boolean hasFood = foodIsLoaded(), hasFish = fishAreLoaded();

        if (hasFish) {
            // Set the time until the tank becomes dirty
            if (dirtyTime == -1 && world != null && !world.isClient) {
                Random random = Random.create();
                int tankSize = tanks.size() == 0 ? 1 : tanks.size();
                dirtyLowerBound = (int)(120000 * Math.pow(0.85, (((double)fishCount / (double)tankSize) * fishCount) - 1));
                dirtyUpperBound = (int)(160000 * Math.pow(0.85, (((double)fishCount / (double)tankSize) * fishCount) - 1));
                dirtyTimeStored = random.nextBetween(dirtyLowerBound, dirtyUpperBound);
                updateFilterBoost();
                for (ServerPlayerEntity serverPlayerEntity : PlayerLookup.tracking((ServerWorld)world, pos)) {
                    ServerPlayNetworking.send(serverPlayerEntity, new SyncIntPacketS2C(dirtyTimeStored, "dirtyTime", pos));
                }
            }
            // If the dirty timer is up, make the tank dirty
            if ((dirtyStatus >= dirtyTime && dirtyTime > -1) || tankIsDirty) {
                setDirty(true);
                if (dirtyStatus > dirtyTime || dirtyStatus <= 0) dirtyStatus = dirtyTime;
                if (dirtyStatus < dirtyTime) dirtyStatus++;
                if (state2.getBlock() instanceof FishTankBlock) {
                    world.setBlockState(pos, state2.with(FishTankBlock.IS_DIRTY, true));
                }
            // Otherwise, tick the dirty timer
            } else dirtyStatus++;
        }

        // Tick the food timer if there is food in the tank, and remove food if necessary
        if (hasFood && hasFish) {
            if (bagReturnCanHold()) {
                foodStatus++;
                if (foodStatus >= foodLasts) {
                    foodStatus = 0;
                    emptyFoodBag();
                }
            }
        }

        // Detect the entities in the tank and process accordingly
        if (world != null) {
            if (!world.isClient) {
                detectEntities((ServerWorld)world);
            }
        }

        // Process status changes every 10 ticks (0.5 seconds) -- lessened to reduce lag
        if (tickCounter % 10 == 0) {
            for (Optional<FishStatus> status : statuses) if (status.isPresent())  {
                status.get().tickEntity(hasFood, tankIsDirty, tickCounter, hasDecor(), world);
            }
            if (tickCounter >= 200) tickCounter = 0;
        }

        // Remvoe blocks set for removal (left to the end to avoid issues with finding nonexistent blocks)
        if (!pendingRemovals.isEmpty()) {
            for (BlockPos removePos : pendingRemovals) brainRemoveTank(removePos);
            pendingRemovals.clear();
        }
    }

    // Reduce amount of food in food back and change texture accordingly to visually represent fullness
    private void emptyFoodBag() {
        ItemStack foodSlot = inventory.get(0);
        if (foodSlot.contains(FishTankComponents.FOOD_STATUS) && foodSlot.get(FishTankComponents.FOOD_STATUS) > 1) {
            foodSlot.set(FishTankComponents.FOOD_STATUS, foodSlot.get(FishTankComponents.FOOD_STATUS) - 1);
            int status = foodSlot.get(FishTankComponents.FOOD_STATUS);
            if (status <= 16 && !foodSlot.contains(FishTankComponents.PARTIALLY_FILLED)) foodSlot.set(FishTankComponents.PARTIALLY_FILLED, true);
            else if (status > 16 && foodSlot.contains(FishTankComponents.PARTIALLY_FILLED)) foodSlot.remove(FishTankComponents.PARTIALLY_FILLED); 
            if (status > 16 && status <= 32 && !foodSlot.contains(FishTankComponents.HALF_FILLED)) foodSlot.set(FishTankComponents.HALF_FILLED, true);
            else if ((status > 32 || status <= 16) && foodSlot.contains(FishTankComponents.HALF_FILLED)) foodSlot.remove(FishTankComponents.HALF_FILLED); 
            if (status > 32 && status <= 48 && !foodSlot.contains(FishTankComponents.MOSTLY_FILLED)) foodSlot.set(FishTankComponents.MOSTLY_FILLED, true);
            else if ((status > 48 || status <= 32) && foodSlot.contains(FishTankComponents.MOSTLY_FILLED)) foodSlot.remove(FishTankComponents.MOSTLY_FILLED); 
        } else {
            Inventories.splitStack(inventory, 0, 1);
            inventory.set(1, new ItemStack(Items.BUNDLE));
        }

        markDirty();
    }

    // Check if the tank has food
    private boolean foodIsLoaded() {
        ItemStack stack = this.inventory.get(0);
        return stack.getItem().equals(FishTankItems.FISH_FOOD) && stack.getCount() > 0;
    }

    // Check if there are any fish in the tank
    private boolean fishAreLoaded() {
        int fishCounter = 0;
        for (int i = 0; i < connected; i++) {
            if (i + 2 >= inventory.size()) return false;
            if (isFish(inventory.get(i + 2))) {
                fishCounter++;
                buckets.set(i, inventory.get(i + 2));
            } else if (buckets.get(i) != ItemStack.EMPTY) {
                buckets.set(i, ItemStack.EMPTY);
            }
        }
        fishCount = fishCounter;
        return fishCounter > 0;
    }

    private boolean isFish(ItemStack stack) {
        return stack.getItem() instanceof EntityBucketItem;
    }

    private boolean bagReturnCanHold() {
        ItemStack stack = inventory.get(1);
        return stack.isEmpty();
    }

    @Override
    public void onBlockReplaced(BlockPos pos, BlockState oldState) {
        // Prepare for brain deletion
        if (!this.getWorld().isClient && isBrain) {
            ServerWorld world = (ServerWorld)this.getWorld();
            for (int i = 0; i < connected; i++) {
                Optional<UUID> id = spawned.get(i);
                if (id.isPresent()) if (checkIfSpawned(world, id)) {
                    removeFish(world, id, i);
                }
                spawned.set(i, Optional.empty());
                alreadySpawned.set(i, Optional.empty());
                buckets.set(i, ItemStack.EMPTY);
                statuses.set(i, Optional.empty());
                markDirty();
            }
        }
        if (isBrain) super.onBlockReplaced(pos, oldState);
    }

    // Detect the fish of the tank
    private void detectEntities(ServerWorld world) {
        if (startup < 10) return;
        for (int i = 0; i < connected; i++) {
            Optional<UUID> id = spawned.get(i);
            if (buckets.get(i) != ItemStack.EMPTY) {
                // If the fish is not spawned
                if (!checkIfSpawned(world, id)) {

                    // If the fish should be spawned but is not, get rid of it and declare it dead or missing (if someone removed it manually, it would be missing)
                    if (alreadySpawned.get(i).isPresent()) {
                        alreadySpawned.set(i, Optional.empty());
                        ItemStack newStack = new ItemStack(Items.BUCKET, 1);
                        Optional<FishStatus> status = statuses.get(i);
                        if (status.isPresent()) {
                            FishStatus status1 = status.get();
                            newStack.set(FishTankComponents.FISH_STATUS, new FishStatusDataCarrier(status1.getHealth(), status1.getMaxHealth(), status1.getHunger(), status1.getMaxHunger(), status1.getHappiness(), status1.getStatus() == FishDescription.DEAD ? FishDescription.DEAD : FishDescription.MISSING));
                        }
                        inventory.set(i + 2, newStack);
                        buckets.set(i, ItemStack.EMPTY);
                        statuses.set(i, Optional.empty());
                        markDirty();

                    // If the fish has never spawned yet, spawn it
                    } else {
                        spawnFish(world, i);
                    }
                
                // Tick the fish if it is spawned and has been found
                } else {
                    updateEntity(world, id.get(), i);
                }

            // If the bucket is gone, remove the fish from the tank
            } else if (id.isPresent()) {
                if (checkIfSpawned(world, id)) removeFish(world, id, i);
                spawned.set(i, Optional.empty());
                alreadySpawned.set(i, Optional.empty());
                statuses.set(i, Optional.empty());
                markDirty();
            }
        }
    }

    // Tick the entity, its status, and its data in the tank
    private void updateEntity(ServerWorld world, UUID id, int index) {
        Entity fish = world.getEntity(id);
        if (fish == null) {
            FishTanks.LOGGER.warn("Fish {} not found when it should have been!", id);
            return;
        }
        if (fish instanceof Bucketable bucketable) {
            ItemStack newStack = inventory.get(index + 2);
            Optional<FishStatus> status = statuses.get(index);
            if (status.isPresent()) {
                FishStatus status1 = status.get();
                FishStatusDataCarrier carrier = new FishStatusDataCarrier(status1.getHealth(), status1.getMaxHealth(), status1.getHunger(), status1.getMaxHunger(), status1.getHappiness(), status1.getStatus());
                newStack.set(FishTankComponents.FISH_STATUS, carrier);
            } else statuses.set(index, Optional.of(new FishStatus(((LivingEntity)fish).getHealth(), ((LivingEntity)fish).getMaxHealth(), 10.0F, 50, fish.getUuid())));
            bucketable.copyDataToStack(newStack);
            inventory.set(index + 2, newStack);
            buckets.set(index, newStack);
        }
    }

    // Remove a fish from the tank
    private void removeFish(ServerWorld world, Optional<UUID> id, int index) {
        Entity fish = world.getEntity(id.get());
        if (fish != null) {
            fish.remove(RemovalReason.DISCARDED);
            spawned.set(index, Optional.empty());
            buckets.set(index, ItemStack.EMPTY);
            statuses.set(index, Optional.empty());
            markDirty();
        }
        else FishTanks.LOGGER.warn("There was an error removing the entity {}!", id);
    }

    private boolean checkIfSpawned(ServerWorld World, Optional<UUID> id) {
        if (!id.isPresent()) return false;
        if (world.getEntity(id.get()) == null) return false;
        return true;
    }

    // Spawn a new fish in the tank with any necessary info that its item may be carrying
    private void spawnFish(ServerWorld world, int index) {
        ItemStack fish = buckets.get(index);
        BlockPos center = index < tanks.size() ? tanks.get(index) : this.getPos();
        if (fish.getItem() instanceof EntityBucketItem bucketItem) {
            Vec3d spawnPos = Vec3d.ofCenter(center).subtract(0, 0.3, 0);
            MobEntity mobEntity = (MobEntity)((FishGetterMixin)bucketItem).entityType().create(world, EntityType.copier(world, fish, (LivingEntity)null), pos, SpawnReason.BUCKET, true, false);
            if (mobEntity == null) return;
            mobEntity.refreshPositionAndAngles(spawnPos, 0, 0);

            float hunger = 10.0F;
            int happiness = 50;

            // Add info from item
            if (fish.contains(FishTankComponents.FISH_STATUS)) {
                FishStatusDataCarrier carrier = fish.get(FishTankComponents.FISH_STATUS);
                hunger = carrier.hunger();
                happiness = carrier.happiness();
            }

            if (mobEntity instanceof Bucketable bucketable) {
                NbtComponent nbtComponent = (NbtComponent)fish.getOrDefault(DataComponentTypes.BUCKET_ENTITY_DATA, NbtComponent.DEFAULT);
                bucketable.copyDataFromNbt(nbtComponent.copyNbt());
                bucketable.setFromBucket(true);
            }

            // Add to tank lists
            spawned.set(index, Optional.of(mobEntity.getUuid()));
            world.spawnEntity(mobEntity);
            mobEntity.playAmbientSound();
            alreadySpawned.set(index, Optional.of(mobEntity.getUuid()));
            statuses.set(index, Optional.of(new FishStatus(mobEntity.getMaxHealth(), mobEntity.getMaxHealth(), hunger, happiness, mobEntity.getUuid())));
        } else {
            FishTanks.LOGGER.warn("Missing bucket item at index {}!", index);
            buckets.set(index, ItemStack.EMPTY);
        }
        markDirty();
        dirtyTime = -1;
    }

    public void setDirty(boolean value) {
        this.tankIsDirty = value;
        markDirty();
    }

    public boolean isDirty() {
        return tankIsDirty;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[0];
    }

    public boolean isFeeding() {
        return foodStatus > 0;
    }

    // Call to clean the tank one iteration
    public int clean() {
        dirtyStatus -= dirtyTime / (9 + tanks.size());
        if (dirtyStatus <= 0) {
            dirtyStatus = 0;
            setDirty(false);
            dirtyTime = -1;
        }
        return (int)(100 * ((dirtyTime - dirtyStatus) / (double)dirtyTime));
    }

    public boolean hasFloor() {
        return !floorBlock.isEmpty();
    }

    public void setFloor(ItemStack stack) {
        floorBlock = stack;
        markDirty();
    }

    public ItemStack removeFloor() {
        if (!hasFloor()) return ItemStack.EMPTY;
        ItemStack old = floorBlock;
        floorBlock = ItemStack.EMPTY;
        markDirty();
        return old;
    }

    public ItemStack getFloor() {
        return floorBlock;
    }

    public int getFloorLevel() {
        if (world.getBlockEntity(getPos().up()) instanceof FishTankBlockEntity) return floorLevel;
        return 1;
    }

    public void setFloorLevel(int level) {
        floorLevel = level;
        makeShape(world, pos);
    }

    public boolean hasPlant() {
        return !plantBlock.isEmpty();
    }

    public void setPlant(ItemStack stack) {
        plantBlock = stack;
        markDirty();
    }

    public ItemStack removePlant() {
        if (!hasPlant()) return ItemStack.EMPTY;
        ItemStack old = plantBlock;
        plantBlock = ItemStack.EMPTY;
        markDirty();
        return old;
    }

    public ItemStack getPlant() {
        return plantBlock;
    }
    
    public int hasDecor() {
        int floorCount = 0;
        int plantCount = 0;
        List<BlockPos> floorables = new ArrayList<>();
        for (BlockPos pos : tanks) {
            if (world.getBlockEntity(pos) instanceof FishTankBlockEntity tank && tank.isFloorable()) {
                floorables.add(pos);
                if (tank.hasFloor()) floorCount++;
                if (tank.hasPlant()) plantCount++;
            }
        }
        int count = 0;
        double twoFifths = (floorables.size() * 2) / 5;
        if (floorCount / twoFifths >= 1) count++;
        if (plantCount / twoFifths >= 1) count++;
        return count;
    }
    
    
    // NBT and Networking

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }

    // Used to store lists on log out
    private <T> void writeOptionalList(WriteView view, DefaultedList<Optional<T>> list, String name, Codec<T> codec) {
        int length = list.size();
        view.putInt(name + "Size", length);
        for (int i = 0; i < length; i++) {
            if (list.get(i).isPresent()) view.put(name + i, codec, list.get(i).get());
        }
    }

    // Used to store lists on log out
    private <T> void writeList(WriteView view, DefaultedList<T> list, String name, Codec<T> codec) {
        int length = list.size();
        view.putInt(name + "Size", length);
        for (int i = 0; i < length; i++) {
            view.put(name + i, codec, list.get(i));
        }
    }

    // Used to retrieve lists on log out
    private <T> DefaultedList<Optional<T>> readOptionalList(ReadView view, String name, Codec<T> codec) {
        int length = view.getInt(name + "Size", connected);
        DefaultedList<Optional<T>> list = DefaultedList.ofSize(length, Optional.empty());
        for (int i = 0; i < length; i++) {
            list.set(i, view.read(name + i, codec));
        }
        return list;
    }

    // Used to retrieve lists on log out
    private <T> DefaultedList<T> readList(ReadView view, String name, @Nullable T obj, Codec<T> codec) {
        int length = view.getInt(name + "Size", connected);
        DefaultedList<T> list = DefaultedList.ofSize(length, obj);
        for (int i = 0; i < length; i++) {
            list.set(i, view.read(name + i, codec).orElse(obj));
        }
        return list;
    }
    
    // Set all of the tank lists to the parameters
    public void setLists(DefaultedList<Optional<UUID>> spawned, DefaultedList<ItemStack> buckets, DefaultedList<Optional<UUID>> alreadySpawned, DefaultedList<Optional<FishStatus>> statuses, DefaultedList<ItemStack> inventory) {
        this.spawned = spawned;
        this.buckets = buckets;
        this.alreadySpawned = alreadySpawned;
        this.statuses = statuses;
        this.inventory = inventory;
    }
    
    // Sync all lists between the server and the client
    public void syncLists(ServerWorld world) {
        List<UUID> spawned = new ArrayList<UUID>();
        List<UUID> alreadySpawned = new ArrayList<UUID>();
        List<FishStatusDataCarrier> statuses = new ArrayList<FishStatusDataCarrier>();
        
        this.spawned.forEach(value -> {
            if (value.isPresent()) spawned.add(value.get());
        });
        this.alreadySpawned.forEach(value -> {
            if (value.isPresent()) alreadySpawned.add(value.get());
        });
        this.statuses.forEach(value -> {
            if (value.isPresent()) {
                FishStatus status = value.get();
                statuses.add(new FishStatusDataCarrier(status.getHealth(), status.getMaxHealth(), status.getHunger(), status.getMaxHunger(), status.getHappiness(), status.getStatus()));
            }
        });
        
        for (ServerPlayerEntity serverPlayerEntity : PlayerLookup.tracking(world, this.getPos())) {
            ServerPlayNetworking.send(serverPlayerEntity, new SyncListsPacket(spawned, makeListOfElements(this.spawned), buckets, alreadySpawned, makeListOfElements(this.alreadySpawned), statuses, makeListOfElements(this.statuses), inventory, this.getPos()));
        }
    }
    
    // Sync changes between the server and client when the tank is updated, and handle any other updates
    @Override
    public void markDirty() {
        if (world != null) {
            if (!world.isClient && isBrain) {
                syncLists((ServerWorld)world);
            }
            if (isBrain) {
                for (BlockPos pos : tanks) {
                    if (getWorld().getBlockEntity(pos) instanceof FishTankBlockEntity tank) tank.updateTank();
                }
            }
        }
        foodLasts = (int)(5000 * Math.pow(0.95, fishCount - 1));
        super.markDirty();
    }

    // Change the time until the tank is dirty again
    public void setDirtyTime(int value) {
        dirtyTimeStored = value;
        updateFilterBoost();
    }
    
    /**
     * Returns a list where it contains the position of each non-optional element.
     * At the end, adds the original list length.
     * @param <T>
     * @param list
     * @return
     */
    public <T> List<Integer> makeListOfElements(DefaultedList<Optional<T>> list) {
        List<Integer> newList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) if (list.get(i).isPresent()) newList.add(i);
        newList.add(list.size());
        return newList;
    }
    
    
    // Rendering
    
    public float getPlantScale() {
        return plantScale;
    }

    public void setPlantScale(float scale) {
        plantScale = scale;
        markDirty();
    }

    public float[] getPlantPos() {
        return plantXYZ;
    }

    // Set the rendering position of the plant in the tank
    public void setPlantPos(float[] pos) {
        if (pos.length == 3) plantXYZ = pos;
        else {
            FishTanks.LOGGER.warn("Invalid plant position! Must be an array with a size of 3 as {x, y, z}!");
            plantXYZ[0] = plantScale / 2;
            plantXYZ[2] = plantScale / 2;
        }
        markDirty();
    }
    
    public boolean hasConnectedTextures() {
        return connectedTextures;
    }

    public boolean isFloorable() {
        if (brainPos != null && world.getBlockEntity(brainPos) instanceof FishTankBlockEntity brain) return brain.floorables.contains(this.getPos());
        return false;
    }

    public void setConnectedTextures(boolean value) {
        this.connectedTextures = value;
        markDirty();
    }

    public boolean hasTop() {
        return hasTop;
    }

    public void setHasTop(boolean value) {
        hasTop = value;
        markDirty();
    }

    private boolean isTank(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() instanceof FishTankBlock;
    }
    
    // For custom tank glass rendering of the sides and bottom, determine where there should be glass and where not
    public HashMap<Identifier, Float[]> makeTextureIDs(BlockPos up, BlockPos right, BlockPos down, BlockPos left, BlockPos upright, BlockPos downright, BlockPos downleft, BlockPos upleft) {
        sideRender[0] = isTank(world, up);
        sideRender[1] = isTank(world, right);
        sideRender[2] = isTank(world, down);
        sideRender[3] = isTank(world, left);
        sideRender[4] = isTank(world, upright) && sideRender[0] && sideRender[1];
        sideRender[5] = isTank(world, downright) && sideRender[1] && sideRender[2];
        sideRender[6] = isTank(world, downleft) && sideRender[2] && sideRender[3];
        sideRender[7] = isTank(world, upleft) && sideRender[3] && sideRender[0];
        HashMap<Identifier, Float[]> ids = new HashMap<>();
        if (!sideRender[0] || isDirty()) ids.put(texID("connected_tank/" + (sideRender[0] && isDirty() ? "algae_up" : "up")), new Float[]{1f/16f, 15f/16f, 15f/16f, 1f});
        if (!sideRender[1] || isDirty()) ids.put(texID("connected_tank/" + (sideRender[1] && isDirty() ? "algae_right" : "right")), new Float[]{15f/16f, 1f, 1f/16f, 15f/16f});
        if (!sideRender[2] || isDirty()) ids.put(texID("connected_tank/" + (sideRender[2] && isDirty() ? "algae_down" : "down")), new Float[]{1f/16f, 15f/16f, 0f, 1f/16f});
        if (!sideRender[3] || isDirty()) ids.put(texID("connected_tank/" + (sideRender[3] && isDirty() ? "algae_left" : "left")), new Float[]{0f, 1f/16f, 1f/16f, 15f/16f});
        if (!sideRender[4] || isDirty()) ids.put(texID("connected_tank/" + (sideRender[4] && isDirty() ? "algae_upright" : sideRender[1] ? "upright_light" : "upright")), new Float[]{15f/16f, 1f, 15f/16f, 1f});
        if (!sideRender[5] || isDirty()) ids.put(texID("connected_tank/" + (sideRender[5] && isDirty() ? "algae_downright" : "downright")), new Float[]{15f/16f, 1f, 0f, 1f/16f});
        if (!sideRender[6] || isDirty()) ids.put(texID("connected_tank/" + (sideRender[6] && isDirty() ? "algae_downleft" : "downleft")), new Float[]{0f, 1f/16f, 0f, 1f/16f});
        if (!sideRender[7] || isDirty()) ids.put(texID("connected_tank/" + (sideRender[7] && isDirty() ? "algae_upleft" : "upleft")), new Float[]{0f, 1f/16f, 15f/16f, 1f});
        return ids;
    }
    
    // For custom tank glass rendering of the top, determine where there should be glass and where not
    public HashMap<Identifier, Float[]> makeTopTextureIDs() {
        topRender[0] = isTank(world, pos.north());
        topRender[1] = isTank(world, pos.east());
        topRender[2] = isTank(world, pos.south());
        topRender[3] = isTank(world, pos.west());
        topRender[4] = isTank(world, pos.north().east()) && topRender[0] && topRender[1];
        topRender[5] = isTank(world, pos.south().east()) && topRender[1] && topRender[2];
        topRender[6] = isTank(world, pos.south().west()) && topRender[2] && topRender[3];
        topRender[7] = isTank(world, pos.north().west()) && topRender[3] && topRender[0];
        HashMap<Identifier, Float[]> ids = new HashMap<>();
        if (!topRender[0]) ids.put(texID("tank_top/north"), new Float[]{3f/16f, 13f/16f, 13f/16f, 1f});
        if (!topRender[1]) ids.put(texID("tank_top/east"), new Float[]{13f/16f, 1f, 3f/16f, 13f/16f});
        if (!topRender[2]) ids.put(texID("tank_top/south"), new Float[]{3f/16f, 13f/16f, 0f, 3f/16f});
        if (!topRender[3]) ids.put(texID("tank_top/west"), new Float[]{0f, 3f/16f, 3f/16f, 13f/16f});
        if (!topRender[4]) ids.put(texID("tank_top/" + (topRender[0] ? connectedTextures ? "north_" : "up_" : "") + (topRender[1] ? connectedTextures ? "east_" : "right_" : "") + "northeast"), new Float[]{13f/16f, 1f, 13f/16f, 1f});
        if (!topRender[5]) ids.put(texID("tank_top/" + (topRender[1] ? connectedTextures ? "east_" : "right_" : "") + (topRender[2] ? connectedTextures ? "south_" : "down_" : "") + "southeast"), new Float[]{13f/16f, 1f, 0f, 3f/16f});
        if (!topRender[6]) ids.put(texID("tank_top/" + (topRender[2] ? connectedTextures ? "south_" : "down_" : "") + (topRender[3] ? connectedTextures ? "west_" : "left_" : "") + "southwest"), new Float[]{0f, 3f/16f, 0f, 3f/16f});
        if (!topRender[7]) ids.put(texID("tank_top/" + (topRender[0] ? connectedTextures ? "north_" : "up_" : "") + (topRender[3] ? connectedTextures ? "west_" : "left_" : "") + "northwest"), new Float[]{0f, 3f/16f, 13f/16f, 1f});
        return ids;
    }
    
    private Identifier texID(String name) {
        return Identifier.of(FishTanks.MOD_ID, "block/" + name + (isDirty() ? "_dirty" : ""));
    }
    
    // Construct a custom block hit box depending on connected tanks
    public void makeShape(World world, BlockPos pos) {
        if (world == null) return;
        VoxelShape[] shapes = new VoxelShape[]{VoxelShapes.empty(), VoxelShapes.empty(), VoxelShapes.empty(), VoxelShapes.empty(), VoxelShapes.empty(), VoxelShapes.empty()};
        if (!isTank(world, pos.north())) shapes[0] = FishTankBlock.NORTH_WALL;
        if (!isTank(world, pos.south())) shapes[1] = FishTankBlock.SOUTH_WALL;
        if (!isTank(world, pos.east())) shapes[2] = FishTankBlock.EAST_WALL;
        if (!isTank(world, pos.west())) shapes[3] = FishTankBlock.WEST_WALL;
        if (!isTank(world, pos.down())) shapes[4] = hasFloor() ? FishTankBlock.bottomFloor(floorLevel) : FishTankBlock.BOTTOM_NO_FLOOR;
        if (!isTank(world, pos.up())) {
            if (hasTop) {
                makeTopTextureIDs();
                VoxelShape[] top = new VoxelShape[]{VoxelShapes.empty(), VoxelShapes.empty(), VoxelShapes.empty(), VoxelShapes.empty(), VoxelShapes.empty(), VoxelShapes.empty(), VoxelShapes.empty(), VoxelShapes.empty()};
                if (!topRender[0]) top[0] = FishTankBlock.TOP_NORTH;
                if (!topRender[1]) top[1] = FishTankBlock.TOP_EAST;
                if (!topRender[2]) top[2] = FishTankBlock.TOP_SOUTH;
                if (!topRender[3]) top[3] = FishTankBlock.TOP_WEST;
                if (!topRender[4]) top[4] = FishTankBlock.TOP_NORTHEAST;
                if (!topRender[5]) top[5] = FishTankBlock.TOP_SOUTHEAST;
                if (!topRender[6]) top[6] = FishTankBlock.TOP_SOUTHWEST;
                if (!topRender[7]) top[7] = FishTankBlock.TOP_NORTHWEST;
                shapes[5] = VoxelShapes.union(VoxelShapes.empty(), top);
            } else shapes[5] = FishTankBlock.TOP_CLOSED;
        }
        shape = VoxelShapes.union(VoxelShapes.empty(), shapes);
    }

    public void setShouldUpdateShape(boolean value) {
        shouldUpdateShape = value;
    }
    
    public VoxelShape getShape() {
        return shape;
    }


    // Tank System

    public boolean hasBrain() {
        return (isBrain || hasBrain) && brainPos != null;
    }

    public BlockPos getBrainPos() {
        return brainPos;
    }

    // Initialize data of a newly placed tank after loading has been completed (to avoid null errors)
    private void initTankData(@Nullable BlockPos exc) {
        boolean isIsolated = true;
        // Find any surrounding tanks and add this tank to them
        for (Direction dir : Direction.values()) {
            if (this.getWorld().getBlockEntity(this.getPos().offset(dir)) instanceof FishTankBlockEntity tank && tank.hasBrain && tank.brainPos != null && !getPos().offset(dir).equals(exc)) {
                isIsolated = false;
                tank.addNewTank(this.getPos(), false);
                // Add any other tanks around this one to the new system
                for (Direction dir2 : Direction.values()) {
                    if (this.getWorld().getBlockEntity(this.getPos().offset(dir2)) instanceof FishTankBlockEntity tank2 && tank2.hasBrain && tank2.brainPos != null && tank2.brainPos != brainPos && world.getBlockEntity(tank2.brainPos) instanceof FishTankBlockEntity && !getPos().offset(dir).equals(exc)) {
                        addNewTank(tank2.brainPos, false);
                    }
                }
                break;
            }
        }
        // If this is the only nearby tank, make it into its own system
        if (isIsolated) {
            isBrain = true;
            brainPos = this.getPos();
            hasBrain = true;
            tanks.add(getPos());
            floorables.add(getPos());
        }
        init = false;
    }

    // Add a new tank to the system (handled by the brain) -- merge any data the new tank(s) may have, and add the tank to this system
    // Note: parameter 'light' is used if the new tank is a sub-tank in a larger system without data of its own to reduce lag and unnecessary processing
    public void addNewTank(BlockPos pos, boolean light) {
        if (pos.equals(getPos())) return;
        if (isBrain) {
            if (world.getBlockEntity(pos) instanceof FishTankBlockEntity newTank) {
                tanks.add(pos);
                if (!(world.getBlockEntity(pos.down()) instanceof FishTankBlockEntity)) {
                    floorables.add(pos);
                }
                // Communicate with the new tank and sync it and it's system to this system
                newTank.joinSystem(this.getPos(), light);
                if (world.getBlockEntity(pos.up()) instanceof FishTankBlockEntity tank && floorables.contains(pos.up())) {
                    floorables.remove(pos.up());
                    if (world != null && !world.isClient && tank.hasFloor()) ItemScatterer.spawn(world, pos.up(), DefaultedList.copyOf(ItemStack.EMPTY, tank.removeFloor()));
                    tank.removeFloor();
                    BlockPos checkPos = pos.up();
                    while (world.getBlockEntity(checkPos) instanceof FishTankBlockEntity removeTank) {
                        if (removeTank.hasPlant()) {
                            if (world != null && !world.isClient) ItemScatterer.spawn(world, pos.up(), DefaultedList.copyOf(ItemStack.EMPTY, removeTank.getPlant()));
                            removeTank.removePlant();
                            removeTank.setPlantScale(0);
                            removeTank.setPlantPos(new float[]{0, 0, 0});
                            checkPos = checkPos.up();
                        } else break;
                    }
                }
            } else FishTanks.LOGGER.warn("Invalid tank to sync at pos {}!", pos);
        } else if (hasBrain && brainPos != null && world.getBlockEntity(brainPos) instanceof FishTankBlockEntity brain) brain.addNewTank(pos, light);
    }

    // Add the new tank to the system (used by new tank)
    public void joinSystem(BlockPos brainPos, boolean light) {
        this.brainPos = brainPos;
        this.hasBrain = true;
        if (isBrain) {
            // Remove any tanks set for removal, and transfer all of this system's tanks to the new system
            if (!pendingRemovals.isEmpty()) for (BlockPos pos : pendingRemovals) brainRemoveTank(pos);
            pendingRemovals.clear();
            tanks.removeFirst();
            isBrain = false;
            for (BlockPos pos : tanks) addNewTank(pos, true);
            tanks.clear();
        }
        if (!light) copyDataToBrain();
    }

    // Mark a tank for removal
    public void removeTank(BlockPos pos) {
        if (!isBrain && brainPos != null && world.getBlockEntity(brainPos) instanceof FishTankBlockEntity brain) brain.removeTank(this.getPos());
        else if (isBrain && !pendingRemovals.contains(pos)) pendingRemovals.add(pos);
    }

    // Used by the brain, handles removing tanks and transforming detached tanks into their own systems as they are no longer connected
    public void brainRemoveTank(BlockPos pos) {
        // Temporarily despawn fish that are inside of the broken tank (to avoid them winding up outside of the tank and suffocating)
        removeFishTemporary(pos);
        // If this brain tank is the one being removed, transfer the brain to a new tank and remove itself from the system
        if (pos.equals(this.getPos())) {
            for (BlockPos pos1 : tanks) {
                if (pos1.equals(pos)) continue;
                if (transferBrain(pos1)) break;
                else pendingRemovals.add(pos1);
            }
        // Remove the given tank from the system
        } else {
            if (world.getBlockEntity(pos) instanceof FishTankBlockEntity remove) remove.reset();
            if (tanks.contains(pos)) tanks.remove(pos);
            // Reset every tank to determine which are still a part of the system and which are not
            for (BlockPos reset : tanks) if (!reset.equals(getPos()) && world.getBlockEntity(reset) instanceof FishTankBlockEntity tank) tank.reset();
            List<BlockPos> oldTanks = List.copyOf(tanks);
            tanks.clear();
            floorables.clear();
            tanks.add(getPos());
            List<BlockPos> scan = new ArrayList<>();
            scan.add(getPos());
            // Scan to find every tank connected to this brain tank, and re-add it to the system
            while (!scan.isEmpty()) {
                BlockPos thisPos = scan.getFirst();
                for (Direction dir : Direction.values()) {
                    BlockPos current = thisPos.offset(dir);
                    if (world.getBlockEntity(current) instanceof FishTankBlockEntity && !tanks.contains(current) && !pos.equals(current)) {
                        scan.add(current);
                        tanks.add(current);
                    }
                }
                scan.removeFirst();
            }
    
            // Make the detached tanks into their own systems
            for (BlockPos checkPos : oldTanks) if (!tanks.contains(checkPos) && world.getBlockEntity(checkPos) instanceof FishTankBlockEntity tank) {
                tank.initTankData(checkPos);
                scan.add(checkPos);
            // Continue re-adding connected tanks to the system, and determine what blocks are valid to have a floor
            } else if (world.getBlockEntity(checkPos) instanceof FishTankBlockEntity tank) {
                tank.brainPos = this.getPos();
                tank.hasBrain = true;
                if (!(world.getBlockEntity(checkPos.down()) instanceof FishTankBlockEntity)) floorables.add(checkPos);
            }

            // Resize the inventory and drop any items that no longer fit / transfer them to the new tank systems
            connected = tanks.size();
            List<ItemStack> extras = removeEmptyOrElse();
            if (!extras.isEmpty()) for (BlockPos pos2 : scan) if (world.getBlockEntity(pos2) instanceof FishTankBlockEntity tank && tank.isBrain) {
                for (int i = 2; i < tank.inventory.size(); i++) {
                    if (!extras.isEmpty()) {
                        tank.inventory.set(i, extras.getFirst());
                        extras.removeFirst();
                    }
                }
                if (extras.isEmpty()) break;
            }
            for (BlockPos pos2 : scan) removeFishTemporary(pos2);
            if (!extras.isEmpty()) ItemScatterer.spawn(world, pos, listToDefaultedList(extras, ItemStack.EMPTY));
            markDirty();
        }
    }

    // Temporarily despawn fish who are inside of the removed tank
    public void removeFishTemporary(BlockPos pos) {
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, new Box(pos).expand(0.2), entity -> this.spawned.contains(Optional.of(entity.getUuid())));
        if (!entities.isEmpty()) for (LivingEntity entity : entities) {
            Optional<UUID> uuid = Optional.of(entity.getUuid());
            if (spawned.contains(uuid)) spawned.set(spawned.indexOf(uuid), Optional.empty());
            if (alreadySpawned.contains(uuid)){
                if (statuses.get(alreadySpawned.indexOf(uuid)).isPresent()) statuses.set(alreadySpawned.indexOf(uuid), Optional.empty());
                alreadySpawned.set(alreadySpawned.indexOf(uuid), Optional.empty());
            }
            entity.remove(RemovalReason.DISCARDED);
        }
    }

    // Shrink all lists, aiming to remove empty slots first, but removing and returning items if necessary
    public List<ItemStack> removeEmptyOrElse() {
        DefaultedList<ItemStack> inv = DefaultedList.ofSize(connected + 2, ItemStack.EMPTY);
        DefaultedList<Optional<UUID>> newSpawned = DefaultedList.ofSize(connected, Optional.empty());
        DefaultedList<ItemStack> newBuckets = DefaultedList.ofSize(connected, ItemStack.EMPTY);
        DefaultedList<Optional<UUID>> newAlreadySpawned = DefaultedList.ofSize(connected, Optional.empty());
        DefaultedList<Optional<FishStatus>> newStatuses = DefaultedList.ofSize(connected, Optional.empty());

        List<ItemStack> extras = new ArrayList<>();

        int currentIndex = 0;
        inv.set(0, inventory.get(0));
        inv.set(1, inventory.get(1));

        for (int i = 0; i < inventory.size() - 2; i++) {
            if (currentIndex < connected) {
                if (!inventory.get(i + 2).isEmpty()) {
                    inv.set(currentIndex + 2, inventory.get(i + 2));
                    newSpawned.set(currentIndex, spawned.get(i));
                    newBuckets.set(currentIndex, buckets.get(i));
                    newAlreadySpawned.set(currentIndex, alreadySpawned.get(i));
                    newStatuses.set(currentIndex, statuses.get(i));
                    currentIndex++;
                }
            } else if (!inventory.get(i + 2).isEmpty()) {
                extras.add(inventory.get(i + 2));
                if (spawned.get(i).isPresent() && world.getEntity(spawned.get(i).get()) != null) world.getEntity(spawned.get(i).get()).remove(RemovalReason.DISCARDED);
            }
        }

        inventory = inv;
        spawned = newSpawned;
        buckets = newBuckets;
        alreadySpawned = newAlreadySpawned;
        statuses = newStatuses;

        return extras;
    }

    // Update each tank with necessary info from brain
    public void updateTank() {
        if (!isBrain && brainPos != null && world.getBlockEntity(brainPos) instanceof FishTankBlockEntity tank) {
            tankIsDirty = tank.isDirty();
            hasTop = tank.hasTop();
            connectedTextures = tank.hasConnectedTextures();

            world.setBlockState(this.getPos(), world.getBlockState(this.getPos()).with(FishTankBlock.CONNECTED_TEXTURES, connectedTextures).with(FishTankBlock.OPEN_TOP, hasTop).with(FishTankBlock.IS_DIRTY, tankIsDirty));
        } else if (!isBrain) FishTanks.LOGGER.warn("That is not a valid brain tank at pos {}!", brainPos);
    }

    // Send data to the new brain system to facilitate merging with it
    public void copyDataToBrain() {
        if (brainPos != null && world.getBlockEntity(brainPos) instanceof FishTankBlockEntity tank) {
            updateTank();
            tank.receiveData(inventory, connected, spawned, buckets, alreadySpawned, statuses, filters);
            connected = 1;
            inventory = DefaultedList.ofSize(3, ItemStack.EMPTY);
            spawned = DefaultedList.ofSize(1, Optional.empty());
            buckets = DefaultedList.ofSize(1, ItemStack.EMPTY);
            alreadySpawned = DefaultedList.ofSize(1, Optional.empty());
            statuses = DefaultedList.ofSize(1, Optional.empty());
        } else FishTanks.LOGGER.warn("That's not a valid brain tank at pos {}!", brainPos);
    }

    // (Brain Tank) reveive data from the tank being newly added and its system
    public void receiveData(DefaultedList<ItemStack> otherInv, int otherConnected, DefaultedList<Optional<UUID>> otherSpawned, DefaultedList<ItemStack> otherBuckets, DefaultedList<Optional<UUID>> otherAlreadySpawned, DefaultedList<Optional<FishStatus>> otherStatuses, List<BlockPos> otherFilters) {
        int oldConnected = connected;
        connected += otherConnected;
        expandInventory(otherConnected);
        if (!otherInv.get(0).isEmpty() && inventory.get(0).isEmpty()) inventory.set(0, otherInv.get(0));
        ItemScatterer.spawn(world, this.getPos(), DefaultedList.copyOf(ItemStack.EMPTY, otherInv.get(0), otherInv.get(1)));
        int j = 0;
        for (int i = oldConnected; i < connected; i++) {
            try {
                inventory.set(i + 2, otherInv.get(j + 2));
                spawned.set(i, otherSpawned.get(j));
                buckets.set(i, otherBuckets.get(j));
                alreadySpawned.set(i, otherAlreadySpawned.get(j));
                statuses.set(i, otherStatuses.get(j));
            } catch (Exception e) {
                FishTanks.LOGGER.warn("There was an error adding that tank's inventory!");
            }
            j++;
        }
        for (BlockPos pos : otherFilters) if (!filters.contains(pos)) filters.add(pos);
        markDirty();
    }

    // Expand this tank system's inventory upon addition of new tank(s)
    public void expandInventory(int by) {
        DefaultedList<ItemStack> newInv = DefaultedList.ofSize(inventory.size() + by, ItemStack.EMPTY);
        for (int i = 0; i < inventory.size(); i++) newInv.set(i, inventory.get(i));

        int newSize = spawned.size() + by;
        DefaultedList<Optional<UUID>> newSpawned = DefaultedList.ofSize(newSize, Optional.empty()), newAlreadySpawned = DefaultedList.ofSize(newSize, Optional.empty());
        DefaultedList<ItemStack> newBuckets = DefaultedList.ofSize(newSize, ItemStack.EMPTY);
        DefaultedList<Optional<FishStatus>> newStatuses = DefaultedList.ofSize(newSize, Optional.empty());
        for (int i = 0; i < spawned.size(); i++) {
            newSpawned.set(i, spawned.get(i));
            newBuckets.set(i, buckets.get(i));
            newAlreadySpawned.set(i, alreadySpawned.get(i));
            newStatuses.set(i, statuses.get(i));
        }

        inventory = newInv;
        spawned = newSpawned;
        buckets = newBuckets;
        alreadySpawned = newAlreadySpawned;
        statuses = newStatuses;
    }

    @Override
    public int size() {
        return inventory.size();
    }

    public PropertyDelegate getDelegate() {
        return delegate;
    }

    public <T> DefaultedList<T> listToDefaultedList(List<T> list, T defaultValue) {
        DefaultedList<T> newList = DefaultedList.ofSize(list.size(), defaultValue);
        for (int i = 0; i < list.size(); i++) newList.set(i, list.get(i));
        return newList;
    }

    public <T> List<T> defaultedListToList(DefaultedList<T> list) {
        List<T> newList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) newList.add(list.get(i));
        return newList;
    }

    // Transfer the brain to a new tank
    private boolean transferBrain(BlockPos pos) {
        // Make sure the new block pos is actually a fish tank
        if (world.getBlockEntity(pos) instanceof FishTankBlockEntity newBrain) {
            brainPos = pos;
            isBrain = false;
            newBrain.inventory = DefaultedList.ofSize(inventory.size(), ItemStack.EMPTY);
            newBrain.spawned = DefaultedList.ofSize(connected, Optional.empty());
            newBrain.buckets = DefaultedList.ofSize(connected, ItemStack.EMPTY);
            newBrain.alreadySpawned = DefaultedList.ofSize(connected, Optional.empty());
            newBrain.statuses = DefaultedList.ofSize(connected, Optional.empty());
            newBrain.tanks = new ArrayList<>(tanks);
            newBrain.pendingRemovals = new ArrayList<>(pendingRemovals);
            newBrain.filters = new ArrayList<>(filters);
            for (int i = 0; i < inventory.size(); i++) {
                newBrain.inventory.set(i, inventory.get(i));
                if (i > 1) {
                    newBrain.spawned.set(i - 2, spawned.get(i - 2));
                    newBrain.buckets.set(i - 2, buckets.get(i - 2));
                    newBrain.alreadySpawned.set(i - 2, alreadySpawned.get(i - 2));
                    newBrain.statuses.set(i - 2, statuses.get(i - 2));
                }
            }
            newBrain.isBrain = true;
            newBrain.connected = connected;
            newBrain.brainPos = pos;
            for (BlockPos pos1 : tanks) if (world.getBlockEntity(pos1) instanceof FishTankBlockEntity tank) tank.brainPos = pos;
            newBrain.brainRemoveTank(this.getPos());
            return true;
        } else return false;
    }

    public boolean isBrain() {
        return isBrain;
    }

    // Reset this tank back to default
    private void reset() {
        this.brainPos = null; 
        this.hasBrain = false;
        this.isBrain = false;
        this.connected = 1;
    }

    // Remove filters upon deletion and remove their boosts
    public void removeFilter(BlockPos pos) {
        if (isBrain && filters.contains(pos)) {
            filters.remove(pos);
            updateFilterBoost();
        }
        else if (!isBrain && brainPos != null && world.getBlockEntity(brainPos) instanceof FishTankBlockEntity brain) brain.removeFilter(pos); 
    }

    // Add filters to this system and add the boosts they give
    public void addFilter(BlockPos pos) {
        if (init) initTankData(null);
        if (isBrain && !filters.contains(pos)) {
            filters.add(pos);
            updateFilterBoost();
        }
        else if (!isBrain && brainPos != null && world.getBlockEntity(brainPos) instanceof FishTankBlockEntity brain) brain.addFilter(pos); 
    }

    // Update the boost given by attached filters
    public void updateFilterBoost() {
        if (isBrain) {
            dirtyTime = dirtyTimeStored;
            for (BlockPos filter : filters) if (world.getBlockEntity(filter) instanceof FilterBlockEntity f) dirtyTime += f.getBoost();
        } else if (brainPos != null && world.getBlockEntity(brainPos) instanceof FishTankBlockEntity brain) brain.updateFilterBoost();
    }
}