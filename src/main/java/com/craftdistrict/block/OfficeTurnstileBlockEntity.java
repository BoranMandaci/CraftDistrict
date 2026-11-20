package com.craftdistrict.block;

import com.craftdistrict.registry.CraftDistrictDoors;
import net.minecraft.core.BlockPos;
//? if >=1.21 {
import net.minecraft.core.HolderLookup;
//?}
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** A push advances the first 60 degrees. Crossing the lane centre commits the second 60 degrees. */
public final class OfficeTurnstileBlockEntity extends BlockEntity {
    private static final int HALF_STEP_TICKS = 6, PASSAGE_TIMEOUT = 30;
    private static final int STEP_ANGLE = 120, HALF_STEP_ANGLE = STEP_ANGLE / 2;
    private int detent, from, to;
    private int visitor = -1;
    private long started;
    private boolean entering, crossed;
    private float visualFrom, visualStartPartial, lastRenderPartial;
    private int visualTo;
    private long visualStarted, lastRenderTick = Long.MIN_VALUE;
    private AABB renderBounds;

    public OfficeTurnstileBlockEntity(BlockPos pos, BlockState state) {
        super(CraftDistrictDoors.OFFICE_TURNSTILE.get(), pos, state);
    }

    public boolean busy(long tick) {
        return entering || from != to && (tick - started < HALF_STEP_TICKS
                || level != null && level.isClientSide && visualElapsed(tick, 0) < HALF_STEP_TICKS);
    }

    boolean allowsPassage(Player player, long tick) {
        if (player.getId() != visitor) return false;
        int limit = entering ? PASSAGE_TIMEOUT : HALF_STEP_TICKS;
        if (level != null && level.isClientSide) {
            // Open on the first movement tick after the locally rendered phase starts.
            double elapsed = visualElapsed(tick, 0);
            return elapsed > 0 && elapsed < limit;
        }
        long elapsed = tick - started;
        return elapsed >= 0 && elapsed < limit;
    }

    public void tryStart(ServerLevel level, Player player, int direction) {
        long tick = level.getGameTime();
        if (busy(tick)) return;
        if (visitor != -1 && level.getEntity(visitor) instanceof Player previous && OfficeTurnstileBlock.canPush(previous)
                && OfficeTurnstileBlock.overlapsBar(getBlockState(), worldPosition, previous)) return;

        from = detent * STEP_ANGLE;
        to = from + direction * HALF_STEP_ANGLE;
        started = tick;
        visitor = player.getId();
        entering = true;
        crossed = false;
        sendMotion(level);
        level.scheduleTick(worldPosition, getBlockState().getBlock(), HALF_STEP_TICKS);
    }

    void observePassage(Player player, double depth) {
        if (entering && player.getId() == visitor && depth * (to - from) > 0) crossed = true;
    }

    void finishOrWait(ServerLevel level) {
        if (!entering) return;
        long tick = level.getGameTime();
        long elapsed = tick - started;
        if (elapsed < HALF_STEP_TICKS || !crossed && elapsed < PASSAGE_TIMEOUT && visitorIsAtLane(level)) {
            // Check only while a player has reserved the lane. No idle block-entity ticker.
            level.scheduleTick(worldPosition, getBlockState().getBlock(), 2);
            return;
        }
        int halfStep = to - from;
        from = to;
        started = tick;
        entering = false;
        if (crossed) {
            to += halfStep;
            detent = Math.floorMod(to / STEP_ANGLE, 3);
            setChanged();
        } else {
            to -= halfStep;
            visitor = -1;
        }
        sendMotion(level);
    }

    private boolean visitorIsAtLane(ServerLevel level) {
        return level.getEntity(visitor) instanceof Player player && OfficeTurnstileBlock.canPush(player)
                && OfficeTurnstileBlock.nearLane(getBlockState(), worldPosition, player)
                && Math.abs(OfficeTurnstileBlock.depth(getBlockState(), worldPosition, player)) < .75;
    }

    public float angle(long tick, float partialTick) {
        lastRenderTick = tick;
        lastRenderPartial = partialTick;
        return interpolate(visualFrom, visualTo, visualElapsed(tick, partialTick)) % STEP_ANGLE;
    }

    private double visualElapsed(long tick, float partialTick) {
        return (tick - visualStarted) + (double) partialTick - visualStartPartial;
    }

    private static float interpolate(float from, int to, double elapsed) {
        if (from == to || elapsed >= HALF_STEP_TICKS) return to;
        float t = (float) Mth.clamp(elapsed / HALF_STEP_TICKS, 0, 1);
        return Mth.lerp(t * t * (3 - 2 * t), from, to);
    }

    private void sendMotion(ServerLevel level) {
        var players = level.getChunkSource().chunkMap.getPlayers(new ChunkPos(worldPosition), false);
        if (players.isEmpty()) return;
        var packet = getUpdatePacket();
        for (var player : players) player.connection.send(packet);
    }

    // Persist the last completed detent. Reservations and entity ids belong to the live session.
    private void readMotion(CompoundTag tag) {
        detent = Math.floorMod(tag.getInt("Detent"), 3);
        boolean live = tag.contains("Started");
        int nextFrom = live ? tag.getInt("From") : detent * STEP_ANGLE;
        int nextTo = live ? tag.getInt("To") : nextFrom;
        long nextStarted = live ? tag.getLong("Started") : 0;
        if (live && level != null && level.isClientSide && lastRenderTick != Long.MIN_VALUE) {
            if (from != nextFrom || to != nextTo || started != nextStarted) {
                long tick = level.getGameTime();
                float partial = lastRenderTick == tick ? lastRenderPartial : 0;
                // Match the prior endpoint to the next phase's copy of the same 120-degree detent.
                int turnShift = STEP_ANGLE * Math.round((nextFrom - to) / (float) STEP_ANGLE);
                visualFrom = interpolate(visualFrom, visualTo, visualElapsed(tick, partial)) + turnShift;
                visualTo = nextTo;
                visualStarted = tick;
                visualStartPartial = partial;
            }
        } else {
            visualFrom = nextFrom;
            visualTo = nextTo;
            visualStarted = nextStarted;
            visualStartPartial = 0;
        }
        from = nextFrom;
        to = nextTo;
        started = nextStarted;
        visitor = live ? tag.getInt("Visitor") : -1;
        entering = live && tag.getBoolean("Entering");
        crossed = false;
    }

    //? if >=1.21 {
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.putInt("Detent", detent);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        readMotion(tag);
    }
    //?} else {
    /*@Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Detent", detent);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        readMotion(tag);
    }
    *///?}

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override public CompoundTag getUpdateTag(
            //? if >=1.21 {
            HolderLookup.Provider lookup
            //?}
    ) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Detent", detent);
        tag.putInt("From", from);
        tag.putInt("To", to);
        tag.putLong("Started", started);
        tag.putInt("Visitor", visitor);
        tag.putBoolean("Entering", entering);
        return tag;
    }

    public AABB getRenderBoundingBox() {
        if (renderBounds == null) renderBounds = new AABB(worldPosition);
        return renderBounds;
    }
}
