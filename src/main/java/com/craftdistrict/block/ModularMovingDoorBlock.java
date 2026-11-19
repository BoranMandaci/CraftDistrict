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
    private static final VoxelShape[][][][] SHAPES=new VoxelShape[2][2][48][];
    static {
        for(int garage=0;garage<2;garage++)for(int open=0;open<2;open++)for(int role=0;role<3;role++)for(int edges=0;edges<16;edges++) {
            boolean shared=open==1&&role==2;
            VoxelShape shape=open==0?box(0,0,13.5,16,16,16):Shapes.empty();
            if(open==1&&role!=0)shape=garage==1?box(0,13,0,16,16,16):box(0,0,13.5,16,16,16);
            if(open==0||role!=1) {
                double top=shared&&garage==1?14:16;
                if((edges&1)==0)shape=Shapes.or(shape,box(0,0,13, .65,top,16));
                if((edges&2)==0)shape=Shapes.or(shape,box(15.35,0,13,16,top,16));
                if((edges&4)==0&&!shared)shape=Shapes.or(shape,box(0,15.35,13,16,16,16));
                if((edges&8)==0)shape=Shapes.or(shape,box(0,0,13,16,.35,16));
            }
            SHAPES[garage][open][role*16+edges]=RestaurantKitchenBlock.rotations(shape.optimize());
        }
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
        Direction right=state.getValue(FACING).getClockWise();
        Direction[] directions={right.getOpposite(),right,Direction.UP,Direction.DOWN};
        BooleanProperty[] properties={LEFT,RIGHT,UP,DOWN};
        for(int i=0;i<4;i++) {
            BlockPos p=pos.relative(directions[i]);
            boolean joined=level.hasChunkAt(p)&&compatible(state,level.getBlockState(p))&&!level.getBlockState(p).getValue(OPEN);
            state=state.setValue(properties[i],joined);
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
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c) {
        int edges=(s.getValue(LEFT)?1:0)|(s.getValue(RIGHT)?2:0)|(s.getValue(UP)?4:0)|(s.getValue(DOWN)?8:0);
        return SHAPES[garage()?1:0][s.getValue(OPEN)?1:0][s.getValue(ROLE).ordinal()*16+edges][RestaurantKitchenBlock.turn(s)];
    }
    @Override public VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return getShape(s,l,p,c);}
}
