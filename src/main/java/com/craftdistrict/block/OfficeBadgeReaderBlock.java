package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Compact static reader; reuse the established wall-only mounting and support rules. */
public final class OfficeBadgeReaderBlock extends BakerySignageBlock implements EntityBlock {
    private static final int RESULT_EVENT = 1;
    //? if >=1.21 {
    public static final MapCodec<OfficeBadgeReaderBlock> CODEC = simpleCodec(OfficeBadgeReaderBlock::new);
    //?}
    private static final VoxelShape[] SHAPES = RestaurantKitchenBlock.rotations(box(5.5, 4.5, 14, 10.5, 12.5, 16));
    public OfficeBadgeReaderBlock(Properties properties) { super(Variant.WALL_SHOP, properties); }

    public static void showResult(ServerLevel level, BlockPos pos, boolean granted) {
        if (level.getBlockState(pos).getBlock() instanceof OfficeBadgeReaderBlock reader)
            level.blockEvent(pos, reader, RESULT_EVENT, granted ? 1 : 0);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OfficeBadgeReaderBlockEntity(pos, state);
    }

    @Override public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int type, int data) {
        if (type != RESULT_EVENT) return super.triggerEvent(state, level, pos, type, data);
        if (level.isClientSide) {
            BlockEntity existing = level.getChunkAt(pos).getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
            if (existing instanceof OfficeBadgeReaderBlockEntity reader) {
                reader.blink(level.getGameTime(), data != 0);
            } else if (level.getBlockEntity(pos) instanceof OfficeBadgeReaderBlockEntity reader) {
                // Older readers gain a renderer on the first event even if their section has already set up its mesh.
                reader.blink(level.getGameTime(), data != 0);
                level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS | Block.UPDATE_IMMEDIATE);
            }
        }
        return true;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[RestaurantKitchenBlock.turn(state)];
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
