package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class OverbedTableBlock extends HospitalUtilityBlock {
    // False is the saved-world/default extended design; no additional block/item is registered.
    public static final BooleanProperty NORMAL = BooleanProperty.create("normal");
    //? if >=1.21 {
    public static final MapCodec<OverbedTableBlock> CODEC = simpleCodec(OverbedTableBlock::new);
    //?}
    private static final VoxelShape[] NORMAL_SHAPES = RestaurantKitchenBlock.rotations(Shapes.or(
            box(1.5, 0, 2, 14.5, 2.5, 15), box(12, 2.5, 2.5, 14, 14, 4.5),
            box(1.25, 14, .75, 14.75, 17.5, 15.25)).optimize());

    public OverbedTableBlock(Properties properties) {
        super(Design.OVERBED, properties);
        registerDefaultState(defaultBlockState().setValue(NORMAL, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(NORMAL);
    }
    /** Only the wrench item calls this: ordinary use remains the existing decoration interaction. */
    public InteractionResult cycleDesign(BlockState state, Level level, BlockPos pos, Player player) {
        if (player == null || !player.mayBuild() || !level.mayInteract(player, pos)) return InteractionResult.PASS;
        if (level.getBlockState(pos) != state) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockState next = state.cycle(NORMAL);
        if (!level.isUnobstructed(next, pos, CollisionContext.of(player))) return InteractionResult.FAIL;
        if (!level.setBlock(pos, next, Block.UPDATE_ALL)) return InteractionResult.FAIL;
        level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, .35F, 1.2F);
        return InteractionResult.CONSUME;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(NORMAL) ? NORMAL_SHAPES[RestaurantKitchenBlock.turn(state)] : super.getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
