package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import net.minecraft.world.ItemInteractionResult;
//?}
import com.craftdistrict.item.WrenchItem;
import com.craftdistrict.lighting.LightDyeData;
import com.craftdistrict.menu.LightColorMenu;
import com.craftdistrict.network.LightDyeNetworking;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import java.util.ArrayList;
import java.util.List;

/** The original table states/shapes, with sparse saved fabric tinting and action-time group walks. */
public final class TableclothTableBlock extends ModularDiningTableBlock {
    // Bound maliciously large requests without recoloring only part of a group or loading chunks.
    private static final int MAX_GROUP = 16384;
    public record Group(List<BlockPos> parts, boolean complete) { }
    public enum ColorResult { CHANGED, UNCHANGED, MISSING_DYE, STALE }
    //? if >=1.21 {
    public static final MapCodec<TableclothTableBlock> CODEC = simpleCodec(TableclothTableBlock::new);
    //?}
    public TableclothTableBlock(Properties properties) { super(properties); }

    public Group connected(Level level, BlockPos origin) {
        if (!level.hasChunkAt(origin) || !level.getBlockState(origin).is(this)) return new Group(List.of(), false);
        List<BlockPos> parts = new ArrayList<>(); LongOpenHashSet seen = new LongOpenHashSet();
        parts.add(origin.immutable()); seen.add(origin.asLong()); boolean complete = true;
        for (int index = 0; index < parts.size(); index++) {
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockPos next = parts.get(index).relative(side);
                if (!level.getWorldBorder().isWithinBounds(next)) continue;
                if (!level.hasChunkAt(next)) { complete = false; continue; }
                if (!level.getBlockState(next).is(this) || !seen.add(next.asLong())) continue;
                if (parts.size() == MAX_GROUP) return new Group(List.of(), false);
                parts.add(next);
            }
        }
        return new Group(List.copyOf(parts), complete);
    }

    public ColorResult color(ServerLevel level, BlockPos pos, Player player, int selected, int expected, List<BlockPos> snapshot) {
        if (selected < 0 || selected > 15 || player == null || !player.isAlive() || player.isSpectator()
                || !player.mayBuild() || player.level() != level
                || player.distanceToSqr(pos.getX()+.5, pos.getY()+.5, pos.getZ()+.5) > 64) return ColorResult.STALE;
        Group group = connected(level, pos);
        if (!group.complete() || group.parts().isEmpty() || snapshot != null && !snapshot.equals(group.parts())) return ColorResult.STALE;
        LightDyeData data = LightDyeData.get(level);
        for (BlockPos part : group.parts())
            if (!level.getWorldBorder().isWithinBounds(part) || !level.mayInteract(player, part) || data.color(part) != expected)
                return ColorResult.STALE;
        if (selected == expected) return ColorResult.UNCHANGED;
        // One dye for the whole connected cloth. Table recoloring does not create dye refunds.
        if (player.getAbilities().instabuild) {
            for (BlockPos part : group.parts()) data.putUnpaid(part, selected);
        } else {
            LightDyeData.Payment payment = data.payment(player, group.parts(), selected, false, false);
            if (payment == null) return ColorResult.MISSING_DYE;
            payment.commit(player);
        }
        LightDyeNetworking.changed(level, group.parts());
        return ColorResult.CHANGED;
    }

    /** Merging/new sections inherit a deterministic existing color, with no dye transactions. */
    private void reconcile(ServerLevel level, List<BlockPos> parts) {
        if (parts.isEmpty()) return;
        LightDyeData data = LightDyeData.get(level); BlockPos anchor = null; int chosen = -1;
        for (BlockPos part : parts) {
            int color = data.color(part);
            if (color >= 0 && (anchor == null || part.asLong() < anchor.asLong())) { anchor = part; chosen = color; }
        }
        if (anchor == null) return;
        List<BlockPos> changes = new ArrayList<>();
        for (BlockPos part : parts) if (data.inherit(part, chosen)) changes.add(part);
        if (!changes.isEmpty()) LightDyeNetworking.changed(level, changes);
    }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        if (!old.is(this) && level instanceof ServerLevel server) reconcile(server, connected(level, pos).parts());
    }

    /** Chunk delivery is already event-driven; reconcile newly visible joins without a ticker or new cache. */
    public static void reconcileChunk(ServerLevel level, int x, int z) {
        LightDyeData data = LightDyeData.get(level); LongOpenHashSet visited = null;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            if (dx != 0 && dz != 0) continue;
            var palette = data.snapshot(new ChunkPos(x + dx, z + dz));
            for (long packed : palette.keySet()) {
                BlockPos pos = BlockPos.of(packed);
                if (dx < 0 && (pos.getX() & 15) != 15 || dx > 0 && (pos.getX() & 15) != 0
                        || dz < 0 && (pos.getZ() & 15) != 15 || dz > 0 && (pos.getZ() & 15) != 0
                        || !level.hasChunkAt(pos) || visited != null && visited.contains(packed)) continue;
                if (!(level.getBlockState(pos).getBlock() instanceof TableclothTableBlock table)) continue;
                Group group = table.connected(level, pos);
                if (visited == null) visited = new LongOpenHashSet();
                for (BlockPos part : group.parts()) visited.add(part.asLong());
                table.reconcile(level, group.parts());
            }
        }
    }

    public InteractionResult useColor(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (player == null || !player.mayBuild() || !level.mayInteract(player, pos)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof WrenchItem) {
            if (!player.isShiftKeyDown()) {
                if (level instanceof ServerLevel server) {
                    Group group = connected(level, pos);
                    if (!group.complete() || group.parts().isEmpty()) {
                        player.displayClientMessage(Component.translatable("screen.craftdistrict.tablecloth_color.unavailable"), true);
                        return InteractionResult.CONSUME;
                    }
                    reconcile(server, group.parts());
                }
                return LightColorMenu.open(state, level, pos, player, hand);
            }
            InteractionResult result = toggleSupport(state, level, pos, player);
            if (!level.isClientSide && result.consumesAction() && level.getBlockState(pos) != state) WrenchItem.used(player, hand, held);
            return result;
        }
        if (!(held.getItem() instanceof DyeItem dye) || dye.getDyeColor().getId() < 0 || dye.getDyeColor().getId() > 15)
            return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            ColorResult result = color(server, pos, player, dye.getDyeColor().getId(), LightDyeData.get(server).color(pos), null);
            if (result == ColorResult.STALE)
                player.displayClientMessage(Component.translatable("screen.craftdistrict.tablecloth_color.unavailable"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public InteractionResult toggleSupport(BlockState state, Level level, BlockPos pos, Player player) {
        // Sneaking cycles the original 0/1/2/4 supports, then restores automatic placement.
        boolean automatic = state.getValue(MANUAL_SUPPORT) && state.getValue(LEG_NW) && state.getValue(LEG_NE)
                && state.getValue(LEG_SE) && state.getValue(LEG_SW);
        return configureSupport(state, level, pos, player, automatic);
    }
    //? if >=1.21 {
    @Override public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                   Player player, InteractionHand hand, BlockHitResult hit) {
        return useColor(state, level, pos, player, hand).consumesAction() ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return useColor(state, level, pos, player, hand);
    }
    *///?}
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!next.is(this) && level instanceof ServerLevel server) {
            for (var player : server.players())
                if (player.containerMenu instanceof LightColorMenu menu && menu.targets(pos)) player.closeContainer();
            if (LightDyeData.get(server).put(pos, -1)) LightDyeNetworking.changed(server, List.of(pos));
        }
        super.onRemove(state, level, pos, next, moving);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    //?}
}
