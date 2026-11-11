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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Shared floor/wall mounting and cached simple shapes for the static restroom fittings. */
public final class HotelRestroomBlock extends MultiBlockDecorationBlock {
    public static final BooleanProperty TALL = BooleanProperty.create("tall");

    public enum Design implements StringRepresentable {
        LAUNDRY_BASKET(false, false, Shapes.or(box(2.25, 0, 2.25, 13.75, 10.75, 13.75),
                box(1.75, 10.75, 2, 14.25, 11.75, 14), box(5, 8.75, 1.5, 8, 12, 3),
                box(2, 11.75, 5.25, 2.75, 13.25, 10.75), box(13.25, 11.75, 5.25, 14, 13.25, 10.75)).optimize()),
        URINAL(true, true, Shapes.or(box(4.5, 8, 14.5, 11.5, 23.75, 16),
                box(6, 3.75, 11.25, 10, 5.25, 15.5), box(4.25, 4.5, 8.25, 11.75, 9, 15.5),
                box(5.25, 7.75, 7, 10.75, 9, 8.5), box(3.75, 7.75, 8.5, 5.25, 16, 15.5),
                box(10.75, 7.75, 8.5, 12.25, 16, 15.5), box(4.25, 16, 12, 5.25, 22.5, 15.5),
                box(10.75, 16, 12, 11.75, 22.5, 15.5), box(6.75, 23.75, 13.5, 9.25, 30.5, 16),
                box(5.125, 27.375, 14, 7, 28.375, 14.75)).optimize()),
        URINAL_DIVIDER(true, true, Shapes.or(box(7.375, 1.25, 1.75, 8.625, 31.75, 15.75),
                box(6.75, 5, 14.75, 9.25, 6.5, 16), box(6.75, 16, 14.75, 9.25, 17.5, 16),
                box(6.75, 27, 14.75, 9.25, 28.5, 16)).optimize()),
        HAND_DRYER(true, false, Shapes.or(box(3.75, 5, 10.8125, 12.25, 12.375, 16),
                box(5.25, 12.375, 12.25, 10.75, 13, 15.5), box(5, 4.375, 10.25, 11, 6, 15.5),
                box(5.5, 3.875, 11.5, 10.5, 4.375, 14.5)).optimize());

        private final boolean wall, tall;
        private final VoxelShape[] shapes;
        private final VoxelShape[][] tallShapes;
        Design(boolean wall, boolean tall, VoxelShape shape) {
            this.wall = wall; this.tall = tall;
            VoxelShape compact = shape;
            if (tall) {
                compact = Shapes.empty();
                for (var bounds : shape.toAabbs()) compact = Shapes.or(compact, Shapes.box(
                        bounds.minX, bounds.minY / 2, bounds.minZ, bounds.maxX, bounds.maxY / 2, bounds.maxZ));
                tallShapes = new VoxelShape[2][];
                for (int part = 0; part < 2; part++) tallShapes[part] = RestaurantKitchenBlock.rotations(
                        Shapes.join(shape.move(0, -part, 0), Shapes.block(), BooleanOp.AND).optimize());
            } else tallShapes = null;
            shapes = RestaurantKitchenBlock.rotations(compact);
        }
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    private final Design design;
    //? if >=1.21 {
    public static final MapCodec<HotelRestroomBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((HotelRestroomBlock b) -> b.design),
            propertiesCodec()).apply(instance, HotelRestroomBlock::new));
    //?}

    public HotelRestroomBlock(Design design, Properties properties) {
        super(properties); this.design = design;
        // Existing one-cell fixtures keep their footprint; only new placements reserve the upper cell.
        registerDefaultState(defaultBlockState().setValue(TALL, false));
    }

    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder); builder.add(TALL);
    }

    @Override protected int partCount(BlockState state) { return state.getValue(TALL) ? 2 : 1; }
    @Override protected Direction getExtensionDirection(BlockState state) { return Direction.UP; }

    @Override protected Direction getPlacementFacing(BlockPlaceContext context) {
        if (!design.wall) return super.getPlacementFacing(context);
        return context.getClickedFace().getAxis().isHorizontal() ? context.getClickedFace() : null;
    }

    @Override protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(TALL, design.tall);
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if ((!state.getValue(TALL) && !state.getValue(PRIMARY)) || (state.getValue(TALL) && !design.tall)) return false;
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        Direction face = design.wall ? state.getValue(FACING) : Direction.UP;
        BlockPos support = design.wall ? pos.relative(face.getOpposite()) : pos.below();
        return level.hasChunkAt(support) && level.getBlockState(support).isFaceSturdy(level, support, face);
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        if (state.getValue(TALL)) {
            Direction partner = state.getValue(PRIMARY) ? Direction.UP : Direction.DOWN;
            if (side == partner && neighbor != stateForPart(state, state.getValue(PRIMARY) ? 1 : 0))
                return Blocks.AIR.defaultBlockState();
        }
        Direction support = design.wall ? state.getValue(FACING).getOpposite() : Direction.DOWN;
        return side == support && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int turn = RestaurantKitchenBlock.turn(state);
        return state.getValue(TALL) && design.tall
                ? design.tallShapes[state.getValue(PRIMARY) ? 0 : 1][turn] : design.shapes[turn];
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
