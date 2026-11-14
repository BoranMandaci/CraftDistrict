package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Static floor grids use the existing atomic placement and single-drop removal lifecycle. */
public final class CasinoTableBlock extends MultiBlockDecorationBlock {
    public enum Design implements StringRepresentable {
        ROULETTE(3, new double[][]{{0, 2, 4, 44}, {2, 30, 0, 48}, {30, 32, 4, 44}}),
        BLACKJACK(3, new double[][]{{0, 2, 19, 29}, {2, 4, 11, 37}, {4, 7, 6, 42},
                {7, 11, 3, 45}, {11, 16, 1, 47}, {16, 32, 0, 48}}),
        POKER(3, new double[][]{{0, 2, 12, 36}, {2, 5, 6, 42}, {5, 9, 2, 46},
                {9, 23, 0, 48}, {23, 27, 2, 46}, {27, 30, 6, 42}, {30, 32, 12, 36}}),
        BACCARAT(3, new double[][]{{0, 3, 6, 42}, {3, 8, 2, 46}, {8, 30, 0, 48}, {30, 32, 3, 45}}),
        CRAPS(4, new double[][]{{0, 2, 5, 59}, {2, 4, 2, 62}, {4, 28, 0, 64},
                {28, 30, 2, 62}, {30, 32, 5, 59}});

        final int width;
        final double[][] outline;
        Design(int width, double[][] outline) { this.width = width; this.outline = outline; }
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 7);
    private static final VoxelShape[][][] SHAPES = createShapes();
    private final Design design;
    //? if >=1.21 {
    public static final MapCodec<CasinoTableBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((CasinoTableBlock b) -> b.design),
            propertiesCodec()).apply(instance, CasinoTableBlock::new));
    //?}

    public CasinoTableBlock(Design design, Properties properties) { super(properties); this.design = design; }

    private static VoxelShape[][][] createShapes() {
        VoxelShape[][][] result = new VoxelShape[Design.values().length][8][];
        for (Design design : Design.values()) {
            VoxelShape whole = Shapes.empty();
            for (double[] strip : design.outline)
                whole = Shapes.or(whole, box(strip[2], 13.25, strip[0], strip[3],
                        design == Design.CRAPS ? 14 : 16, strip[1]));
            if (design == Design.CRAPS) {
                // Raised rails surround a recessed playing bed; they do not fill the interior collision.
                whole = Shapes.or(whole, box(5, 14, 0, 59, 16, 2), box(5, 14, 30, 59, 16, 32),
                        box(0, 14, 4, 2, 16, 28), box(62, 14, 4, 64, 16, 28),
                        box(2, 14, 2, 5, 16, 4), box(59, 14, 2, 62, 16, 4),
                        box(2, 14, 28, 5, 16, 30), box(59, 14, 28, 62, 16, 30));
            } else if (design == Design.ROULETTE) {
                whole = Shapes.or(whole, box(2.5, 16, 9.25, 16, 18.35, 22.75));
            }
            double left = design.width == 4 ? 14 : 11;
            double right = design.width * 16 - left;
            for (double center : new double[]{left, right})
                whole = Shapes.or(whole, box(center - 6, 0, 8, center + 6, 1.5, 24),
                        box(center - 3, 1.5, 12, center + 3, 13.25, 20));
            whole = Shapes.or(whole, box(left, 4.25, 14.75, right, 5.5, 17.25)).optimize();
            for (int part = 0; part < 8; part++) {
                VoxelShape local = part < design.width * 2
                        ? Shapes.join(whole.move(-(part % design.width), 0, -(part / design.width)),
                                box(0, 0, 0, 16, 20, 16), BooleanOp.AND).optimize()
                        : Shapes.empty();
                result[design.ordinal()][part] = RestaurantKitchenBlock.rotations(local);
            }
        }
        return result;
    }

    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PART); }
    @Override protected int partCount() { return design.width * 2; }
    @Override protected int partIndex(BlockState state) { return state.getValue(PART); }
    @Override protected BlockState stateForPart(BlockState state, int index) { return state.setValue(PART, index); }
    @Override protected BlockPos partOffset(BlockState state, int index) {
        Direction facing = state.getValue(FACING);
        return BlockPos.ZERO.relative(facing.getClockWise(), index % design.width)
                .relative(facing.getOpposite(), index / design.width);
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (partIndex(state) >= partCount()) return false;
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos floor = pos.below();
        if (!level.hasChunkAt(floor) || !level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) return false;
        // The static roulette spindle rises slightly above the top; validate its clearance too.
        return design != Design.ROULETTE || !Shapes.joinIsNotEmpty(
                getShape(state, level, pos, CollisionContext.empty()).move(0, -1, 0),
                level.getBlockState(pos.above()).getCollisionShape(level, pos.above()), BooleanOp.AND);
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        BlockPos origin = structureOrigin(state, pos);
        for (int part = 0; part < partCount(); part++)
            if (origin.offset(partOffset(state, part)).equals(otherPos) && neighbor != stateForPart(state, part))
                return Blocks.AIR.defaultBlockState();
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        int part = partIndex(state);
        return mirror == Mirror.NONE || part >= partCount() ? result
                : result.setValue(PART, part / design.width * design.width + design.width - 1 - part % design.width);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[design.ordinal()][partIndex(state)][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
