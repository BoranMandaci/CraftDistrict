package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Retains the appliance's support/facing rules; the front door folds down. */
public final class DishwasherBlock extends RestaurantStoragePropBlock {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private static final VoxelShape[] OPEN_SHAPES = RestaurantKitchenBlock.rotations(Shapes.or(
            Block.box(0, 0, 1.5, 16, 3, 16),
            Block.box(0, 3, 1.5, .75, 12.5, 16), Block.box(15.25, 3, 1.5, 16, 12.5, 16),
            Block.box(.75, 3, 15.25, 15.25, 12.5, 16), Block.box(0, 12.5, 0, 16, 14.5, 16),
            Block.box(.5, 1.5, -8, 15.5, 3.25, 1.75)).optimize());
    //? if >=1.21 {
    public static final MapCodec<DishwasherBlock> CODEC = simpleCodec(DishwasherBlock::new);
    //?}

    public DishwasherBlock(Properties properties) {
        super(Design.DISHWASHER, properties);
        registerDefaultState(defaultBlockState().setValue(OPEN, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPEN);
    }

    InteractionResult toggleDoor(BlockState state, Level level, BlockPos pos, Player player) {
        return CabinetInteraction.toggle(state, level, pos, player,
                SoundEvents.IRON_TRAPDOOR_OPEN, SoundEvents.IRON_TRAPDOOR_CLOSE);
    }

    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return toggleDoor(state, level, pos, player);
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return toggleDoor(state, level, pos, player);
    }
    *///?}

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? OPEN_SHAPES[RestaurantKitchenBlock.turn(state)]
                : super.getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
