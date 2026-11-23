package com.craftdistrict.lighting;

import com.craftdistrict.block.ControlledLight;
import com.craftdistrict.block.ExitSignBlock;
import com.craftdistrict.block.OpenSignBlock;
import com.craftdistrict.block.MultiMountLightBlock;
import com.craftdistrict.block.MultiBlockDecorationBlock;
import com.craftdistrict.network.LightDyeNetworking;
import com.craftdistrict.item.WrenchItem;
import com.craftdistrict.menu.LightColorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import java.util.List;
import java.util.function.Predicate;

public final class LightControls {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty REDSTONE = BooleanProperty.create("redstone_controlled");
    public static final EnumProperty<LightEffect.Stage> EFFECT = EnumProperty.create("effect", LightEffect.Stage.class);
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final Direction[] POWER_SIDES = Direction.values();
    private static final Predicate<Entity> ELIGIBLE = player -> player.isAlive() && !player.isSpectator();
    private LightControls() { }

    public static boolean settingsEligible(Block block) {
        return block instanceof ControlledLight && !(block instanceof ExitSignBlock) && !(block instanceof OpenSignBlock);
    }

    public static boolean dyeable(BlockState state) {
        return settingsEligible(state.getBlock()) && !(state.getBlock() instanceof MultiMountLightBlock);
    }

    public static LightEffect effect(BlockState state) {
        return state.hasProperty(EFFECT) ? state.getValue(EFFECT).effect : LightEffect.STEADY;
    }

    /** LIT drives both the existing OFF model and the vanilla light engine. */
    public static int emission(BlockState state, int maximum) {
        if (!state.getValue(LIT)) return 0;
        return state.hasProperty(EFFECT) ? (maximum * state.getValue(EFFECT).level + 7) / 15 : maximum;
    }

    static void updateEffect(ServerLevel level, BlockPos pos, BlockState state, LightEffect effect, int brightness) {
        LightEffect.Stage next = LightEffect.Stage.of(effect, brightness);
        if (state.getValue(EFFECT) == next && state.getValue(LIT) == (brightness > 0)) return;
        if (!(state.getBlock() instanceof MultiBlockDecorationBlock)) {
            // Most fixtures need one write, with no linked-layout lists or rollback arrays per animation step.
            level.setBlock(pos, state.setValue(LIT, brightness > 0).setValue(EFFECT, next), FLAGS);
            return;
        }
        List<BlockPos> parts = parts(state, pos);
        // Scheduled animation already knows the frame and does not change the fixture's schedule.
        if (complete(level, state, parts)) applyFrame(level, parts, state.getValue(REDSTONE), brightness > 0, next);
    }

    /** An OFF animation frame is still activated. Keep that intent in the sparse scheduler, not another state property. */
    public static LedLightGroups.Group group(Level level, BlockState state, BlockPos pos) {
        if (!(level instanceof ServerLevel server) || !LedLightGroups.panel(state)) return null;
        LedLightGroups groups = LedLightGroups.get(server);
        var group = groups.find(pos); return group == null || group.mount != LedLightGroups.mounting(state) ? groups.ensure(server, pos) : group;
    }
    public static boolean active(Level level, BlockState state, BlockPos pos) {
        var shared = group(level, state, pos); if (shared != null) return shared.active();
        return state.getValue(LIT) || settingsEligible(state.getBlock()) && level instanceof ServerLevel server
                && LightProximityData.get(server).animated(origin(state, pos), effect(state));
    }

    public static LightMode mode(Level level, BlockState state, BlockPos pos) {
        var shared = group(level, state, pos); if (shared != null) return shared.mode();
        if (state.hasProperty(REDSTONE) && state.getValue(REDSTONE)) return LightMode.REDSTONE;
        return settingsEligible(state.getBlock()) && level instanceof ServerLevel server
                && LightProximityData.get(server).contains(origin(state, pos)) ? LightMode.PROXIMITY : LightMode.MANUAL;
    }

    public static BlockPos origin(BlockState state, BlockPos pos) {
        return state.getBlock() instanceof MultiBlockDecorationBlock light ? light.structureOrigin(state, pos) : pos;
    }

