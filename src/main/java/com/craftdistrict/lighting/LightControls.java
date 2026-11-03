package com.craftdistrict.lighting;

import com.craftdistrict.block.ControlledLight;
import com.craftdistrict.block.ExitSignBlock;
import com.craftdistrict.block.OpenSignBlock;
import com.craftdistrict.block.MultiMountLightBlock;
import com.craftdistrict.block.MultiBlockDecorationBlock;
import com.craftdistrict.network.LightDyeNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import java.util.ArrayList;
import java.util.List;

public final class LightControls {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty REDSTONE = BooleanProperty.create("redstone_controlled");
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private LightControls() { }

    public static boolean dyeable(BlockState state) {
        return state.getBlock() instanceof ControlledLight
                && !(state.getBlock() instanceof ExitSignBlock)
                && !(state.getBlock() instanceof OpenSignBlock)
                && !(state.getBlock() instanceof MultiMountLightBlock);
    }

    /** Linked fixtures are one light, including horizontal tubes and wall-mounted tracks. */
    public static List<BlockPos> parts(BlockState state, BlockPos pos) {
        return state.getBlock() instanceof MultiBlockDecorationBlock light
                ? light.structureParts(state, pos) : List.of(pos);
    }

    private static boolean complete(Level level, BlockState state, List<BlockPos> parts) {
        for (int i = 0; i < parts.size(); i++) {
            BlockState other = level.getBlockState(parts.get(i));
            if (!other.is(state.getBlock())) return false;
            if (state.getBlock() instanceof MultiBlockDecorationBlock light
                    && other.setValue(LIT, state.getValue(LIT)).setValue(REDSTONE, state.getValue(REDSTONE))
                    != light.structurePartState(state, i)) return false;
        }
        return true;
    }

    private static boolean powered(Level level, List<BlockPos> parts) {
        for (BlockPos pos : parts) if (level.hasNeighborSignal(pos)) return true;
        return false;
    }

    private static boolean apply(Level level, List<BlockPos> parts, boolean redstone, boolean lit) {
        List<BlockState> before = new ArrayList<>(parts.size());
        for (BlockPos pos : parts) before.add(level.getBlockState(pos));
        int written = 0;
        for (int i = 0; i < parts.size(); i++) {
            BlockState next = before.get(i).setValue(REDSTONE, redstone).setValue(LIT, lit);
            if (next != before.get(i) && !level.setBlock(parts.get(i), next, FLAGS)) {
                for (int j = written - 1; j >= 0; j--) level.setBlock(parts.get(j), before.get(j), FLAGS);
                return false;
            }
            written++;
        }
        return true;
    }

    public static void neighborChanged(BlockState state, Level level, BlockPos pos) {
        if (level.isClientSide || !state.getValue(REDSTONE)) return;
        BlockState current = level.getBlockState(pos);
        if (!current.is(state.getBlock()) || !current.getValue(REDSTONE)) return;
        List<BlockPos> parts = parts(current, pos);
        if (!complete(level, current, parts)) return;
        boolean lit = powered(level, parts);
        for (BlockPos part : parts) {
            if (level.getBlockState(part).getValue(LIT) != lit) {
                apply(level, parts, true, lit);
                break;
            }
        }
    }

    public static InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        BlockState current = level.getBlockState(pos);
        if (!current.is(state.getBlock()) || !(current.getBlock() instanceof ControlledLight)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        // Dye has priority, including a dye in the other hand when the active hand is empty.
        if (held.isEmpty()) {
            ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (other.getItem() instanceof DyeItem) held = other;
        }
        if (held.getItem() instanceof DyeItem dye) {
            if (!dyeable(current)) return InteractionResult.PASS;
            List<BlockPos> parts = parts(current, pos);
            if (!complete(level, current, parts)) return InteractionResult.PASS;
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (!(level instanceof ServerLevel server)) return InteractionResult.PASS;
            LightDyeData data = LightDyeData.get(server);
            int color = dye.getDyeColor().getId();
            boolean changed = false;
            for (BlockPos part : parts) changed |= data.put(part, color);
            if (changed) {
                if (!player.getAbilities().instabuild) held.shrink(1);
                LightDyeNetworking.changed(server, parts);
            }
            return InteractionResult.CONSUME;
        }
        if (!held.isEmpty()) return InteractionResult.PASS;
        List<BlockPos> parts = parts(current, pos);
        if (!complete(level, current, parts)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        boolean mode = current.getValue(REDSTONE);
        if (player.isShiftKeyDown()) {
            mode = !mode;
            if (apply(level, parts, mode, mode ? powered(level, parts) : current.getValue(LIT)))
                player.displayClientMessage(Component.translatable("message.craftdistrict.light.mode." + (mode ? "redstone" : "manual")), true);
        } else if (!mode) apply(level, parts, false, !current.getValue(LIT));
        return InteractionResult.CONSUME;
    }

    public static void removed(BlockState state, Level level, BlockPos pos, BlockState replacement) {
        if (!level.isClientSide && !replacement.is(state.getBlock()) && level instanceof ServerLevel server
                && LightDyeData.get(server).put(pos, -1)) LightDyeNetworking.changed(server, List.of(pos));
    }
}
