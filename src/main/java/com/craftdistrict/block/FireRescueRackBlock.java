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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Four-part floor rack or wall panel; validation/removal stay in the shared base. */
public final class FireRescueRackBlock extends WideTallDecorationBlock {
    public enum Rack implements StringRepresentable {
        HOSE("hose", 1), TOOLS("tools", 2);

        private final String name;
        private final int shapeIndex;

        Rack(String name, int shapeIndex) {
            this.name = name;
            this.shapeIndex = shapeIndex;
        }

        @Override
        public String getSerializedName() { return name; }
    }

    //? if >=1.21 {
    public static final MapCodec<FireRescueRackBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Rack::values).fieldOf("rack")
                    .forGetter((FireRescueRackBlock block) -> block.rack),
            propertiesCodec()).apply(instance, FireRescueRackBlock::new));
    //?}

    private final Rack rack;

    public FireRescueRackBlock(Rack rack, Properties properties) {
        super(properties);
        this.rack = rack;
    }

    @Override
    protected Direction getPlacementFacing(BlockPlaceContext context) {
        if (rack == Rack.HOSE) { return super.getPlacementFacing(context); }
        Direction face = context.getClickedFace();
        return face.getAxis().isHorizontal() ? face : null;
    }

    private Direction supportDirection(BlockState state) {
        return rack == Rack.TOOLS
                ? state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite() : Direction.DOWN;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // Every wall cell needs backing; only the lower row of the floor rack needs a floor.
        if (rack == Rack.HOSE && partIndex(state) >= 2) { return true; }
        Direction direction = supportDirection(state);
        BlockPos support = pos.relative(direction);
        return level.getBlockState(support).isFaceSturdy(level, support, direction.getOpposite());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == supportDirection(state) && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FireRescueShapes.get(rack.shapeIndex, partIndex(state), state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
