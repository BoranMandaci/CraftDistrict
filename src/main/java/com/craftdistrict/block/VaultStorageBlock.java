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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two static hinged doors expose decorative contents; there is no inventory or block entity. */
public final class VaultStorageBlock extends MultiBlockDecorationBlock {
    private static final VoxelShape[] CLOSED = RestaurantKitchenBlock.rotations(Shapes.block());
    private static final VoxelShape[] OPEN = RestaurantKitchenBlock.rotations(Shapes.or(box(0, 0, 1.5, 16, 16, 16),
            box(-1, 0, -5.75, 2.5, 16, 1.5), box(13.5, 0, -5.75, 17, 16, 1.5)).optimize());
    //? if >=1.21 {
    public static final MapCodec<VaultStorageBlock> CODEC = simpleCodec(VaultStorageBlock::new);
    //?}
    public VaultStorageBlock(Properties properties) {
        super(properties); registerDefaultState(defaultBlockState().setValue(BlockStateProperties.OPEN, false));
    }
    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(BlockStateProperties.OPEN);
    }
    @Override protected Direction getExtensionDirection(BlockState state) { return Direction.UP; }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos floor = structureOrigin(state, pos).below();
        return level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos other) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        if (side == (state.getValue(PRIMARY) ? Direction.UP : Direction.DOWN)
                && (!neighbor.is(this) || neighbor.getValue(PRIMARY) == state.getValue(PRIMARY)
                || neighbor.getValue(FACING) != state.getValue(FACING))) return Blocks.AIR.defaultBlockState();
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return CabinetInteraction.toggleLinked(this, level, pos, player, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_DOOR_CLOSE);
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND
                ? CabinetInteraction.toggleLinked(this, level, pos, player, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_DOOR_CLOSE)
                : InteractionResult.PASS;
    }
    *///?}
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(BlockStateProperties.OPEN) ? OPEN : CLOSED)[RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
