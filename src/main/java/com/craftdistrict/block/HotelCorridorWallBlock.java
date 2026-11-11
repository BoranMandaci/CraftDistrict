package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Thin, static corridor fittings share four cached outlines and wall mounting rules. */
public class HotelCorridorWallBlock extends RotatableDecorationBlock {
    public enum Design implements StringRepresentable {
        PLAQUE(3, 5, 14.75, 13, 11, 16), CALL_PANEL(5.5, 3, 14.75, 10.5, 13, 16),
        INDICATOR(2, 5, 14.75, 14, 11, 16), DIRECTION_SIGN(.5, 4, 14.75, 15.5, 12, 16),
        EVACUATION_MAP(1.5, 1, 14.75, 14.5, 15, 16), WALL_ART(.5, 1, 14.75, 15.5, 15, 16);
        final VoxelShape[] shapes;
        Design(double x0, double y0, double z0, double x1, double y1, double z1) {
            shapes = RestaurantKitchenBlock.rotations(box(x0, y0, z0, x1, y1, z1));
        }
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    protected final Design design;
    //? if >=1.21 {
    public static final MapCodec<HotelCorridorWallBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((HotelCorridorWallBlock b) -> b.design),
            propertiesCodec()).apply(instance, HotelCorridorWallBlock::new));
    //?}
    public HotelCorridorWallBlock(Design design, Properties properties) { super(properties); this.design = design; }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos wall = pos.relative(state.getValue(FACING).getOpposite());
        return DecorationSupport.isPlaced(state, level, pos)
                || level.hasChunkAt(wall) && level.getBlockState(wall).isFaceSturdy(level, wall, state.getValue(FACING));
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (!face.getAxis().isHorizontal()) return null;
        BlockState state = defaultBlockState().setValue(FACING, face);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos other) {
        return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return design.shapes[RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return getShape(state, level, pos, context); }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
