package com.craftdistrict.item;

import com.craftdistrict.block.NestingShoppingCartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
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
import net.minecraft.world.level.gameevent.GameEvent;

/** Inserting a cart uses one ordinary cart item; packed cells return every cart on breaking. */
public final class ShoppingCartBlockItem extends BlockItem {
    public ShoppingCartBlockItem(Block block, Properties properties) { super(block, properties); }

    @Override public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isSpectator() || player.isShiftKeyDown()) return super.useOn(context);
        NestingShoppingCartBlock cart = (NestingShoppingCartBlock) getBlock();
        Level level = context.getLevel();
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos target = new BlockPlaceContext(context).getClickedPos();
        for (BlockPos candidate : new BlockPos[]{context.getClickedPos(), target, target.relative(facing)}) {
            if (!level.hasChunkAt(candidate)) continue;
            BlockState clicked = level.getBlockState(candidate);
            if (!clicked.is(cart) || clicked.getValue(NestingShoppingCartBlock.FACING) != facing) continue;
            BlockPos origin = cart.structureOrigin(clicked, candidate);
            // Bounded, event-only traversal; no entity, block entity or repeating group tick.
            for (int step = 0; step < 256; step++) {
                if (!cart.rootAt(level, origin, facing)) return InteractionResult.FAIL;
                if (cart.insertCart(level, origin, player, context.getItemInHand())) {
                    if (level.isClientSide) return InteractionResult.SUCCESS;
                    return complete(context, origin, level.getBlockState(origin));
                }
                BlockPos rear = origin.relative(facing.getOpposite());
                if (!level.hasChunkAt(rear)) return InteractionResult.FAIL;
                if (!cart.rootAt(level, rear, facing)) {
                    if (level.isOutsideBuildHeight(rear) || !level.getWorldBorder().isWithinBounds(rear)
                            || !level.mayInteract(player, rear)
                            || !player.mayUseItemAt(rear, Direction.UP, context.getItemInHand())) return InteractionResult.FAIL;
                    // Grow the row using vanilla placement, permissions, item use and multiblock checks.
                    return place(BlockPlaceContext.at(new BlockPlaceContext(context), rear, Direction.UP));
                }
                origin = rear;
            }
            return InteractionResult.FAIL;
        }
        return super.useOn(context);
    }

    private InteractionResult complete(UseOnContext context, BlockPos pos, BlockState state) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        player.awardStat(Stats.ITEM_USED.get(this));
        if (!player.getAbilities().instabuild) stack.shrink(1);
        SoundType sound = state.getSoundType();
        context.getLevel().playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1) / 2, sound.getPitch() * .8F);
        context.getLevel().gameEvent(player, GameEvent.BLOCK_PLACE, pos);
        return InteractionResult.CONSUME;
    }
}
