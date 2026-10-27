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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Office furniture uses the existing atomic placement/removal and one-drop lifecycle. */
public final class OfficeFurnitureBlock extends MultiBlockDecorationBlock {
    public enum Design implements StringRepresentable {
        WORKSTATION(2, false), EXECUTIVE(3, false), RECEPTION(3, false),
        SCREEN(2, false), WHITEBOARD(2, true), SERVER_RACK(2, true), WATER_COOLER(2, true);
        final int parts;
        final boolean vertical;
        Design(int parts, boolean vertical) { this.parts = parts; this.vertical = vertical; }
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 2);
    //? if >=1.21 {
    public static final MapCodec<OfficeFurnitureBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((OfficeFurnitureBlock b) -> b.design),
            propertiesCodec()).apply(instance, OfficeFurnitureBlock::new));
    //?}
    private static final VoxelShape[][][] SHAPES = buildShapes();
    private final Design design;

    public OfficeFurnitureBlock(Design design, Properties properties) { super(properties); this.design = design; }

    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(PART);
    }
    @Override protected int partCount() { return design.parts; }
    @Override protected int partIndex(BlockState state) { return state.getValue(PART); }
    @Override protected BlockState stateForPart(BlockState state, int index) {
        return state.setValue(PRIMARY, index == 0).setValue(PART, index);
    }
    @Override protected Direction getExtensionDirection(BlockState state) {
        return design.vertical ? Direction.UP : state.getValue(FACING).getClockWise();
    }
    @Override protected Direction getPlacementFacing(BlockPlaceContext context) {
        if (design != Design.SCREEN) return super.getPlacementFacing(context);
        Direction side = context.getClickedFace();
        return side.getAxis().isHorizontal() ? side : null;
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(PART) >= design.parts) return false;
        if (design == Design.SCREEN) {
            Direction facing = state.getValue(FACING);
            BlockPos wall = pos.relative(facing.getOpposite());
            return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
        }
        BlockPos floor = (design.vertical ? structureOrigin(state, pos) : pos).below();
        return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState other,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        Direction support = design == Design.SCREEN ? state.getValue(FACING).getOpposite() : Direction.DOWN;
        if (side == support && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        BlockPos origin = structureOrigin(state, pos);
        for (int i = 0; i < design.parts; i++) {
            if (i != partIndex(state) && otherPos.equals(origin.offset(partOffset(state, i)))
                    && other != stateForPart(state, i)) return Blocks.AIR.defaultBlockState();
        }
        return state;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[design.ordinal()][Math.min(state.getValue(PART), design.parts - 1)][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    private static VoxelShape[][][] buildShapes() {
        VoxelShape[][][] shapes = new VoxelShape[Design.values().length][][];
        for (Design d : Design.values()) {
            shapes[d.ordinal()] = new VoxelShape[d.parts][];
            for (int p = 0; p < d.parts; p++) {
                VoxelShape shape = switch (d) {
                    case WORKSTATION -> Shapes.or(box(0, 13.5, 1, 16, 14.5, 15),
                            box(p == 0 ? 1 : 13, 0, 2, p == 0 ? 3 : 15, 13.5, 14));
                    case EXECUTIVE -> Shapes.or(box(0, 13.5, 1, 16, 14.5, 15),
                            box(0, 4.5, 13.5, 16, 13.05, 14.5), p == 1 ? Shapes.empty()
                                    : box(p == 0 ? 1 : 8, 0, 4, p == 0 ? 10 : 15, 13.5, 14));
                    case RECEPTION -> Shapes.or(box(0, 1, 1, 16, 19, 4), box(0, 15, 4, 16, 16, 15),
                            p == 0 ? box(1, 0, 4, 8, 15, 14) : p == 2 ? box(13, 0, 4, 15, 15, 14) : Shapes.empty());
                    case SCREEN -> box(p == 0 ? .5 : 0, 2, 14, p == 0 ? 16 : 15.5, 15, 16);
                    case WHITEBOARD -> p == 0 ? Shapes.or(box(2, 0, 3, 14, 2, 13), box(1, 7, 7, 15, 16, 9))
                            : box(1, 0, 7, 15, 14, 9);
                    case SERVER_RACK -> box(0, 0, 0, 16, 16, 16);
                    case WATER_COOLER -> p == 0 ? box(3, 0, 2, 13, 16, 14)
                            : Shapes.or(box(3.25, 0, 2.5, 12.75, 4.5, 13.5),
                                    box(4.25, 4.5, 3.75, 11.75, 15.25, 11.25));
                };
                shapes[d.ordinal()][p] = RestaurantKitchenBlock.rotations(shape);
            }
        }
        return shapes;
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