    /** Linked fixtures are one light, including horizontal tubes and wall-mounted tracks. */
    public static List<BlockPos> parts(BlockState state, BlockPos pos) {
        return state.getBlock() instanceof MultiBlockDecorationBlock light ? light.structureParts(state, pos) : List.of(pos);
    }

    /** Settings/activation are not structural identity and may be committed together across several sections. */
    public static BlockState shape(BlockState state) {
        if (!(state.getBlock() instanceof ControlledLight)) return state;
        BlockState result = state.setValue(LIT, false).setValue(REDSTONE, false);
        return result.hasProperty(EFFECT) ? result.setValue(EFFECT, LightEffect.Stage.STEADY) : result;
    }

    public static boolean complete(Level level, BlockState state, List<BlockPos> parts) {
        for (int i = 0; i < parts.size(); i++) {
            if (!level.hasChunkAt(parts.get(i))) return false;
            BlockState other = level.getBlockState(parts.get(i));
            if (!other.is(state.getBlock())) return false;
            if (state.getBlock() instanceof MultiBlockDecorationBlock light
                    && shape(other) != shape(light.structurePartState(state, i))) return false;
        }
        return true;
    }

    /** Use the platform's signal rules without loading a chunk through a conductor's extra neighbor reads. */
    static final class PowerCheck implements SignalGetter {
        private final Level level;
        private final BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();
        private long chunkKey = Long.MIN_VALUE;
        private LevelChunk chunk;

        PowerCheck(Level level) { this.level = level; }

        boolean hasPower(long pos) {
            int x = BlockPos.getX(pos), y = BlockPos.getY(pos), z = BlockPos.getZ(pos);
            for (Direction side : POWER_SIDES) {
                neighbor.set(x + side.getStepX(), y + side.getStepY(), z + side.getStepZ());
                if (getSignal(neighbor, side) > 0) return true;
            }
            return false;
        }

        private LevelChunk chunkAt(BlockPos pos) {
            int x = pos.getX() >> 4, z = pos.getZ() >> 4;
            long key = ChunkPos.asLong(x, z);
            if (key != chunkKey) {
                chunkKey = key;
                chunk = level.getChunkSource().getChunkNow(x, z);
            }
            return chunk;
        }

        @Override public BlockState getBlockState(BlockPos pos) {
            if (isOutsideBuildHeight(pos)) return Blocks.VOID_AIR.defaultBlockState();
            LevelChunk loaded = chunkAt(pos);
            return loaded == null ? Blocks.AIR.defaultBlockState() : loaded.getBlockState(pos);
        }

        @Override public BlockEntity getBlockEntity(BlockPos pos) {
            if (isOutsideBuildHeight(pos)) return null;
            LevelChunk loaded = chunkAt(pos);
            return loaded == null ? null : loaded.getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE);
        }

