package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Existing two-part behavior with an open workbench shape, cached for all eight states. */
public final class DoughPreparationTableBlock extends MultiBlockDecorationBlock {
    private static final VoxelShape[][] SHAPES = createShapes();
    //? if >=1.21 {
    public static final MapCodec<DoughPreparationTableBlock> CODEC = simpleCodec(DoughPreparationTableBlock::new);
    //?}

    public DoughPreparationTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected Direction getExtensionDirection(BlockState state) {
        return state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise();
    }

    private static VoxelShape[][] createShapes() {
        VoxelShape[][] shapes = new VoxelShape[2][4];
        for (int part = 0; part < 2; part++) {
            double outerStart = part == 0 ? 1.25 : 12.75;
            double outerEnd = part == 0 ? 3.25 : 14.75;
            double shelfStart = part == 0 ? 1.5 : 0;
            double shelfEnd = part == 0 ? 16 : 14.5;
            // Broad structural envelopes preserve the open legs and tabletop height.
            VoxelShape shape = Shapes.or(
                    Block.box(0, 9.5, 0.5, 16, 12.5, 15.5),
                    Block.box(outerStart, 0, 1.25, outerEnd, 9.5, 3.25),
                    Block.box(outerStart, 0, 12.75, outerEnd, 9.5, 14.75),
                    Block.box(shelfStart, 2.5, 2, shelfEnd, part == 0 ? 6.75 : 8.25, 14),
                    part == 0 ? Block.box(3.5, 12.5, 2.75, 14.5, 14.5, 12.5)
                            : Block.box(2, 12.5, 3.125, 13.625, 16, 13.5));
            shapes[part][0] = shape.optimize();
            for (int turn = 1; turn < 4; turn++) {
                VoxelShape rotated = Shapes.empty();
                for (AABB box : shapes[part][turn - 1].toAabbs()) {
                    rotated = Shapes.or(rotated, Shapes.box(1 - box.maxZ, box.minY, box.minX,
                            1 - box.minZ, box.maxY, box.maxX));
                }
                shapes[part][turn] = rotated.optimize();
            }
        }
        return shapes;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int turn = switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        return SHAPES[partIndex(state)][turn];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}
}
