package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Locale;

/** Gym fixtures with shared placement and removal. */
public class GymEquipmentBlock extends MultiBlockDecorationBlock {
    private enum Layout { LONG, WIDE, TALL, HANGING }

    public enum Design implements StringRepresentable {
        TREADMILL(Layout.LONG, GymEquipmentShapes.TREADMILL),
        ROWING_MACHINE(Layout.LONG, GymEquipmentShapes.ROWING_MACHINE),
        BENCH_PRESS(Layout.LONG, GymEquipmentShapes.BENCH_PRESS),
        PUNCHING_BAG(Layout.HANGING, GymEquipmentShapes.PUNCHING_BAG),
        STANDING_PUNCHING_BAG(Layout.TALL, GymEquipmentShapes.STANDING_PUNCHING_BAG),
        EXERCISE_BIKE(Layout.LONG, GymEquipmentShapes.EXERCISE_BIKE),
        WEIGHT_PLATE_RACK(Layout.TALL, GymEquipmentShapes.WEIGHT_PLATE_RACK),
        YOGA_MAT(Layout.LONG, GymEquipmentShapes.YOGA_MAT),
        BARBELL_ON_FLOOR(Layout.WIDE, GymEquipmentShapes.BARBELL_ON_FLOOR),
        GYM_LOCKERS(Layout.TALL, GymEquipmentShapes.GYM_LOCKERS);

        private final Layout layout;
        private final VoxelShape[][] parts;

        Design(Layout layout, VoxelShape[][] parts) {
            this.layout = layout;
            this.parts = parts;
        }

        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    private static final BooleanProperty EXPANDED = BooleanProperty.create("expanded");
    private final Design design;

