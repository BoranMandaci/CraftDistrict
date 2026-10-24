package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import com.craftdistrict.entity.Seating;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Floor-supported toilet/sink and a freely tileable asymmetric glass panel. */
public final class PoliceDetentionPropBlock extends CashRegisterBlock implements SittableBlock {
    //? if >=1.21 {
    public static final MapCodec<PoliceDetentionPropBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Prop::values).fieldOf("prop")
                    .forGetter((PoliceDetentionPropBlock block) -> block.prop),
            propertiesCodec()).apply(instance, PoliceDetentionPropBlock::new));
    //?}

    public enum Prop implements StringRepresentable {
        TOILET_SINK("toilet_sink", 1), GLASS("glass", 2);

        private final String name;
        private final int shapeIndex;

        Prop(String name, int shapeIndex) {
            this.name = name;
            this.shapeIndex = shapeIndex;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    private final Prop prop;

    public PoliceDetentionPropBlock(Prop prop, Properties properties) {
        super(properties);
        this.prop = prop;
    }

    @Override public boolean isSittable(BlockState state) { return prop == Prop.TOILET_SINK; }
    @Override public double seatHeight() { return 8.25 / 16; }
    @Override public double forwardOffset() { return 2.5 / 16; }

    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return Seating.sit(state, level, pos, player);
    }
    //?} else {
    /*@Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND ? Seating.sit(state, level, pos, player) : InteractionResult.PASS;
    }
    *///?}

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (isSittable(state) && (!replacement.is(this) || replacement.getValue(FACING) != state.getValue(FACING))) Seating.removeAt(level, pos);
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return prop == Prop.GLASS || super.canSurvive(state, level, pos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return PoliceDetentionShapes.get(prop.shapeIndex, 0, state);
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        // Remove only coplanar glass edge faces, retaining the distinct front/rear.
        if (prop == Prop.GLASS && adjacent.is(this)
                && adjacent.getValue(BlockStateProperties.HORIZONTAL_FACING) == state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                && direction.getAxis() != state.getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis()) {
            return true;
        }
        return super.skipRendering(state, adjacent, direction);
    }

    //? if >=1.21 {
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
    //?}
}
