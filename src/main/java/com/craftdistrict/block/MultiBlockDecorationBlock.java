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

import java.util.HashSet;
import java.util.Set;

public class MultiBlockDecorationBlock extends RotatableDecorationBlock {
    public static final BooleanProperty PRIMARY = BooleanProperty.create("primary");
    private static final int LINKED_PART_UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    // Removal must notify supported decorations above associated parts as vanilla does.
    private static final int LINKED_PART_REMOVAL_FLAGS = Block.UPDATE_ALL;
    // A different supported structure may lose support during partner cleanup.
    // Suppress recursion only for the structure currently being removed.
    private record RemovalKey(Level level, Block block, BlockPos origin) {
    }

    private static final ThreadLocal<Set<RemovalKey>> REMOVING_LINKED_PART = ThreadLocal.withInitial(HashSet::new);
    //? if >=1.21 {
    public static final MapCodec<MultiBlockDecorationBlock> CODEC = simpleCodec(MultiBlockDecorationBlock::new);
    //?}

    public MultiBlockDecorationBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateForPart(defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH), 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        addPartProperties(builder);
    }

    // Existing two-part decorations retain their saved primary=true/false states.
    protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PRIMARY);
    }

    protected int partCount() {
        return 2;
    }

    protected int partIndex(BlockState state) {
        return state.getValue(PRIMARY) ? 0 : 1;
    }

    protected BlockState stateForPart(BlockState state, int index) {
        return state.setValue(PRIMARY, index == 0);
    }

    /** Offset from the structure's origin, transformed by its horizontal facing. */
    protected BlockPos partOffset(BlockState state, int index) {
        return BlockPos.ZERO.relative(getExtensionDirection(state), index);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos primaryPos = context.getClickedPos();
        Direction facing = getPlacementFacing(context);
        if (facing == null) {
            return null;
        }
        BlockState placementState = stateForPart(defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing), 0);
        CollisionContext collisionContext = context.getPlayer() == null
                ? CollisionContext.empty() : CollisionContext.of(context.getPlayer());
        for (int index = 0; index < partCount(); index++) {
            BlockPos partPos = primaryPos.offset(partOffset(placementState, index));
            if (!isWithinPlacementBounds(level, partPos)
                    || !level.getBlockState(partPos).canBeReplaced(context)
                    || !stateForPart(placementState, index).canSurvive(level, partPos)
                    || !level.isUnobstructed(stateForPart(placementState, index), partPos, collisionContext)) {
                return null;
            }
        }

        return placementState;
    }

    /** Default furniture orientation; wall-mounted subclasses use the clicked face. */
    protected Direction getPlacementFacing(BlockPlaceContext context) {
        return context.getHorizontalDirection().getOpposite();
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
        if (level.isClientSide || partIndex(state) != 0) {
            return;
        }

        // A callback may have changed the primary; never remove an unrelated replacement.
        if (level.getBlockState(pos) != state) {
            return;
        }
        if (!state.canSurvive(level, pos)) {
            rollbackPlacement(level, pos, state, 1);
            return;
        }
        CollisionContext collisionContext = placer == null
                ? CollisionContext.empty() : CollisionContext.of(placer);
        // Check the complete footprint before writing any associated part.
        for (int index = 1; index < partCount(); index++) {
            if (!canPlacePart(level, pos, state, index, collisionContext)) {
                rollbackPlacement(level, pos, state, 1);
                return;
            }
        }
        int placed = 1;
        for (int index = 1; index < partCount(); index++) {
            BlockPos partPos = pos.offset(partOffset(state, index));
            // Earlier writes can invoke other mods' callbacks: recheck each write.
            if (!canPlacePart(level, pos, state, index, collisionContext)
                    || !level.setBlock(partPos, stateForPart(state, index), LINKED_PART_UPDATE_FLAGS)) {
                rollbackPlacement(level, pos, state, placed);
                return;
            }
            placed++;
            for (int written = 0; written < placed; written++) {
                if (level.getBlockState(pos.offset(partOffset(state, written))) != stateForPart(state, written)) {
                    rollbackPlacement(level, pos, state, placed);
                    return;
                }
            }
        }
    }

    private boolean canPlacePart(Level level, BlockPos origin, BlockState state, int index,
                                 CollisionContext collisionContext) {
        BlockPos partPos = origin.offset(partOffset(state, index));
        return isWithinPlacementBounds(level, partPos)
                && level.getBlockState(partPos).canBeReplaced()
                && stateForPart(state, index).canSurvive(level, partPos)
                && level.isUnobstructed(stateForPart(state, index), partPos, collisionContext);
    }

    private void rollbackPlacement(Level level, BlockPos origin, BlockState state, int placed) {
        RemovalKey key = new RemovalKey(level, this, origin.immutable());
        Set<RemovalKey> removing = REMOVING_LINKED_PART.get();
        boolean added = removing.add(key);
        try {
            for (int index = placed - 1; index >= 0; index--) {
                BlockPos partPos = origin.offset(partOffset(state, index));
                // Only roll back states written by this placement, never replacements.
                if (level.getBlockState(partPos) == stateForPart(state, index)) {
                    level.setBlock(partPos, Blocks.AIR.defaultBlockState(), LINKED_PART_REMOVAL_FLAGS);
                }
            }
        } finally {
            if (added) {
                removing.remove(key);
            }
            if (removing.isEmpty()) {
                REMOVING_LINKED_PART.remove();
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !newState.is(this)) {
            int removedIndex = partIndex(state);
            BlockPos origin = pos.subtract(partOffset(state, removedIndex));
            RemovalKey key = new RemovalKey(level, this, origin.immutable());
            Set<RemovalKey> removing = REMOVING_LINKED_PART.get();
            if (removing.add(key)) {
                try {
                    for (int index = 0; index < partCount(); index++) {
                        if (index == removedIndex) {
                            continue;
                        }
                        BlockPos partPos = origin.offset(partOffset(state, index));
                        // A world border can shrink after placement; cleanup is still valid.
                        if (!level.isOutsideBuildHeight(partPos)
                                && level.getBlockState(partPos) == stateForPart(state, index)) {
                            // setBlock removes the part without invoking loot a second time.
                            level.setBlock(partPos, Blocks.AIR.defaultBlockState(), LINKED_PART_REMOVAL_FLAGS);
                        }
                    }
                } finally {
                    removing.remove(key);
                    if (removing.isEmpty()) {
                        REMOVING_LINKED_PART.remove();
                    }
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
