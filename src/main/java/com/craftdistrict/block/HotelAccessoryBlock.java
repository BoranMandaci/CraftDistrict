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

/** Static guest-room accessories share directional placement and cached, simple envelopes. */
public final class HotelAccessoryBlock extends RotatableDecorationBlock {
    public enum Design implements StringRepresentable {
        TELEPHONE, KETTLE_SET, SERVICE_TRAY, AIR_CONDITIONER, THERMOSTAT;
        boolean wall() { return this == AIR_CONDITIONER || this == THERMOSTAT; }
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    private final Design design;
    private static final VoxelShape[][] SHAPES = {
            RestaurantKitchenBlock.rotations(box(4, 0, 4, 12, 3.75, 12)),
            RestaurantKitchenBlock.rotations(box(1.5, 0, 2, 14.5, 7.75, 14)),
            RestaurantKitchenBlock.rotations(box(1, 0, 2, 15, 5.25, 14)),
            RestaurantKitchenBlock.rotations(box(1, 7, 11.5, 15, 14, 16)),
            RestaurantKitchenBlock.rotations(box(5.5, 6, 14.25, 10.5, 11.5, 16))};
    //? if >=1.21 {
    public static final MapCodec<HotelAccessoryBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((HotelAccessoryBlock b) -> b.design),
            propertiesCodec()).apply(instance, HotelAccessoryBlock::new));
    //?}
    public HotelAccessoryBlock(Design design, Properties properties) { super(properties); this.design = design; }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        Direction face = design.wall() ? state.getValue(FACING) : Direction.UP;
        BlockPos support = design.wall() ? pos.relative(face.getOpposite()) : pos.below();
        return level.hasChunkAt(support) && level.getBlockState(support).isFaceSturdy(level, support, face);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (design.wall() && !context.getClickedFace().getAxis().isHorizontal()) return null;
        BlockState state = super.getStateForPlacement(context);
        if (design.wall()) state = state.setValue(FACING, context.getClickedFace());
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        return !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[design.ordinal()][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
