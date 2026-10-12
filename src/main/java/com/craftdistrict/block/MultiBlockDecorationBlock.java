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
import net.minecraft.world.phys.shapes.CollisionContext;

public class MultiBlockDecorationBlock extends RotatableDecorationBlock {
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
        BlockState placementState = defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(PRIMARY, true);
        BlockPos extensionPos = primaryPos.relative(getExtensionDirection(placementState));

        BlockState extensionState = placementState.setValue(PRIMARY, false);
        CollisionContext collisionContext = context.getPlayer() == null
                ? CollisionContext.empty() : CollisionContext.of(context.getPlayer());
        if (!isWithinPlacementBounds(level, primaryPos)
                || !isWithinPlacementBounds(level, extensionPos)
                || !level.getBlockState(primaryPos).canBeReplaced(context)
                || !level.getBlockState(extensionPos).canBeReplaced(context)
                || !level.isUnobstructed(placementState, primaryPos, collisionContext)
                || !level.isUnobstructed(extensionState, extensionPos, collisionContext)) {
            return null;
        }

        return placementState;
    }

    /** Direction from the primary part to its partner, relative to the block's facing. */
    protected Direction getExtensionDirection(BlockState state) {
        return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    private static boolean isWithinPlacementBounds(Level level, BlockPos pos) {
        return !level.isOutsideBuildHeight(pos)
                && level.getWorldBorder().isWithinBounds(pos)
                && level.hasChunkAt(pos);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !state.getValue(PRIMARY)) {
            return;
        }

        BlockPos extensionPos = pos.relative(getExtensionDirection(state));
        // A callback may have changed the primary; never remove an unrelated replacement.
        if (level.getBlockState(pos) != state) {
            return;
        }
        BlockState extensionState = state.setValue(PRIMARY, false);
        CollisionContext collisionContext = placer == null
                ? CollisionContext.empty() : CollisionContext.of(placer);
        if (!isWithinPlacementBounds(level, extensionPos)
                || !level.getBlockState(extensionPos).canBeReplaced()
                || !level.isUnobstructed(extensionState, extensionPos, collisionContext)
                || !level.setBlock(extensionPos, extensionState, LINKED_PART_UPDATE_FLAGS)) {
            if (level.getBlockState(pos) == state) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), LINKED_PART_UPDATE_FLAGS);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !newState.is(this) && !REMOVING_LINKED_PART.get()) {
            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            Direction extensionDirection = getExtensionDirection(state);
            BlockPos partnerPos = state.getValue(PRIMARY)
                    ? pos.relative(extensionDirection) : pos.relative(extensionDirection.getOpposite());
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
