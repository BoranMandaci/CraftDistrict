package com.craftdistrict.block;

import com.craftdistrict.registry.CraftDistrictDoors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.phys.shapes.VoxelShape;

public final class RotaryEntranceBlockEntity extends BlockEntity {
    private static final int REVOLUTION_TICKS = 200;
    public static final float DEGREES_PER_TICK = 360F / REVOLUTION_TICKS;
    private static final int RAMP_TICKS = 20;
    private static final int BLEND_TICKS = 8;
    private static final double PARK_CLEARANCE = 22.3 / 16;
    private Motion motion;
    private boolean parkPending;
    private VoxelShape[] parkedShapes;
    private Direction parkedFacing;
    private long lastRenderTick = Long.MIN_VALUE;
    private float lastRenderPartial;
    private long blendStart;
    private float blendPartial, blendAngle;
    private AABB bounds;

    /** Integrate a smooth speed ramp directly from the saved endpoints. */
    private record Motion(long start, float angle, float speed, boolean running) {
        private float targetSpeed() { return running ? DEGREES_PER_TICK : 0; }

        private boolean settled(long time) { return time - start >= RAMP_TICKS; }

        private float angleAt(long time, float partialTick) {
            long elapsed = time - start;
            float target = targetSpeed();
            if (elapsed >= RAMP_TICKS) {
                float endAngle = angle + RAMP_TICKS * (speed + target) * .5F;
                return running ? endAngle + revolvingAngle(elapsed - RAMP_TICKS, partialTick) : endAngle;
            }
            float t = Mth.clamp((elapsed + partialTick) / RAMP_TICKS, 0F, 1F);
            return angle + RAMP_TICKS * t * (speed + (target - speed) * t * t * (1F - .5F * t));
        }

        private float speedAt(long time) {
            return Mth.lerp(smoothStep((time - start) / (float) RAMP_TICKS), speed, targetSpeed());
        }
    }

    public RotaryEntranceBlockEntity(BlockPos pos, BlockState state) {
        super(CraftDistrictDoors.ROTARY.get(), pos, state);
    }

    public static float revolvingAngle(long gameTime, float partialTick) {
        return (Math.floorMod(gameTime, REVOLUTION_TICKS) + partialTick) * DEGREES_PER_TICK;
    }

    private static float smoothStep(float progress) {
        float t = Mth.clamp(progress, 0F, 1F);
        return t * t * (3F - 2F * t);
    }

    private float angleAt(long time, float partialTick) {
        return motion == null ? revolvingAngle(time, partialTick) : motion.angleAt(time, partialTick);
    }

    public float angle(long time, float partialTick) {
        float angle = angleAt(time, partialTick);
        if (blendAngle == 0 || time - blendStart > BLEND_TICKS) return angle;
        float progress = ((time - blendStart) + partialTick - blendPartial) / BLEND_TICKS;
        return angle + blendAngle * (1F - smoothStep(progress));
    }

    public float renderAngle(long time, float partialTick) {
        lastRenderTick = time;
        lastRenderPartial = partialTick;
        return angle(time, partialTick);
    }

    public float step(long time) {
        if (motion == null) return DEGREES_PER_TICK;
        if (motion.settled(time) && (blendAngle == 0 || time - blendStart > BLEND_TICKS)) return motion.targetSpeed();
        return Mth.wrapDegrees(angle(time + 1, 0) - angle(time, 0));
    }

    public boolean hasParkedCollision(long time) {
        return motion != null && !motion.running() && !parkPending && motion.settled(time)
                && (blendAngle == 0 || time - blendStart > BLEND_TICKS);
    }

    VoxelShape parkedShape(int part, Direction facing) {
        if (parkedShapes == null || parkedFacing != facing) {
            parkedShapes = RotaryEntranceShapes.parked(motion.angleAt(motion.start() + RAMP_TICKS, 0), facing);
            parkedFacing = facing;
        }
        return parkedShapes[part];
    }

    public void toggle(ServerLevel server) {
        long now = server.getGameTime();
        boolean running = motion != null && !motion.running();
        beginMotion(now, running);
        parkPending = !running;
        if (parkPending) server.scheduleTick(worldPosition, getBlockState().getBlock(), RAMP_TICKS);
        publishMotion(server);
    }

