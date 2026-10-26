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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import dev.architectury.event.events.common.ChunkEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A single-cell filing cabinet and two-cell wall monitors, with old cabinet-state cleanup. */
public final class PoliceOfficeSecurityBlock extends MultiBlockDecorationBlock {
    public enum Design implements StringRepresentable {
        FILE_CABINET, MONITOR_WALL;

        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    //? if >=1.21 {
    public static final MapCodec<PoliceOfficeSecurityBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design")
                    .forGetter((PoliceOfficeSecurityBlock block) -> block.design),
            propertiesCodec()).apply(instance, PoliceOfficeSecurityBlock::new));
    //?}

    private static final VoxelShape[][][] SHAPES = {
            {RestaurantKitchenBlock.rotations(Block.box(0, 0, 1.5, 16, 16, 14.5)),
             RestaurantKitchenBlock.rotations(Shapes.empty())},
            {RestaurantKitchenBlock.rotations(Shapes.or(Block.box(2.5, 1.25, 12.5, 15.5, 7.5, 16),
                     Block.box(2.5, 8.5, 12.5, 15.5, 14.75, 16))),
             RestaurantKitchenBlock.rotations(Shapes.or(Block.box(.5, 1.25, 12.5, 13.5, 7.5, 16),
                     Block.box(.5, 8.5, 12.5, 13.5, 14.75, 16)))}
    };

    private final Design design;

    public PoliceOfficeSecurityBlock(Design design, Properties properties) {
        super(properties);
        this.design = design;
    }

    @Override
    protected int partCount() { return design == Design.FILE_CABINET ? 1 : 2; }

    @Override
    protected int partIndex(BlockState state) {
        return design == Design.FILE_CABINET ? 0 : super.partIndex(state);
    }

    private static boolean isObsoleteCabinetTop(BlockState state) {
        return state.getBlock() instanceof PoliceOfficeSecurityBlock block
                && block.design == Design.FILE_CABINET && !state.getValue(PRIMARY);
    }

    public static void registerLegacyCleanup() {
        ChunkEvent.LOAD_DATA.register((chunk, level, tag) -> removeObsoleteCabinetTops(chunk));
    }

    /** Only sections whose palettes contain old upper parts are visited; no world ticking. */
    public static void removeObsoleteCabinetTops(ChunkAccess chunk) {
        // Fully generated chunks can arrive through a read-only proto wrapper.
        if (chunk instanceof ImposterProtoChunk wrapper) chunk = wrapper.getWrapped();
        var sections = chunk.getSections();
        for (int section = 0; section < sections.length; section++) {
            var blocks = sections[section];
            if (blocks == null || !blocks.maybeHas(PoliceOfficeSecurityBlock::isObsoleteCabinetTop)) continue;
            int baseY = chunk.getSectionYFromSectionIndex(section) * 16;
            for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                if (!isObsoleteCabinetTop(blocks.getBlockState(x, y, z))) continue;
                BlockPos pos = new BlockPos(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z);
                chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                chunk.setUnsaved(true);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!level.isClientSide && design == Design.FILE_CABINET && state.getValue(PRIMARY) && !replacement.is(this)) {
            BlockState above = level.getBlockState(pos.above());
            if (isObsoleteCabinetTop(above) && above.getValue(FACING) == state.getValue(FACING)) {
                level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override
    protected Direction getExtensionDirection(BlockState state) {
        return design == Design.MONITOR_WALL ? state.getValue(FACING).getClockWise() : Direction.UP;
    }

    @Override
    protected Direction getPlacementFacing(BlockPlaceContext context) {
        if (design != Design.MONITOR_WALL) return super.getPlacementFacing(context);
        Direction face = context.getClickedFace();
        return face.getAxis().isHorizontal() ? face : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (design == Design.MONITOR_WALL) {
            Direction facing = state.getValue(FACING);
            BlockPos wall = pos.relative(facing.getOpposite());
            return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
        }
        if (!state.getValue(PRIMARY)) return false;
        BlockPos floor = pos.below();
        return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction side, BlockState other,
                                  LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        if (design == Design.FILE_CABINET && !state.getValue(PRIMARY)) return Blocks.AIR.defaultBlockState();
        Direction support = design == Design.MONITOR_WALL ? state.getValue(FACING).getOpposite() : Direction.DOWN;
        if (side == support && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        if (design == Design.FILE_CABINET) return state;
        Direction partner = getExtensionDirection(state);
        if (!state.getValue(PRIMARY)) partner = partner.getOpposite();
        if (side == partner && other != stateForPart(state, 1 - partIndex(state))) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int part = design == Design.FILE_CABINET ? (state.getValue(PRIMARY) ? 0 : 1) : partIndex(state);
        return SHAPES[design.ordinal()][part][RestaurantKitchenBlock.turn(state)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
