package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Simple cached chair envelopes; solid-floor placement, no seating. */
public final class OfficeChairBlock extends CashRegisterBlock {
    private static final VoxelShape[] SHAPES = rotations(Shapes.or(
            Block.box(1.5, 0, 1.5, 14.5, 2, 14.5),
            Block.box(7.125, 2, 7.125, 8.875, 7.25, 8.875),
            Block.box(3, 7.25, 3, 13, 8.75, 12.25),
            Block.box(3, 8.75, 11.151845, 13, 15.841633, 14.736395),
            Block.box(2.375, 7.25, 4.75, 3.375, 11.375, 10),
            Block.box(12.625, 7.25, 4.75, 13.625, 11.375, 10)).optimize());
    //? if >=1.21 {
    public static final MapCodec<OfficeChairBlock> CODEC = simpleCodec(OfficeChairBlock::new);
    //?}

    public OfficeChairBlock(Properties properties) {
        super(properties);
    }

    private static VoxelShape[] rotations(VoxelShape north) {
        VoxelShape[] result = new VoxelShape[4];
        result[0] = north;
        for (int turn = 1; turn < 4; turn++) {
            VoxelShape rotated = Shapes.empty();
            for (AABB box : result[turn - 1].toAabbs()) {
                rotated = Shapes.or(rotated, Shapes.box(1 - box.maxZ, box.minY, box.minX,
                        1 - box.minZ, box.maxY, box.maxX));
            }
            result[turn] = rotated.optimize();
        }
        return result;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int turn = switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        return SHAPES[turn];
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}
}
