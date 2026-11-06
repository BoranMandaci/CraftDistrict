package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.craftdistrict.item.WrenchItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;

/** Vanilla bed interaction and paired removal; all visuals are static JSON models. */
public final class HospitalBedBlock extends BedBlock {
    //? if >=1.21 {
    public static final MapCodec<BedBlock> CODEC = simpleCodec(HospitalBedBlock::new);
    @Override public MapCodec<BedBlock> codec() { return CODEC; }
    //?}
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(Shapes.or(box(1, 4, 0, 15, 10.5, 16),
                    box(2, 0, 2, 14, 4, 6), box(1, 10.5, 0, 15, 15, 1.5),
                    box(.75, 10.5, 6, 1.75, 13.5, 16), box(14.25, 10.5, 6, 15.25, 13.5, 16)).optimize()),
            RestaurantKitchenBlock.rotations(Shapes.or(box(1, 4, 0, 15, 10.5, 16),
                    box(2, 0, 10, 14, 4, 14), box(1, 10.5, 14.5, 15, 13, 16),
                    box(.75, 10.5, 0, 1.75, 13.5, 6), box(14.25, 10.5, 0, 15.25, 13.5, 6)).optimize())};

    public HospitalBedBlock(Properties properties) { super(DyeColor.WHITE, properties); }

    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return WrenchItem.isHeldBy(player) ? InteractionResult.PASS : super.useWithoutItem(state, level, pos, player, hit);
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return WrenchItem.isHeldBy(player) ? InteractionResult.PASS : super.use(state, level, pos, player, hand, hit);
    }
    *///?}

    private static boolean inBounds(Level level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos) && level.hasChunkAt(pos);
    }
    private static boolean supported(Level level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        Level level = context.getLevel();
        BlockPos foot = context.getClickedPos(), head = foot.relative(state.getValue(FACING));
        CollisionContext collision = context.getPlayer() == null ? CollisionContext.empty() : CollisionContext.of(context.getPlayer());
        return inBounds(level, foot) && inBounds(level, head) && supported(level, foot) && supported(level, head)
                && level.isUnobstructed(state, foot, collision)
                && level.isUnobstructed(state.setValue(PART, BedPart.HEAD), head, collision) ? state : null;
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.isClientSide || level.getBlockState(pos) != state) return;
        BlockPos headPos = pos.relative(state.getValue(FACING));
        BlockState head = state.setValue(PART, BedPart.HEAD);
        CollisionContext collision = placer == null ? CollisionContext.empty() : CollisionContext.of(placer);
        if (!inBounds(level, headPos) || !supported(level, pos) || !supported(level, headPos)
                || !level.getBlockState(headPos).canBeReplaced() || !level.isUnobstructed(head, headPos, collision)
                || !level.setBlock(headPos, head, Block.UPDATE_ALL)) {
            if (level.getBlockState(pos) == state) level.removeBlock(pos, false);
            return;
        }
        level.blockUpdated(pos, Blocks.AIR);
        state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(PART) == BedPart.HEAD ? 0 : 1][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return null; }
}
