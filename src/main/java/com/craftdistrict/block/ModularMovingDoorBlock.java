package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** One crafted segment per cell. Open panels have owned parking cells, not entities. */
public final class ModularMovingDoorBlock extends HorizontalDirectionalBlock {
    public enum Design implements StringRepresentable {
        INDUSTRIAL, SOLID_GARAGE, WINDOWED_GARAGE;
        @Override public String getSerializedName(){return name().toLowerCase(java.util.Locale.ROOT);}
    }
    public enum Role implements StringRepresentable {
        ANCHOR, PANEL, SHARED;
        @Override public String getSerializedName(){return name().toLowerCase(java.util.Locale.ROOT);}
    }
    public static final BooleanProperty OPEN=BlockStateProperties.OPEN, LEFT=BooleanProperty.create("left"),
            RIGHT=BooleanProperty.create("right"), UP=BooleanProperty.create("up"), DOWN=BooleanProperty.create("down");
    public static final EnumProperty<Role> ROLE=EnumProperty.create("role",Role.class);
    private static final BooleanProperty[] EDGES = {LEFT, RIGHT, UP, DOWN};
    private static final int LEFT_EDGE = 1, RIGHT_EDGE = 2, TOP_EDGE = 4, BOTTOM_EDGE = 8;
    private static final Direction[][] ADJACENT = new Direction[4][];
    private static final VoxelShape[][] CLOSED_SHAPES = new VoxelShape[16][];
    private static final VoxelShape[][] OPEN_FRAME_SHAPES = new VoxelShape[16][];
    private static final VoxelShape[][] PARKED_SHAPES = new VoxelShape[2][];
    private static final VoxelShape[][][] SHARED_SHAPES = new VoxelShape[2][16][];
    static {
        for (Direction facing : Direction.Plane.HORIZONTAL)
            ADJACENT[facing.get2DDataValue()] = new Direction[]{facing.getCounterClockWise(), facing.getClockWise(), Direction.UP, Direction.DOWN};
        VoxelShape uprightPanel = box(0, 0, 13.5, 16, 16, 16);
        VoxelShape overheadPanel = box(0, 13, 0, 16, 16, 16);
        PARKED_SHAPES[0] = RestaurantKitchenBlock.rotations(uprightPanel.optimize());
        PARKED_SHAPES[1] = RestaurantKitchenBlock.rotations(overheadPanel.optimize());
        for (int edges = 0; edges < CLOSED_SHAPES.length; edges++) {
            CLOSED_SHAPES[edges] = frameShapes(uprightPanel, edges, 16, true);
            OPEN_FRAME_SHAPES[edges] = frameShapes(Shapes.empty(), edges, 16, true);
            // A panel sharing its anchor never has a top rail.
            if ((edges & TOP_EDGE) == 0) {
                SHARED_SHAPES[0][edges] = frameShapes(uprightPanel, edges, 16, false);
                SHARED_SHAPES[1][edges] = frameShapes(overheadPanel, edges, 14, false);
            }
        }
    }

    private static VoxelShape[] frameShapes(VoxelShape panel, int edges, double sideTop, boolean topRail) {
        VoxelShape shape = panel;
        if ((edges & LEFT_EDGE) == 0) shape = Shapes.or(shape, box(0, 0, 13, .65, sideTop, 16));
        if ((edges & RIGHT_EDGE) == 0) shape = Shapes.or(shape, box(15.35, 0, 13, 16, sideTop, 16));
        if (topRail && (edges & TOP_EDGE) == 0) shape = Shapes.or(shape, box(0, 15.35, 13, 16, 16, 16));
        if ((edges & BOTTOM_EDGE) == 0) shape = Shapes.or(shape, box(0, 0, 13, 16, .35, 16));
        return RestaurantKitchenBlock.rotations(shape.optimize());
    }

