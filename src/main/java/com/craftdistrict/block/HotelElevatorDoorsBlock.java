package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Four saved parts change together through the existing atomic cabinet interaction. */
public final class HotelElevatorDoorsBlock extends WideTallDecorationBlock {
    private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(box(0, 0, 13.5, 16, 16, 16));
    private static final VoxelShape[][] OPEN_SHAPES = new VoxelShape[4][];
    static {
        for (int part = 0; part < 4; part++) {
            boolean right = part % 2 == 1, upper = part >= 2;
            VoxelShape side = box(right ? 12 : 0, 0, 13.5, right ? 16 : 4, 16, 16);
            VoxelShape edge = upper ? box(0, 14.75, 13.5, 16, 16, 16) : box(0, 0, 13.5, 16, .375, 16);
            OPEN_SHAPES[part] = RestaurantKitchenBlock.rotations(Shapes.or(side, edge).optimize());
        }
    }
    //? if >=1.21 {
    public static final MapCodec<HotelElevatorDoorsBlock> CODEC = simpleCodec(HotelElevatorDoorsBlock::new);
    //?}
    public HotelElevatorDoorsBlock(Properties properties) { super(properties); registerDefaultState(defaultBlockState().setValue(BlockStateProperties.OPEN, false)); }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(BlockStateProperties.OPEN);
    }
    //? if >=1.21 {
    @Override public net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return CabinetInteraction.toggleLinked(this, level, pos, player, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_DOOR_CLOSE);
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND ? CabinetInteraction.toggleLinked(this, level, pos, player, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_DOOR_CLOSE) : InteractionResult.PASS;
    }
    *///?}
    @Override protected Direction getPlacementFacing(BlockPlaceContext context) {
        return context.getClickedFace().getAxis().isHorizontal() ? context.getClickedFace() : super.getPlacementFacing(context);
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos wall = pos.relative(state.getValue(FACING).getOpposite());
        BlockPos floor = pos.below(state.getValue(PART) / 2 + 1);
        return DecorationSupport.isPlaced(state, level, pos)
                || level.hasChunkAt(wall) && level.getBlockState(wall).isFaceSturdy(level, wall, state.getValue(FACING))
                || level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos other) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        BlockPos origin = structureOrigin(state, pos);
        for (int i = 0; i < 4; i++) if (origin.offset(partOffset(state, i)).equals(other) && neighbor != stateForPart(state, i))
            return Blocks.AIR.defaultBlockState();
        return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int turn = RestaurantKitchenBlock.turn(state);
        return state.getValue(BlockStateProperties.OPEN) ? OPEN_SHAPES[state.getValue(PART)][turn] : SHAPES[turn];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return getShape(state, level, pos, context); }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
