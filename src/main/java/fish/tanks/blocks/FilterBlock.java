package fish.tanks.blocks;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.MapCodec;

import fish.tanks.registries.FishTankBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
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

public class FilterBlock extends BlockWithEntity implements Waterloggable {

    public static final MapCodec<FilterBlock> CODEC = createCodec(FilterBlock::new);

    public static final EnumProperty<Direction> FACING = HorizontalFacingBlock.FACING;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    public static final VoxelShape SHAPE_NORTH = VoxelShapes.union(Block.createCuboidShape(4, 0, 2, 12, 12, 10), Block.createCuboidShape(7, 3, 0, 9, 11, 2), Block.createCuboidShape(5, 12, 3, 11, 13, 9));
    public static final VoxelShape SHAPE_SOUTH = VoxelShapes.union(Block.createCuboidShape(4, 0, 6, 12, 12, 14), Block.createCuboidShape(7, 3,14, 9, 11, 16), Block.createCuboidShape(5, 12, 7, 11, 13, 13));
    public static final VoxelShape SHAPE_EAST = VoxelShapes.union(Block.createCuboidShape(6, 0, 4, 14, 12, 12), Block.createCuboidShape(14, 3, 7, 16, 11, 9), Block.createCuboidShape(7, 12, 5, 13, 13, 11));
    public static final VoxelShape SHAPE_WEST = VoxelShapes.union(Block.createCuboidShape(2, 0, 4, 10, 12, 12), Block.createCuboidShape(0, 3, 7, 2, 11, 9), Block.createCuboidShape(3, 12, 5, 9, 13, 11));

    public FilterBlock(Settings settings) {
        super(settings);
        this.setDefaultState((BlockState)((BlockState)((BlockState)this.stateManager.getDefaultState())
        .with(FACING, Direction.NORTH)).with(WATERLOGGED, false));
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (state.contains(FACING)) {
            switch (state.get(FACING)) {
                case SOUTH: return SHAPE_SOUTH;
                case EAST: return SHAPE_EAST;
                case WEST: return SHAPE_WEST;
                default: return SHAPE_NORTH;
            }
        }
        return SHAPE_NORTH;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FilterBlockEntity(pos, state);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }
    
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return (BlockState)this.getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing())
        .with(WATERLOGGED, ctx.getWorld().getFluidState(ctx.getBlockPos()).getFluid() == Fluids.WATER);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return (BlockState)state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        if (state.get(WATERLOGGED).booleanValue()) {
            return Fluids.WATER.getStill(false);
        }
        return super.getFluidState(state);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView view, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        if (state.get(WATERLOGGED)) {
            view.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        if (state.contains(FACING) && neighborPos.equals(pos.offset(state.get(FACING))) && world.getBlockEntity(neighborPos) instanceof FishTankBlockEntity tank) {
            tank.addFilter(pos);
        }
        return state;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (state.contains(FACING) && world.getBlockEntity(pos.offset(state.get(FACING))) instanceof FishTankBlockEntity tank) tank.removeFilter(pos);
        return super.onBreak(world, pos, state, player);
    }

    @Override
    public void onDestroyedByExplosion(ServerWorld world, BlockPos pos, Explosion explosion) {
        if (world.getBlockState(pos).contains(FACING) && world.getBlockEntity(pos.offset(world.getBlockState(pos).get(FACING))) instanceof FishTankBlockEntity tank) tank.removeFilter(pos);
        super.onDestroyedByExplosion(world, pos, explosion);
    }

    @Override
    public void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        ItemScatterer.onStateReplaced(state, world, pos);
    }

    @Override
    public ActionResult onUseWithItem(ItemStack item, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            NamedScreenHandlerFactory screen = (FilterBlockEntity)world.getBlockEntity(pos);
            if (screen != null) {
                player.openHandledScreen(screen);
            }
        }
        return ActionResult.SUCCESS;
    }
    

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, FishTankBlocks.FILTER_BE, (world1, pos, state1, blockEntity) -> blockEntity.tick(world1, pos, state1));
    }

    @Override
    public boolean isTransparent(BlockState state) {
        return true;
    }
}
