package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class DocumentCabinetBlock extends MultiBlockDecorationBlock {
    private static final VoxelShape LOWER_CASE = Shapes.or(sidesAndBack(1.43, 16),
            box(.32, 0, 7.36, 15.68, .80, 15.58), box(0, .80, 7.12, 16, 1.43, 15.78),
            shelf(7.42, 7.94), shelf(15.44, 16));
    private static final VoxelShape UPPER_CASE = Shapes.or(sidesAndBack(0, 14.68), shelf(6.89, 7.47),
            box(0, 14.68, 7.12, 16, 16, 15.78));
    private static final VoxelShape[][] CLOSED = {
            RestaurantKitchenBlock.rotations(Shapes.or(LOWER_CASE, box(1.06, 1.18, 6.56, 14.94, 16, 7.12))),
            RestaurantKitchenBlock.rotations(Shapes.or(UPPER_CASE, box(1.06, 0, 6.56, 14.94, 14.68, 7.12),
                    box(6.915, 2.08, 6.19, 7.325, 5.04, 6.56), box(8.675, 2.08, 6.19, 9.085, 5.04, 6.56)))
    };
    private static final VoxelShape[][] OPEN = {
            RestaurantKitchenBlock.rotations(Shapes.or(LOWER_CASE, box(.50, 1.18, .18, 1.06, 16, 7.12),
                    box(14.94, 1.18, .18, 15.50, 16, 7.12))),
            RestaurantKitchenBlock.rotations(Shapes.or(UPPER_CASE, box(.50, 0, .18, 1.06, 14.68, 7.12),
                    box(14.94, 0, .18, 15.50, 14.68, 7.12),
                    box(.13, 2.08, .855, .50, 5.04, 1.265), box(15.50, 2.08, .855, 15.87, 5.04, 1.265)))
    };

    private static VoxelShape sidesAndBack(double bottom, double top) {
        return Shapes.or(box(0, bottom, 7.12, 1.18, top, 15.78),
                box(14.82, bottom, 7.12, 16, top, 15.78),
                box(1.18, bottom, 14.85, 14.82, top, 15.78));
    }

    private static VoxelShape shelf(double bottom, double top) {
        return box(1.18, bottom, 7.12, 14.82, top, 14.85);
    }

    public DocumentCabinetBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.OPEN, false));
    }

    @Override protected void addPartProperties(StateDefinition.Builder<Block, BlockState> builder) {
        super.addPartProperties(builder);
        builder.add(BlockStateProperties.OPEN);
    }

    @Override protected Direction getExtensionDirection(BlockState state) { return Direction.UP; }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        BlockPos floor = structureOrigin(state, pos).below();
        return level.hasChunkAt(floor) && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        boolean primary = state.getValue(PRIMARY);
        if (side == (primary ? Direction.UP : Direction.DOWN) && !isRemovingLinkedStructure(state, level, pos)
                && neighbor != stateForPart(state, primary ? 1 : 0)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return toggle(level, pos, player);
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND ? toggle(level, pos, player) : InteractionResult.PASS;
    }
    *///?}

    private InteractionResult toggle(Level level, BlockPos pos, Player player) {
        return CabinetInteraction.toggleLinked(this, level, pos, player, SoundEvents.IRON_DOOR_OPEN, SoundEvents.IRON_DOOR_CLOSE);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(BlockStateProperties.OPEN) ? OPEN : CLOSED)[partIndex(state)][RestaurantKitchenBlock.turn(state)];
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    public static final MapCodec<DocumentCabinetBlock> CODEC = simpleCodec(DocumentCabinetBlock::new);
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
