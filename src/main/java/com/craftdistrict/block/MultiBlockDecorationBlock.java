package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public final class MultiBlockDecorationBlock extends RotatableDecorationBlock {
    public static final BooleanProperty PRIMARY = BooleanProperty.create("primary");
    private static final int LINKED_PART_UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final ThreadLocal<Boolean> REMOVING_LINKED_PART = ThreadLocal.withInitial(() -> false);
    //? if >=1.21 {
    public static final MapCodec<MultiBlockDecorationBlock> CODEC = simpleCodec(MultiBlockDecorationBlock::new);
    //?}

    public MultiBlockDecorationBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(PRIMARY, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PRIMARY);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos primaryPos = context.getClickedPos();
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos extensionPos = primaryPos.relative(facing);

        if (!level.getWorldBorder().isWithinBounds(primaryPos)
                || !level.getWorldBorder().isWithinBounds(extensionPos)
                || !level.getBlockState(primaryPos).canBeReplaced(context)
                || !level.getBlockState(extensionPos).canBeReplaced(context)) {
            return null;
        }

        return defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(PRIMARY, true);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !state.getValue(PRIMARY)) {
            return;
        }

        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockPos extensionPos = pos.relative(facing);
        if (!level.getWorldBorder().isWithinBounds(extensionPos)
                || !level.getBlockState(pos).is(this)
                || !level.getBlockState(extensionPos).canBeReplaced()) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), LINKED_PART_UPDATE_FLAGS);
            return;
        }

        BlockState extensionState = defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(PRIMARY, false);
        if (!level.setBlock(extensionPos, extensionState, LINKED_PART_UPDATE_FLAGS)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), LINKED_PART_UPDATE_FLAGS);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !newState.is(this) && !REMOVING_LINKED_PART.get()) {
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            BlockPos partnerPos = state.getValue(PRIMARY) ? pos.relative(facing) : pos.relative(facing.getOpposite());
            BlockState partnerState = level.getBlockState(partnerPos);
            if (partnerState.is(this)
                    && partnerState.getValue(BlockStateProperties.HORIZONTAL_FACING) == facing
                    && partnerState.getValue(PRIMARY) != state.getValue(PRIMARY)) {
                REMOVING_LINKED_PART.set(true);
                try {
                    level.setBlock(partnerPos, Blocks.AIR.defaultBlockState(), LINKED_PART_UPDATE_FLAGS);
                } finally {
                    REMOVING_LINKED_PART.set(false);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}
}
