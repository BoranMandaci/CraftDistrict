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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Locale;

/** Stackable court supports with caps and feet only on their exposed ends. */
public final class SportsPostBlock extends Block {
    private static final BooleanProperty UP = BlockStateProperties.UP;
    private static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    public enum Design implements StringRepresentable {
        BASKETBALL(1.75, 3.25, 1.85),
        NET(1.10, 2.65, 1.24);

        private final double radius;
        private final VoxelShape[] shapes = new VoxelShape[4];

        Design(double radius, double foot, double cap) {
            this.radius = radius;
            VoxelShape tube = column(radius, 0, 16);
            VoxelShape base = column(foot, 0, .34);
            VoxelShape top = column(cap, 15.28, 16);
            shapes[0] = Shapes.or(tube, base, top);
            shapes[1] = Shapes.or(tube, base);
            shapes[2] = Shapes.or(tube, top);
            shapes[3] = tube;
        }

        private static VoxelShape column(double radius, double y, double top) {
            return Block.box(8 - radius, y, 8 - radius, 8 + radius, top, 8 + radius);
        }

        double radius() { return radius; }

        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    private final Design design;

    //? if >=1.21 {
    public static final MapCodec<SportsPostBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((SportsPostBlock post) -> post.design),
            propertiesCodec()).apply(instance, SportsPostBlock::new));

    @Override protected MapCodec<SportsPostBlock> codec() { return CODEC; }
    //?}

    public SportsPostBlock(Design design, Properties properties) {
        super(properties);
        this.design = design;
        registerDefaultState(defaultBlockState().setValue(UP, false).setValue(DOWN, false));
    }

    static boolean isPost(BlockState state, Design design) {
        return state.getBlock() instanceof SportsPostBlock post && post.design == design;
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, DOWN);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return defaultBlockState().setValue(UP, level.getBlockState(pos.above()).is(this))
                .setValue(DOWN, level.getBlockState(pos.below()).is(this));
    }

    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                            LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        return switch (side) {
            case UP -> state.setValue(UP, neighbor.is(this));
            case DOWN -> state.setValue(DOWN, neighbor.is(this));
            default -> state;
        };
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return design.shapes[(state.getValue(UP) ? 1 : 0) | (state.getValue(DOWN) ? 2 : 0)];
    }
}
