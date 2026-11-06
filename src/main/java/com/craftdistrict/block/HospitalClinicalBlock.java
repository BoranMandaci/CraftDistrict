package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import com.craftdistrict.entity.Seating;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Clinical equipment shares atomic multiblock placement and single-drop removal. */
public abstract class HospitalClinicalBlock extends MultiBlockDecorationBlock {
    private static final IntegerProperty TALL_PART = IntegerProperty.create("part", 0, 1);
    private static final IntegerProperty LONG_PART = IntegerProperty.create("part", 0, 3);

    protected HospitalClinicalBlock(Properties properties) { super(properties); }
    protected int depth() { return 1; }
    protected abstract VoxelShape[][] shapes();
    private IntegerProperty partsProperty() { return depth() == 1 ? TALL_PART : LONG_PART; }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) { builder.add(partsProperty()); }
    @Override protected int partCount() { return depth() * 2; }
    @Override protected int partIndex(BlockState state) { return state.getValue(partsProperty()); }
    @Override protected BlockState stateForPart(BlockState state, int index) { return state.setValue(partsProperty(), index); }
    @Override protected BlockPos partOffset(BlockState state, int index) {
        return BlockPos.ZERO.relative(state.getValue(FACING).getOpposite(), index % depth()).above(index / depth());
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        if (state == null) return null;
        for (int z = 0; z < depth(); z++) {
            BlockPos floor = context.getClickedPos().relative(state.getValue(FACING).getOpposite(), z).below();
            if (context.getLevel().isOutsideBuildHeight(floor) || !context.getLevel().hasChunkAt(floor)
                    || !context.getLevel().getWorldBorder().isWithinBounds(floor)
                    || !context.getLevel().getBlockState(floor).isFaceSturdy(context.getLevel(), floor, Direction.UP)) return null;
        }
        return state;
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) { return true; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes()[partIndex(state)][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    private static VoxelShape[][] shapes(int depth, double[]... envelopes) {
        VoxelShape[][] result = new VoxelShape[depth * 2][];
        for (int part = 0; part < result.length; part++) {
            int z = part % depth * 16, y = part / depth * 16;
            VoxelShape shape = Shapes.empty();
            for (double[] b : envelopes) {
                double y0 = Math.max(y, b[1]), z0 = Math.max(z, b[2]);
                double y1 = Math.min(y + 16, b[4]), z1 = Math.min(z + 16, b[5]);
                if (y0 < y1 && z0 < z1) shape = Shapes.or(shape, box(b[0], y0 - y, z0 - z, b[3], y1 - y, z1 - z));
            }
            result[part] = RestaurantKitchenBlock.rotations(shape.optimize());
        }
        return result;
    }

    public static final class Autoclave extends CashRegisterBlock {
        public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
        private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(Shapes.or(
                box(0, 0, 2, 16, 16, 16), box(1.5, 2, 0, 14.5, 12.75, 2)).optimize());
        private static final VoxelShape[] OPEN_SHAPES = RestaurantKitchenBlock.rotations(Shapes.or(
                box(0, 0, 2, 1.5, 16, 16), box(14.5, 0, 2, 16, 16, 16),
                box(1.5, 0, 2, 14.5, 2.5, 16), box(1.5, 12.5, 2, 14.5, 16, 16),
                box(1.5, 2.5, 14.5, 14.5, 12.5, 16), box(.25, 2.25, -11, 1.75, 12.5, 1.5)).optimize());
        //? if >=1.21 {
        public static final MapCodec<Autoclave> CODEC = simpleCodec(Autoclave::new);
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}
        public Autoclave(Properties properties) {
            super(properties);
            registerDefaultState(defaultBlockState().setValue(OPEN, false));
        }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(OPEN);
        }
        InteractionResult toggleDoor(BlockState state, Level level, BlockPos pos, Player player) {
            return CabinetInteraction.toggle(state, level, pos, player, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_DOOR_CLOSE);
        }
        //? if >=1.21 {
        @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            return toggleDoor(state, level, pos, player);
        }
        //?} else {
        /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return hand == InteractionHand.MAIN_HAND ? toggleDoor(state, level, pos, player) : InteractionResult.PASS;
        }
        *///?}
        @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return (state.getValue(OPEN) ? OPEN_SHAPES : SHAPES)[RestaurantKitchenBlock.turn(state)];
        }
    }
    public static final class Endoscopy extends HospitalClinicalBlock {
        private static final VoxelShape[][] SHAPES = HospitalClinicalBlock.shapes(1,
                new double[]{2, 0, 2, 14, 3, 14}, new double[]{2, 3, 3, 14, 20.125, 14.5},
                new double[]{7.25, 20, 10.75, 8.75, 23, 12}, new double[]{1.75, 21.75, 9.5, 14.25, 30.25, 11.5});
        //? if >=1.21 {
        public static final MapCodec<Endoscopy> CODEC = simpleCodec(Endoscopy::new);
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}
        public Endoscopy(Properties properties) { super(properties); }
        @Override protected VoxelShape[][] shapes() { return SHAPES; }
    }
    public static final class Electrosurgical extends HospitalClinicalBlock {
        // Keep the saved part=1 state for old carts, but reserve only one cell for new placements.
        private static final VoxelShape[][] SHAPES = HospitalClinicalBlock.shapes(1,
                new double[]{2, 0, 3.25, 14, 8.75, 13.5}, new double[]{4, 8.75, 7.75, 12, 10.25, 9.25});
        //? if >=1.21 {
        public static final MapCodec<Electrosurgical> CODEC = simpleCodec(Electrosurgical::new);
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}
        public Electrosurgical(Properties properties) { super(properties); }
        @Override protected int partCount() { return 1; }
        @Override protected VoxelShape[][] shapes() { return SHAPES; }
        @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
            if (!level.isClientSide && partIndex(state) == 0
                    && (!replacement.is(this) || replacement.getValue(FACING) != state.getValue(FACING))) {
                BlockPos upperPos = pos.above();
                BlockState upper = level.getBlockState(upperPos);
                if (upper.is(this) && partIndex(upper) == 1 && upper.getValue(FACING) == state.getValue(FACING))
                    level.setBlock(upperPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            super.onRemove(state, level, pos, replacement, moving);
        }
    }
    public static final class Ekg extends HospitalClinicalBlock {
        private static final VoxelShape[][] SHAPES = HospitalClinicalBlock.shapes(1,
                new double[]{2, 0, 2, 14, 3, 14}, new double[]{7, 3, 8, 9, 12, 10},
                new double[]{1, 12, 2, 15, 19, 14}, new double[]{2.5, 18.5, 9, 13.5, 25.5, 11});
        //? if >=1.21 {
        public static final MapCodec<Ekg> CODEC = simpleCodec(Ekg::new);
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}
        public Ekg(Properties properties) { super(properties); }
        @Override protected VoxelShape[][] shapes() { return SHAPES; }
    }
    public static final class Incubator extends HospitalClinicalBlock {
        private static final VoxelShape[][] SHAPES = HospitalClinicalBlock.shapes(2,
                new double[]{2, 0, 2, 14, 3, 30}, new double[]{4, 3, 12, 12, 7, 20},
                new double[]{2, 7, 2, 14, 12, 30}, new double[]{2, 12, 3, 14, 24, 29});
        //? if >=1.21 {
        public static final MapCodec<Incubator> CODEC = simpleCodec(Incubator::new);
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}
        public Incubator(Properties properties) { super(properties); }
        @Override protected int depth() { return 2; }
        @Override protected VoxelShape[][] shapes() { return SHAPES; }
    }
    public static final class DentalChair extends HospitalClinicalBlock implements SittableBlock {
        private static final VoxelShape[][] SHAPES = HospitalClinicalBlock.shapes(2,
                new double[]{4, 0, 8, 12, 3, 24}, new double[]{6, 3, 10, 10, 8.5, 17},
                new double[]{3.5, 8.5, 9, 12.5, 11, 17.5}, new double[]{3.5, 3.5, 4, 12.5, 8.5, 10},
                new double[]{3.5, 11, 17.5, 12.5, 26, 27},
                new double[]{1.5, 10, 9.5, 3.5, 14, 17.5}, new double[]{12.5, 10, 9.5, 14.5, 14, 17.5});
        //? if >=1.21 {
        public static final MapCodec<DentalChair> CODEC = simpleCodec(DentalChair::new);
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}
        public DentalChair(Properties properties) { super(properties); }
        @Override protected int depth() { return 2; }
        @Override protected VoxelShape[][] shapes() { return SHAPES; }
        @Override public double seatHeight() { return 11.0 / 16; }
        @Override public double forwardOffset() { return -3.5 / 16; }
        @Override public boolean isSittable(BlockState state) { return partIndex(state) == 0; }
        private InteractionResult sit(BlockState state, Level level, BlockPos pos, Player player) {
            BlockPos seat = structureOrigin(state, pos);
            BlockState anchor = level.getBlockState(seat);
            return anchor.is(this) && anchor.getValue(FACING) == state.getValue(FACING)
                    ? Seating.sit(anchor, level, seat, player) : InteractionResult.PASS;
        }
        //? if >=1.21 {
        @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            return sit(state, level, pos, player);
        }
        //?} else {
        /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return hand == InteractionHand.MAIN_HAND ? sit(state, level, pos, player) : InteractionResult.PASS;
        }
        *///?}
        @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
            if (!isRemovingLinkedStructure(state, level, pos)
                    && (!replacement.is(this) || replacement.getValue(FACING) != state.getValue(FACING)))
                Seating.removeAt(level, structureOrigin(state, pos));
            super.onRemove(state, level, pos, replacement, moving);
        }
    }
}
