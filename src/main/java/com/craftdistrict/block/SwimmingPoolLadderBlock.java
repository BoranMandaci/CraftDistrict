package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashSet;
import java.util.Set;

/** Independent fluid states in each half allow a ladder to straddle the pool's waterline. */
public final class SwimmingPoolLadderBlock extends RotatableDecorationBlock implements SimpleWaterloggedBlock {
    public static final BooleanProperty PRIMARY = MultiBlockDecorationBlock.PRIMARY, WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private record Removal(Level level, BlockPos origin) { }
    private static final ThreadLocal<Set<Removal>> REMOVING = ThreadLocal.withInitial(HashSet::new);
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(Shapes.or(box(1.625, 0, 8.375, 3.875, 16, 10.625),
                    box(12.125, 0, 8.375, 14.375, 16, 10.625), box(3.875, 3.5, 8, 12.125, 4.5, 11),
                    box(3.875, 8.75, 8, 12.125, 9.75, 11), box(3.875, 14, 8, 12.125, 15, 11),
                    box(1.5, 5, 10.25, 4, 6.25, 16), box(12, 5, 10.25, 14.5, 6.25, 16)).optimize()),
            RestaurantKitchenBlock.rotations(Shapes.or(box(1.625, 0, 8.375, 3.875, 11.25, 10.625),
                    box(12.125, 0, 8.375, 14.375, 11.25, 10.625), box(1.5625, 11.25, 8.375, 3.9375, 14.75, 19.625),
                    box(12.0625, 11.25, 8.375, 14.4375, 14.75, 19.625), box(1.625, .5, 17.375, 3.875, 11.25, 19.625),
                    box(12.125, .5, 17.375, 14.375, 11.25, 19.625), box(1, 0, 16.75, 4.5, .625, 20.25),
                    box(11.5, 0, 16.75, 15, .625, 20.25)).optimize())};
    //? if >=1.21 {
    public static final MapCodec<SwimmingPoolLadderBlock> CODEC = simpleCodec(SwimmingPoolLadderBlock::new);
    //?}
    public SwimmingPoolLadderBlock(Properties properties) {
        super(properties); registerDefaultState(defaultBlockState().setValue(PRIMARY, true).setValue(WATERLOGGED, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(PRIMARY, WATERLOGGED);
    }
    private boolean partner(BlockState state, BlockState other) {
        return other.is(this) && other.getValue(PRIMARY) != state.getValue(PRIMARY) && other.getValue(FACING) == state.getValue(FACING);
    }
    private static BlockPos origin(BlockState state, BlockPos pos) { return state.getValue(PRIMARY) ? pos : pos.below(); }
    private static boolean replaceable(BlockPlaceContext context, BlockPos pos) {
        Level level = context.getLevel();
        return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos) && level.hasChunkAt(pos)
                && level.getBlockState(pos).canBeReplaced(context)
                && (level.getFluidState(pos).isEmpty() || level.getFluidState(pos).is(FluidTags.WATER));
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!replaceable(context, pos) || !replaceable(context, pos.above())) return null;
        Direction facing = context.getClickedFace().getAxis().isHorizontal() ? context.getClickedFace() : context.getHorizontalDirection().getOpposite();
        BlockState state = defaultBlockState().setValue(FACING, facing).setValue(WATERLOGGED, level.getFluidState(pos).is(FluidTags.WATER));
        BlockState upper = state.setValue(PRIMARY, false).setValue(WATERLOGGED, level.getFluidState(pos.above()).is(FluidTags.WATER));
        CollisionContext collision = context.getPlayer() == null ? CollisionContext.empty() : CollisionContext.of(context.getPlayer());
        return state.canSurvive(level, pos) && level.isUnobstructed(state, pos, collision)
                && level.isUnobstructed(upper, pos.above(), collision) ? state : null;
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos lower = origin(state, pos), wall = lower.relative(state.getValue(FACING).getOpposite());
        // The return legs rest on this deck, rather than on an unsupported point inside the water cell.
        return level.hasChunkAt(wall) && level.getBlockState(wall).isFaceSturdy(level, wall, state.getValue(FACING))
                && level.getBlockState(wall).isFaceSturdy(level, wall, Direction.UP)
                && level.getBlockState(wall.above()).getCollisionShape(level, wall.above()).isEmpty();
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !state.getValue(PRIMARY) || level.getBlockState(pos) != state) return;
        BlockPos above = pos.above();
        BlockState previous = level.getBlockState(above);
        BlockState upper = state.setValue(PRIMARY, false).setValue(WATERLOGGED, previous.getFluidState().is(FluidTags.WATER));
        CollisionContext collision = placer == null ? CollisionContext.empty() : CollisionContext.of(placer);
        if (level.isOutsideBuildHeight(above) || !level.getWorldBorder().isWithinBounds(above) || !level.hasChunkAt(above)
                || !previous.canBeReplaced() || (!previous.getFluidState().isEmpty() && !previous.getFluidState().is(FluidTags.WATER))
                || !level.isUnobstructed(upper, above, collision) || !level.setBlock(above, upper, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)) {
            if (level.getBlockState(pos) == state) level.setBlock(pos, state.getFluidState().createLegacyBlock(), Block.UPDATE_ALL);
            return;
        }
        // Replacement callbacks must not leave a detached half or overwrite another block.
        if (level.getBlockState(pos) != state || level.getBlockState(above) != upper) {
            if (level.getBlockState(above) == upper) level.setBlock(above, previous, Block.UPDATE_ALL);
            if (level.getBlockState(pos) == state) level.setBlock(pos, state.getFluidState().createLegacyBlock(), Block.UPDATE_ALL);
            return;
        }
        level.updateNeighborsAt(pos, this);
        level.updateNeighborsAt(above, this);
        if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        if (upper.getValue(WATERLOGGED)) level.scheduleTick(above, Fluids.WATER, Fluids.WATER.getTickDelay(level));
    }
    @Override public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        if (level instanceof Level world && REMOVING.get().contains(new Removal(world, origin(state, pos)))) return state;
        if (side == (state.getValue(PRIMARY) ? Direction.UP : Direction.DOWN) && !partner(state, neighbor))
            return state.getFluidState().createLegacyBlock();
        return state;
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!level.isClientSide && !next.is(this)) {
            Removal key = new Removal(level, origin(state, pos).immutable());
            Set<Removal> removing = REMOVING.get();
            if (removing.add(key)) {
                try {
                    BlockPos other = pos.relative(state.getValue(PRIMARY) ? Direction.UP : Direction.DOWN);
                    if (level.hasChunkAt(other)) {
                        BlockState otherState = level.getBlockState(other);
                        if (partner(state, otherState)) level.setBlock(other, otherState.getFluidState().createLegacyBlock(), Block.UPDATE_ALL);
                    }
                } finally {
                    removing.remove(key);
                    if (removing.isEmpty()) REMOVING.remove();
                }
            }
        }
        super.onRemove(state, level, pos, next, moving);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(PRIMARY) ? 0 : 1][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return getShape(state, level, pos, context); }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
