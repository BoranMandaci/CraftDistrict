package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Both tray finishes share world-aligned edges, including trays attached to shower plumbing. */
public final class HotelShowerFloorBlock extends RotatableDecorationBlock {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH, EAST = BlockStateProperties.EAST,
            SOUTH = BlockStateProperties.SOUTH, WEST = BlockStateProperties.WEST;
    private static final VoxelShape[] SHAPES = new VoxelShape[16];
    static {
        for (int mask = 0; mask < 16; mask++) {
            VoxelShape shape = box(0, 0, 0, 16, .5, 16);
            if ((mask & 1) == 0) shape = Shapes.or(shape, box(.5, .5, 0, 15.5, 1, .5));
            if ((mask & 2) == 0) shape = Shapes.or(shape, box(15.5, .5, .5, 16, 1, 15.5));
            if ((mask & 4) == 0) shape = Shapes.or(shape, box(.5, .5, 15.5, 15.5, 1, 16));
            if ((mask & 8) == 0) shape = Shapes.or(shape, box(0, .5, .5, .5, 1, 15.5));
            if ((mask & 9) != 9) shape = Shapes.or(shape, box(0, .5, 0, .5, 1, .5));
            if ((mask & 3) != 3) shape = Shapes.or(shape, box(15.5, .5, 0, 16, 1, .5));
            if ((mask & 6) != 6) shape = Shapes.or(shape, box(15.5, .5, 15.5, 16, 1, 16));
            if ((mask & 12) != 12) shape = Shapes.or(shape, box(0, .5, 15.5, .5, 1, 16));
            SHAPES[mask] = shape.optimize();
        }
    }
    private final boolean drain;
    //? if >=1.21 {
    public static final MapCodec<HotelShowerFloorBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            com.mojang.serialization.Codec.BOOL.fieldOf("drain").forGetter((HotelShowerFloorBlock block) -> block.drain),
            propertiesCodec()).apply(instance, HotelShowerFloorBlock::new));
    //?}
    public HotelShowerFloorBlock(boolean drain, Properties properties) {
        super(properties); this.drain = drain;
        registerDefaultState(defaultBlockState().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false));
    }
    public boolean hasDrain() { return drain; }
    public static BooleanProperty property(Direction side) {
        return switch (side) { case NORTH -> NORTH; case EAST -> EAST; case SOUTH -> SOUTH; case WEST -> WEST;
            default -> throw new IllegalArgumentException("Horizontal shower floor edge required"); };
    }
    public static boolean compatible(BlockState state) {
        return state.getBlock() instanceof HotelShowerFloorBlock || state.getBlock() instanceof HotelShowerFixtureBlock
                && state.getValue(HotelShowerFixtureBlock.TALL) && state.getValue(MultiBlockDecorationBlock.PRIMARY)
                && state.getValue(HotelShowerFixtureBlock.BASE);
    }
    public static BlockState connections(BlockState state, LevelReader level, BlockPos pos) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos other = pos.relative(side);
            state = state.setValue(property(side), level.hasChunkAt(other) && compatible(level.getBlockState(other)));
        }
        return state;
    }
    public static int mask(BlockState state) {
        return (state.getValue(NORTH) ? 1 : 0) | (state.getValue(EAST) ? 2 : 0)
                | (state.getValue(SOUTH) ? 4 : 0) | (state.getValue(WEST) ? 8 : 0);
    }
    public static VoxelShape shapeFor(int mask) { return SHAPES[mask]; }
    /** A bounded five-cell refresh; no controllers, scheduled updates, or chunk loading. */
    public static void refreshAround(Level level, BlockPos origin) {
        if (level.isClientSide) return;
        refresh(level, origin);
        for (Direction side : Direction.Plane.HORIZONTAL) refresh(level, origin.relative(side));
    }
    private static void refresh(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return;
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof HotelShowerFloorBlock) {
            BlockState next = connections(state, level, pos);
            if (next != state) level.setBlock(pos, next, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        } else if (compatible(state) && state.getBlock() instanceof HotelShowerFixtureBlock fixture) fixture.refreshFloor(level, pos);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(NORTH, EAST, SOUTH, WEST);
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos floor = pos.below();
        return DecorationSupport.isPlaced(state, level, pos)
                || level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state != null && state.canSurvive(context.getLevel(), context.getClickedPos())
                ? connections(state, context.getLevel(), context.getClickedPos()) : null;
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (side.getAxis().isHorizontal()) return state.setValue(property(side), compatible(neighbor));
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!old.is(this)) refreshAround(level, pos);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        super.onRemove(state, level, pos, next, moving);
        if (!next.is(this)) refreshAround(level, pos);
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState result = super.rotate(state, rotation);
        for (Direction side : Direction.Plane.HORIZONTAL) result = result.setValue(property(rotation.rotate(side)), state.getValue(property(side)));
        return result;
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
        for (Direction side : Direction.Plane.HORIZONTAL) result = result.setValue(property(mirror.mirror(side)), state.getValue(property(side)));
        return result;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return shapeFor(mask(state)); }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return getShape(state, level, pos, context); }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
