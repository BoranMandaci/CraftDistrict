package com.craftdistrict.client;

import com.craftdistrict.block.RotaryEntranceBlock;
import com.craftdistrict.block.RotaryEntranceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Walking and swept contact against three rotating wings over the stationary world collision. */
public final class RevolvingWingContact {
    private static final double WING_RADIUS = 20.35 / 16;
    private static final double HALF_THICKNESS = .45 / 16;
    private static final double TICK_ANGLE = Math.toRadians(RotaryEntranceBlockEntity.DEGREES_PER_TICK);
    private static final double TICK_SIN = Math.sin(TICK_ANGLE);
    private static final double TICK_COS = Math.cos(TICK_ANGLE);

    private RevolvingWingContact() { }

    public static Vec3 adjust(Player player, Vec3 movement, long gameTime, boolean advance) {
        if ((!advance && movement.horizontalDistanceSqr() < 1e-8) || player.isSpectator() || player.getAbilities().flying
                || player.isPassenger()) return movement;

        Level level = player.level();
        BlockPos pos = player.blockPosition();
        BlockState state = level.getBlockState(pos);
        if (!isRevolvingDoor(state)) {
            int x = Mth.floor(player.getX() + movement.x);
            int z = Mth.floor(player.getZ() + movement.z);
            if (x == pos.getX() && z == pos.getZ()) return movement;
            pos = new BlockPos(x, pos.getY(), z);
            state = level.getBlockState(pos);
            if (!isRevolvingDoor(state)) return movement;
        }

        RotaryEntranceBlock door = (RotaryEntranceBlock) state.getBlock();
        BlockPos controller = door.controllerPos(state, pos);
        int baseY = controller.getY() - 1;
        if (player.getY() - baseY >= 42.55 / 16 || player.getBoundingBox().maxY - baseY <= 1.55 / 16) return movement;

        // Once parked, vanilla resolves the cached wing shapes on both sides.
        var entrance = RotaryEntranceBlock.loadedController(level, controller);
        if (entrance == null || entrance.hasParkedCollision(gameTime)) return movement;
        float step = advance ? Math.max(0, entrance.step(gameTime)) : 0;
        if (step == 0 && movement.horizontalDistanceSqr() < 1e-8) return movement;

        Direction facing = state.getValue(RotaryEntranceBlock.FACING);
        double x = player.getX() - (controller.getX() + .5);
        double z = player.getZ() - (controller.getZ() + .5);
        float angle = entrance.angle(gameTime, 0) + 180 - facing.toYRot();
        return adjustAtAngle(x, z, player.getBbWidth() * .5, angle, movement, step);
    }

    private static boolean isRevolvingDoor(BlockState state) {
        return state.getBlock() instanceof RotaryEntranceBlock;
    }

    private static Vec3 adjustAtAngle(double x, double z, double bodyRadius, float angle, Vec3 movement, float step) {
        double moveX = movement.x, moveZ = movement.z;
        for (int blade = 0; blade < 3; blade++) {
            float radians = (angle + blade * 120) * Mth.DEG_TO_RAD;
            double axisX = Mth.cos(radians), axisZ = -Mth.sin(radians);
            double normalX = -axisZ, normalZ = axisX;
            double distance = x * normalX + z * normalZ;
            double side = Math.signum(distance);
            double inward = -(moveX * normalX + moveZ * normalZ) * side;
            if (inward <= 0 || Math.abs(distance) < 1e-5) continue;

            double clearance = Math.max(0, Math.abs(distance) - bodyRadius - HALF_THICKNESS);
            if (inward <= clearance) continue;
            double along = x * axisX + z * axisZ + (moveX * axisX + moveZ * axisZ) * (clearance / inward);
            if (along < 0 || along >= WING_RADIUS + bodyRadius) continue;
            if (along > WING_RADIUS) {
                double pastTip = along - WING_RADIUS;
                clearance = Math.max(0, Math.abs(distance) - Math.sqrt(Math.max(0, bodyRadius * bodyRadius - pastTip * pastTip))
                        - HALF_THICKNESS);
            }

            // Preserve walking along the panel and away from it.
            double correction = Math.max(0, inward - clearance) * side;
            moveX += correction * normalX;
            moveZ += correction * normalZ;
        }
        if (step > 0) {
            double endX = x + moveX, endZ = z + moveZ;
            double turn = sweptPush(x, z, endX, endZ, bodyRadius, angle, step);
            if (turn > 0) {
                // Follow the player's own radius to avoid pushing them outward against the enclosure.
                double cos = Math.cos(turn), sin = Math.sin(turn);
                moveX = endX * cos + endZ * sin - x;
                moveZ = endZ * cos - endX * sin - z;
            }
        }
        return moveX == movement.x && moveZ == movement.z ? movement : new Vec3(moveX, movement.y, moveZ);
    }

    private static double sweptPush(double x, double z, double endX, double endZ, double bodyRadius, float angle, float step) {
        double radiansPerTick = Math.toRadians(step);
        boolean steady = step == RotaryEntranceBlockEntity.DEGREES_PER_TICK;
        double sweepSin = steady ? TICK_SIN : Math.sin(radiansPerTick);
        double sweepCos = steady ? TICK_COS : Math.cos(radiansPerTick);
        double turn = 0;
        for (int blade = 0; blade < 3; blade++) {
            float radians = (angle + blade * 120) * Mth.DEG_TO_RAD;
            double axisX = Mth.cos(radians), axisZ = -Mth.sin(radians);
            double along = x * axisX + z * axisZ;
            double leading = x * axisZ - z * axisX;
            // Only the advancing face carries the player. Leave an escape off the trailing face.
            if (leading < -Math.max(0, along) * sweepSin) continue;

            double nextX = axisX * sweepCos + axisZ * sweepSin;
            double nextZ = axisZ * sweepCos - axisX * sweepSin;
            double nextAlong = endX * nextX + endZ * nextZ;
            if (nextAlong < 0 || nextAlong >= WING_RADIUS + bodyRadius) continue;
            double pastTip = Math.max(0, nextAlong - WING_RADIUS);
            double clearance = HALF_THICKNESS + Math.sqrt(Math.max(0, bodyRadius * bodyRadius - pastTip * pastTip));
            double overlap = clearance - (endX * nextZ - endZ * nextX);
            turn = Math.max(turn, overlap / Math.max(.2, nextAlong));
        }
        return Math.min(turn, radiansPerTick * 1.4);
    }
}
