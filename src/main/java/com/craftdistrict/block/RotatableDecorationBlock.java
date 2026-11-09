package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import com.craftdistrict.lighting.LightControls;
import com.craftdistrict.lighting.LightEffect;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import java.util.List;
//? if >=1.21 {
import net.minecraft.world.ItemInteractionResult;
//?}
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class RotatableDecorationBlock extends HorizontalDirectionalBlock {
    //? if >=1.21 {
    public static final MapCodec<RotatableDecorationBlock> CODEC = simpleCodec(RotatableDecorationBlock::new);
    //?}

    public RotatableDecorationBlock(Properties properties) {
        super(properties);
        BlockState state = defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        if (this instanceof ControlledLight) state = state.setValue(LightControls.LIT, true).setValue(LightControls.REDSTONE, false);
        if (LightControls.settingsEligible(this)) state = state.setValue(LightControls.EFFECT, LightEffect.Stage.STEADY);
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
        if (this instanceof ControlledLight) builder.add(LightControls.LIT, LightControls.REDSTONE);
        if (LightControls.settingsEligible(this)) builder.add(LightControls.EFFECT);
    }

    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block source, BlockPos sourcePos, boolean moving) {
        super.neighborChanged(state, level, pos, source, sourcePos, moving);
        if (this instanceof ControlledLight) LightControls.neighborChanged(state, level, pos);
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (this instanceof ControlledLight) LightControls.removed(state, level, pos, replacement);
        super.onRemove(state, level, pos, replacement, moving);
    }

    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return this instanceof ControlledLight ? LightControls.use(state, level, pos, player, InteractionHand.MAIN_HAND)
                : super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                   Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(this instanceof ControlledLight)) return super.useItemOn(stack, state, level, pos, player, hand, hit);
        InteractionResult result = LightControls.use(state, level, pos, player, hand);
        return result.consumesAction() ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return this instanceof ControlledLight ? LightControls.use(state, level, pos, player, hand)
                : super.use(state, level, pos, player, hand, hit);
    }
    *///?}

    @Override public void appendHoverText(ItemStack stack,
            //? if >=1.21 {
            Item.TooltipContext context,
            //?} else {
            /*BlockGetter context,
            *///?}
            List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (this instanceof ControlledLight) {
            tooltip.add(Component.translatable("tooltip.craftdistrict.light.control").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.craftdistrict.light.mode").withStyle(ChatFormatting.GRAY));
            if (LightControls.settingsEligible(this))
                tooltip.add(Component.translatable("tooltip.craftdistrict.light.settings").withStyle(ChatFormatting.GRAY));
            if (LightControls.dyeable(defaultBlockState()))
                tooltip.add(Component.translatable("tooltip.craftdistrict.light.dye").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(BlockStateProperties.HORIZONTAL_FACING,
                rotation.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}
}
