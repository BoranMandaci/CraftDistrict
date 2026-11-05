package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Floor-standing clinical equipment, using the shared two-part lifecycle. */
public final class HospitalEquipmentBlock extends MultiBlockDecorationBlock {
    public enum Design implements StringRepresentable {
        MONITOR, IV, SUPPLY_CABINET, STERILE_CABINET;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    private final Design design;
    //? if >=1.21 {
    public static final MapCodec<HospitalEquipmentBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((HospitalEquipmentBlock b) -> b.design),
            propertiesCodec()).apply(instance, HospitalEquipmentBlock::new));
    //?}
    private static final VoxelShape[][][] SHAPES = {
            {RestaurantKitchenBlock.rotations(Shapes.or(box(2, 0, 2, 14, 2, 14), box(5, 2, 5, 11, 16, 11)).optimize()),
                    RestaurantKitchenBlock.rotations(Shapes.or(box(7, 0, 7, 9, 5.25, 9), box(2, 5.25, 4.5, 14, 15, 9)).optimize())},
            {RestaurantKitchenBlock.rotations(Shapes.or(box(2, 0, 2, 14, 1.5, 14), box(7.25, 1.5, 7.25, 8.75, 16, 8.75)).optimize()),
                    RestaurantKitchenBlock.rotations(box(3, 0, 5.75, 13, 14, 10.25))},
            {RestaurantKitchenBlock.rotations(box(1, 0, 1, 15, 16, 15)),
                    RestaurantKitchenBlock.rotations(box(1, 0, 1, 15, 16, 15))},
            {RestaurantKitchenBlock.rotations(box(1, 0, 1, 15, 16, 15)),
                    RestaurantKitchenBlock.rotations(box(1, 0, 1, 15, 16, 15))}};

    public HospitalEquipmentBlock(Design design, Properties properties) { super(properties); this.design = design; }
    @Override protected Direction getExtensionDirection(BlockState state) { return Direction.UP; }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos floor = structureOrigin(state, pos).below();
        return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        Direction partner = state.getValue(PRIMARY) ? Direction.UP : Direction.DOWN;
        if (side == partner && (!neighbor.is(this) || neighbor.getValue(PRIMARY) == state.getValue(PRIMARY)
                || neighbor.getValue(FACING) != state.getValue(FACING))) return Blocks.AIR.defaultBlockState();
        return side == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[design.ordinal()][state.getValue(PRIMARY) ? 0 : 1][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
