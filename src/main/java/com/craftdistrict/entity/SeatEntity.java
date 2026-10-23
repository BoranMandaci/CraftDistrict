package com.craftdistrict.entity;

import com.craftdistrict.block.RestaurantSeatBlock;
import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
//? if >=1.21 {
import net.minecraft.server.level.ServerEntity;
//?}
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/** One transient, stationary passenger anchor per occupied segment; no AI or saved data. */
public final class SeatEntity extends Entity {
    private static final EntityDataAccessor<BlockPos> ANCHOR=SynchedEntityData.defineId(SeatEntity.class,EntityDataSerializers.BLOCK_POS);
    private RestaurantSeatBlock anchorBlock;
    private Direction facing=Direction.NORTH;

    public SeatEntity(EntityType<? extends SeatEntity> type, Level level) {
        super(type,level); noPhysics=true; setNoGravity(true); setInvulnerable(true); setInvisible(true);
    }

    public void configure(BlockPos anchor, RestaurantSeatBlock block, Direction facing, double x, double y, double z) {
        entityData.set(ANCHOR,anchor.immutable()); anchorBlock=block; this.facing=facing;
        setPos(x,y,z); setYRot(facing.toYRot());
    }

    public BlockPos anchor() { return entityData.get(ANCHOR); }

    //? if >=1.21 {
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(ANCHOR,BlockPos.ZERO); }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity tracked) {
        return NetworkManager.createAddEntityPacket(this,tracked);
    }
    //?} else {
    /*@Override protected void defineSynchedData() { entityData.define(ANCHOR,BlockPos.ZERO); }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkManager.createAddEntityPacket(this); }
    *///?}

    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean canCollideWith(Entity other) { return false; }
    @Override protected boolean canAddPassenger(Entity passenger) { return passenger instanceof Player && !isVehicle(); }

    @Override protected void positionRider(Entity passenger, MoveFunction move) {
        if(hasPassenger(passenger)) move.accept(passenger,getX(),getY(),getZ());
    }

    @Override public void tick() {
        if(level().isClientSide) return;
        var state=level().getBlockState(anchor());
        Entity passenger=getFirstPassenger();
        if(anchorBlock==null || !state.is(anchorBlock) || state.getValue(BlockStateProperties.HORIZONTAL_FACING)!=facing
                || !(passenger instanceof Player player) || !player.isAlive() || player.isSpectator() || player.isRemoved()) {
            ejectPassengers(); discard();
        }
    }

    public static Vec3 safeExit(Level level, BlockPos pos, Direction facing, LivingEntity passenger) {
        for(Direction direction:new Direction[]{facing,facing.getClockWise(),facing.getCounterClockWise(),facing.getOpposite()}) {
            Vec3 exit=DismountHelper.findSafeDismountLocation(passenger.getType(),level,pos.relative(direction),true);
            if(exit!=null) return exit;
        }
        for(int radius=1;radius<=2;radius++) for(int x=-radius;x<=radius;x++) for(int z=-radius;z<=radius;z++) {
            if(Math.max(Math.abs(x),Math.abs(z))!=radius) continue;
            for(int y=0;y<=1;y++) {
                Vec3 exit=DismountHelper.findSafeDismountLocation(passenger.getType(),level,pos.offset(x,y,z),true);
                if(exit!=null) return exit;
            }
        }
        return DismountHelper.findSafeDismountLocation(passenger.getType(),level,pos,true);
    }

    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Direction direction=level().getBlockState(anchor()).hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? level().getBlockState(anchor()).getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.fromYRot(getYRot());
        Vec3 exit=safeExit(level(),anchor(),direction,passenger);
        return exit!=null ? exit : super.getDismountLocationForPassenger(passenger);
    }
}
