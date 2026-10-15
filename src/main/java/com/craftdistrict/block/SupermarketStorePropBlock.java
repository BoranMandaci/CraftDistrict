package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Independent floor props; broad structural shapes are cached for each facing. */
public final class SupermarketStorePropBlock extends CashRegisterBlock {
    public enum Prop implements StringRepresentable {
        STOCK_PALLET, SECURITY_GATE, CANDY_RACK;

        @Override
        public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }

    //? if >=1.21 {
    public static final MapCodec<SupermarketStorePropBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Prop::values).fieldOf("prop")
                    .forGetter((SupermarketStorePropBlock block) -> block.prop),
            propertiesCodec()).apply(instance, SupermarketStorePropBlock::new));
    //?}

    private static final VoxelShape[][] SHAPES = {
            rotations(Shapes.or(Block.box(1, 0, 1, 15, 3, 15),
                    Block.box(1.5, 3, 2, 14.5, 15.5, 14.25)).optimize()),
            rotations(Block.box(5.75, 0, 2, 10.25, 24, 14)),
            rotations(Shapes.or(Block.box(2.5, 0, 5, 13.5, 1.5, 14),
                    Block.box(3, 1.5, 5, 13, 26, 13.75)).optimize())
    };

    private final Prop prop;

    public SupermarketStorePropBlock(Prop prop, Properties properties) {
        super(properties);
        this.prop = prop;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (prop != Prop.STOCK_PALLET) {
            BlockPos above = context.getClickedPos().above();
            // Tall visuals belong to this single block; no invisible partner is placed.
            if (!context.getLevel().isInWorldBounds(above)
                    || !context.getLevel().getBlockState(above).isAir()) return null;
        }
        return super.getStateForPlacement(context);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int turn = switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        return SHAPES[prop.ordinal()][turn];
    }

    private static VoxelShape[] rotations(VoxelShape north) {
        VoxelShape[] result = new VoxelShape[4];
        result[0] = north;
        for (int turn = 1; turn < 4; turn++) {
            VoxelShape rotated = Shapes.empty();
            for (AABB box : result[turn - 1].toAabbs()) {
                rotated = Shapes.or(rotated, Shapes.box(1 - box.maxZ, box.minY, box.minX,
                        1 - box.minZ, box.maxY, box.maxX));
            }
            result[turn] = rotated.optimize();
        }
        return result;
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
