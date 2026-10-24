package com.craftdistrict.entity;

import com.craftdistrict.block.SittableBlock;
import com.craftdistrict.registry.CraftDistrictEntities;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** No world/player maps: occupancy is a local entity query only on use/removal. */
public final class Seating {
    private Seating() { }

    public static void init() {
        PlayerEvent.PLAYER_QUIT.register(player -> { if(player.getVehicle() instanceof SeatEntity) player.stopRiding(); });
        PlayerEvent.PLAYER_JOIN.register(player -> {
            if(player.isPassenger()) return;
            // A hard stop can save a rider's low foot position without its transient vehicle.
            // Recover only players intersecting our seating; no persistent marker/world map is needed.
            for(BlockPos pos:new BlockPos[]{player.blockPosition(),player.blockPosition().above()}) {
                BlockState state=player.level().getBlockState(pos);
                if(!(state.getBlock() instanceof SittableBlock block) || !block.isSittable(state)
                        || player.level().noCollision(player,player.getBoundingBox())) continue;
                Vec3 exit=SeatEntity.safeExit(player.level(),pos,state.getValue(BlockStateProperties.HORIZONTAL_FACING),player);
                if(exit!=null) {
                    player.setPos(exit.x,exit.y,exit.z);
                    player.connection.teleport(exit.x,exit.y,exit.z,player.getYRot(),player.getXRot());
                }
                break;
            }
        });
        LifecycleEvent.SERVER_STOPPING.register(server -> {
            for(var player:server.getPlayerList().getPlayers()) if(player.getVehicle() instanceof SeatEntity) player.stopRiding();
        });
    }

    private static AABB bounds(BlockPos pos) { return new AABB(pos).inflate(.1,1,.1); }

    public static InteractionResult sit(BlockState state, Level level, BlockPos pos, Player player) {
        if(!(state.getBlock() instanceof SittableBlock block) || !block.isSittable(state) || !player.isAlive() || player.isSpectator()
                || player.isShiftKeyDown() || player.isPassenger() || player.isSleeping() || player.isFallFlying() || !player.getMainHandItem().isEmpty()
                || player.distanceToSqr(Vec3.atCenterOf(pos))>16) return InteractionResult.PASS;
        if(level.isClientSide) return InteractionResult.SUCCESS;
        if(!(level instanceof ServerLevel server) || !level.getBlockState(pos).is(state.getBlock())) return InteractionResult.PASS;
        for(SeatEntity seat:server.getEntitiesOfClass(SeatEntity.class,bounds(pos),e->e.anchor().equals(pos))) {
            if(seat.isVehicle()) return InteractionResult.CONSUME;
            seat.discard();
        }
        Direction facing=state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        double x=pos.getX()+.5+facing.getStepX()*block.forwardOffset();
        double z=pos.getZ()+.5+facing.getStepZ()*block.forwardOffset();
        double top=pos.getY()+block.seatHeight();
        AABB headroom=new AABB(x-.25,top+.0625,z-.25,x+.25,top+1.125,z+.25);
        if(!level.noCollision(player,headroom) || SeatEntity.safeExit(level,pos,facing,player)==null) return InteractionResult.FAIL;
        SeatEntity seat=CraftDistrictEntities.SEAT.get().create(server);
        if(seat==null) return InteractionResult.FAIL;
        seat.configure(pos,state.getBlock(),facing,x,top-.75,z);
        if(!server.addFreshEntity(seat)) return InteractionResult.FAIL;
        float oldYaw=player.getYRot(),oldHead=player.getYHeadRot(),oldBody=player.yBodyRot;
        player.setYRot(facing.toYRot()); player.setYHeadRot(facing.toYRot()); player.setYBodyRot(facing.toYRot());
        if(!player.startRiding(seat)) {
            seat.discard(); player.setYRot(oldYaw); player.setYHeadRot(oldHead); player.setYBodyRot(oldBody);
            return InteractionResult.FAIL;
        }
        player.fallDistance=0;
        seat.positionRider(player);
        return InteractionResult.CONSUME;
    }

    public static void removeAt(Level level, BlockPos pos) {
        if(!(level instanceof ServerLevel server)) return;
        for(SeatEntity seat:server.getEntitiesOfClass(SeatEntity.class,bounds(pos),e->e.anchor().equals(pos))) {
            seat.ejectPassengers(); seat.discard();
        }
    }
}
