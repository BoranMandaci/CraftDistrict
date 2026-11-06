package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import com.craftdistrict.entity.MedicalResting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two-cell treatment furniture sharing lightweight, synchronized lying interaction. */
public final class HospitalTreatmentTableBlock extends MultiBlockDecorationBlock implements LyingBlock {
    public enum Design implements StringRepresentable {
        EXAMINATION, STRETCHER;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    private final Design design;
    //? if >=1.21 {
    public static final MapCodec<HospitalTreatmentTableBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((HospitalTreatmentTableBlock b) -> b.design),
            propertiesCodec()).apply(instance, HospitalTreatmentTableBlock::new));
    //?}
    private static final VoxelShape[][][] SHAPES = {
            {RestaurantKitchenBlock.rotations(Shapes.or(box(1.5, 6, 0, 14.5, 11.25, 16), box(4, 0, 2, 12, 6, 13)).optimize()),
                    RestaurantKitchenBlock.rotations(Shapes.or(box(1.5, 6, 0, 14.5, 11.25, 16),
                            box(2, 11.25, 1, 14, 16, 12), box(4, 0, 3, 12, 6, 13)).optimize())},
            {RestaurantKitchenBlock.rotations(Shapes.or(box(1, 4, 0, 15, 10.75, 16), box(2, 0, 9, 14, 4, 15),
                            box(.5, 10.75, 0, 1.5, 13, 7), box(14.5, 10.75, 0, 15.5, 13, 7)).optimize()),
                    RestaurantKitchenBlock.rotations(Shapes.or(box(1, 4, 0, 15, 10.75, 16), box(2, 0, 2, 14, 4, 8),
                            box(.5, 10.75, 7, 1.5, 13, 16), box(14.5, 10.75, 7, 15.5, 13, 16)).optimize())}};

    public HospitalTreatmentTableBlock(Design design, Properties properties) { super(properties); this.design = design; }
    @Override protected Direction getPlacementFacing(BlockPlaceContext context) {
        // Both medical table heads extend away from their placer, as with a vanilla bed.
        // Existing saved facings and linked-part offsets retain their meaning.
        return context.getHorizontalDirection();
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos origin = structureOrigin(state, pos), end = origin.relative(state.getValue(FACING));
        return level.hasChunkAt(origin) && level.hasChunkAt(end)
                && level.getBlockState(origin.below()).isFaceSturdy(level, origin.below(), Direction.UP)
                && level.getBlockState(end.below()).isFaceSturdy(level, end.below(), Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        Direction partner = state.getValue(PRIMARY) ? state.getValue(FACING) : state.getValue(FACING).getOpposite();
        if (side == partner && (!neighbor.is(this) || neighbor.getValue(PRIMARY) == state.getValue(PRIMARY)
                || neighbor.getValue(FACING) != state.getValue(FACING))) return Blocks.AIR.defaultBlockState();
        return state;
    }
    @Override public BlockPos lyingHead(BlockState state, BlockPos pos) {
        return structureOrigin(state, pos).relative(state.getValue(FACING));
    }
    @Override public double lyingHeight() { return (design == Design.EXAMINATION ? 13 : 12) / 16.0; }
    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return MedicalResting.lie(state, level, pos, player);
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND ? MedicalResting.lie(state, level, pos, player) : InteractionResult.PASS;
    }
    *///?}
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!replacement.is(this) || replacement.getValue(FACING) != state.getValue(FACING))
            MedicalResting.removeAt(level, lyingHead(state, pos));
        super.onRemove(state, level, pos, replacement, moving);
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
