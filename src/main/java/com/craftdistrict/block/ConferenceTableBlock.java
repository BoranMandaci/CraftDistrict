package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;

/** End and Center sections retain their shared automatic layout and original column model. */
public final class ConferenceTableBlock extends OfficeConnectedBlock {
    public static final BooleanProperty MANUAL_SUPPORT = ModularDiningTableBlock.MANUAL_SUPPORT;
    //? if >=1.21 {
    public static final MapCodec<ConferenceTableBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((ConferenceTableBlock b) -> b.design),
            propertiesCodec()).apply(instance, ConferenceTableBlock::new));
    //?}

    public ConferenceTableBlock(Design design, Properties properties) {
        super(design, properties);
        registerDefaultState(defaultBlockState().setValue(MANUAL_SUPPORT, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MANUAL_SUPPORT);
    }

    @Override BlockState layout(BlockState state, LevelReader level, BlockPos pos, BlockPos removed) {
        BlockState automatic = super.layout(state, level, pos, removed);
        return state.getValue(MANUAL_SUPPORT) ? automatic.setValue(SUPPORT, state.getValue(SUPPORT)) : automatic;
    }

    public InteractionResult toggleSupport(BlockState state, Level level, BlockPos pos, Player player) {
        if (player == null || !player.mayBuild() || !level.mayInteract(player, pos) || level.getBlockState(pos) != state)
            return InteractionResult.PASS;
        BlockState automatic = super.layout(state.setValue(MANUAL_SUPPORT, false), level, pos, null);
        BlockState next = player.isShiftKeyDown() ? automatic
                : automatic.setValue(MANUAL_SUPPORT, true).setValue(SUPPORT, !state.getValue(SUPPORT));
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (next == state) return InteractionResult.CONSUME;
        if (!level.isUnobstructed(next, pos, CollisionContext.of(player))) return InteractionResult.FAIL;
        return level.setBlock(pos, next, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE)
                ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
