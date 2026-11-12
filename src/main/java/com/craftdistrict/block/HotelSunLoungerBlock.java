package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import com.craftdistrict.entity.MedicalResting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Decorative reclining uses the existing non-night-sleep pose and linked furniture lifecycle. */
public final class HotelSunLoungerBlock extends MultiBlockDecorationBlock implements LyingBlock {
    public static final BooleanProperty FOOT_ORIGIN = BooleanProperty.create("foot_origin");
    private static final VoxelShape[][] SHAPES = createShapes();
    //? if >=1.21 {
    public static final MapCodec<HotelSunLoungerBlock> CODEC = simpleCodec(HotelSunLoungerBlock::new);
    //?}
    public HotelSunLoungerBlock(Properties properties) {
        super(properties); registerDefaultState(defaultBlockState().setValue(FOOT_ORIGIN, false));
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(FOOT_ORIGIN);
    }
    private static VoxelShape[][] createShapes() {
        VoxelShape whole = Shapes.or(box(1, 5.5, 14, 15, 8.75, 31.25),
                box(1.125, 9.5, 1, 14.875, 15, 7), box(1.125, 6.25, 7, 14.875, 12, 14),
                box(1.25, 0, 6, 2.75, 7.125, 10.5), box(13.25, 0, 6, 14.75, 7.125, 10.5),
                box(1.25, 0, 24.5, 2.75, 7.125, 29), box(13.25, 0, 24.5, 14.75, 7.125, 29),
                box(2, .75, 6.625, 14, 1.5, 7.375), box(2, .75, 27.625, 14, 1.5, 28.375)).optimize();
        VoxelShape[][] shapes = new VoxelShape[2][];
        for (int part = 0; part < 2; part++) shapes[part] = RestaurantKitchenBlock.rotations(
                Shapes.join(whole.move(0, 0, -part), Shapes.block(), BooleanOp.AND).optimize());
        return shapes;
    }
    @Override protected Direction getExtensionDirection(BlockState state) {
        return state.getValue(FOOT_ORIGIN) ? state.getValue(FACING) : state.getValue(FACING).getOpposite();
    }
    @Override protected Direction getPlacementFacing(BlockPlaceContext context) { return context.getHorizontalDirection(); }
    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        return super.getPlacementState(context).setValue(FOOT_ORIGIN, true);
    }
    @Override public BlockPos lyingHead(BlockState state, BlockPos pos) {
        BlockPos origin = structureOrigin(state, pos);
        return state.getValue(FOOT_ORIGIN) ? origin.relative(state.getValue(FACING)) : origin;
    }
    @Override public double lyingHeight() { return 9.75 / 16; }
    @Override public AABB lyingClearance(BlockState state, BlockPos head) {
        Vec3 center = Vec3.atCenterOf(head);
        Vec3 along = Vec3.atLowerCornerOf(lyingDirection(state).getNormal());
        Vec3 foot = center.subtract(along.scale(1.45)), tip = center.add(along.scale(.3));
        // Check free air above the raised back, rather than its broad collision envelope.
        return new AABB(Math.min(foot.x, tip.x) - .3, head.getY() + 1, Math.min(foot.z, tip.z) - .3,
                Math.max(foot.x, tip.x) + .3, head.getY() + 1.5, Math.max(foot.z, tip.z) + .3);
    }
    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return MedicalResting.lie(state, level, pos, player);
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND ? MedicalResting.lie(state, level, pos, player) : InteractionResult.PASS;
    }
    *///?}
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!replacement.is(this) || replacement.getValue(FACING) != state.getValue(FACING)
                || replacement.getValue(FOOT_ORIGIN) != state.getValue(FOOT_ORIGIN)
                || replacement.getValue(PRIMARY) != state.getValue(PRIMARY))
            MedicalResting.removeAt(level, lyingHead(state, pos), state);
        super.onRemove(state, level, pos, replacement, moving);
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return DecorationSupport.isPlaced(state, level, pos)
                || level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        Direction partner = state.getValue(PRIMARY) ? getExtensionDirection(state) : getExtensionDirection(state).getOpposite();
        if (side == partner && neighbor != stateForPart(state, state.getValue(PRIMARY) ? 1 : 0)) return Blocks.AIR.defaultBlockState();
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int part = state.getValue(PRIMARY) ? 0 : 1;
        return SHAPES[state.getValue(FOOT_ORIGIN) ? 1 - part : part][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return getShape(state, level, pos, context); }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