    private final Design design;
    //? if >=1.21 {
    public static final MapCodec<ModularMovingDoorBlock> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((ModularMovingDoorBlock b)->b.design),
            propertiesCodec()).apply(instance,ModularMovingDoorBlock::new));
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){return CODEC;}
    //?}
    public ModularMovingDoorBlock(Design design,Properties properties) {
        super(properties);this.design=design;
        registerDefaultState(defaultBlockState().setValue(FACING,Direction.NORTH).setValue(OPEN,false).setValue(ROLE,Role.ANCHOR)
                .setValue(LEFT,false).setValue(RIGHT,false).setValue(UP,false).setValue(DOWN,false));
    }
    public boolean garage(){return design!=Design.INDUSTRIAL;}
    boolean compatible(BlockState a,BlockState b) {
        return b.getBlock() instanceof ModularMovingDoorBlock door && door.garage()==garage()
                && a.getValue(FACING)==b.getValue(FACING);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){super.createBlockStateDefinition(b);b.add(FACING,OPEN,ROLE,LEFT,RIGHT,UP,DOWN);}
    BlockState layout(BlockState state,LevelAccessor level,BlockPos pos) {
        if(state.getValue(OPEN))return state;
        Direction[] directions = ADJACENT[state.getValue(FACING).get2DDataValue()];
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < directions.length; i++) {
            neighborPos.setWithOffset(pos, directions[i]);
            BlockState neighbor = level.hasChunkAt(neighborPos) ? level.getBlockState(neighborPos) : null;
            boolean joined = neighbor != null && compatible(state, neighbor) && !neighbor.getValue(OPEN);
            state = state.setValue(EDGES[i], joined);
        }
        return state;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing=context.getHorizontalDirection().getOpposite();
        BlockPos behind=context.getClickedPos().relative(context.getClickedFace().getOpposite());
        BlockState neighbor=context.getLevel().getBlockState(behind);
        if(neighbor.getBlock() instanceof ModularMovingDoorBlock door&&door.garage()==garage()
                && (context.getClickedFace().getAxis()==Direction.Axis.Y
                || context.getClickedFace().getAxis()!=neighbor.getValue(FACING).getAxis())) facing=neighbor.getValue(FACING);
        return layout(defaultBlockState().setValue(FACING,facing),context.getLevel(),context.getClickedPos());
    }
    @Override public BlockState updateShape(BlockState state,Direction side,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos other){return layout(state,level,pos);}
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        super.onRemove(state,level,pos,next,moving);
        if(!level.isClientSide&&!next.is(this)&&state.getValue(OPEN))MovingDoorGroups.removed(level,pos);
    }
    private InteractionResult toggle(BlockState state,Level level,BlockPos pos,Player player) {
        if(level.isClientSide)return InteractionResult.SUCCESS;
        return MovingDoorGroups.toggle(level,pos,player);
    }
    //? if >=1.21 {
    @Override public InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player u,BlockHitResult h){return toggle(s,l,p,u);}
    //?} else {
    /*@Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player u,net.minecraft.world.InteractionHand hand,BlockHitResult h){return toggle(s,l,p,u);}
    *///?}
    private static int edges(BlockState state) {
        return (state.getValue(LEFT) ? LEFT_EDGE : 0) | (state.getValue(RIGHT) ? RIGHT_EDGE : 0)
                | (state.getValue(UP) ? TOP_EDGE : 0) | (state.getValue(DOWN) ? BOTTOM_EDGE : 0);
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int facing = RestaurantKitchenBlock.turn(state);
        if (!state.getValue(OPEN)) return CLOSED_SHAPES[edges(state)][facing];
        return switch (state.getValue(ROLE)) {
            case ANCHOR -> OPEN_FRAME_SHAPES[edges(state)][facing];
            case PANEL -> PARKED_SHAPES[garage() ? 1 : 0][facing];
            case SHARED -> SHARED_SHAPES[garage() ? 1 : 0][edges(state) & ~TOP_EDGE][facing];
        };
    }
    @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return getShape(s,l,p,c);}
}