    private void beginMotion(long now, boolean running) {
        motion = new Motion(now, Mth.positiveModulo(angleAt(now, 0), 360F),
                motion == null ? DEGREES_PER_TICK : motion.speedAt(now), running);
        parkedShapes = null;
    }

    void finishParking(ServerLevel server) {
        if (!parkPending) return;
        long now = server.getGameTime();
        if (!motion.settled(now)) {
            // A quick restart and stop can leave the earlier scheduled check in place.
            int remaining = (int) (motion.start() + RAMP_TICKS - now);
            server.scheduleTick(worldPosition, getBlockState().getBlock(), remaining);
            return;
        }
        parkPending = false;
        // At some angles a compartment has no exit. Resume from rest if the chamber is occupied.
        if (occupied(server)) beginMotion(now, true);
        publishMotion(server);
    }

    private boolean occupied(ServerLevel server) {
        double x = worldPosition.getX() + .5, z = worldPosition.getZ() + .5;
        double y = worldPosition.getY() - 1;
        AABB chamber = new AABB(x - PARK_CLEARANCE, y + 1.55 / 16, z - PARK_CLEARANCE,
                x + PARK_CLEARANCE, y + 42.55 / 16, z + PARK_CLEARANCE);
        return !server.getEntitiesOfClass(Player.class, chamber, player -> {
            AABB body = player.getBoundingBox();
            double dx = Mth.clamp(x, body.minX, body.maxX) - x, dz = Mth.clamp(z, body.minZ, body.maxZ) - z;
            return player.isAlive() && !player.isSpectator() && dx * dx + dz * dz < PARK_CLEARANCE * PARK_CLEARANCE;
        }).isEmpty();
    }

    private void publishMotion(ServerLevel server) {
        setChanged();
        var players = server.getChunkSource().chunkMap.getPlayers(new ChunkPos(worldPosition), false);
        if (players.isEmpty()) return;
        var packet = getUpdatePacket();
        for (var player : players) player.connection.send(packet);
    }

    private void writeMotion(CompoundTag tag) {
        if (motion == null) return;
        tag.putLong("MotionStart", motion.start());
        tag.putFloat("MotionAngle", motion.angle());
        tag.putFloat("MotionSpeed", motion.speed());
        tag.putBoolean("Running", motion.running());
        if (parkPending) tag.putBoolean("ParkPending", true);
    }

    private void readMotion(CompoundTag tag) {
        Motion next = null;
        if (tag.contains("MotionStart")) {
            float angle = tag.getFloat("MotionAngle"), speed = tag.getFloat("MotionSpeed");
            if (Float.isFinite(angle) && Float.isFinite(speed)) {
                next = new Motion(tag.getLong("MotionStart"), Mth.positiveModulo(angle, 360F),
                        Mth.clamp(speed, 0F, DEGREES_PER_TICK), tag.getBoolean("Running"));
            }
        }
        parkPending = next != null && !next.running() && tag.getBoolean("ParkPending");
        if (java.util.Objects.equals(motion, next)) return;
        parkedShapes = null;
        if (level != null && level.isClientSide && lastRenderTick != Long.MIN_VALUE) {
            long now = level.getGameTime();
            float partial = lastRenderTick == now ? lastRenderPartial : 0;
            // Reconcile late packets from the displayed pose rather than jumping to the server angle.
            float previous = angle(now, partial);
            motion = next;
            blendAngle = Mth.wrapDegrees(previous - angleAt(now, partial));
            blendStart = now;
            blendPartial = partial;
        } else {
            motion = next;
            blendAngle = 0;
        }
    }

    //? if >=1.21 {
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        writeMotion(tag);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        readMotion(tag);
    }
    //?} else {
    /*@Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeMotion(tag);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        readMotion(tag);
    }
    *///?}

    @Override public CompoundTag getUpdateTag(
            //? if >=1.21 {
            HolderLookup.Provider lookup
            //?}
    ) {
        CompoundTag tag = new CompoundTag();
        writeMotion(tag);
        return tag;
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // The controller occupies the middle cell of the 3x3x3 door.
    public AABB getRenderBoundingBox() {
        if (bounds == null) bounds = new AABB(worldPosition).inflate(1);
        return bounds;
    }
}
