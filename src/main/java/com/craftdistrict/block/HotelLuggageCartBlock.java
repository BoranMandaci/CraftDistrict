package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Four-cell trolleys use the shared placement/removal transaction; saved narrow carts stay two-cell. */
public final class HotelLuggageCartBlock extends MultiBlockDecorationBlock {
    public static final BooleanProperty EXPANDED = BooleanProperty.create("expanded");
    public static final IntegerProperty COLUMN = IntegerProperty.create("column", 0, 1);
    private final boolean loaded;
    private static final VoxelShape[][][] SHAPES = buildShapes();
    //? if >=1.21 {
    public static final MapCodec<HotelLuggageCartBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.fieldOf("loaded").forGetter((HotelLuggageCartBlock b) -> b.loaded),
            propertiesCodec()).apply(instance, HotelLuggageCartBlock::new));
    //?}

    private static VoxelShape[][][] buildShapes() {
        VoxelShape[][][] result = new VoxelShape[2][4][];
        for (int contents = 0; contents < 2; contents++) {
            for (int part = 0; part < 4; part++) {
                boolean right = (part & 1) != 0, upper = part >= 2;
                VoxelShape shape;
                if (upper) {
                    shape = Shapes.or(box(right ? 12.25 : 2.25, 0, 2.25, right ? 13.75 : 3.75, 7.25, 13.75),
                            box(right ? 0 : 2.25, 7.25, 2.25, right ? 13.75 : 16, 12, 13.75));
                    // The push grip is on the short rear end, entirely within the right-hand column.
                    if (right) shape = Shapes.or(shape, box(12.75, .5, 2.9375, 15, 1.9375, 13.0625));
                    if (contents != 0) shape = Shapes.or(shape, box(right ? 0 : 4, 0, 4, right ? 12 : 16, right ? 3 : 5.25, 12.25));
                } else {
                    shape = Shapes.or(box(right ? 0 : 1, 0, 1, right ? 15 : 16, 4.25, 15),
                            box(right ? 12.25 : 2.25, 4.25, 2.25, right ? 13.75 : 3.75, 16, 13.75));
                    if (contents != 0) shape = Shapes.or(shape, box(right ? 0 : 4, 4.25, 4, right ? 12 : 16, 16, 12.25));
                }
                result[contents][part] = RestaurantKitchenBlock.rotations(shape.optimize());
            }
        }
        return result;
    }

    public HotelLuggageCartBlock(boolean loaded, Properties properties) {
        super(properties); this.loaded = loaded;
        registerDefaultState(defaultBlockState().setValue(EXPANDED, false).setValue(COLUMN, 0).setValue(PRIMARY, true));
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(EXPANDED, COLUMN);
    }
    @Override protected int partCount() { return 4; }
    @Override protected int partCount(BlockState state) { return state.getValue(EXPANDED) ? 4 : 2; }
    @Override protected int partIndex(BlockState state) {
        return state.getValue(EXPANDED) ? state.getValue(COLUMN) + (state.getValue(PRIMARY) ? 0 : 2)
                : (state.getValue(PRIMARY) ? 0 : 1);
    }
    @Override protected BlockState stateForPart(BlockState state, int index) {
        return state.getValue(EXPANDED) ? state.setValue(COLUMN, index & 1).setValue(PRIMARY, index < 2)
                : state.setValue(COLUMN, 0).setValue(PRIMARY, index == 0);
    }
    @Override protected BlockPos partOffset(BlockState state, int index) {
        return state.getValue(EXPANDED)
                ? BlockPos.ZERO.relative(state.getValue(FACING).getClockWise(), index & 1).above(index / 2)
                : BlockPos.ZERO.above(index);
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        return super.getPlacementState(context).setValue(EXPANDED, true).setValue(COLUMN, 0).setValue(PRIMARY, true);
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (!state.getValue(EXPANDED) && state.getValue(COLUMN) != 0) return false;
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos origin = structureOrigin(state, pos);
        for (int column = 0; column < (state.getValue(EXPANDED) ? 2 : 1); column++) {
            BlockPos floor = origin.relative(state.getValue(FACING).getClockWise(), column).below();
            if (!level.hasChunkAt(floor) || !level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) return false;
        }
        return true;
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        BlockPos origin = structureOrigin(state, pos);
        for (int part = 0; part < partCount(state); part++) {
            if (otherPos.equals(origin.offset(partOffset(state, part))) && neighbor != stateForPart(state, part))
                return Blocks.AIR.defaultBlockState();
        }
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = super.mirror(state, mirror);
        return mirror != Mirror.NONE && state.getValue(EXPANDED)
                ? result.setValue(COLUMN, 1 - state.getValue(COLUMN)) : result;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int turn = RestaurantKitchenBlock.turn(state);
        return state.getValue(EXPANDED) ? SHAPES[loaded ? 1 : 0][partIndex(state)][turn]
                : HotelTallBlock.legacyCartShape(loaded, state.getValue(PRIMARY), turn);
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
