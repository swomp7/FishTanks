package fish.tanks.blocks;

import java.util.ArrayList;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import fish.tanks.items.TankBuilderItem;
import fish.tanks.mixins.PlantGetterMixin;
import fish.tanks.networking.SyncBlocksPacket;
import fish.tanks.networking.SyncBooleanPacketS2C;
import fish.tanks.networking.SyncIntPacketS2C;
import fish.tanks.networking.SyncPlantPacket;
import fish.tanks.networking.SyncTankCleaningPacket;
import fish.tanks.registries.FishTankBlocks;
import fish.tanks.registries.FishTankComponents;
import fish.tanks.util.FishTankTags;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager.Builder;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.tick.ScheduledTickView;

public class FishTankBlock extends BlockWithEntity {

    public static final MapCodec<FishTankBlock> CODEC = createCodec(FishTankBlock::new);

    public static final BooleanProperty IS_DIRTY = BooleanProperty.of("dirty");
    public static final BooleanProperty OPEN_TOP = BooleanProperty.of("open_top");
    public static final BooleanProperty CONNECTED_TEXTURES = BooleanProperty.of("connected_textures");

    // Default floor shape
    public static final VoxelShape SHAPE_SINGLE_NO_FLOOR_WT = VoxelShapes.union(Block.createCuboidShape(0, 0, 0, 16, 16, 1), Block.createCuboidShape(0, 0, 0, 1, 16, 16), Block.createCuboidShape(0, 0, 15, 16, 16, 16), Block.createCuboidShape(15, 0, 0, 16, 16, 16), Block.createCuboidShape(0, 0, 0, 16, 1, 16), Block.createCuboidShape(0, 15, 0, 16, 16, 3), Block.createCuboidShape(0, 15, 13, 16, 16, 16), Block.createCuboidShape(0, 15, 3, 3, 16, 13), Block.createCuboidShape(13, 15, 3, 16, 16, 13));

    // Each individual shape component for making a custom shape (see makeShape() in FishTankBlockEntity for more info)
    public static final VoxelShape NORTH_WALL = Block.createCuboidShape(0, 0, 0, 16, 16, 1);
    public static final VoxelShape SOUTH_WALL = Block.createCuboidShape(0, 0, 15, 16, 16, 16);
    public static final VoxelShape EAST_WALL = Block.createCuboidShape(15, 0, 0, 16, 16, 16);
    public static final VoxelShape WEST_WALL = Block.createCuboidShape(0, 0, 0, 1, 16, 16);
    public static final VoxelShape TOP_NORTH = Block.createCuboidShape(3, 15, 0, 13, 16, 3);
    public static final VoxelShape TOP_SOUTH = Block.createCuboidShape(3, 15, 13, 13, 16, 16);
    public static final VoxelShape TOP_EAST = Block.createCuboidShape(13, 15, 3, 16, 16, 13);
    public static final VoxelShape TOP_WEST = Block.createCuboidShape(0, 15, 3, 3, 16, 13);
    public static final VoxelShape TOP_NORTHEAST = Block.createCuboidShape(13, 15, 0, 16, 16, 3);
    public static final VoxelShape TOP_NORTHWEST = Block.createCuboidShape(0, 15, 0, 3, 16, 3);
    public static final VoxelShape TOP_SOUTHEAST = Block.createCuboidShape(13, 15, 13, 16, 16, 16);
    public static final VoxelShape TOP_SOUTHWEST = Block.createCuboidShape(0, 15, 13, 3, 16, 16);
    public static final VoxelShape TOP_CLOSED = Block.createCuboidShape(0, 15, 0, 16, 16, 16);
    public static final VoxelShape BOTTOM_NO_FLOOR = Block.createCuboidShape(0, 0, 0, 16, 1, 16);

    public static VoxelShape SHAPE = SHAPE_SINGLE_NO_FLOOR_WT;

    public FishTankBlock(Settings settings) {
        super(settings);
        this.setDefaultState((BlockState)this.stateManager.getDefaultState().with(IS_DIRTY, false).with(OPEN_TOP, true).with(CONNECTED_TEXTURES, false));
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos arg0, BlockState arg1) {
        return new FishTankBlockEntity(arg0, arg1);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (world.getBlockEntity(pos) instanceof FishTankBlockEntity tank) return tank.getShape();
        return SHAPE_SINGLE_NO_FLOOR_WT;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return super.getPlacementState(ctx).with(IS_DIRTY, false).with(OPEN_TOP, true).with(CONNECTED_TEXTURES, false);
    }

