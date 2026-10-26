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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Static retail/buffet equipment on the existing two-part placement and single-drop lifecycle. */
public final class CommercialExpansionBlock extends MultiBlockDecorationBlock {
    public enum Design implements StringRepresentable {
        BOTTLE_RETURN, SALAD_BAR, HOT_BUFFET;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    //? if >=1.21 {
    public static final MapCodec<CommercialExpansionBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((CommercialExpansionBlock b) -> b.design),
            propertiesCodec()).apply(instance, CommercialExpansionBlock::new));
    //?}
    private static final VoxelShape[][][] SHAPES = createShapes();
    private final Design design;

    public CommercialExpansionBlock(Design design, Properties properties) { super(properties); this.design = design; }

    private static VoxelShape[][][] createShapes() {
        VoxelShape[][][] result = new VoxelShape[3][2][];
        for (int part = 0; part < 2; part++) {
            result[0][part] = RestaurantKitchenBlock.rotations(Block.box(.5, 0, 1.5, 15.5, 16, 15.5));
            VoxelShape body = Block.box(0, 0, 1, 16, 12.5, 15);
            result[1][part] = RestaurantKitchenBlock.rotations(Shapes.or(body,
                    Block.box(part == 0 ? .5 : 0, 12.5, 2.75, part == 0 ? 16 : 15.5, 16, 11.75)));
            result[2][part] = RestaurantKitchenBlock.rotations(Shapes.or(body,
                    Block.box(part == 0 ? 1.5 : 0, 12.5, 2, part == 0 ? 16 : 14.5, 16, 11.75)));
        }
        return result;
    }

    @Override protected Direction getExtensionDirection(BlockState state) {
        return design == Design.BOTTLE_RETURN ? Direction.UP : state.getValue(FACING).getClockWise();
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos origin = pos.subtract(partOffset(state, partIndex(state)));
        int supports = design == Design.BOTTLE_RETURN ? 1 : 2;
        for (int part = 0; part < supports; part++) {
            BlockPos floor = origin.offset(partOffset(state, part)).below();
            if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) return false;
        }
        return true;
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState other,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        if (side == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        Direction partner = getExtensionDirection(state);
        if (!state.getValue(PRIMARY)) partner = partner.getOpposite();
        return side == partner && other != stateForPart(state, 1 - partIndex(state))
                ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[design.ordinal()][partIndex(state)][RestaurantKitchenBlock.turn(state)];
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
