package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Passive kitchen equipment on the established solid-support placement lifecycle. */
public class RestaurantKitchenBlock extends CashRegisterBlock {
    public enum Equipment implements StringRepresentable {
        RANGE, FRYER, GRIDDLE, PREP_TABLE, SINK;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    //? if >=1.21 {
    public static final MapCodec<RestaurantKitchenBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Equipment::values).fieldOf("equipment")
                    .forGetter((RestaurantKitchenBlock block) -> block.equipment),
            propertiesCodec()).apply(instance, RestaurantKitchenBlock::new));
    //?}

    private static final VoxelShape[][] SHAPES = {
            rotations(Shapes.or(Block.box(0, 0, 1, 16, 13.5, 16),
                    Block.box(0, 13.5, 0, 16, 15.125, 16), Block.box(0, 15.125, 14.5, 16, 16.5, 16))),
            rotations(Shapes.or(Block.box(0, 0, 1, 16, 13.5, 16),
                    Block.box(0, 13.5, 0, 16, 15.375, 16), Block.box(1, 15.375, 13, 15, 17, 16))),
            rotations(Shapes.or(Block.box(0, 0, 1, 16, 13.5, 16),
                    Block.box(0, 13.5, 0, 16, 14.5, 16), Block.box(0, 14.5, 14.5, 16, 16.5, 16))),
            rotations(openFrame()),
            rotations(Shapes.or(openFrame(), Block.box(2, 11, 2.5, 14, 13.5, 12),
                    Block.box(7.5, 14.5, 13.5, 8.5, 19.25, 14.5),
                    Block.box(7.5, 17.75, 9, 8.5, 19.25, 13.5)))
    };

    private final Equipment equipment;

    public RestaurantKitchenBlock(Equipment equipment, Properties properties) {
        super(properties);
        this.equipment = equipment;
    }

    private static VoxelShape openFrame() {
        return Shapes.or(Block.box(0, 13.5, 0, 16, 14.5, 16), Block.box(1, 3.5, 1.5, 15, 4.25, 15),
                Block.box(1.25, 0, 1.5, 2.5, 13.5, 2.75), Block.box(13.5, 0, 1.5, 14.75, 13.5, 2.75),
                Block.box(1.25, 0, 13.5, 2.5, 13.5, 14.75), Block.box(13.5, 0, 13.5, 14.75, 13.5, 14.75));
    }

    static VoxelShape[] rotations(VoxelShape north) {
        VoxelShape[] result = new VoxelShape[4];
        result[0] = north.optimize();
        for (int turn = 1; turn < 4; turn++) {
            VoxelShape next = Shapes.empty();
            for (AABB box : result[turn - 1].toAabbs()) {
                next = Shapes.or(next, Shapes.box(1 - box.maxZ, box.minY, box.minX,
                        1 - box.minZ, box.maxY, box.maxX));
            }
            result[turn] = next.optimize();
        }
        return result;
    }

    static int turn(BlockState state) {
        return switch (state.getValue(FACING)) { case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[equipment.ordinal()][turn(state)];
    }

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