        @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
        @Override public int getHeight() { return level.getHeight(); }
        @Override public int getMinBuildHeight() { return level.getMinBuildHeight(); }
    }

    private static boolean powered(Level level, List<BlockPos> parts) {
        PowerCheck power = new PowerCheck(level);
        for (BlockPos pos : parts) if (power.hasPower(pos.asLong())) return true;
        return false;
    }

    /** Change only when necessary; a rejected linked write cannot consume a dye or partially change the fixture. */
    private static boolean apply(Level level, List<BlockPos> parts, boolean redstone, boolean lit, LightEffect effect) {
        BlockState first = level.getBlockState(parts.get(0));
        BlockPos origin = origin(first, parts.get(0));
        int brightness = lit ? effect.levelAt(level.getGameTime() + LightProximityData.phase(origin.asLong())) : 0;
        LightEffect.Stage stage = LightEffect.Stage.of(effect, brightness);
        if (!applyFrame(level, parts, redstone, brightness > 0, stage)) return false;
        if (settingsEligible(first.getBlock()) && level instanceof ServerLevel server)
            LightProximityData.get(server).setAnimation(origin, lit ? effect : LightEffect.STEADY);
        return true;
    }

    private static boolean applyFrame(Level level, List<BlockPos> parts, boolean redstone, boolean lit, LightEffect.Stage stage) {
        BlockState[] before = new BlockState[parts.size()], after = new BlockState[parts.size()];
        for (int i = 0; i < parts.size(); i++) {
            before[i] = level.getBlockState(parts.get(i));
            after[i] = before[i].setValue(REDSTONE, redstone).setValue(LIT, lit);
            if (after[i].hasProperty(EFFECT)) after[i] = after[i].setValue(EFFECT, stage);
        }
        int written = 0;
        boolean valid = true;
        for (int i = 0; i < parts.size(); i++) {
            if (level.getBlockState(parts.get(i)) != before[i]
                    || (after[i] != before[i] && !level.setBlock(parts.get(i), after[i], FLAGS))) { valid = false; break; }
            written++;
            for (int j = 0; j < written; j++)
                if (level.getBlockState(parts.get(j)) != after[j]) { valid = false; break; }
            if (!valid) break;
        }
        if (valid) return true;
        for (int i = written - 1; i >= 0; i--)
            if (level.getBlockState(parts.get(i)) == after[i] && before[i] != after[i]) level.setBlock(parts.get(i), before[i], FLAGS);
        return false;
    }

    public static boolean nearby(ServerLevel level, List<BlockPos> parts, boolean wasActive) {
        double range = proximityRange(wasActive);
        for (BlockPos pos : parts)
            if (nearby(level, pos, range)) return true;
        return false;
    }

    private static double proximityRange(boolean wasActive) {
        return Math.nextUp(wasActive ? LightProximityData.RELEASE_RANGE : LightProximityData.RANGE);
    }

    private static boolean nearby(ServerLevel level, BlockPos pos, double range) {
        return level.getNearestPlayer(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, range, ELIGIBLE) != null;
    }

    public static boolean setSettings(Level level, BlockState state, BlockPos pos, LightMode mode, LightEffect effect) {
        var group = group(level, state, pos);
        return setSettings(level, state, pos, mode, effect, group != null ? group.manual() : active(level, state, pos));
    }
    public static boolean setSettings(Level level, BlockState state, BlockPos pos, LightMode mode, LightEffect effect, boolean manualPower) {
        var group = group(level, state, pos);
        if (group != null) return LedLightGroups.get((ServerLevel)level).apply((ServerLevel)level, group, mode, effect, manualPower);
        if (mode == LightMode.PROXIMITY && (!settingsEligible(state.getBlock()) || !(level instanceof ServerLevel))) return false;
        List<BlockPos> parts = parts(state, pos);
        if (!complete(level, state, parts)) return false;
        boolean active = switch (mode) {
            case MANUAL -> manualPower;
            case REDSTONE -> powered(level, parts);
            case PROXIMITY -> nearby((ServerLevel)level, parts, mode(level, state, pos) == LightMode.PROXIMITY && active(level, state, pos));
        };
        if (!apply(level, parts, mode == LightMode.REDSTONE, active, effect)) return false;
        if (settingsEligible(state.getBlock()) && level instanceof ServerLevel server)
            LightProximityData.get(server).set(origin(state, pos), mode == LightMode.PROXIMITY);
        return true;
    }

    public static void updateProximity(ServerLevel level, BlockPos pos, BlockState state) {
        boolean enabled = active(level, state, pos);
        if (!(state.getBlock() instanceof MultiBlockDecorationBlock)) {
            // One exact test for an individual fixture, with no layout unless power changes.
            boolean active = nearby(level, pos, proximityRange(enabled));
            if (active != enabled) apply(level, List.of(pos), false, active, effect(state));
            return;
        }
        // Current linked lights span at most three cells. This coarse rejection avoids layout allocations for distant, off fixtures.
        boolean possible = nearby(level, pos, 13);
        if (!possible && !enabled) return;
        List<BlockPos> parts = parts(state, pos);
        if (!complete(level, state, parts)) return;
        boolean active = possible && nearby(level, parts, enabled);
        if (active != enabled) apply(level, parts, false, active, effect(state));
    }

    public static void neighborChanged(BlockState state, Level level, BlockPos pos) {
        if (level instanceof ServerLevel server && LedLightGroups.panel(state)) {
            var group = group(level, state, pos); if (group != null) LedLightGroups.get(server).control(server, group, pos);
            return;
        }
        if (level.isClientSide || !state.getValue(REDSTONE)) return;
        BlockState current = level.getBlockState(pos);
        if (!current.is(state.getBlock()) || !current.getValue(REDSTONE)) return;
        List<BlockPos> parts = parts(current, pos);
        if (!complete(level, current, parts)) return;
        boolean lit = powered(level, parts);
        if (active(level, current, pos) != lit) apply(level, parts, true, lit, effect(current));
    }

    public static InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        BlockState current = level.getBlockState(pos);
        if (!current.is(state.getBlock()) || !(current.getBlock() instanceof ControlledLight)) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            if (other.getItem() instanceof DyeItem || other.getItem() instanceof WrenchItem) held = other;
        }
        InteractionHand usedHand = player.getItemInHand(hand) == held ? hand
                : hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (held.getItem() instanceof WrenchItem && !player.isShiftKeyDown() && settingsEligible(current.getBlock()))
            return LightColorMenu.open(current, level, pos, player, usedHand);
        if (held.getItem() instanceof DyeItem dye) {
            int color = dye.getDyeColor().getId();
            if (!dyeable(current) || color < 0 || color > 15) return InteractionResult.PASS;
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (!(level instanceof ServerLevel server)) return InteractionResult.PASS;
            var shared = group(level, current, pos);
            List<BlockPos> parts = shared == null ? parts(current, pos) : LedLightGroups.get(server).allParts(shared);
            if (shared == null && !complete(level, current, parts)) return InteractionResult.PASS;
            LightDyeData data = LightDyeData.get(server);
            int previous = shared == null ? data.color(pos) : shared.color();
            if (color == previous) return InteractionResult.CONSUME;
            LightDyeData.Payment payment = data.payment(player, parts, color, shared != null, false);
            if (payment == null) return InteractionResult.CONSUME;
            payment.commit(player);
            if (shared != null) LedLightGroups.get(server).color(shared, color);
            LightDyeNetworking.changed(server, shared == null ? parts : LedLightGroups.get(server).loadedParts(server, shared));
            WrenchItem.usedIfHeld(player);
            return InteractionResult.CONSUME;
        }
        // Power/mode controls are Wrench-only, including the legacy signs.
        if (!(held.getItem() instanceof WrenchItem)) return InteractionResult.PASS;
        List<BlockPos> parts = parts(current, pos);
        if (!complete(level, current, parts)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!player.isAlive() || player.isSpectator() || !player.mayBuild() || !level.mayInteract(player, pos)) return InteractionResult.PASS;
        InteractionHand wrenchHand = usedHand;
        if (player.isShiftKeyDown()) {
            LightMode next = mode(level, current, pos) == LightMode.REDSTONE ? LightMode.MANUAL : LightMode.REDSTONE;
            if (setSettings(level, current, pos, next, effect(current))) {
                player.displayClientMessage(Component.translatable("message.craftdistrict.light.mode." + next.key()), true);
                WrenchItem.used(player, wrenchHand, held);
            }
        } else if (!settingsEligible(current.getBlock()) && mode(level, current, pos) == LightMode.MANUAL) {
            if (apply(level, parts, false, !active(level, current, pos), effect(current)))
                WrenchItem.used(player, wrenchHand, held);
        }
        return InteractionResult.CONSUME;
    }

    public static void removed(BlockState state, Level level, BlockPos pos, BlockState replacement) {
        if (!level.isClientSide && !replacement.is(state.getBlock()) && level instanceof ServerLevel server) {
            if (settingsEligible(state.getBlock())) {
                LightProximityData.get(server).set(origin(state, pos), false);
                LightProximityData.get(server).setAnimation(origin(state, pos), LightEffect.STEADY);
            }
            if (LightDyeData.get(server).put(pos, -1)) LightDyeNetworking.changed(server, List.of(pos));
        }
    }
}
