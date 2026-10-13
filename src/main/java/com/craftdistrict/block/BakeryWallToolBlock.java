package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Uses the signage wall-only mounting branch, with independent fixed tool layouts. */
public final class BakeryWallToolBlock extends BakerySignageBlock {
    //? if >=1.21 {
    public static final MapCodec<BakeryWallToolBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Tool::values).fieldOf("tool")
                    .forGetter((BakeryWallToolBlock block) -> block.tool),
            propertiesCodec()).apply(instance, BakeryWallToolBlock::new));
    //?}

    public enum Tool implements StringRepresentable {
        BREAD_PEEL("bread_peel"), UTENSIL("utensil"), ROLLING_PIN("rolling_pin");

        private final String name;

        Tool(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private final Tool tool;

    public BakeryWallToolBlock(Tool tool, Properties properties) {
        // WALL_SHOP selects the established wall support/placement rules. Only
        // those rules are inherited; this class supplies its own models/shapes.
        super(BakerySignageBlock.Variant.WALL_SHOP, properties);
        this.tool = tool;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int turn = switch (state.getValue(BlockStateProperties.HORIZONTAL_FACING)) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        return BakeryWallToolShapes.SHAPES[tool.ordinal()][turn];
    }

    // The inherited collision method dispatches to the shape above as well.
    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}
}
