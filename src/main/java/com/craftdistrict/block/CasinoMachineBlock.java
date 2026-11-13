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

/** Static two-cell machines; placement and single-drop removal use the shared multiblock lifecycle. */
public final class CasinoMachineBlock extends MultiBlockDecorationBlock {
    public enum Design implements StringRepresentable {
        CLASSIC, MODERN;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    private final Design design;
    private static final VoxelShape[][][] SHAPES = {
            {RestaurantKitchenBlock.rotations(Shapes.or(box(1, 0, 1, 15, 2, 15),
                    box(2, 2, 2.5, 14, 14.5, 14.5), box(1.5, 14.5, .75, 14.5, 16, 14.5)).optimize()),
                    RestaurantKitchenBlock.rotations(Shapes.or(box(2, 0, 5.5, 14, 16, 14.5),
                            box(13, 1, 7, 16, 9, 10)).optimize())},
            {RestaurantKitchenBlock.rotations(Shapes.or(box(1, 0, 1, 15, 2, 15),
                    box(2, 2, 3, 14, 13.5, 14.5), box(1.5, 13.5, 1, 14.5, 16, 14.5)).optimize()),
                    RestaurantKitchenBlock.rotations(box(1.75, 0, 5.25, 14.25, 16, 14.5))}};
    //? if >=1.21 {
    public static final MapCodec<CasinoMachineBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((CasinoMachineBlock b) -> b.design),
            propertiesCodec()).apply(instance, CasinoMachineBlock::new));
    //?}
    public CasinoMachineBlock(Design design, Properties properties) { super(properties); this.design = design; }
    @Override protected Direction getExtensionDirection(BlockState state) { return Direction.UP; }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos below = structureOrigin(state, pos).below();
        return level.hasChunkAt(below) && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        if (side == (state.getValue(PRIMARY) ? Direction.UP : Direction.DOWN)
                && neighbor != stateForPart(state, state.getValue(PRIMARY) ? 1 : 0)) return Blocks.AIR.defaultBlockState();
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
