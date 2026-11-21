package com.craftdistrict.block;

import com.craftdistrict.access.AccessDoorSecurity;
import com.craftdistrict.access.AccessDoorTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Dispatch authorized motion through each door's own transition, using atomic updates for vanilla leaves. */
public final class AccessDoorMotion {
    private AccessDoorMotion() { }

    public static boolean nativePower(AccessDoorTarget target) { return BuiltInRegistries.BLOCK.get(target.blockId()) instanceof DoorBlock; }

    public static BlockPos missing(ServerLevel level, AccessDoorTarget target, Set<Long> frameChunks) {
        for (long chunk : frameChunks) {
            int x = ChunkPos.getX(chunk), z = ChunkPos.getZ(chunk);
            if (level.getChunkSource().getChunkNow(x, z) == null) return new BlockPos(x << 4, target.root().getY(), z << 4);
        }
        BlockState state = AccessDoorTarget.loadedState(level, target.root());
        if (state == null) return target.root();
        if (state.getBlock() instanceof VaultDoorBlock vault && state.getValue(VaultDoorBlock.LARGE)) {
            for (BlockPos part : vault.structureParts(state.setValue(BlockStateProperties.OPEN, true), target.root()))
                if (AccessDoorTarget.loadedState(level, part) == null) return part;
        }
        if (state.getBlock() instanceof ModularMovingDoorBlock && state.getValue(ModularMovingDoorBlock.OPEN))
            return MovingDoorGroups.unloadedPanel(level, target.root());
        return null;
    }

    public static boolean open(ServerLevel level, AccessDoorTarget target, Player player) {
        return move(level, target, player, true);
    }

    public static boolean closeSafely(ServerLevel level, AccessDoorTarget target) {
        if (!anyOpen(level, target)) return true;
        return !occupied(level, target) && move(level, target, null, false);
    }

    private static boolean anyOpen(ServerLevel level, AccessDoorTarget target) {
        for (BlockPos pos : target.parts()) {
            BlockState state = AccessDoorTarget.loadedState(level, pos);
            if (state != null && state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN)) return true;
        }
        return false;
    }

    private static boolean occupied(ServerLevel level, AccessDoorTarget target) {
        AABB area = new AABB(target.root());
        for (BlockPos pos : target.parts()) area = area.minmax(new AABB(pos));
        return !level.getEntitiesOfClass(Player.class, area.inflate(.15, 0, .15), p -> p.isAlive() && !p.isSpectator()).isEmpty();
    }

    private static boolean move(ServerLevel level, AccessDoorTarget target, Player source, boolean open) {
        BlockState state = AccessDoorTarget.loadedState(level, target.root());
        if (state == null || !state.hasProperty(BlockStateProperties.OPEN)) return false;
        if (state.getBlock() instanceof PairedDoorBlock door) return door.accessMove(level, target.root(), open);
        if (state.getBlock() instanceof DoorBlock door) return vanilla(level, target, door, open, false);
        if (state.getBlock() instanceof ModularMovingDoorBlock door && !door.garage()) return MovingDoorGroups.accessMove(level, target, source, open);
        if (state.getBlock() instanceof VaultDoorBlock vault) return vault.accessMove(level, target.root(), source, open);
        if (state.getBlock() instanceof SecretBookshelfDoorBlock bookshelf) return bookshelf.move(state, level, target.root(), source, open);
        if (state.getBlock() instanceof HotelElevatorDoorsBlock elevator)
            return CabinetInteraction.setLinkedDoor(elevator, level, target.root(), null, open, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_DOOR_CLOSE);
        return false;
    }

    public static boolean restorePower(ServerLevel level, AccessDoorTarget target) {
        BlockState state = AccessDoorTarget.loadedState(level, target.root());
        if (state != null && state.getBlock() instanceof PairedDoorBlock door)
            return door.restoreAccessPower(level, target.root(), anyOpen(level, target) && occupied(level, target));
        if (state != null && state.getBlock() instanceof DoorBlock door) return vanilla(level, target, door, false, true);
        return true;
    }

    public record Repair(BlockPos missing, boolean finished) { }

    /** Reconcile surviving leaves with live Redstone after the old link is invalidated. */
    public static Repair restoreSurvivors(ServerLevel level, AccessDoorTarget removed) {
        Set<BlockPos> seen = new HashSet<>();
        BlockPos missing = null;
        boolean finished = true;
        for (BlockPos pos : removed.parts()) {
            BlockState state = AccessDoorTarget.loadedState(level, pos);
            if (state == null) { if (missing == null) missing = pos; continue; }
            if (!(state.getBlock() instanceof DoorBlock) || !removed.matches(state)
                    || state.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER || AccessDoorSecurity.secured(level, pos, state)) continue;
            var resolved = AccessDoorTarget.resolve(level, pos);
            if (missing == null) missing = resolved.missing();
            AccessDoorTarget current = resolved.target();
            if (current != null && seen.add(current.root()) && current.parts().stream().noneMatch(part -> {
                BlockState partState = AccessDoorTarget.loadedState(level, part);
                return partState != null && AccessDoorSecurity.secured(level, part, partState);
            })) finished &= restorePower(level, current);
        }
        return new Repair(missing, finished);
    }

    private static boolean vanilla(ServerLevel level, AccessDoorTarget target, DoorBlock door, boolean open, boolean restore) {
        List<BlockPos> parts = target.parts();
        BlockPos[] positions = parts.toArray(BlockPos[]::new);
        BlockState[] before = new BlockState[positions.length], after = new BlockState[positions.length];
        boolean changed = false, closes = false;
        for (int i = 0; i < positions.length; i++) {
            BlockPos pos = positions[i];
            BlockState state = AccessDoorTarget.loadedState(level, pos);
            if (state == null || !state.is(door)) return false;
            before[i] = state;
            if (restore) {
                BlockPos lower = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
                boolean power = level.hasNeighborSignal(lower) || level.hasNeighborSignal(lower.above());
                after[i] = restoredVanillaState(state, door, power);
            } else after[i] = state.setValue(DoorBlock.OPEN, open);
            changed |= before[i] != after[i];
            closes |= state.getValue(DoorBlock.OPEN) && !after[i].getValue(DoorBlock.OPEN);
        }
        if (!changed) return true;
        if (restore && closes && occupied(level, target)) return false;
        if (!DoorStateUpdates.commit(level, positions, before, after)) return false;
        for (int i = 0; i < positions.length; i++) {
            if (before[i] != after[i]) after[i].updateNeighbourShapes(level, positions[i], Block.UPDATE_CLIENTS, 512);
            if (before[i].getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER || before[i].getValue(DoorBlock.OPEN) == after[i].getValue(DoorBlock.OPEN)) continue;
            boolean opening = after[i].getValue(DoorBlock.OPEN);
            level.playSound(null, positions[i], opening ? door.type().doorOpen() : door.type().doorClose(), SoundSource.BLOCKS,
                    1, level.random.nextFloat() * .1F + .9F);
            level.gameEvent(null, opening ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, positions[i]);
        }
        return true;
    }

    private static BlockState restoredVanillaState(BlockState state, DoorBlock door, boolean power) {
        boolean desired = power || !state.getValue(DoorBlock.POWERED) && door.type().canOpenByHand() && state.getValue(DoorBlock.OPEN);
        return state.setValue(DoorBlock.POWERED, power).setValue(DoorBlock.OPEN, desired);
    }
}
