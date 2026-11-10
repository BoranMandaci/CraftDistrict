package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Guest-room seating reuses the existing local seat entity and its removal/disconnect cleanup. */
public class HotelRoomSeatBlock extends RestaurantSeatBlock {
    private final boolean bench;
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(Shapes.or(box(2.5, 0, 2.25, 13.5, 10, 13.75), box(3.25, 10, 11.75, 12.75, 18.5, 14.5)).optimize()),
            RestaurantKitchenBlock.rotations(box(.5, 0, 1, 15.5, 8.5, 15))};
    //? if >=1.21 {
    public static final MapCodec<HotelRoomSeatBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.fieldOf("bench").forGetter((HotelRoomSeatBlock b) -> b.bench),
            propertiesCodec()).apply(instance, HotelRoomSeatBlock::new));
    //?}
    public HotelRoomSeatBlock(boolean bench, Properties properties) { super(Design.CUSHIONED, properties); this.bench = bench; }
    @Override public double seatHeight() { return (bench ? 8.5 : 10) / 16; }
    @Override public double forwardOffset() { return bench ? 0 : 1.0 / 16; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[bench ? 1 : 0][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
