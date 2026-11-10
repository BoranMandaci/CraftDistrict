package com.craftdistrict.item;

import com.craftdistrict.block.HotelShowerFloorBlock;
import com.craftdistrict.block.HotelShowerFixtureBlock;
import com.craftdistrict.block.MultiBlockDecorationBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.stats.Stats;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;

/** A shower fixture and its separate floor tray share the lower cell without storage. */
public final class ShowerFixtureBlockItem extends BlockItem {
    public ShowerFixtureBlockItem(Block block, Properties properties) { super(block, properties); }
    private static boolean base(BlockState state) {
        return state.getBlock() instanceof HotelShowerFloorBlock;
    }
    private static boolean allowed(UseOnContext context, BlockPos pos) {
        Player player = context.getPlayer(); Level level = context.getLevel();
        return player != null && !player.isSpectator() && level.hasChunkAt(pos) && !level.isOutsideBuildHeight(pos)
                && level.getWorldBorder().isWithinBounds(pos) && level.mayInteract(player, pos)
                && player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand());
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        for (BlockPos pos : new BlockPos[]{context.getClickedPos(), context.getClickedPos().relative(context.getClickedFace())}) {
            if (!level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (getBlock() instanceof HotelShowerFixtureBlock fixture && base(state)) return placeFixture(context, pos, state, fixture);
            if (getBlock() instanceof HotelShowerFloorBlock
                    && state.getBlock() instanceof HotelShowerFixtureBlock fixture
                    && state.getValue(HotelShowerFixtureBlock.TALL) && !state.getValue(HotelShowerFixtureBlock.BASE))
                return addBase(context, pos, state, fixture);
        }
        return super.useOn(context);
    }
    private InteractionResult placeFixture(UseOnContext context, BlockPos pos, BlockState oldBase, HotelShowerFixtureBlock fixture) {
        Level level = context.getLevel(); Player player = context.getPlayer();
        if (!allowed(context, pos) || !allowed(context, pos.above())) return InteractionResult.FAIL;
        Direction facing = context.getClickedFace().getAxis().isHorizontal() ? context.getClickedFace() : oldBase.getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockState lower = fixture.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(HotelShowerFixtureBlock.TALL, true).setValue(HotelShowerFixtureBlock.BASE, true)
                .setValue(HotelShowerFixtureBlock.PLAIN_BASE, !((HotelShowerFloorBlock) oldBase.getBlock()).hasDrain());
        lower = HotelShowerFloorBlock.connections(lower, level, pos);
        BlockState upper = fixture.structurePartState(lower, 1);
        BlockPlaceContext placement = new BlockPlaceContext(context);
        if (!lower.canSurvive(level, pos) || !upper.canSurvive(level, pos.above())
                || !level.getBlockState(pos.above()).canBeReplaced(placement)
                || !level.isUnobstructed(lower, pos, CollisionContext.of(player))
                || !level.isUnobstructed(upper, pos.above(), CollisionContext.of(player))) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockState(pos) != oldBase || !level.setBlock(pos, lower, Block.UPDATE_ALL)) return InteractionResult.FAIL;
        fixture.setPlacedBy(level, pos, lower, player, context.getItemInHand());
        if (level.getBlockState(pos) != lower || level.getBlockState(pos.above()) != upper) {
            BlockState current = level.getBlockState(pos);
            if (current == lower || current.isAir()) level.setBlock(pos, oldBase, Block.UPDATE_ALL);
            return InteractionResult.FAIL;
        }
        HotelShowerFloorBlock.refreshAround(level, pos);
        return complete(context, pos, lower);
    }
    private InteractionResult addBase(UseOnContext context, BlockPos clickedPos, BlockState clicked, HotelShowerFixtureBlock fixture) {
        if (!clicked.getValue(HotelShowerFixtureBlock.TALL)) return InteractionResult.FAIL;
        Level level = context.getLevel(); Player player = context.getPlayer();
        BlockPos pos = fixture.structureOrigin(clicked, clickedPos);
        if (!allowed(context, pos) || !allowed(context, pos.above())) return InteractionResult.FAIL;
        BlockState lower = level.getBlockState(pos), upper = level.getBlockState(pos.above());
        if (!lower.is(fixture) || lower.getValue(HotelShowerFixtureBlock.BASE) || !lower.getValue(MultiBlockDecorationBlock.PRIMARY)
                || upper != fixture.structurePartState(lower, 1)) return InteractionResult.FAIL;
        BlockPlaceContext placement = new BlockPlaceContext(context) {
            @Override public BlockPos getClickedPos() { return pos; }
        };
        BlockState baseState = getBlock().getStateForPlacement(placement);
        if (baseState == null) return InteractionResult.FAIL;
        BlockState joined = lower.setValue(HotelShowerFixtureBlock.BASE, true)
                .setValue(HotelShowerFixtureBlock.PLAIN_BASE, !((HotelShowerFloorBlock) getBlock()).hasDrain());
        joined = HotelShowerFloorBlock.connections(joined, level, pos);
        BlockState joinedUpper = fixture.structurePartState(joined, 1);
        if (!level.isUnobstructed(joined, pos, CollisionContext.of(player))) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        if (!level.setBlock(pos, joined, flags)) return InteractionResult.FAIL;
        boolean valid = level.getBlockState(pos) == joined && level.getBlockState(pos.above()) == upper
                && level.setBlock(pos.above(), joinedUpper, flags)
                && level.getBlockState(pos) == joined && level.getBlockState(pos.above()) == joinedUpper;
        if (!valid) {
            if (level.getBlockState(pos.above()) == joinedUpper) level.setBlock(pos.above(), upper, flags);
            if (level.getBlockState(pos) == joined) level.setBlock(pos, lower, flags);
            return InteractionResult.FAIL;
        }
        HotelShowerFloorBlock.refreshAround(level, pos);
        return complete(context, pos, baseState);
    }
    private InteractionResult complete(UseOnContext context, BlockPos pos, BlockState placed) {
        Player player = context.getPlayer(); ItemStack stack = context.getItemInHand();
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        if (!player.getAbilities().instabuild) stack.shrink(1);
        SoundType sound = placed.getSoundType();
        context.getLevel().playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2, sound.getPitch() * .8F);
        context.getLevel().gameEvent(player, GameEvent.BLOCK_PLACE, pos);
        return InteractionResult.CONSUME;
    }
}
