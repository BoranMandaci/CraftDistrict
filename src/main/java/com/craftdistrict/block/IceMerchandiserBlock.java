package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** New freezers occupy four cells; saved narrow freezers retain their original two-cell footprint. */
public final class IceMerchandiserBlock extends WideTallDecorationBlock {
    public static final BooleanProperty EXPANDED = BooleanProperty.create("expanded"), OPEN = BlockStateProperties.OPEN;
    private static final VoxelShape[][][][] SHAPES = buildShapes();
    //? if >=1.21 {
    public static final MapCodec<IceMerchandiserBlock> CODEC = simpleCodec(IceMerchandiserBlock::new);
    //?}
    public IceMerchandiserBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(EXPANDED, false).setValue(OPEN, false));
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(PRIMARY, EXPANDED, OPEN);
    }
    @Override protected int partCount(BlockState state) { return state.getValue(EXPANDED) ? 4 : 2; }
    @Override protected int partIndex(BlockState state) {
        return state.getValue(EXPANDED) ? state.getValue(PART) : state.getValue(PRIMARY) ? 0 : 1;
    }
    @Override protected BlockState stateForPart(BlockState state, int index) {
        return state.setValue(PART, state.getValue(EXPANDED) ? index : 0).setValue(PRIMARY, index == 0);
    }
    @Override protected BlockPos partOffset(BlockState state, int index) {
        return state.getValue(EXPANDED) ? super.partOffset(state, index) : BlockPos.ZERO.above(index);
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(EXPANDED, true).setValue(PART, 0).setValue(PRIMARY, true);
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        int row = state.getValue(EXPANDED) ? partIndex(state) / 2 : partIndex(state);
        BlockPos floor = pos.below(row + 1);
        return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
    private boolean isPart(BlockState state, BlockState other, int index) {
        return other.is(this) && other.getValue(EXPANDED) == state.getValue(EXPANDED)
                && other.getValue(FACING) == state.getValue(FACING) && partIndex(other) == index;
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState other,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        if (side == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        BlockPos origin = structureOrigin(state, pos);
        for (int i = 0; i < partCount(state); i++) {
            if (i != partIndex(state) && otherPos.equals(origin.offset(partOffset(state, i))) && !isPart(state, other, i))
                return Blocks.AIR.defaultBlockState();
        }
        return state;
    }
    private InteractionResult toggle(Level level, BlockPos pos, Player player) {
        return CabinetInteraction.toggleLinked(this, level, pos, player, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_DOOR_CLOSE);
    }
    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return toggle(level, pos, player);
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND ? toggle(level, pos, player) : InteractionResult.PASS;
    }
    *///?}
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        if (!state.getValue(EXPANDED)) return state.rotate(mirror.getRotation(state.getValue(FACING)));
        BlockState result = super.mirror(state, mirror);
        return mirror != Mirror.NONE && partIndex(state) < 2 ? result.setValue(PRIMARY, !state.getValue(PRIMARY)) : result;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(EXPANDED) ? 1 : 0][state.getValue(OPEN) ? 1 : 0][partIndex(state)][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    private static VoxelShape[][][][] buildShapes() {
        VoxelShape[][][][] shapes = new VoxelShape[2][2][4][];
        for (int wide = 0; wide < 2; wide++) for (int open = 0; open < 2; open++) for (int part = 0; part < 4; part++) {
            boolean upper = wide == 1 ? part >= 2 : part == 1;
            VoxelShape shape = box(0, 0, upper ? 2.75 : .375, 16, 16, 16);
            if (open == 1) {
                double depth = wide == 1 ? -12.75 : -5.5;
                if (wide == 0 || part % 2 == 0) shape = Shapes.or(shape, box(0, upper ? 0 : 10.5, depth, 1.75, 16, 6));
                if (wide == 0 || part % 2 == 1) shape = Shapes.or(shape, box(14.25, upper ? 0 : 10.5, depth, 16, 16, 6));
            }
            shapes[wide][open][part] = RestaurantKitchenBlock.rotations(shape.optimize());
        }
        return shapes;
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
