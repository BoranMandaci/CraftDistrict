package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import com.craftdistrict.lighting.LightControls;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two-tier ceiling fixtures share one light controller; saved shallow fixtures retain four cells. */
public final class CasinoChandelierBlock extends WideTallDecorationBlock implements ControlledLight {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 7);
    public static final BooleanProperty TALL = BooleanProperty.create("tall");
    private static final VoxelShape[][][] SHAPES = createShapes();
    //? if >=1.21 {
    public static final MapCodec<CasinoChandelierBlock> CODEC = simpleCodec(CasinoChandelierBlock::new);
    //?}
    public CasinoChandelierBlock(Properties properties) {
        super(properties); registerDefaultState(defaultBlockState().setValue(TALL, false));
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PART, TALL); }
    @Override protected int partCount() { return 8; }
    @Override protected int partCount(BlockState state) { return state.getValue(TALL) ? 8 : 4; }
    @Override protected int partIndex(BlockState state) { return state.getValue(PART); }
    @Override protected BlockState stateForPart(BlockState state, int index) { return state.setValue(PART, index); }
    @Override protected BlockPos partOffset(BlockState state, int index) {
        Direction facing = state.getValue(FACING);
        return BlockPos.ZERO.relative(facing.getClockWise(), index % 2)
                .relative(facing.getOpposite(), index / 2 % 2).below(index / 4);
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        return context.getClickedFace() == Direction.DOWN ? super.getPlacementState(context).setValue(TALL, true) : null;
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        // The expanded part property has eight values, rather than the parent's four-value property.
        BlockState result = state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
        return mirror == Mirror.NONE ? result : result.setValue(PART, partIndex(state) ^ 1);
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (partIndex(state) >= partCount(state)) return false;
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos ceiling = pos.above(partIndex(state) / 4 + 1);
        return level.hasChunkAt(ceiling) && level.getBlockState(ceiling).isFaceSturdy(level, ceiling, Direction.DOWN);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        BlockPos origin = structureOrigin(state, pos);
        for (int part = 0; part < partCount(state); part++)
            if (origin.offset(partOffset(state, part)).equals(otherPos)
                    && LightControls.shape(neighbor) != LightControls.shape(stateForPart(state, part)))
                return Blocks.AIR.defaultBlockState();
        return side == Direction.UP && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    private static VoxelShape[][][] createShapes() {
        VoxelShape whole = Shapes.or(box(13, 14.5, 13, 19, 16, 19), box(15.4, 3, 15.4, 16.6, 14.5, 16.6),
                box(5, 9, 15.6, 27, 9.4, 16.4), box(15.6, 9, 5, 16.4, 9.4, 27),
                box(4, 2.5, 12, 7.5, 9.5, 20), box(24.5, 2.5, 12, 28, 9.5, 20),
                box(6, 2.5, 8, 10, 8, 12), box(22, 2.5, 8, 26, 8, 12),
                box(6, 2.5, 20, 10, 8, 24), box(22, 2.5, 20, 26, 8, 24),
                box(12, 2.5, 4, 20, 9.5, 7.5), box(12, 2.5, 24.5, 20, 9.5, 28),
                box(11, .25, 11, 21, 4.5, 21)).optimize();
        VoxelShape tall = Shapes.empty();
        for (var a : whole.toAabbs()) tall = Shapes.or(tall, Shapes.box(
                1 + (a.minX - 1) * 1.12, 1 - (1 - a.minY) * 2, 1 + (a.minZ - 1) * 1.12,
                1 + (a.maxX - 1) * 1.12, 1 - (1 - a.maxY) * 2, 1 + (a.maxZ - 1) * 1.12));
        VoxelShape[][][] result = new VoxelShape[2][8][];
        for (int mode = 0; mode < 2; mode++) for (int part = 0; part < 8; part++) {
            VoxelShape local = mode == 0 && part >= 4 ? Shapes.empty()
                    : Shapes.join((mode == 0 ? whole : tall).move(-(part % 2), part / 4, -(part / 2 % 2)),
                            Shapes.block(), BooleanOp.AND).optimize();
            result[mode][part] = RestaurantKitchenBlock.rotations(local);
        }
        return result;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(TALL) ? 1 : 0][partIndex(state)][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
