package com.craftdistrict.mixin;

import com.craftdistrict.access.AccessControlData;
import com.craftdistrict.access.AccessDoorTarget;
import com.craftdistrict.block.ModularMovingDoorBlock;
import com.craftdistrict.block.MultiBlockDecorationBlock;
import com.craftdistrict.block.PairedDoorBlock;
import com.craftdistrict.block.VaultDoorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class AccessDoorChunkMixin {
    @Inject(method = "setLoaded", at = @At("TAIL"))
    private void craftdistrict$resumeDoorWork(boolean loaded, CallbackInfo callback) {
        LevelChunk chunk = (LevelChunk) (Object) this;
        if (!loaded || !(chunk.getLevel() instanceof ServerLevel level)) return;
        long key = chunk.getPos().toLong();
        if (level.getServer().isSameThread()) AccessControlData.get(level).chunkReady(key);
        else level.getServer().execute(() -> AccessControlData.get(level).chunkReady(key));
    }

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void craftdistrict$changedDoorFrame(BlockPos pos, BlockState state, boolean moving, CallbackInfoReturnable<BlockState> callback) {
        BlockState previous = callback.getReturnValue();
        if (previous == null || previous == state || !craftdistrict$frameChange(pos, previous, state)) return;
        LevelChunk chunk = (LevelChunk) (Object) this;
        if (chunk.getLevel() instanceof ServerLevel level) AccessControlData.get(level).topologyChanged(pos);
    }

    @Unique private static boolean craftdistrict$frameChange(BlockPos pos, BlockState before, BlockState after) {
        if (before.getBlock() != after.getBlock()) return craftdistrict$frame(before) || craftdistrict$frame(after);
        if (!AccessDoorTarget.supports(after.getBlock())) return false;
        if (before.getValue(BlockStateProperties.HORIZONTAL_FACING) != after.getValue(BlockStateProperties.HORIZONTAL_FACING)) return true;
        if (after.getBlock() instanceof DoorBlock) return before.getValue(DoorBlock.HINGE) != after.getValue(DoorBlock.HINGE)
                || before.getValue(DoorBlock.HALF) != after.getValue(DoorBlock.HALF)
                || after.hasProperty(PairedDoorBlock.PAIRED) && before.getValue(PairedDoorBlock.PAIRED) != after.getValue(PairedDoorBlock.PAIRED);
        if (after.getBlock() instanceof VaultDoorBlock && (before.getValue(VaultDoorBlock.LARGE) != after.getValue(VaultDoorBlock.LARGE)
                || before.getValue(VaultDoorBlock.FORWARD) != after.getValue(VaultDoorBlock.FORWARD))) return true;
        return after.getBlock() instanceof MultiBlockDecorationBlock door && !door.structureOrigin(before, pos).equals(door.structureOrigin(after, pos));
    }

    @Unique private static boolean craftdistrict$frame(BlockState state) {
        return AccessDoorTarget.supports(state.getBlock())
                && (!(state.getBlock() instanceof VaultDoorBlock) || state.getValue(VaultDoorBlock.PART) < 9)
                && (!(state.getBlock() instanceof ModularMovingDoorBlock)
                    || !state.getValue(ModularMovingDoorBlock.OPEN));
    }
}
