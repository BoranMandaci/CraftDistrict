package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import com.craftdistrict.lighting.LightControls;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Ceiling fixtures whose linked parts share one light controller. */
public abstract class CasinoHangingFixtureBlock extends MultiBlockDecorationBlock implements ControlledLight {
    protected CasinoHangingFixtureBlock(Properties properties) {
        super(properties);
    }

    protected abstract IntegerProperty partProperty();
    protected abstract int mirroredPart(int part);
    protected abstract BlockPos ceiling(BlockState state, BlockPos pos);

    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(partProperty());
    }

    @Override protected int partIndex(BlockState state) { return state.getValue(partProperty()); }
    @Override protected BlockState stateForPart(BlockState state, int index) { return state.setValue(partProperty(), index); }

    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        return context.getClickedFace() == Direction.DOWN ? super.getPlacementState(context) : null;
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos support = ceiling(state, pos);
        return level.hasChunkAt(support) && level.getBlockState(support).isFaceSturdy(level, support, Direction.DOWN);
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        BlockPos origin = structureOrigin(state, pos);
        for (int part = 0; part < partCount(state); part++) {
            if (origin.offset(partOffset(state, part)).equals(neighborPos)
                    && LightControls.shape(neighbor) != LightControls.shape(stateForPart(state, part))) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? mirrored : stateForPart(mirrored, mirroredPart(partIndex(state)));
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    // Approximate a diagonal rail with short joined boxes, leaving the center of each ring clear.
    private static VoxelShape diagonal(double x0, double z0, double x1, double z1,
                                       double lowY, double highY, double radius) {
        VoxelShape result = Shapes.empty();
        for (int i = 0; i < 4; i++) {
            double startX = Mth.lerp(i / 4.0, x0, x1), endX = Mth.lerp((i + 1) / 4.0, x0, x1);
            double startZ = Mth.lerp(i / 4.0, z0, z1), endZ = Mth.lerp((i + 1) / 4.0, z0, z1);
            result = Shapes.or(result, box(
                    Math.max(0, Math.min(startX, endX) - radius), lowY, Math.max(0, Math.min(startZ, endZ) - radius),
                    Math.min(16, Math.max(startX, endX) + radius), highY, Math.min(16, Math.max(startZ, endZ) + radius)));
        }
        return result;
    }

    public static final class Ring extends CasinoHangingFixtureBlock {
        private static final IntegerProperty PART = IntegerProperty.create("part", 0, 3);
        private static final int[] TURNS = {0, 1, 3, 2};
        private static final VoxelShape[] QUARTER = RestaurantKitchenBlock.rotations(Shapes.or(
                box(13.36, 6.65, 2.98, 16, 7.92, 4.50), box(2.98, 6.65, 13.36, 4.50, 7.92, 16),
                diagonal(13.56, 3.74, 9.05, 5.61, 6.65, 7.92, .76),
                diagonal(9.05, 5.61, 5.61, 9.05, 6.65, 7.92, .76),
                diagonal(5.61, 9.05, 3.74, 13.56, 6.65, 7.92, .76),
                box(6.67, 7.92, 6.67, 7.99, 8.28, 7.99), box(7.16, 8.28, 7.16, 7.50, 14.86, 7.50),
                box(6.33, 14.86, 6.33, 8.33, 16, 8.33)));

        public Ring(Properties properties) { super(properties); }
        @Override protected IntegerProperty partProperty() { return PART; }
        @Override protected int partCount() { return 4; }
        @Override protected int mirroredPart(int part) { return part ^ 1; }
        @Override protected BlockPos ceiling(BlockState state, BlockPos pos) { return pos.above(); }
        @Override protected BlockPos partOffset(BlockState state, int part) {
            Direction facing = state.getValue(FACING);
            return BlockPos.ZERO.relative(facing.getClockWise(), part % 2).relative(facing.getOpposite(), part / 2);
        }
        @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return QUARTER[(RestaurantKitchenBlock.turn(state) + TURNS[partIndex(state)]) % 4];
        }
        //? if >=1.21 {
        public static final MapCodec<Ring> CODEC = simpleCodec(Ring::new);
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}
    }

    public static final class Grand extends CasinoHangingFixtureBlock {
        private static final IntegerProperty PART = IntegerProperty.create("part", 0, 9);
        private static final int[] TURNS = {0, 0, 0, 1, 3, 0, 1, 3, 2, 2};
        private static final VoxelShape[] CANOPY = RestaurantKitchenBlock.rotations(Shapes.or(
                box(4.64, 14.61, 4.64, 11.36, 16, 11.36), box(5.88, 12.85, 5.88, 10.12, 14.61, 10.12),
                box(7.08, 0, 7.08, 8.92, 12.85, 8.92)));
        private static final VoxelShape[] CORNER = RestaurantKitchenBlock.rotations(Shapes.or(
                diagonal(5.91, 16, 16, 5.91, 9.96, 11.52, .65),
                box(6.94, 4.86, 12.60, 9.28, 9.96, 14.94), box(12.60, 4.86, 6.94, 14.94, 9.96, 9.28)));
        private static final VoxelShape[] EDGE = RestaurantKitchenBlock.rotations(Shapes.or(
                box(0, 9.96, 4.95, 16, 11.52, 6.16), box(7.31, 11.52, 5.01, 8.69, 13.39, 16),
                box(1.56, 5.38, 4.52, 3.62, 9.96, 6.58), box(6.79, 4.13, 4.34, 9.21, 9.96, 6.76),
                box(12.38, 5.38, 4.52, 14.44, 9.96, 6.58)));
        private static final VoxelShape[] CENTER = RestaurantKitchenBlock.rotations(Shapes.or(
                box(6.98, 6.63, 6.98, 9.02, 16, 9.02), box(7.52, 12.67, 0, 8.48, 13.39, 16),
                box(0, 12.67, 7.52, 16, 13.39, 8.48), box(6.44, 12.41, 6.44, 9.56, 13.8, 9.56),
                box(6.04, .64, 6.04, 9.96, 6.63, 9.96), box(7.64, 6.98, 2.53, 8.36, 7.29, 13.47),
                box(2.53, 6.98, 7.64, 13.47, 7.29, 8.36),
                box(5.84, 5.63, 2.36, 10.16, 6.98, 3.54), box(5.84, 5.63, 12.46, 10.16, 6.98, 13.64),
                box(2.36, 5.63, 5.84, 3.54, 6.98, 10.16), box(12.46, 5.63, 5.84, 13.64, 6.98, 10.16),
                diagonal(5.91, 2.95, 2.95, 5.91, 5.63, 6.98, .59),
                diagonal(10.09, 2.95, 13.05, 5.91, 5.63, 6.98, .59),
                diagonal(5.91, 13.05, 2.95, 10.09, 5.63, 6.98, .59),
                diagonal(10.09, 13.05, 13.05, 10.09, 5.63, 6.98, .59),
                box(7.14, 2.77, 2.12, 8.86, 5.63, 3.84), box(7.14, 2.77, 12.16, 8.86, 5.63, 13.88),
                box(2.12, 2.77, 7.14, 3.84, 5.63, 8.86), box(12.16, 2.77, 7.14, 13.88, 5.63, 8.86)));

        public Grand(Properties properties) { super(properties); }
        @Override protected IntegerProperty partProperty() { return PART; }
        @Override protected int partCount() { return 10; }
        @Override protected int mirroredPart(int part) {
            if (part == 0) return 0;
            int index = part - 1;
            return index / 3 * 3 + 3 - index % 3;
        }
        @Override protected BlockPos ceiling(BlockState state, BlockPos pos) { return structureOrigin(state, pos).above(); }
        @Override protected BlockPos partOffset(BlockState state, int part) {
            if (part == 0) return BlockPos.ZERO;
            int index = part - 1;
            Direction facing = state.getValue(FACING);
            return BlockPos.ZERO.relative(facing.getClockWise(), index % 3 - 1)
                    .relative(facing.getOpposite(), index / 3 - 1).below();
        }
        @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            int part = partIndex(state);
            VoxelShape[] shape = switch (part) {
                case 0 -> CANOPY;
                case 5 -> CENTER;
                case 2, 4, 6, 8 -> EDGE;
                default -> CORNER;
            };
            return shape[(RestaurantKitchenBlock.turn(state) + TURNS[part]) % 4];
        }
        //? if >=1.21 {
        public static final MapCodec<Grand> CODEC = simpleCodec(Grand::new);
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}
    }
}
