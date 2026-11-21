package com.craftdistrict.access;

import com.craftdistrict.block.HotelElevatorDoorsBlock;
import com.craftdistrict.block.ModularMovingDoorBlock;
import com.craftdistrict.block.MovingDoorGroups;
import com.craftdistrict.block.MultiBlockDecorationBlock;
import com.craftdistrict.block.PairedDoorBlock;
import com.craftdistrict.block.SecretBookshelfDoorBlock;
import com.craftdistrict.block.VaultDoorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** An entrance's persistent frame cells, independent of its open, powered or sensor state. */
public record AccessDoorTarget(BlockPos root, ResourceLocation blockId, Direction facing, List<BlockPos> parts) {
    private static final int MAX_PARTS = 256, MAX_SPAN = 16;
    private static final Comparator<BlockPos> PART_ORDER = Comparator.comparingLong(BlockPos::asLong);

    public AccessDoorTarget {
        root = root.immutable();
        parts = parts.stream().map(BlockPos::immutable).sorted(PART_ORDER).toList();
    }

    public record Resolution(AccessDoorTarget target, BlockPos missing) {
        public boolean unloaded() { return missing != null; }
    }

    public boolean matches(BlockState state) {
        return state != null && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                && state.getValue(BlockStateProperties.HORIZONTAL_FACING) == facing
                && blockId.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }

