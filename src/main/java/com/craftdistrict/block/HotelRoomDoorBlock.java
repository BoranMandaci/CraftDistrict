package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Original room-door leaf and jambs, with the shared inner jamb omitted only in a pair. */
public final class HotelRoomDoorBlock extends PairedDoorBlock {
    private static final VoxelShape[][][][][] SHAPES = new VoxelShape[2][2][2][2][];
    static {
        for (int paired = 0; paired < 2; paired++) for (int upper = 0; upper < 2; upper++)
                for (int open = 0; open < 2; open++) for (int right = 0; right < 2; right++) {
            VoxelShape leaf = open == 0 ? box(0, 0, 13, 16, 16, 16) : box(right == 0 ? 0 : 13, 0, 0, right == 0 ? 3 : 16, 16, 16);
            VoxelShape frame = paired == 0
                    ? Shapes.or(box(0, 0, 13.25, .375, 16, 15.75), box(15.625, 0, 13.25, 16, 16, 15.75))
                    : box(right == 0 ? 0 : 15.625, 0, 13.25, right == 0 ? .375 : 16, 16, 15.75);
            if (upper == 1) frame = Shapes.or(frame, box(.375, 15.5, 13.25, 15.625, 16, 15.75));
            SHAPES[paired][upper][open][right] = RestaurantKitchenBlock.rotations(Shapes.or(leaf, frame).optimize());
        }
    }
    //? if >=1.21 {
    public static final MapCodec<HotelRoomDoorBlock> CODEC = simpleCodec(HotelRoomDoorBlock::new);
    //?}
    public HotelRoomDoorBlock(Properties properties) {
        super(BlockSetType.OAK, properties, 3);
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER && DecorationSupport.isPlaced(state, level, pos)
                || super.canSurvive(state, level, pos);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(PAIRED) ? 1 : 0][state.getValue(HALF) == DoubleBlockHalf.UPPER ? 1 : 0][state.getValue(OPEN) ? 1 : 0]
                [state.getValue(HINGE) == DoorHingeSide.RIGHT ? 1 : 0][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return getShape(state, level, pos, context); }
    //? if >=1.21 {
    @Override public MapCodec<? extends DoorBlock> codec() { return CODEC; }
    //?}
}
