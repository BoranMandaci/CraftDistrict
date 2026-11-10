package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The existing ControlledLight path owns defaults, effects, power, proximity, dyes and removal cleanup. */
public final class HotelRoomLightBlock extends RotatableDecorationBlock implements ControlledLight {
    private final boolean wall;
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(box(4, 0, 4, 12, 9.25, 12)),
            RestaurantKitchenBlock.rotations(box(6.5, 6.5, 10.5, 9.5, 12.5, 16))};
    //? if >=1.21 {
    public static final MapCodec<HotelRoomLightBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.fieldOf("wall").forGetter((HotelRoomLightBlock b) -> b.wall),
            propertiesCodec()).apply(instance, HotelRoomLightBlock::new));
    //?}
    public HotelRoomLightBlock(boolean wall, Properties properties) { super(properties); this.wall = wall; }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        Direction face = wall ? state.getValue(FACING) : Direction.UP;
        BlockPos support = wall ? pos.relative(face.getOpposite()) : pos.below();
        return level.hasChunkAt(support) && level.getBlockState(support).isFaceSturdy(level, support, face);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (wall && !context.getClickedFace().getAxis().isHorizontal()) return null;
        BlockState state = super.getStateForPlacement(context);
        if (wall) state = state.setValue(FACING, context.getClickedFace());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        return !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPES[wall ? 1 : 0][RestaurantKitchenBlock.turn(state)]; }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
