package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** One-cell visual lid toggle; retains the existing prop's facing/support properties. */
public final class KitchenTrashBinBlock extends RestaurantStoragePropBlock {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(Shapes.or(Block.box(2.5, 0, 4.75, 13.5, 10.625, 13.125),
                    Block.box(6.25, 0, 2.25, 9.75, 1.25, 5))),
            RestaurantKitchenBlock.rotations(Shapes.or(Block.box(2.75, 0, 5, 13.25, 10, 12.25),
                    Block.box(2.5, 10, 7, 13.5, 16, 13.125), Block.box(6.25, 0, 2.25, 9.75, 1.25, 5)))
    };
    //? if >=1.21 {
    public static final MapCodec<KitchenTrashBinBlock> CODEC = simpleCodec(KitchenTrashBinBlock::new);
    //?}

    public KitchenTrashBinBlock(Properties properties) {
        super(Design.TRASH_BIN, properties);
        registerDefaultState(defaultBlockState().setValue(OPEN, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPEN);
    }

    InteractionResult toggleLid(BlockState state, Level level, BlockPos pos, Player player) {
        BlockState current = level.getBlockState(pos);
        if (!current.is(this)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        boolean open = !current.getValue(OPEN);
        if (!level.setBlock(pos, current.setValue(OPEN, open), Block.UPDATE_ALL)) return InteractionResult.PASS;
        level.playSound(null, pos, open ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE,
                SoundSource.BLOCKS, .55F, 1.2F);
        level.gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        return InteractionResult.CONSUME;
    }

    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return toggleLid(state, level, pos, player);
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return toggleLid(state, level, pos, player);
    }
    *///?}

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(OPEN) ? 1 : 0][RestaurantKitchenBlock.turn(state)];
    }

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