    //? if >=1.21 {
    public static final MapCodec<GymEquipmentBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((GymEquipmentBlock block) -> block.design),
            propertiesCodec()).apply(instance, GymEquipmentBlock::new));
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}

    public GymEquipmentBlock(Design design, Properties properties) {
        super(properties);
        this.design = design;
    }

    static boolean supported(LevelReader level, BlockPos support, Direction face) {
        return level.hasChunkAt(support) && level.getBlockState(support).isFaceSturdy(level, support, face);
    }

    @Override protected Direction getPlacementFacing(BlockPlaceContext context) {
        if (design.layout == Layout.HANGING && context.getClickedFace() != Direction.DOWN) return null;
        return super.getPlacementFacing(context);
    }

    @Override protected Direction getExtensionDirection(BlockState state) {
        return switch (design.layout) {
            case LONG -> state.getValue(FACING).getOpposite();
            case WIDE -> state.getValue(FACING).getClockWise();
            case TALL -> Direction.UP;
            case HANGING -> Direction.DOWN;
        };
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        return switch (design.layout) {
            case HANGING -> supported(level, structureOrigin(state, pos).above(), Direction.DOWN);
            case TALL -> supported(level, structureOrigin(state, pos).below(), Direction.UP);
            case LONG, WIDE -> supported(level, pos.below(), Direction.UP);
        };
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = super.mirror(state, mirror);
        return design.layout == Layout.WIDE && mirror != Mirror.NONE
                ? stateForPart(mirrored, 1 - partIndex(state)) : mirrored;
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (partCount(state) == 2) {
            Direction extension = getExtensionDirection(state);
            boolean primary = state.getValue(PRIMARY);
            if (side == (primary ? extension : extension.getOpposite()) && neighbor != stateForPart(state, primary ? 1 : 0)
                    && !isRemovingLinkedStructure(state, level, pos)) return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int part = partIndex(state);
        return design.parts[design.layout == Layout.HANGING ? 1 - part : part][RestaurantKitchenBlock.turn(state)];
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    /** The taller console adds a cell over the front, leaving the running deck clear. */
    public static final class Treadmill extends GymEquipmentBlock {
        private static final int CONSOLE_PART = 2;
        private static final BooleanProperty UPPER = BooleanProperty.create("upper");

        //? if >=1.21 {
        public static final MapCodec<Treadmill> CODEC = simpleCodec(Treadmill::new);
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}

        public Treadmill(Properties properties) {
            super(Design.TREADMILL, properties);
            registerDefaultState(defaultBlockState().setValue(EXPANDED, false).setValue(UPPER, false));
        }

        @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
            super.addPartProperties(builder);
            builder.add(EXPANDED, UPPER);
        }

        @Override protected int partCount(BlockState state) { return state.getValue(EXPANDED) ? 3 : 2; }

        @Override protected int partIndex(BlockState state) {
            return state.getValue(EXPANDED) && state.getValue(UPPER) ? CONSOLE_PART : super.partIndex(state);
        }

        @Override protected BlockState stateForPart(BlockState state, int index) {
            return super.stateForPart(state, index).setValue(UPPER, index == CONSOLE_PART);
        }

        @Override protected BlockPos partOffset(BlockState state, int index) {
            return index == CONSOLE_PART ? BlockPos.ZERO.above() : super.partOffset(state, index);
        }

        @Override protected BlockState getPlacementState(BlockPlaceContext context) {
            return super.getPlacementState(context).setValue(EXPANDED, true);
        }

        @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            if (partIndex(state) == CONSOLE_PART) {
                return DecorationSupport.isPlaced(state, level, pos) || supported(level, pos.below(2), Direction.UP);
            }
            return super.canSurvive(state, level, pos);
        }

        @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                                LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
            if (state.getValue(EXPANDED)) {
                Direction rear = getExtensionDirection(state);
                int linkedPart = switch (partIndex(state)) {
                    case 0 -> side == Direction.UP ? CONSOLE_PART : side == rear ? 1 : -1;
                    case CONSOLE_PART -> side == Direction.DOWN ? 0 : -1;
                    default -> side == rear.getOpposite() ? 0 : -1;
                };
                if (linkedPart >= 0 && neighbor != stateForPart(state, linkedPart)
                        && !isRemovingLinkedStructure(state, level, pos)) {
                    return Blocks.AIR.defaultBlockState();
                }
            }
            return super.updateShape(state, side, neighbor, level, pos, neighborPos);
        }

        @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            int part = partIndex(state);
            if (state.getValue(EXPANDED) && part != 1) {
                return GymEquipmentShapes.TALL_TREADMILL[state.getValue(UPPER) ? 1 : 0][RestaurantKitchenBlock.turn(state)];
            }
            return super.getShape(state, level, pos, context);
        }
    }

    /** The original bike and plate rack require no neighboring cell in saved worlds. */
    public static final class ResizedPair extends GymEquipmentBlock {
        public enum Design implements StringRepresentable {
            EXERCISE_BIKE(GymEquipmentBlock.Design.EXERCISE_BIKE, GymEquipmentShapes.OLD_EXERCISE_BIKE),
            WEIGHT_PLATE_RACK(GymEquipmentBlock.Design.WEIGHT_PLATE_RACK, GymEquipmentShapes.OLD_WEIGHT_PLATE_RACK);

            private final GymEquipmentBlock.Design fixture;
            private final VoxelShape[] legacy;

            Design(GymEquipmentBlock.Design fixture, VoxelShape[][] legacy) {
                this.fixture = fixture;
                this.legacy = legacy[0];
            }

            @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
        }

        private final Design design;

        //? if >=1.21 {
        public static final MapCodec<ResizedPair> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((ResizedPair block) -> block.design),
                propertiesCodec()).apply(instance, ResizedPair::new));
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}

        public ResizedPair(Design design, Properties properties) {
            super(design.fixture, properties);
            this.design = design;
            registerDefaultState(defaultBlockState().setValue(EXPANDED, false));
        }

        @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
            super.addPartProperties(builder);
            builder.add(EXPANDED);
        }

        @Override protected int partCount(BlockState state) { return state.getValue(EXPANDED) ? 2 : 1; }
        @Override protected int partIndex(BlockState state) { return state.getValue(EXPANDED) ? super.partIndex(state) : 0; }

        @Override protected BlockState getPlacementState(BlockPlaceContext context) {
            return super.getPlacementState(context).setValue(EXPANDED, true);
        }

        @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getValue(EXPANDED) ? super.getShape(state, level, pos, context)
                    : design.legacy[RestaurantKitchenBlock.turn(state)];
        }
    }

    /** Frames grow upward from a supported floor under each column. */
    private abstract static class Frame extends MultiBlockDecorationBlock {
        Frame(Properties properties) {
            super(properties);
            registerDefaultState(defaultBlockState().setValue(EXPANDED, false));
        }

        protected abstract int width(BlockState state);
        protected abstract VoxelShape[][] parts(BlockState state);

        @Override protected final BlockPos partOffset(BlockState state, int index) {
            int width = width(state);
            return BlockPos.ZERO.relative(state.getValue(FACING).getClockWise(), index % width).above(index / width);
        }

        @Override public final boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            return DecorationSupport.isPlaced(state, level, pos)
                    || supported(level, pos.below(1 + partIndex(state) / width(state)), Direction.UP);
        }

        @Override protected final BlockState getPlacementState(BlockPlaceContext context) {
            return super.getPlacementState(context).setValue(EXPANDED, true);
        }

        @Override public final BlockState mirror(BlockState state, Mirror mirror) {
            BlockState reflected = super.mirror(state, mirror);
            return mirror != Mirror.NONE && width(state) == 2
                    ? stateForPart(reflected, partIndex(state) ^ 1) : reflected;
        }

        @Override public final VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return parts(state)[partIndex(state)][RestaurantKitchenBlock.turn(state)];
        }

        @Override public final VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return getShape(state, level, pos, context);
        }
    }

    public static final class SquatRack extends Frame {
        private static final BooleanProperty RIGHT_PART = BooleanProperty.create("right_part");

        public SquatRack(Properties properties) {
            super(properties);
        }

        @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
            super.addPartProperties(builder);
            builder.add(EXPANDED, RIGHT_PART);
        }

        @Override protected int width(BlockState state) { return state.getValue(EXPANDED) ? 2 : 1; }
        @Override protected int partCount(BlockState state) { return width(state) * 2; }
        @Override protected int partIndex(BlockState state) {
            return (state.getValue(PRIMARY) ? 0 : width(state))
                    + (state.getValue(EXPANDED) && state.getValue(RIGHT_PART) ? 1 : 0);
        }
        @Override protected BlockState stateForPart(BlockState state, int part) {
            int width = width(state);
            return state.setValue(PRIMARY, part < width).setValue(RIGHT_PART, part % width == 1);
        }
        @Override protected VoxelShape[][] parts(BlockState state) {
            return state.getValue(EXPANDED) ? GymEquipmentShapes.SQUAT_RACK : GymEquipmentShapes.OLD_SQUAT_RACK;
        }

        //? if >=1.21 {
        public static final MapCodec<SquatRack> CODEC = simpleCodec(SquatRack::new);
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}
    }

    public static final class CableMachine extends Frame {
        private static final IntegerProperty PART = IntegerProperty.create("part", 0, 5);

        public CableMachine(Properties properties) { super(properties); }

        @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(EXPANDED, PART);
        }
        @Override protected int width(BlockState state) { return 2; }
        @Override protected int partCount(BlockState state) { return state.getValue(EXPANDED) ? 6 : 4; }
        @Override protected int partIndex(BlockState state) { return state.getValue(PART) % partCount(state); }
        @Override protected BlockState stateForPart(BlockState state, int index) { return state.setValue(PART, index); }
        @Override protected VoxelShape[][] parts(BlockState state) {
            return state.getValue(EXPANDED) ? GymEquipmentShapes.CABLE_MACHINE : GymEquipmentShapes.OLD_CABLE_MACHINE;
        }

        //? if >=1.21 {
        public static final MapCodec<CableMachine> CODEC = simpleCodec(CableMachine::new);
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}
    }
}
