package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Matching two-high panels join without internal uprights or duplicated feet. */
public final class MedicalPrivacyCurtainBlock extends MultiBlockDecorationBlock {
    public static final BooleanProperty LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right");
    //? if >=1.21 {
    public static final MapCodec<MedicalPrivacyCurtainBlock> CODEC = simpleCodec(MedicalPrivacyCurtainBlock::new);
    //?}
    private static final VoxelShape[] LOWER = RestaurantKitchenBlock.rotations(box(0, 0, 3.5, 16, 16, 12.5));
    private static final VoxelShape[] UPPER = RestaurantKitchenBlock.rotations(box(0, 0, 6.5, 16, 16, 9.5));

    public MedicalPrivacyCurtainBlock(Properties properties) {
        super(properties); registerDefaultState(defaultBlockState().setValue(LEFT, false).setValue(RIGHT, false));
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(LEFT, RIGHT);
    }
    @Override protected Direction getExtensionDirection(BlockState state) { return Direction.UP; }
    private boolean matches(BlockState state, BlockState neighbor, boolean primary) {
        return neighbor.is(this) && neighbor.getValue(PRIMARY) == primary && neighbor.getValue(FACING) == state.getValue(FACING);
    }
    private BlockState layout(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return state.setValue(LEFT, matches(state, level.getBlockState(pos.relative(facing.getCounterClockWise())), state.getValue(PRIMARY)))
                .setValue(RIGHT, matches(state, level.getBlockState(pos.relative(facing.getClockWise())), state.getValue(PRIMARY)));
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        return layout(super.getPlacementState(context), context.getLevel(), context.getClickedPos());
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockState(pos).is(this) && level.getBlockState(pos.above()).is(this)) {
            for (BlockPos part : structureParts(state, pos))
                level.getBlockState(part).updateNeighbourShapes(level, part, Block.UPDATE_ALL);
        }
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos floor = structureOrigin(state, pos).below();
        return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        Direction partner = state.getValue(PRIMARY) ? Direction.UP : Direction.DOWN;
        if (side == partner && !matches(state, neighbor, !state.getValue(PRIMARY))) return Blocks.AIR.defaultBlockState();
        if (side == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        return layout(state, level, pos);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(PRIMARY) ? LOWER : UPPER)[RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
