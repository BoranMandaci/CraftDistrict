package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Four linked cabinet sections; the shared grid lifecycle owns placement and loot cleanup. */
public final class RestaurantStorageCabinetBlock extends WideTallDecorationBlock {
    public enum Design implements StringRepresentable {
        REFRIGERATOR, PANTRY;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private static final int DOOR_UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final VoxelShape[] CLOSED = RestaurantKitchenBlock.rotations(Block.box(0, 0, 0, 16, 16, 16));
    private static final VoxelShape[][] OPEN_SHAPES = {
            RestaurantKitchenBlock.rotations(Shapes.or(Block.box(0, 0, 2, 16, 16, 16),
                    Block.box(-.75, 0, -12.75, 1.25, 16, 2))),
            RestaurantKitchenBlock.rotations(Shapes.or(Block.box(0, 0, 2, 16, 16, 16),
                    Block.box(14.75, 0, -12.75, 16.75, 16, 2)))
    };
    //? if >=1.21 {
    public static final MapCodec<RestaurantStorageCabinetBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design")
                    .forGetter((RestaurantStorageCabinetBlock block) -> block.design),
            propertiesCodec()).apply(instance, RestaurantStorageCabinetBlock::new));
    //?}
    private final Design design;

    public RestaurantStorageCabinetBlock(Design design, Properties properties) {
        super(properties);
        this.design = design;
        registerDefaultState(defaultBlockState().setValue(OPEN, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OPEN);
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos floor = pos.below(partIndex(state) / 2 + 1);
        return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }

    private boolean isPart(BlockState origin, BlockState other, int index) {
        return other.is(this) && partIndex(other) == index && other.getValue(FACING) == origin.getValue(FACING);
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState other,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        // Neighbor notifications still reach other decorations, but cabinet sections must
        // not auto-destroy/drop themselves midway through the shared no-loot cleanup.
        if (isRemovingLinkedStructure(state, level, pos)) return state;
        if (side == Direction.DOWN && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        int index = partIndex(state);
        BlockPos origin = pos.subtract(partOffset(state, index));
        for (int bit = 1; bit <= 2; bit++) {
            int neighbor = index ^ bit;
            if (otherPos.equals(origin.offset(partOffset(state, neighbor))) && !isPart(state, other, neighbor)) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        // Only associated sections follow the origin, so correction cannot oscillate.
        BlockState primary = level.getBlockState(origin);
        if (index != 0 && isPart(state, primary, 0)) return state.setValue(OPEN, primary.getValue(OPEN));
        return state;
    }

    InteractionResult toggleDoors(BlockState clicked, Level level, BlockPos pos, Player player) {
        BlockState current = level.getBlockState(pos);
        if (!current.is(this)) return InteractionResult.PASS;
        BlockPos origin = pos.subtract(partOffset(current, partIndex(current)));
        BlockState primary = level.getBlockState(origin);
        BlockState[] before = new BlockState[partCount()];
        for (int index = 0; index < partCount(); index++) {
            before[index] = level.getBlockState(origin.offset(partOffset(current, index)));
            if (!isPart(current, before[index], index) || before[index].getValue(OPEN) != primary.getValue(OPEN)) {
                return InteractionResult.PASS;
            }
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;
        boolean open = !primary.getValue(OPEN);
        // Persist/synchronize ordinary block states without propagating a partial door update.
        int written = 0;
        for (int index = 0; index < partCount(); index++) {
            BlockPos section = origin.offset(partOffset(primary, index));
            if (level.getBlockState(section) != before[index]
                    || !level.setBlock(section, before[index].setValue(OPEN, open), DOOR_UPDATE_FLAGS)) break;
            written++;
        }
        if (written != partCount()) {
            for (int index = written - 1; index >= 0; index--) {
                BlockPos section = origin.offset(partOffset(primary, index));
                if (level.getBlockState(section) == before[index].setValue(OPEN, open)) {
                    level.setBlock(section, before[index], DOOR_UPDATE_FLAGS);
                }
            }
            return InteractionResult.PASS;
        }
        level.playSound(null, origin, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
                SoundSource.BLOCKS, .65F, 1.15F);
        level.gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, origin);
        return InteractionResult.CONSUME;
    }

    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return toggleDoors(state, level, pos, player);
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return toggleDoors(state, level, pos, player);
    }
    *///?}

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int turn = RestaurantKitchenBlock.turn(state);
        return state.getValue(OPEN) ? OPEN_SHAPES[partIndex(state) % 2][turn] : CLOSED[turn];
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
