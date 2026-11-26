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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Locale;

/** Static floor games using the shared placement and single-drop removal for linked decorations. */
public abstract class GameTableBlock extends MultiBlockDecorationBlock {
    private final GameTableShapes.Layout layout;

    private GameTableBlock(GameTableShapes.Layout layout, Properties properties) {
        super(properties);
        this.layout = layout;
    }

    @Override protected final int partCount() { return layout.collision().length; }

    protected GameTableShapes.Layout layout(BlockState state) { return layout; }

    @Override protected final int partCount(BlockState state) { return layout(state).collision().length; }

    @Override protected final BlockPos partOffset(BlockState state, int index) {
        Direction facing = state.getValue(FACING);
        int width = layout(state).width();
        return BlockPos.ZERO.relative(facing.getClockWise(), index % width)
                .relative(facing.getOpposite(), index / width);
    }

    @Override public final boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos floor = pos.below();
        return level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }

    @Override public final BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                                  LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (side.getAxis().isHorizontal()) {
            int linked = adjacentPart(state, side);
            if (linked >= 0 && neighbor != stateForPart(state, linked) && !isRemovingLinkedStructure(state, level, pos)) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, side, neighbor, level, pos, otherPos);
    }

    private int adjacentPart(BlockState state, Direction side) {
        Direction front = state.getValue(FACING);
        int part = partIndex(state);
        int width = layout(state).width();
        if (side == front) return part >= width ? part - width : -1;
        if (side == front.getOpposite()) return part + width < partCount(state) ? part + width : -1;
        if (side == front.getClockWise()) return part % width < width - 1 ? part + 1 : -1;
        return part % width > 0 ? part - 1 : -1;
    }

    @Override public final VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return layout(state).outline()[partIndex(state)][RestaurantKitchenBlock.turn(state)];
    }

    @Override public final VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return layout(state).collision()[partIndex(state)][RestaurantKitchenBlock.turn(state)];
    }

    public static class Narrow extends GameTableBlock {
        public enum Design implements StringRepresentable {
            FOOSBALL(GameTableShapes.FOOSBALL_LEGACY), AIR_HOCKEY(GameTableShapes.AIR_HOCKEY_LEGACY);

            private final GameTableShapes.Layout layout;
            Design(GameTableShapes.Layout layout) { this.layout = layout; }
            @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
        }

        private final Design design;

        //? if >=1.21 {
        public static final MapCodec<Narrow> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((Narrow table) -> table.design),
                propertiesCodec()).apply(instance, Narrow::new));
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}

        public Narrow(Design design, Properties properties) {
            super(design.layout, properties);
            this.design = design;
        }
    }

    /** Part zero retains a table's original footprint and primary flag in saved worlds. */
    private abstract static class Expanded extends Narrow {
        private final GameTableShapes.Layout expandedLayout;

        Expanded(Design design, GameTableShapes.Layout expandedLayout, Properties properties) {
            super(design, properties);
            this.expandedLayout = expandedLayout;
            registerDefaultState(defaultBlockState().setValue(partProperty(), 0).setValue(PRIMARY, true));
        }

        protected abstract IntegerProperty partProperty();

        @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
            super.addPartProperties(builder);
            builder.add(partProperty());
        }

        @Override protected BlockState getPlacementState(BlockPlaceContext context) {
            return super.getPlacementState(context).setValue(partProperty(), 1);
        }

        @Override protected GameTableShapes.Layout layout(BlockState state) {
            return state.getValue(partProperty()) == 0 ? super.layout(state) : expandedLayout;
        }

        @Override protected int partIndex(BlockState state) {
            int part = state.getValue(partProperty());
            return part == 0 ? super.partIndex(state) : part - 1;
        }

        @Override protected BlockState stateForPart(BlockState state, int index) {
            IntegerProperty property = partProperty();
            return state.getValue(property) == 0 ? super.stateForPart(state, index) : state.setValue(property, index + 1);
        }

        @Override public BlockState mirror(BlockState state, Mirror mirror) {
            BlockState rotated = super.mirror(state, mirror);
            return mirror == Mirror.NONE || state.getValue(partProperty()) == 0 ? rotated : stateForPart(rotated, partIndex(state) ^ 1);
        }
    }

    public static final class Foosball extends Expanded {
        private static final IntegerProperty PART = IntegerProperty.create("part", 0, 4);
        //? if >=1.21 {
        public static final MapCodec<Foosball> CODEC = simpleCodec(Foosball::new);
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}

        public Foosball(Properties properties) { super(Design.FOOSBALL, GameTableShapes.FOOSBALL, properties); }
        @Override protected IntegerProperty partProperty() { return PART; }
    }

    public static final class AirHockey extends Expanded {
        private static final IntegerProperty PART = IntegerProperty.create("part", 0, 6);
        //? if >=1.21 {
        public static final MapCodec<AirHockey> CODEC = simpleCodec(AirHockey::new);
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}

        public AirHockey(Properties properties) { super(Design.AIR_HOCKEY, GameTableShapes.AIR_HOCKEY, properties); }
        @Override protected IntegerProperty partProperty() { return PART; }
    }

    public static final class Wide extends GameTableBlock {
        private static final IntegerProperty PART = IntegerProperty.create("part", 0, 5);

        public enum Design implements StringRepresentable {
            BILLIARD(GameTableShapes.BILLIARD), TABLE_TENNIS(GameTableShapes.TABLE_TENNIS);

            private final GameTableShapes.Layout layout;
            Design(GameTableShapes.Layout layout) { this.layout = layout; }
            @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
        }

        private final Design design;

        //? if >=1.21 {
        public static final MapCodec<Wide> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((Wide table) -> table.design),
                propertiesCodec()).apply(instance, Wide::new));
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}

        public Wide(Design design, Properties properties) {
            super(design.layout, properties);
            this.design = design;
        }

        @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) { builder.add(PART); }
        @Override protected int partIndex(BlockState state) { return state.getValue(PART); }
        @Override protected BlockState stateForPart(BlockState state, int index) { return state.setValue(PART, index); }

        @Override public BlockState mirror(BlockState state, Mirror mirror) {
            BlockState rotated = super.mirror(state, mirror);
            // Mirroring reverses the two columns while retaining the row relative to the front.
            return mirror == Mirror.NONE ? rotated : stateForPart(rotated, partIndex(state) ^ 1);
        }
    }
}
