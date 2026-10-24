package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Package 1 workstation: static model and action-bar information only. */
public final class AssemblyStationBlock extends RotatableDecorationBlock {
    // The frame is symmetric, so all four orientations share this immutable shape.
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 12.5, 0, 16, 16, 16), Block.box(1, 3, 1, 15, 4, 15),
            Block.box(1, 0, 1, 3, 12.5, 3), Block.box(13, 0, 1, 15, 12.5, 3),
            Block.box(1, 0, 13, 3, 12.5, 15), Block.box(13, 0, 13, 15, 12.5, 15)).optimize();
    private static final Component INFO = Component.translatable("message.craftdistrict.assembly_station.unavailable");
    //? if >=1.21 {
    public static final MapCodec<AssemblyStationBlock> CODEC = simpleCodec(AssemblyStationBlock::new);
    //?}

    public AssemblyStationBlock(Properties properties) {
        super(properties);
    }

    private InteractionResult explain(Level level, Player player) {
        // Replaces the action bar rather than adding repeated chat messages.
        if (!level.isClientSide) player.displayClientMessage(INFO, true);
        return level.isClientSide ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return explain(level, player);
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND ? explain(level, player) : InteractionResult.PASS;
    }
    *///?}

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