    public static boolean supports(Block block) {
        return block instanceof PairedDoorBlock || block instanceof VaultDoorBlock || block instanceof SecretBookshelfDoorBlock
                || block instanceof HotelElevatorDoorsBlock || block instanceof ModularMovingDoorBlock moving && !moving.garage()
                || block instanceof DoorBlock && "minecraft".equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace());
    }

    public static BlockState loadedState(ServerLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) return Blocks.AIR.defaultBlockState();
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockState(pos);
    }

    public static Resolution resolve(ServerLevel level, BlockPos clicked) {
        return resolve(clicked, pos -> loadedState(level, pos), pos -> MovingDoorGroups.entranceAnchors(level, pos));
    }

    static Resolution resolve(BlockPos clicked, Function<BlockPos, BlockState> states, Function<BlockPos, List<BlockPos>> openAnchors) {
        Lookup lookup = new Lookup(states, openAnchors);
        BlockState state = lookup.at(clicked);
        AccessDoorTarget target = null;
        if (state != null && supports(state.getBlock())) {
            if (state.getBlock() instanceof DoorBlock) target = hinged(lookup, clicked, state);
            else if (state.getBlock() instanceof ModularMovingDoorBlock) target = industrial(lookup, clicked, state);
            else if (state.getBlock() instanceof MultiBlockDecorationBlock door) target = multiblock(lookup, clicked, state, door);
        }
        return new Resolution(target, lookup.missing);
    }

    private static AccessDoorTarget hinged(Lookup lookup, BlockPos clicked, BlockState state) {
        BlockPos root = state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? clicked.below() : clicked;
        BlockState lower = lookup.at(root);
        if (lower == null || !lower.is(state.getBlock()) || !completeLeaf(lookup, root, lower)) return null;
        Direction facing = lower.getValue(DoorBlock.FACING);
        boolean left = lower.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT;
        boolean builtInPair = lower.hasProperty(PairedDoorBlock.PAIRED);
        if (builtInPair && !lower.getValue(PairedDoorBlock.PAIRED)) return target(root, lower, List.of(root, root.above()));

        BlockPos other = root.relative(left ? facing.getClockWise() : facing.getCounterClockWise());
        BlockState peer = lookup.at(other);
        if (peer != null && peer.is(lower.getBlock()) && peer.getValue(DoorBlock.FACING) == facing
                && peer.getValue(DoorBlock.HINGE) != lower.getValue(DoorBlock.HINGE)
                && (!builtInPair || peer.getValue(PairedDoorBlock.PAIRED)) && completeLeaf(lookup, other, peer))
            return target(left ? root : other, lower, List.of(root, root.above(), other, other.above()));
        return builtInPair || lookup.missing != null ? null : target(root, lower, List.of(root, root.above()));
    }

    private static boolean completeLeaf(Lookup lookup, BlockPos root, BlockState lower) {
        if (lower.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) return false;
        BlockState upper = lookup.at(root.above());
        return upper != null && upper.is(lower.getBlock()) && upper.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER
                && upper.getValue(DoorBlock.FACING) == lower.getValue(DoorBlock.FACING)
                && upper.getValue(DoorBlock.HINGE) == lower.getValue(DoorBlock.HINGE);
    }

    private static AccessDoorTarget multiblock(Lookup lookup, BlockPos clicked, BlockState state, MultiBlockDecorationBlock door) {
        BlockPos root = door.structureOrigin(state, clicked);
        BlockState primary = lookup.at(root);
        if (primary == null || !primary.is(door) || !door.structureOrigin(primary, root).equals(root)) return null;
        List<BlockPos> positions = door.structureParts(primary, root);
        if (!positions.contains(clicked)) return null;
        for (int index = 0; index < positions.size(); index++) {
            if (lookup.at(positions.get(index)) != door.structurePartState(primary, index)) return null;
        }
        // The vault's swing occupies extra cells only while open. Its frame keeps the same identity.
        if (door instanceof VaultDoorBlock && primary.getValue(VaultDoorBlock.LARGE)) positions = positions.subList(0, 9);
        return target(root, primary, positions);
    }

    private static AccessDoorTarget industrial(Lookup lookup, BlockPos clicked, BlockState start) {
        Direction right = start.getValue(ModularMovingDoorBlock.FACING).getClockWise();
        boolean open = start.getValue(ModularMovingDoorBlock.OPEN);
        List<BlockPos> parts;
        if (open) {
            parts = lookup.openAnchors.apply(clicked);
            for (BlockPos pos : parts) {
                BlockState state = lookup.at(pos);
                if (!industrialMatch(start, state, true)) return null;
            }
        } else {
            parts = new ArrayList<>();
            Set<BlockPos> visited = new HashSet<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            Direction[] edges = { right, right.getOpposite(), Direction.UP, Direction.DOWN };
            visited.add(clicked);
            queue.add(clicked);
            int minY = clicked.getY(), maxY = minY, minSide = lateral(clicked, right), maxSide = minSide;
            while (!queue.isEmpty()) {
                BlockPos pos = queue.removeFirst();
                BlockState state = lookup.at(pos);
                if (state == null) return null;
                if (!industrialMatch(start, state, false)) continue;
                int side = lateral(pos, right);
                minY = Math.min(minY, pos.getY()); maxY = Math.max(maxY, pos.getY());
                minSide = Math.min(minSide, side); maxSide = Math.max(maxSide, side);
                if (parts.size() >= MAX_PARTS || maxY - minY >= MAX_SPAN || maxSide - minSide >= MAX_SPAN) return null;
                parts.add(pos);
                for (Direction edge : edges) {
                    BlockPos next = pos.relative(edge);
                    if (visited.add(next)) queue.add(next);
                }
            }
        }
        if (parts.isEmpty()) return null;
        BlockPos root = parts.stream().min(Comparator.comparingInt((BlockPos pos) -> pos.getY())
                .thenComparingInt(pos -> lateral(pos, right))).orElseThrow();
        return target(root, start, parts);
    }

    private static boolean industrialMatch(BlockState start, BlockState state, boolean open) {
        return state != null && state.is(start.getBlock()) && state.getValue(ModularMovingDoorBlock.FACING) == start.getValue(ModularMovingDoorBlock.FACING)
                && state.getValue(ModularMovingDoorBlock.OPEN) == open;
    }

    private static int lateral(BlockPos pos, Direction right) { return pos.getX() * right.getStepX() + pos.getZ() * right.getStepZ(); }

    private static AccessDoorTarget target(BlockPos root, BlockState state, List<BlockPos> parts) {
        return new AccessDoorTarget(root, BuiltInRegistries.BLOCK.getKey(state.getBlock()), state.getValue(BlockStateProperties.HORIZONTAL_FACING), parts);
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("root", root.asLong());
        tag.putString("block", blockId.toString());
        tag.putString("facing", facing.getName());
        tag.putLongArray("parts", parts.stream().mapToLong(BlockPos::asLong).toArray());
        return tag;
    }

    static AccessDoorTarget load(CompoundTag tag) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("block"));
        Direction facing = Direction.byName(tag.getString("facing"));
        long[] cells = tag.getLongArray("parts");
        if (id == null || facing == null || !facing.getAxis().isHorizontal() || !tag.contains("root", Tag.TAG_LONG)
                || cells.length == 0 || cells.length > MAX_PARTS) return null;
        BlockPos root = BlockPos.of(tag.getLong("root"));
        List<BlockPos> parts = new ArrayList<>(cells.length);
        Set<BlockPos> visited = new HashSet<>();
        for (long cell : cells) {
            BlockPos pos = BlockPos.of(cell);
            if (!visited.add(pos) || pos.distManhattan(root) > 2 * MAX_SPAN) return null;
            parts.add(pos);
        }
        return visited.contains(root) ? new AccessDoorTarget(root, id, facing, parts) : null;
    }

    private static final class Lookup {
        private final Function<BlockPos, BlockState> states;
        private final Function<BlockPos, List<BlockPos>> openAnchors;
        private BlockPos missing;

        private Lookup(Function<BlockPos, BlockState> states, Function<BlockPos, List<BlockPos>> openAnchors) {
            this.states = states;
            this.openAnchors = openAnchors;
        }

        private BlockState at(BlockPos pos) {
            BlockState state = states.apply(pos);
            if (state == null && missing == null) missing = pos.immutable();
            return state;
        }
    }
}
