package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Locale;

public class BankDecorationBlock extends RotatableDecorationBlock {
    public enum Design implements StringRepresentable {
        LOGO_SIGN, COMPUTER_WORKSTATION;

        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    private static final VoxelShape[] SIGN = RestaurantKitchenBlock.rotations(Shapes.or(
            box(1.1, 4.70, 15.40, 14.9, 11.30, 16),
            box(.75, 4.35, 14.62, 15.25, 11.65, 15.40),
            box(1.04, 4.64, 14.47, 14.96, 11.36, 14.62),
            box(1.28, 4.88, 14.38, 14.72, 11.12, 14.47)));
    private static final VoxelShape[] COMPUTER = RestaurantKitchenBlock.rotations(Shapes.or(
            box(5.1, 0, 2.05, 13.95, .64, 5.95),
            box(2.15, 0, 2.86, 4.51, 1.05, 5.61),
            box(4.95, 0, 8.95, 11.05, .65, 13.45),
            box(7.31, .65, 10.31, 8.69, 4.75, 11.55),
            box(1.85, 3.45, 9.12, 14.15, 9.87, 10.16),
            box(4.35, 3.78, 10.16, 11.65, 8.63, 11.17)));
    private final Design design;

    public BankDecorationBlock(Design design, Properties properties) {
        super(properties);
        this.design = design;
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (design == Design.LOGO_SIGN) {
            Direction face = context.getClickedFace();
            if (!face.getAxis().isHorizontal()) return null;
            state = state.setValue(FACING, face);
        }
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (DecorationSupport.isPlaced(state, level, pos)) return true;
        Direction face = design == Design.LOGO_SIGN ? state.getValue(FACING) : Direction.UP;
        BlockPos support = pos.relative(face.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, face);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (design == Design.LOGO_SIGN ? SIGN : COMPUTER)[RestaurantKitchenBlock.turn(state)];
    }

    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    //? if >=1.21 {
    public static final MapCodec<BankDecorationBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((BankDecorationBlock block) -> block.design),
            propertiesCodec()).apply(instance, BankDecorationBlock::new));
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}

    public static final class Workstation extends BankDecorationBlock {
        public static final BooleanProperty SCREEN_ON = BooleanProperty.create("screen_on");

        public Workstation(Properties properties) {
            super(Design.COMPUTER_WORKSTATION, properties);
            registerDefaultState(defaultBlockState().setValue(SCREEN_ON, false));
        }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(SCREEN_ON);
        }

        //? if >=1.21 {
        @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            return toggleScreen(state, level, pos, player);
        }
        //?} else {
        /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                               InteractionHand hand, BlockHitResult hit) {
            return hand == InteractionHand.MAIN_HAND ? toggleScreen(state, level, pos, player) : InteractionResult.PASS;
        }
        *///?}

        private InteractionResult toggleScreen(BlockState state, Level level, BlockPos pos, Player player) {
            if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
            if (!level.isClientSide) {
                level.setBlock(pos, state.cycle(SCREEN_ON), Block.UPDATE_CLIENTS);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        //? if >=1.21 {
        public static final MapCodec<Workstation> CODEC = simpleCodec(Workstation::new);
        @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
        //?}
    }
}