    public static VoxelShape bottomFloor(int level) {
        return Block.createCuboidShape(0, 0, 0, 16, 3 * level, 16);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
       return CODEC;
    }

    @Override
    protected void appendProperties(Builder<Block, BlockState> builder) {
        builder.add(IS_DIRTY).add(OPEN_TOP).add(CONNECTED_TEXTURES);
    }

    // Prevent water from spawning on block break
    @Override
    public void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        ItemScatterer.onStateReplaced(state, world, pos);
        if (world.getBlockState(pos).getBlock().equals(Blocks.WATER)) world.setBlockState(pos, Blocks.AIR.getDefaultState());
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (world.getBlockEntity(pos) instanceof FishTankBlockEntity tank) {
            // Remove and drop plant and floor on block break
            boolean hasPlant = tank.hasPlant();
            if (!world.isClient) {
                if (tank.hasFloor()) ItemScatterer.spawn(world, pos, DefaultedList.ofSize(1, tank.getFloor()));
                if (tank.hasPlant()) {
                    ItemScatterer.spawn(world, pos, DefaultedList.ofSize(1, tank.getPlant()));
                    hasPlant = true;
                }
            }
            // Remove plants stacked in above tanks on block break
            if (hasPlant) {
                BlockPos checkPos = pos.up();
                while (world.getBlockEntity(checkPos) instanceof FishTankBlockEntity remove) {
                    if (remove.hasPlant()) {
                        if (!world.isClient) ItemScatterer.spawn(world, checkPos, DefaultedList.ofSize(1, remove.getPlant()));
                        remove.removePlant();
                        remove.setPlantPos(new float[]{0, 0, 0});
                        remove.setPlantScale(0);
                        checkPos = checkPos.up();
                    } else break;
                }
            }
            // Remove this tank from the system
            if (tank.isBrain()) tank.brainRemoveTank(pos);
            else if (tank.hasBrain()) tank.removeTank(pos);
            // Remove any filters from the system
            for (Direction dir : Direction.values()) if (!dir.equals(Direction.UP) && !dir.equals(Direction.DOWN)) {
                BlockState otherState = world.getBlockState(pos.offset(dir));
                if (otherState.getBlock() instanceof FilterBlock && otherState.contains(FilterBlock.FACING) && pos.offset(dir).offset(otherState.get(FilterBlock.FACING)).equals(pos)) tank.removeFilter(pos.offset(dir));
            }
        }
        Block.dropStacks(state, world, pos, world.getBlockEntity(pos), player, player.getMainHandStack());
        return super.onBreak(world, pos, state, player);
    }

    // Same as above, but for different circumstances -- see for more info
    @Override
    public void onDestroyedByExplosion(ServerWorld world, BlockPos pos, Explosion explosion) {
        if (world.getBlockEntity(pos) instanceof FishTankBlockEntity tank) {
            if (!world.isClient) {
                if (tank.hasFloor()) ItemScatterer.spawn(world, pos, DefaultedList.ofSize(1, tank.getFloor()));
                if (tank.hasPlant()) ItemScatterer.spawn(world, pos, DefaultedList.ofSize(1, tank.getPlant()));
            }
            if (tank.hasPlant()) {
                BlockPos checkPos = pos.up();
                while (world.getBlockEntity(checkPos) instanceof FishTankBlockEntity remove) {
                    if (remove.hasPlant()) {
                        if (!world.isClient) ItemScatterer.spawn(world, checkPos, DefaultedList.ofSize(1, remove.getPlant()));
                        remove.removePlant();
                        remove.setPlantPos(new float[]{0, 0, 0});
                        remove.setPlantScale(0);
                        checkPos = checkPos.up();
                    } else break;
                }
            }
            if (tank.isBrain()) tank.brainRemoveTank(pos);
            else if (tank.hasBrain()) tank.removeTank(pos);
            for (Direction dir : Direction.values()) if (!dir.equals(Direction.UP) && !dir.equals(Direction.DOWN)) {
                BlockState otherState = world.getBlockState(pos.offset(dir));
                if (otherState.getBlock() instanceof FilterBlock && otherState.contains(FilterBlock.FACING) && pos.offset(dir).offset(otherState.get(FilterBlock.FACING)).equals(pos)) tank.removeFilter(pos.offset(dir));
            }
        }
        super.onDestroyedByExplosion(world, pos, explosion);
    }

    // Add filters to the system
    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView,
            BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        if (neighborState.getBlock() instanceof FilterBlock && neighborState.contains(FilterBlock.FACING) && neighborPos.offset(neighborState.get(FilterBlock.FACING)).equals(pos) && world.getBlockEntity(pos) instanceof FishTankBlockEntity tank) tank.addFilter(neighborPos);
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public ActionResult onUseWithItem(ItemStack item, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity != null && blockEntity instanceof FishTankBlockEntity tank && tank.hasBrain() && world.getBlockEntity(tank.getBrainPos()) instanceof FishTankBlockEntity brain) {
                BlockPos brainPos = tank.getBrainPos();
                BlockState brainState = world.getBlockState(brainPos);

                // If the player is holding a brush, clean the tank
                if (item.getItem().equals(Items.BRUSH)) {
                    world.playSound(null, pos, SoundEvents.BLOCK_GRASS_STEP, SoundCategory.BLOCKS, 1.0f, 0.3f);
                    ((ServerPlayerEntity)player).sendMessageToClient(Text.literal(Text.translatable("fish-tanks.cleaning_progress").getString() + " " + brain.clean() + "%"), true);
                    for (ServerPlayerEntity serverPlayerEntity : PlayerLookup.tracking((ServerWorld)world, brainPos)) {
                        ServerPlayNetworking.send(serverPlayerEntity, new SyncTankCleaningPacket(brainPos));
                    }
                    if (!player.isCreative()) item.damage(1, player);

                // If the player is holding a valid floor block and the tank needs a floor, place it
                } else if (item.isIn(FishTankTags.TANK_FLOOR_PLACEABLE) && !tank.hasFloor() && tank.isFloorable() && hand == Hand.MAIN_HAND) {
                    if (item.getItem() instanceof BlockItem blockItem) world.playSound(null, pos, blockItem.getBlock().getDefaultState().getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS);
                    tank.setFloor(new ItemStack(item.getItem()));
                    for (ServerPlayerEntity serverPlayerEntity : PlayerLookup.tracking((ServerWorld)world, pos)) {
                        ServerPlayNetworking.send(serverPlayerEntity, new SyncBlocksPacket(new ItemStack(item.getItem()), "floor", pos));
                    }
                    if (!player.isCreative()) player.getMainHandStack().decrement(1);
                
                // If the player is holding a valid plant block and the tank needs it, place it
                } else if (item.isIn(FishTankTags.TANK_PLANTS_PLACEABLE) && hand == Hand.MAIN_HAND) {
                    if (!tank.hasPlant() && tank.isFloorable() && tank.hasFloor()) {
                        if (item.getItem() instanceof BlockItem blockItem) {
                            Block plant = blockItem.getBlock();
                            if (plant instanceof PlantBlock plantBlock && tank.getFloor().getItem() instanceof BlockItem blockItem2) {
                                if (!((PlantGetterMixin)plantBlock).callCanPlantOnTop(blockItem2.getBlock().getDefaultState(), world, pos)) {
                                    player.openHandledScreen(tank);
                                    return ActionResult.SUCCESS;
                                }
                            }
                            world.playSound(null, pos, plant.getDefaultState().getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS);
                        }
                        tank.setPlant(new ItemStack(item.getItem()));

                        // Set a random size and position in the tank
                        Random random = Random.create();
                        float scale = (float)random.nextBetween(16, world.getBlockState(pos.up()).getBlock() instanceof FishTankBlock ? 28 : 26) / 32f;
                        float[] xyz = {(float)random.nextBetween(2, 32 - ((int)(scale * 32)) - 2) / 32f, 0, (float)random.nextBetween(2, 32 - ((int)(scale * 32)) - 2) / 32f};
                        tank.setPlantScale(scale);
                        tank.setPlantPos(xyz);
                        for (ServerPlayerEntity serverPlayerEntity : PlayerLookup.tracking((ServerWorld)world, pos)) {
                            ServerPlayNetworking.send(serverPlayerEntity, new SyncBlocksPacket(new ItemStack(item.getItem()), "plant", pos));
                            ServerPlayNetworking.send(serverPlayerEntity, new SyncPlantPacket(scale, xyz[0], xyz[1], xyz[2], pos));
                        }
                        if (!player.isCreative()) player.getMainHandStack().decrement(1);
                    // Place a plant on above tanks is applicable
                    } else if (tank.hasPlant() && tank.hasFloor() && tank.isFloorable()) {
                        BlockPos upPos = pos.up();
                        boolean success = false;
                        while (world.getBlockEntity(upPos) instanceof FishTankBlockEntity upTank) {
                            if (!upTank.hasPlant() && item.getItem().equals(tank.getPlant().getItem())) {
                                if (item.getItem() instanceof BlockItem blockItem) {
                                    Block plant = blockItem.getBlock();
                                    if (plant instanceof PlantBlock plantBlock && tank.getPlant().getItem() instanceof BlockItem blockItem2) {
                                        if (!((PlantGetterMixin)plantBlock).callCanPlantOnTop(blockItem2.getBlock().getDefaultState(), world, upPos) && !(item.getItem().equals(Items.SEAGRASS) && blockItem.equals(Items.SEAGRASS) && upPos.equals(pos.up()))) {
                                            player.openHandledScreen(tank);
                                            return ActionResult.SUCCESS;
                                        }
                                    }
                                    world.playSound(null, upPos, plant.getDefaultState().getSoundGroup().getPlaceSound(), SoundCategory.BLOCKS);
                                }
                                upTank.setPlant(new ItemStack(item.getItem()));
                                float scale = tank.getPlantScale();
                                float[] xyz = tank.getPlantPos().clone();
                                if (world.getBlockEntity(upPos.down()) instanceof FishTankBlockEntity downTank) xyz[1] = -1f + downTank.getPlantPos()[1] + scale;
                                upTank.setPlantScale(scale);
                                upTank.setPlantPos(xyz);
                                for (ServerPlayerEntity serverPlayerEntity : PlayerLookup.tracking((ServerWorld)world, upPos)) {
                                    ServerPlayNetworking.send(serverPlayerEntity, new SyncBlocksPacket(new ItemStack(item.getItem()), "plant", upPos));
                                    ServerPlayNetworking.send(serverPlayerEntity, new SyncPlantPacket(scale, xyz[0], xyz[1], xyz[2], upPos));
                                }
                                if (!player.isCreative()) player.getMainHandStack().decrement(1);
                                success = true;
                                break;
                            } else {
                                upPos = upPos.up();
                            }
                        }
                        if (!success) player.openHandledScreen(tank);
                    } else player.openHandledScreen(tank);
                
                // If the player is holding a tank builder item with mode is REMOVE_FLOOR and the tank has a floor, remove the floor and any plants
                } else if (modeIs(item, TankBuilderItem.BuilderType.REMOVE_FLOOR) && tank.hasFloor()) {
                    ItemScatterer.spawn(world, pos, DefaultedList.ofSize(1, tank.removeFloor()));
                    ArrayList<BlockPos> remove = new ArrayList<>();
                    BlockPos checkPos = pos;
                    while (world.getBlockEntity(checkPos) instanceof FishTankBlockEntity removeTank) {
                        if (removeTank.hasPlant()) {
                            remove.add(checkPos);
                            ItemScatterer.spawn(world, checkPos, DefaultedList.ofSize(1, removeTank.removePlant()));
                        } else break;
                        checkPos = checkPos.up();
                    }
                    ((ServerPlayerEntity)player).sendMessageToClient(Text.translatable("fish-tanks.tank_builder.floor_removed"), true);
                    for (ServerPlayerEntity serverPlayerEntity : PlayerLookup.tracking((ServerWorld)world, pos)) {
                        ServerPlayNetworking.send(serverPlayerEntity, new SyncBlocksPacket(ItemStack.EMPTY, "floor", pos));
                        for (BlockPos removePos : remove) {
                            ServerPlayNetworking.send(serverPlayerEntity, new SyncBlocksPacket(ItemStack.EMPTY, "plant", removePos));
                            ServerPlayNetworking.send(serverPlayerEntity, new SyncPlantPacket(0, 0, 0, 0, removePos));
                        }
                    }
                    if (!player.isCreative()) item.damage(1, player);
                
                // If the player is holding a tank builder item with mode is REMOVE_PLANT and the tank has a plant, remove the plant(s)
                } else if (modeIs(item, TankBuilderItem.BuilderType.REMOVE_PLANT) && tank.hasPlant()) {
                    ArrayList<BlockPos> remove = new ArrayList<>();
                    BlockPos checkPos = pos;
                    while (world.getBlockEntity(checkPos) instanceof FishTankBlockEntity removeTank) {
                        if (removeTank.hasPlant()) {
                            remove.add(checkPos);
                            ItemScatterer.spawn(world, checkPos, DefaultedList.ofSize(1, removeTank.removePlant()));
                        } else break;
                        checkPos = checkPos.up();
                    }
                    ((ServerPlayerEntity)player).sendMessageToClient(Text.translatable("fish-tanks.tank_builder.plant_removed"), true);
                    for (ServerPlayerEntity serverPlayerEntity : PlayerLookup.tracking((ServerWorld)world, pos)) {
                        for (BlockPos removePos : remove) {
                            ServerPlayNetworking.send(serverPlayerEntity, new SyncBlocksPacket(ItemStack.EMPTY, "plant", removePos));
                            ServerPlayNetworking.send(serverPlayerEntity, new SyncPlantPacket(0, 0, 0, 0, removePos));
                        }
                    }
                    if (!player.isCreative()) item.damage(1, player);

                // If the player is holding a tank builder item with mode is SET_OPEN_TOP, open or close the tank lid
                } else if (modeIs(item, TankBuilderItem.BuilderType.SET_OPEN_TOP)) {
                    boolean openTop = brainState.get(OPEN_TOP);
                    world.setBlockState(brainPos, brainState.with(OPEN_TOP, !openTop));
                    brain.setHasTop(!openTop);
                    for (ServerPlayerEntity serverPlayerEntity : PlayerLookup.tracking((ServerWorld)world, brainPos)) {
                        ServerPlayNetworking.send(serverPlayerEntity, new SyncBooleanPacketS2C(!openTop, "hasTop", brainPos));
                    }
                    ((ServerPlayerEntity)player).sendMessageToClient(Text.translatable(openTop ? "fish-tanks.tank_builder.top_closed" : "fish-tanks.tank_builder.top_opened"), true);
                    if (!player.isCreative()) item.damage(1, player);
                
                // If the player is holding a tank builder item with mode is SET_CONNECTED_TEXTURES, set the tank to either have or no longer have connected textures
                } else if (modeIs(item, TankBuilderItem.BuilderType.SET_CONNECTED_TEXTURES)) {
                    boolean connectedTextures = brainState.get(CONNECTED_TEXTURES);
                    world.setBlockState(brainPos, brainState.with(CONNECTED_TEXTURES, !connectedTextures));
                    brain.setConnectedTextures(!connectedTextures);
                    for (ServerPlayerEntity serverPlayerEntity : PlayerLookup.tracking((ServerWorld)world, brainPos)) {
                        ServerPlayNetworking.send(serverPlayerEntity, new SyncBooleanPacketS2C(!connectedTextures, "connectedTextures", brainPos));
                    }
                    ((ServerPlayerEntity)player).sendMessageToClient(Text.translatable(connectedTextures ? "fish-tanks.tank_builder.textures_disconnected" : "fish-tanks.tank_builder.textures_connected"), true);
                    if (!player.isCreative()) item.damage(1, player);

                // If the player is holding a tank builder item with mode is FLOOR_LEVEL and the tank has a floor, change the height of the floor in the tank
                } else if (modeIs(item, TankBuilderItem.BuilderType.FLOOR_LEVEL) && tank.hasFloor()) {
                    if (world.getBlockEntity(pos.up()) instanceof FishTankBlockEntity) {
                        int floorLevel = tank.getFloorLevel();
                        if (floorLevel < 3) floorLevel++;
                        else floorLevel = 1;
                        tank.setFloorLevel(floorLevel);
                        for (ServerPlayerEntity serverPlayerEntity : PlayerLookup.tracking((ServerWorld)world, pos)) {
                            ServerPlayNetworking.send(serverPlayerEntity, new SyncIntPacketS2C(floorLevel, "floorLevel", pos));
                        }
                        ((ServerPlayerEntity)player).sendMessageToClient(Text.literal(Text.translatable("fish-tanks.tank_builder.floor_level_set_to").getString() + floorLevel), true);
                        if (!player.isCreative()) item.damage(1, player);
                    } else ((ServerPlayerEntity)player).sendMessageToClient(Text.translatable("fish-tanks.tank_builder.floor_level_set_to_fail"), true);

                // If none, just open the tank
                } else player.openHandledScreen(brain);
            }
        }
        return ActionResult.SUCCESS;
    }

    private boolean modeIs(ItemStack item, TankBuilderItem.BuilderType mode) {
        return item.contains(FishTankComponents.BUILDER_MODE) && item.get(FishTankComponents.BUILDER_MODE).equals(mode.toString());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, FishTankBlocks.FISH_TANK_BE, (world1, pos, state1, blockEntity) -> blockEntity.tick(world1, pos, state1));
    }

    @Override
    public boolean isTransparent(BlockState state) {
        return true;
    }

    @Override
    protected VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return VoxelShapes.empty();
	}

    @Override
	protected float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
		return 1.0F;
	}

    @Override
    protected FluidState getFluidState(BlockState state) {
        return Fluids.WATER.getStill(false);
    }

    @Override
    protected boolean isSideInvisible(BlockState state, BlockState stateFrom, Direction direction) {
		return stateFrom.isOf(this) ? true : super.isSideInvisible(state, stateFrom, direction);
	}
}
