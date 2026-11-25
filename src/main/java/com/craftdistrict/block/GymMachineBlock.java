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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Locale;

/** Static floor machines with collision cached for each part and facing. */
public class GymMachineBlock extends MultiBlockDecorationBlock {
    private enum Layout {
        FRONT_TOWER(BlockPos.ZERO, new BlockPos(0, 0, 1), new BlockPos(0, 1, 0)),
        WIDE_FRAME(BlockPos.ZERO, new BlockPos(1, 0, 0), new BlockPos(0, 1, 0),
                new BlockPos(1, 1, 0), new BlockPos(0, 2, 0), new BlockPos(1, 2, 0));

        private final BlockPos[] parts;

        Layout(BlockPos... parts) {
            this.parts = parts;
        }
    }

    public enum Design implements StringRepresentable {
        STAIR_CLIMBER(Layout.FRONT_TOWER, GymEquipmentShapes.STAIR_CLIMBER),
        ELLIPTICAL_TRAINER(Layout.FRONT_TOWER, GymEquipmentShapes.ELLIPTICAL_TRAINER),
        PULL_UP_RACK(Layout.WIDE_FRAME, GymEquipmentShapes.PULL_UP_RACK),
        LEG_PRESS_MACHINE(Layout.FRONT_TOWER, GymEquipmentShapes.LEG_PRESS_MACHINE),
        LAT_PULLDOWN_MACHINE(Layout.FRONT_TOWER, GymEquipmentShapes.LAT_PULLDOWN_MACHINE);

        private final Layout layout;
        private final VoxelShape[][] shapes;

        Design(Layout layout, VoxelShape[][] shapes) {
            this.layout = layout;
            this.shapes = shapes;
        }

        @Override public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private static final IntegerProperty PART = IntegerProperty.create("part", 0, 5);
    private final Design design;

    //? if >=1.21 {
    public static final MapCodec<GymMachineBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((GymMachineBlock block) -> block.design),
            propertiesCodec()).apply(instance, GymMachineBlock::new));
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}

    public GymMachineBlock(Design design, Properties properties) {
        super(properties);
        this.design = design;
    }

    @Override protected Direction getPlacementFacing(BlockPlaceContext context) {
        // These machines are approached from the floor part opposite their tower facing.
        return switch (design) {
            case STAIR_CLIMBER, ELLIPTICAL_TRAINER, LEG_PRESS_MACHINE, LAT_PULLDOWN_MACHINE -> context.getHorizontalDirection();
            default -> super.getPlacementFacing(context);
        };
    }

    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override protected int partCount(BlockState state) {
        return design.layout.parts.length;
    }

    @Override protected int partIndex(BlockState state) {
        return state.getValue(PART) % partCount(state);
    }

    @Override protected BlockState stateForPart(BlockState state, int index) {
        return state.setValue(PART, index);
    }

    @Override protected BlockPos partOffset(BlockState state, int index) {
        BlockPos offset = layoutOffset(index);
        Direction facing = state.getValue(FACING);
        return BlockPos.ZERO.relative(facing.getClockWise(), offset.getX())
                .relative(facing.getOpposite(), offset.getZ()).above(offset.getY());
    }

    protected BlockPos layoutOffset(int index) {
        return design.layout.parts[index];
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        int height = layoutOffset(partIndex(state)).getY();
        return DecorationSupport.isPlaced(state, level, pos)
                || GymEquipmentBlock.supported(level, pos.below(height + 1), Direction.UP);
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = super.mirror(state, mirror);
        return design.layout == Layout.WIDE_FRAME && mirror != Mirror.NONE
                ? stateForPart(mirrored, partIndex(state) ^ 1) : mirrored;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return design.shapes[partIndex(state)][RestaurantKitchenBlock.turn(state)];
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    public static final class StairClimber extends GymMachineBlock {
        private static final BooleanProperty RAISED_RAILS = BooleanProperty.create("raised_rails");
        private static final int UPPER_ENTRY_PART = 3;
        private static final BlockPos UPPER_ENTRY_OFFSET = new BlockPos(0, 1, 1);

        //? if >=1.21 {
        public static final MapCodec<StairClimber> CODEC = simpleCodec(StairClimber::new);
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}

        public StairClimber(Properties properties) {
            super(Design.STAIR_CLIMBER, properties);
            // Saved three-part machines retain their footprint even with a block above the entry.
            registerDefaultState(defaultBlockState().setValue(RAISED_RAILS, false));
        }

        @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
            super.addPartProperties(builder);
            builder.add(RAISED_RAILS);
        }

        @Override protected BlockState getPlacementState(BlockPlaceContext context) {
            return super.getPlacementState(context).setValue(RAISED_RAILS, true);
        }

        @Override protected int partCount(BlockState state) {
            return state.getValue(RAISED_RAILS) ? 4 : super.partCount(state);
        }

        @Override protected BlockPos layoutOffset(int index) {
            return index == UPPER_ENTRY_PART ? UPPER_ENTRY_OFFSET : super.layoutOffset(index);
        }

        @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getValue(RAISED_RAILS)
                    ? GymEquipmentShapes.RAISED_STAIR_CLIMBER[partIndex(state)][RestaurantKitchenBlock.turn(state)]
                    : super.getShape(state, level, pos, context);
        }
    }
}
