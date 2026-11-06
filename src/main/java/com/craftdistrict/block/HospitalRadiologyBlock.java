package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.craftdistrict.entity.MedicalResting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Static radiology fixtures; lifecycle and atomic writes belong to the shared multiblock base. */
public abstract class HospitalRadiologyBlock extends MultiBlockDecorationBlock {
    protected HospitalRadiologyBlock(Properties properties) { super(properties); }
    protected abstract int width();
    protected abstract int depth();
    protected abstract int height();
    protected abstract IntegerProperty partsProperty();
    protected abstract VoxelShape[][] shapes();

    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(partsProperty());
    }
    @Override protected int partCount() { return width() * depth() * height(); }
    @Override protected int partIndex(BlockState state) { return state.getValue(partsProperty()); }
    @Override protected BlockState stateForPart(BlockState state, int index) { return state.setValue(partsProperty(), index); }
    @Override protected BlockPos partOffset(BlockState state, int index) {
        int x = index % width(), z = index / width() % depth(), y = index / (width() * depth());
        Direction front = state.getValue(FACING);
        return BlockPos.ZERO.relative(front.getClockWise(), x).relative(front.getOpposite(), z).above(y);
    }

    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        if (state == null) return null;
        // Floor support is checked only for initial placement, never as a survival dependency.
        for (int index = 0; index < width() * depth(); index++) {
            // Initial placement uses the current dimensions, before any saved-layout flag is selected.
            BlockPos floor = context.getClickedPos().relative(state.getValue(FACING).getClockWise(), index % width())
                    .relative(state.getValue(FACING).getOpposite(), index / width()).below();
            if (context.getLevel().isOutsideBuildHeight(floor) || !context.getLevel().hasChunkAt(floor)
                    || !context.getLevel().getWorldBorder().isWithinBounds(floor)
                    || !context.getLevel().getBlockState(floor).isFaceSturdy(context.getLevel(), floor, Direction.UP))
                return null;
        }
        return state;
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // Removing a floor or neighboring support must not tear down a complete machine.
        return true;
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        int part = partIndex(state);
        return mirror == Mirror.NONE ? result : stateForPart(result,
                part - part % width() + width() - 1 - part % width());
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes()[partIndex(state)][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    /** A handful of envelopes, clipped and rotated once; the scanning openings stay hollow. */
    private static VoxelShape[][] shapes(int width, int depth, int height, double[][] envelopes) {
        VoxelShape[][] result = new VoxelShape[width * depth * height][];
        for (int part = 0; part < result.length; part++) {
            int x = (part % width) * 16, z = (part / width % depth) * 16, y = (part / (width * depth)) * 16;
            VoxelShape shape = Shapes.empty();
            for (double[] b : envelopes) {
                double x0 = Math.max(x, b[0]), y0 = Math.max(y, b[1]), z0 = Math.max(z, b[2]);
                double x1 = Math.min(x + 16, b[3]), y1 = Math.min(y + 16, b[4]), z1 = Math.min(z + 16, b[5]);
                if (x0 < x1 && y0 < y1 && z0 < z1)
                    shape = Shapes.or(shape, box(x0 - x, y0 - y, z0 - z, x1 - x, y1 - y, z1 - z));
            }
            result[part] = RestaurantKitchenBlock.rotations(shape);
        }
        return result;
    }

    /** The existing synchronized resting pose, restricted to the integrated patient couch. */
    public abstract static class Scanner extends HospitalRadiologyBlock implements LyingBlock {
        protected Scanner(Properties properties) { super(properties); }
        @Override public BlockPos lyingHead(BlockState state, BlockPos pos) {
            return structureOrigin(state, pos).relative(state.getValue(FACING).getClockWise())
                    .relative(state.getValue(FACING).getOpposite(), 2);
        }
        @Override public Direction lyingDirection(BlockState state) { return state.getValue(FACING).getOpposite(); }
        @Override public BlockPos lyingExitAnchor(BlockState state, BlockPos head) {
            return head.relative(state.getValue(FACING), 2);
        }
        @Override public AABB lyingClearance(BlockState state, BlockPos head) {
            Vec3 center = Vec3.atBottomCenterOf(head).add(0, lyingHeight() + .025, 0);
            Vec3 along = Vec3.atLowerCornerOf(lyingDirection(state).getNormal());
            Vec3 foot = center.subtract(along.scale(1.5)), tip = center.add(along.scale(.25));
            return new AABB(Math.min(foot.x, tip.x) - .32, center.y, Math.min(foot.z, tip.z) - .32,
                    Math.max(foot.x, tip.x) + .32, center.y + .45, Math.max(foot.z, tip.z) + .32);
        }
        public boolean patientTableHit(BlockState state, BlockPos pos, Vec3 hit) {
            BlockPos origin = structureOrigin(state, pos);
            Direction right = state.getValue(FACING).getClockWise(), back = state.getValue(FACING).getOpposite();
            double dx = hit.x - origin.getX() - .5, dz = hit.z - origin.getZ() - .5;
            double x = 8 + 16 * (dx * right.getStepX() + dz * right.getStepZ());
            double z = 8 + 16 * (dx * back.getStepX() + dz * back.getStepZ());
            double y = 16 * (hit.y - origin.getY());
            return x >= 16 && x <= 32 && z >= 2 && z <= (depth() == 5 ? 68 : 62) && y >= 14.75 && y <= 21.25;
        }
        //? if >=1.21 {
        @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            return patientTableHit(state, pos, hit.getLocation()) ? MedicalResting.lie(state, level, pos, player) : InteractionResult.PASS;
        }
        //?} else {
        /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return hand == InteractionHand.MAIN_HAND && patientTableHit(state, pos, hit.getLocation())
                    ? MedicalResting.lie(state, level, pos, player) : InteractionResult.PASS;
        }
        *///?}
        @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
            if (!isRemovingLinkedStructure(state, level, pos)
                    && (!replacement.is(this) || replacement.getValue(FACING) != state.getValue(FACING)))
                MedicalResting.removeAt(level, lyingHead(state, pos), state);
            super.onRemove(state, level, pos, replacement, moving);
        }
    }

    public static final class Mri extends Scanner {
        private static final IntegerProperty PART = IntegerProperty.create("part", 0, 44);
        private static final VoxelShape[][] SHAPES = HospitalRadiologyBlock.shapes(3, 5, 3, new double[][]{
                {2, 7, 30, 12, 43, 78}, {36, 7, 30, 46, 43, 78},
                {12, 38, 30, 36, 47, 78}, {12, 3, 30, 36, 12, 78},
                {16.5, 16, 2, 31.5, 20.25, 68}, {18, 0, 6, 30, 16, 30}});
        //? if >=1.21 {
        public static final MapCodec<Mri> CODEC = simpleCodec(Mri::new);
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}
        public Mri(Properties properties) { super(properties); }
        @Override public double lyingHeight() { return 20.75 / 16; }
        @Override protected int width() { return 3; }
        @Override protected int depth() { return 5; }
        @Override protected int height() { return 3; }
        @Override protected IntegerProperty partsProperty() { return PART; }
        @Override protected VoxelShape[][] shapes() { return SHAPES; }
    }
    public static final class Ct extends Scanner {
        private static final IntegerProperty PART = IntegerProperty.create("part", 0, 35);
        private static final VoxelShape[][] SHAPES = HospitalRadiologyBlock.shapes(3, 4, 3, new double[][]{
                {2, 7, 36, 12, 43, 56}, {36, 7, 36, 46, 43, 56},
                {12, 38, 36, 36, 47, 56}, {12, 3, 36, 36, 12, 56},
                {16, 15, 2, 32, 19.25, 62}, {18.5, 0, 5, 29.5, 15, 30}});
        //? if >=1.21 {
        public static final MapCodec<Ct> CODEC = simpleCodec(Ct::new);
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}
        public Ct(Properties properties) { super(properties); }
        @Override public double lyingHeight() { return 19.75 / 16; }
        @Override protected int width() { return 3; }
        @Override protected int depth() { return 4; }
        @Override protected int height() { return 3; }
        @Override protected IntegerProperty partsProperty() { return PART; }
        @Override protected VoxelShape[][] shapes() { return SHAPES; }
    }
    public static final class Console extends HospitalRadiologyBlock {
        public static final BooleanProperty EXPANDED = BooleanProperty.create("expanded");
        private static final IntegerProperty PART = IntegerProperty.create("part", 0, 5);
        private static final VoxelShape[][] LEGACY_SHAPES = HospitalRadiologyBlock.shapes(2, 1, 2, new double[][]{
                {0, 13.5, 0, 32, 14.75, 16},
                {0.75, 0, 1, 8.5, 13.5, 15.5},
                {24, 0, 1, 31.25, 13.5, 15.5},
                {8.5, 7.5, 14.75, 24, 13.5, 15.5},
                {3.25, 18.5, 8.5, 11.667, 28, 12.75},
                {11.667, 18.5, 11.75, 20.333, 28, 13.25},
                {20.333, 18.5, 8.5, 28.75, 28, 12.75},
                {5.25, 14.75, 9.5, 9.25, 18.5, 13.5},
                {15.5, 14.75, 12.25, 16.5, 18.5, 14},
                {22.75, 14.75, 9.5, 26.75, 18.5, 13.5}});
        private static final VoxelShape[][] SHAPES = HospitalRadiologyBlock.shapes(3, 1, 2, new double[][]{
                {0, 13.5, 0, 48, 14.75, 16},
                {0.75, 0, 1, 8.5, 13.5, 15.5},
                {39.5, 0, 1, 47.25, 13.5, 15.5},
                {8.5, 7.5, 14.75, 39.5, 13.5, 15.5},
                {5, 18.5, 7, 17.5, 28, 12.75},
                {17.5, 18.5, 11.75, 30.5, 28, 13.25},
                {30.5, 18.5, 7, 43, 28, 12.75},
                {8.5, 14.75, 8.5, 13.5, 18.5, 13.25},
                {23.5, 14.75, 12.25, 24.5, 18.5, 14},
                {34.5, 14.75, 8.5, 39.5, 18.5, 13.25}});
        //? if >=1.21 {
        public static final MapCodec<Console> CODEC = simpleCodec(Console::new);
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}
        public Console(Properties properties) { super(properties); registerDefaultState(defaultBlockState().setValue(EXPANDED, false)); }
        @Override protected int width() { return 3; }
        @Override protected int depth() { return 1; }
        @Override protected int height() { return 2; }
        @Override protected IntegerProperty partsProperty() { return PART; }
        @Override protected VoxelShape[][] shapes() { return SHAPES; }
        @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
            super.addPartProperties(builder); builder.add(EXPANDED);
        }
        private int layoutWidth(BlockState state) { return state.getValue(EXPANDED) ? 3 : 2; }
        @Override protected int partCount(BlockState state) { return layoutWidth(state) * 2; }
        @Override protected int partIndex(BlockState state) { return state.getValue(EXPANDED) ? state.getValue(PART) : state.getValue(PART) % 4; }
        @Override protected BlockPos partOffset(BlockState state, int index) {
            int w = layoutWidth(state);
            return BlockPos.ZERO.relative(state.getValue(FACING).getClockWise(), index % w).above(index / w);
        }
        @Override protected BlockState getPlacementState(BlockPlaceContext context) {
            BlockState state = super.getPlacementState(context);
            return state == null ? null : state.setValue(EXPANDED, true);
        }
        @Override public BlockState mirror(BlockState state, Mirror mirror) {
            if (mirror == Mirror.NONE) return state;
            int part = state.getValue(PART), w = layoutWidth(state);
            return stateForPart(state.rotate(mirror.getRotation(state.getValue(FACING))), part - part % w + w - 1 - part % w);
        }
        @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return (state.getValue(EXPANDED) ? SHAPES : LEGACY_SHAPES)[partIndex(state)][RestaurantKitchenBlock.turn(state)];
        }
    }
    public static final class ApronRack extends HospitalRadiologyBlock {
        private static final IntegerProperty PART = IntegerProperty.create("part", 0, 3);
        private static final VoxelShape[][] SHAPES = HospitalRadiologyBlock.shapes(2, 1, 2, new double[][]{
                {1, 0, 1, 4, 2, 15}, {28, 0, 1, 31, 2, 15},
                {2, 2, 10, 3.25, 29.5, 11.25}, {28.75, 2, 10, 30, 29.5, 11.25},
                {2, 29.5, 10, 30, 31, 11.25}, {4, 8.5, 6.25, 10.5, 27, 12.25},
                {12.75, 8.5, 6.25, 19.25, 27, 12.25}, {21.5, 8.5, 6.25, 28, 27, 12.25}});
        //? if >=1.21 {
        public static final MapCodec<ApronRack> CODEC = simpleCodec(ApronRack::new);
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}
        public ApronRack(Properties properties) { super(properties); }
        @Override protected int width() { return 2; }
        @Override protected int depth() { return 1; }
        @Override protected int height() { return 2; }
        @Override protected IntegerProperty partsProperty() { return PART; }
        @Override protected VoxelShape[][] shapes() { return SHAPES; }
    }

    public static final class MobileXray extends HospitalRadiologyBlock {
        private static final IntegerProperty PART = IntegerProperty.create("part", 0, 11);
        private static final VoxelShape[][] SHAPES = HospitalRadiologyBlock.shapes(2, 2, 3, new double[][]{
                {3.75, 0, 22.5, 7.25, 6, 28.5},
                {24.75, 0, 22.5, 27.25, 6, 28.5},
                {8, 0, 3.5, 11, 5.5, 8},
                {20.5, 0, 3.5, 23.5, 5.5, 8},
                {7, 3.5, 13.5, 24.5, 8, 29.5},
                {8, 3.5, 3.5, 23.5, 5.5, 13.5},
                {7, 8, 15, 23.5, 22, 29},
                {7, 21, 22, 13, 46.5, 28},
                {13, 42, 23, 27, 44.5, 27},
                {23, 38, 10, 27, 42, 24},
                {19.5, 28.75, 6, 30.5, 38.5, 14.5},
                {7.5, 21.5, 17, 18, 27, 19.5}});
        //? if >=1.21 {
        public static final MapCodec<MobileXray> CODEC = simpleCodec(MobileXray::new);
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}
        public MobileXray(Properties properties) { super(properties); }
        @Override protected int width() { return 2; }
        @Override protected int depth() { return 2; }
        @Override protected int height() { return 3; }
        @Override protected IntegerProperty partsProperty() { return PART; }
        @Override protected VoxelShape[][] shapes() { return SHAPES; }
    }

    public static final class CArm extends HospitalRadiologyBlock {
        private static final IntegerProperty PART = IntegerProperty.create("part", 0, 17);
        private static final VoxelShape[][] SHAPES = HospitalRadiologyBlock.shapes(3, 2, 3, new double[][]{
                {5.25, 0, 3.75, 7.75, 5, 8.25},
                {5.25, 0, 24.25, 7.75, 5, 28.75},
                {41.25, 0, 3.75, 43.75, 5, 8.25},
                {41.25, 0, 24.25, 43.75, 5, 28.75},
                {4.5, 3.5, 16, 20.5, 7, 29.5},
                {4.5, 3.5, 5, 9.5, 5.5, 16},
                {20.5, 3.5, 24, 44.5, 5.5, 28.5},
                {40.5, 3.5, 5, 44.5, 5.5, 24},
                {5.0, 7.0, 17.0, 20.0, 24.0, 29.0},
                {6.0, 25.0, 20.5, 17.0, 31.0, 22.5},
                {17.0, 23.0, 24.0, 35.0, 29.0, 29.5},
                {32.0, 6.0, 6.0, 38.375, 10.0, 20.0},
                {32.0, 10.0, 16.5, 38.375, 14.0, 25.0},
                {32.0, 14.0, 21.0, 38.375, 18.0, 28.0},
                {32.0, 18.0, 23.0, 38.375, 22.0, 29.5},
                {32.0, 22.0, 24.0, 38.375, 28.0, 30.0},
                {32.0, 28.0, 23.0, 38.375, 32.0, 29.5},
                {32.0, 32.0, 21.0, 38.375, 36.0, 28.0},
                {32.0, 36.0, 16.5, 38.375, 40.0, 25.0},
                {32.0, 40.0, 6.0, 38.375, 44.0, 20.0},
                {30.0, 35.75, 2.0, 40.0, 40.0, 11.0},
                {29.5, 9.0, 2.0, 40.5, 15.5, 11.0}});
        //? if >=1.21 {
        public static final MapCodec<CArm> CODEC = simpleCodec(CArm::new);
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}
        public CArm(Properties properties) { super(properties); }
        @Override protected int width() { return 3; }
        @Override protected int depth() { return 2; }
        @Override protected int height() { return 3; }
        @Override protected IntegerProperty partsProperty() { return PART; }
        @Override protected VoxelShape[][] shapes() { return SHAPES; }
    }

    public static final class Ultrasound extends HospitalRadiologyBlock {
        private static final IntegerProperty PART = IntegerProperty.create("part", 0, 1);
        private static final VoxelShape[][] SHAPES = HospitalRadiologyBlock.shapes(1, 1, 2, new double[][]{
                {1, 0, 1, 15, 5.5, 15},
                {2.25, 5.5, 4, 13.75, 16.5, 13.5},
                {1.75, 16, 2.5, 14.25, 21.5, 12.5},
                {2, 23, 10.75, 14, 31.5, 12},
                {6.75, 19.5, 11, 9.25, 23, 13},
                {0.5, 13, 6, 2.25, 21, 10},
                {13.75, 13, 6, 15.5, 21, 10}});
        //? if >=1.21 {
        public static final MapCodec<Ultrasound> CODEC = simpleCodec(Ultrasound::new);
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}
        public Ultrasound(Properties properties) { super(properties); }
        @Override protected int width() { return 1; }
        @Override protected int depth() { return 1; }
        @Override protected int height() { return 2; }
        @Override protected IntegerProperty partsProperty() { return PART; }
        @Override protected VoxelShape[][] shapes() { return SHAPES; }
    }

    public static final class Mammography extends HospitalRadiologyBlock {
        private static final IntegerProperty PART = IntegerProperty.create("part", 0, 5);
        private static final VoxelShape[][] SHAPES = HospitalRadiologyBlock.shapes(1, 2, 3, new double[][]{
                {1, 0, 3, 15, 2.5, 30},
                {4, 2.5, 24, 12, 46.5, 29},
                {5.5, 33, 21, 10.5, 40, 24},
                {3, 34, 8, 13, 41, 21},
                {4, 31.75, 10, 12, 34, 17},
                {2.75, 18, 6, 13.25, 20, 21},
                {5.5, 16.5, 18, 10.5, 18, 24},
                {3, 25, 6, 13, 25.5, 23.5},
                {7, 26, 22, 9, 33, 24}});
        //? if >=1.21 {
        public static final MapCodec<Mammography> CODEC = simpleCodec(Mammography::new);
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}
        public Mammography(Properties properties) { super(properties); }
        @Override protected int width() { return 1; }
        @Override protected int depth() { return 2; }
        @Override protected int height() { return 3; }
        @Override protected IntegerProperty partsProperty() { return PART; }
        @Override protected VoxelShape[][] shapes() { return SHAPES; }
    }

}

