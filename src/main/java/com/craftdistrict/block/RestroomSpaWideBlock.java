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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** New tables are 2.5 blocks long; saved two/three-cell layouts retain their linked removal. */
public final class RestroomSpaWideBlock extends MultiBlockDecorationBlock {
    public enum Design implements StringRepresentable {
        MASSAGE_TABLE;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public static final BooleanProperty LARGE = BooleanProperty.create("large");
    public static final BooleanProperty EXTENDED = BooleanProperty.create("extended");
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 2);
    private static final VoxelShape[][] SHAPES = createShapes(true), LEGACY_SHAPES = createShapes(false);
    private static final VoxelShape[][] EXTENDED_SHAPES = createExtendedShapes();
    private final Design design;
    //? if >=1.21 {
    public static final MapCodec<RestroomSpaWideBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((RestroomSpaWideBlock b) -> b.design),
            propertiesCodec()).apply(instance, RestroomSpaWideBlock::new));
    //?}
    public RestroomSpaWideBlock(Design design, Properties properties) {
        super(properties); this.design = design;
        registerDefaultState(defaultBlockState().setValue(LARGE, false).setValue(EXTENDED, false).setValue(PART, 0));
    }
    private static VoxelShape[][] createExtendedShapes() {
        VoxelShape whole = Shapes.or(box(-.5, 10.75, 8, 16.5, 14.25, 40.75),
                box(4, 11.5, 2, 6.25, 14.25, 7.5), box(9.75, 11.5, 2, 12, 14.25, 7.5),
                box(6, 11.5, .75, 10, 14.25, 3), box(5, 10.75, 4, 6, 12.25, 8),
                box(10, 10.75, 4, 11, 12.25, 8), box(1.5, 0, 10, 3.25, 10.75, 12.25),
                box(12.75, 0, 10, 14.5, 10.75, 12.25), box(1.5, 0, 34.75, 3.25, 10.75, 37),
                box(12.75, 0, 34.75, 14.5, 10.75, 37), box(5, 14.25, 34.25, 10, 16, 38.25)).optimize();
        VoxelShape[][] result = new VoxelShape[3][];
        for (int part = 0; part < result.length; part++) result[part] = RestaurantKitchenBlock.rotations(
                Shapes.join(whole.move(0, 0, -part), box(-.5, 0, 0, 16.5, 16, 16), BooleanOp.AND).optimize());
        return result;
    }
    private static VoxelShape[][] createShapes(boolean large) {
        VoxelShape whole = Shapes.or(box(3, 10.75, 8, 13, 14.25, 47.5),
                box(4, 11.5, 2, 6.25, 14.25, 7.5), box(9.75, 11.5, 2, 12, 14.25, 7.5),
                box(6, 11.5, .75, 10, 14.25, 3), box(5, 10.75, 4, 6, 12.25, 9),
                box(10, 10.75, 4, 11, 12.25, 9), box(3.5, 0, 10, 5, 10.75, 12),
                box(11, 0, 10, 12.5, 10.75, 12), box(3.5, 0, 39, 5, 10.75, 41.25),
                box(11, 0, 39, 12.5, 10.75, 41.25), box(5, 14.25, 40, 10, 16, 44)).optimize();
        if (!large) whole = Shapes.or(box(-.5, 10.75, 8, 16.5, 14.25, 31.5),
                box(4, 11.5, 2, 6.25, 14.25, 7.5), box(9.75, 11.5, 2, 12, 14.25, 7.5),
                box(6, 11.5, .75, 10, 14.25, 3), box(5, 10.75, 4, 6, 12.25, 8),
                box(10, 10.75, 4, 11, 12.25, 8), box(1.5, 0, 10, 3.25, 10.75, 12.25),
                box(12.75, 0, 10, 14.5, 10.75, 12.25), box(1.5, 0, 25.5, 3.25, 10.75, 27.75),
                box(12.75, 0, 25.5, 14.5, 10.75, 27.75), box(5, 14.25, 25, 10, 16, 29)).optimize();
        VoxelShape[][] result = new VoxelShape[large ? 3 : 2][];
        for (int part = 0; part < result.length; part++) result[part] = RestaurantKitchenBlock.rotations(
                Shapes.join(whole.move(0, 0, -part), large ? Shapes.block() : box(-.5, 0, 0, 16.5, 16, 16), BooleanOp.AND).optimize());
        return result;
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(LARGE, EXTENDED, PART);
    }
    @Override protected int partCount(BlockState state) { return state.getValue(LARGE) || state.getValue(EXTENDED) ? 3 : 2; }
    @Override protected int partIndex(BlockState state) { return partCount(state) == 3 ? state.getValue(PART) : state.getValue(PRIMARY) ? 0 : 1; }
    @Override protected BlockState stateForPart(BlockState state, int index) {
        return state.setValue(PRIMARY, index == 0).setValue(PART, partCount(state) == 3 ? index : 0);
    }
    @Override protected Direction getExtensionDirection(BlockState state) { return state.getValue(FACING).getOpposite(); }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        return super.getPlacementState(context).setValue(LARGE, false).setValue(EXTENDED, true).setValue(PART, 0);
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos floor = pos.below();
        return level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        BlockPos origin = structureOrigin(state, pos);
        for (int part = 0; part < partCount(state); part++)
            if (origin.offset(partOffset(state, part)).equals(otherPos) && neighbor != stateForPart(state, part))
                return Blocks.AIR.defaultBlockState();
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(EXTENDED) ? EXTENDED_SHAPES : state.getValue(LARGE) ? SHAPES : LEGACY_SHAPES)
                [partIndex(state)][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
