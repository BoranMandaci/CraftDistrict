package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/** Sneaking selects the vertical wall model only during initial placement. */
public final class UnderCabinetLightBlock extends SurfaceLightBlock {
    public static final BooleanProperty VERTICAL = BooleanProperty.create("vertical");

    //? if >=1.21 {
    public static final MapCodec<UnderCabinetLightBlock> CODEC = simpleCodec(UnderCabinetLightBlock::new);
    //?}

    public UnderCabinetLightBlock(Properties properties) {
        super(Fixture.STRIP, properties);
        registerDefaultState(defaultBlockState().setValue(VERTICAL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(VERTICAL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(VERTICAL,
                state.getValue(WALL_MOUNTED) && context.isSecondaryUseActive());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(WALL_MOUNTED) && state.getValue(VERTICAL)
                ? LightingShapes.get(LightingShapes.STRIP_WALL_VERTICAL, state)
                : super.getShape(state, level, pos, context);
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                //? if >=1.21 {
                                Item.TooltipContext context,
                                //?} else {
                                /*BlockGetter context,
                                *///?}
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.craftdistrict.lighting_under_cabinet_strip.vertical_placement")
                .withStyle(ChatFormatting.GRAY));
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
