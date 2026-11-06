package com.craftdistrict.entity;

import com.craftdistrict.block.LyingBlock;
import com.craftdistrict.block.MultiBlockDecorationBlock;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Local, on-use occupancy checks; vanilla synchronizes the sleeping pose and wake action. */
public final class MedicalResting {
    private MedicalResting() { }

    public static void init() {
        PlayerEvent.PLAYER_QUIT.register(player -> { if (isResting(player)) player.stopSleepInBed(true, false); });
        PlayerEvent.PLAYER_JOIN.register(player -> {
            if (isResting(player)) { player.stopSleepInBed(true, false); return; }
            // Recover old examination-table riders whose transient seat was not saved.
            if (player.isPassenger() || player.isSleeping()) return;
            for (BlockPos pos : new BlockPos[]{player.blockPosition(), player.blockPosition().above()}) {
                BlockState state = player.level().getBlockState(pos);
                if (state.getBlock() instanceof LyingBlock && !player.level().noCollision(player, player.getBoundingBox())) {
                    Vec3 exit = SeatEntity.safeExit(player.level(), pos, state.getValue(BlockStateProperties.HORIZONTAL_FACING), player);
                    if (exit != null) player.connection.teleport(exit.x, exit.y, exit.z, player.getYRot(), player.getXRot());
                    break;
                }
            }
        });
    }

    public static boolean valid(LevelReader level, BlockPos head) {
        BlockState state = level.getBlockState(head);
        if (!(state.getBlock() instanceof LyingBlock lying)
                || !(state.getBlock() instanceof MultiBlockDecorationBlock multi)
                || !lying.lyingHead(state, head).equals(head)) return false;
        var parts = multi.structureParts(state, head);
        for (int i = 0; i < parts.size(); i++) {
            if (!level.hasChunkAt(parts.get(i)) || level.getBlockState(parts.get(i)) != multi.structurePartState(state, i)) return false;
        }
        return true;
    }

    public static boolean isResting(LivingEntity entity) {
        return entity.getSleepingPos().map(pos -> entity.level().getBlockState(pos).getBlock() instanceof LyingBlock).orElse(false);
    }

    public static InteractionResult lie(BlockState state, Level level, BlockPos pos, Player player) {
        if (!(state.getBlock() instanceof LyingBlock lying) || !player.isAlive() || player.isSpectator()
                || player.isShiftKeyDown() || player.isPassenger() || player.isSleeping() || player.isFallFlying()
                || !player.getMainHandItem().isEmpty() || player.distanceToSqr(Vec3.atCenterOf(pos)) > 16) return InteractionResult.PASS;
        BlockPos head = lying.lyingHead(state, pos);
        if (!valid(level, head)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        for (ServerPlayer other : server.players()) {
            if (other.isSleeping() && other.getSleepingPos().filter(head::equals).isPresent()) {
                player.displayClientMessage(Component.translatable("block.minecraft.bed.occupied"), true);
                return InteractionResult.CONSUME;
            }
        }
        Direction facing = lying.lyingDirection(state);
        // Check free air above the furniture, rather than the broad decorative mattress hitbox.
        AABB clearance = lying.lyingClearance(state, head);
        if (!level.noCollision(player, clearance) || SeatEntity.safeExit(level, lying.lyingExitAnchor(state, head),
                state.getValue(BlockStateProperties.HORIZONTAL_FACING), player) == null) return InteractionResult.FAIL;
        serverPlayer.startSleeping(head);
        player.fallDistance = 0;
        serverPlayer.connection.teleport(player.getX(), player.getY(), player.getZ(), facing.toYRot(), 0);
        return InteractionResult.CONSUME;
    }

    /** Wake occupants before linked-part removal, while safe exit geometry still exists. */
    public static void removeAt(Level level, BlockPos head) {
        if (!(level instanceof ServerLevel server)) return;
        for (ServerPlayer player : server.players()) {
            if (player.isSleeping() && player.getSleepingPos().filter(head::equals).isPresent()) player.stopSleepInBed(true, false);
        }
    }

    /** A scanner anchor may itself be the broken part, so retain its exit geometry explicitly. */
    public static void removeAt(Level level, BlockPos head, BlockState state) {
        if (!(level instanceof ServerLevel server) || !(state.getBlock() instanceof LyingBlock lying)) return;
        for (ServerPlayer player : server.players()) {
            if (player.isSleeping() && player.getSleepingPos().filter(head::equals).isPresent()) {
                Vec3 exit = SeatEntity.safeExit(level, lying.lyingExitAnchor(state, head),
                        state.getValue(BlockStateProperties.HORIZONTAL_FACING), player);
                player.stopSleepInBed(true, false);
                if (exit != null) player.connection.teleport(exit.x, exit.y, exit.z, player.getYRot(), player.getXRot());
            }
        }
    }

    public static void position(LivingEntity entity, BlockPos head) {
        BlockState state = entity.level().getBlockState(head);
        if (state.getBlock() instanceof LyingBlock lying)
            entity.setPos(head.getX() + .5, head.getY() + lying.lyingHeight(), head.getZ() + .5);
    }

    public static void wake(LivingEntity entity) {
        BlockPos head = entity.getSleepingPos().orElseThrow();
        BlockState state = entity.level().getBlockState(head);
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockPos exitAnchor = state.getBlock() instanceof LyingBlock lying ? lying.lyingExitAnchor(state, head) : head;
        Vec3 exit = SeatEntity.safeExit(entity.level(), exitAnchor, facing, entity);
        entity.setPose(Pose.STANDING);
        entity.clearSleepingPos();
        if (exit != null) entity.setPos(exit.x, exit.y, exit.z);
    }
}
