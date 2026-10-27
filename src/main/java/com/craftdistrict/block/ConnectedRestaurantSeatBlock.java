package com.craftdistrict.block;

//? if >=1.21 {
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Local, reciprocal edge connections; no ticking, traversal, or additional block properties. */
public class ConnectedRestaurantSeatBlock extends RestaurantSeatBlock {
    public enum Corner { STRAIGHT, INNER_LEFT, INNER_RIGHT, OUTER_LEFT, OUTER_RIGHT }
    public enum Connection implements StringRepresentable {
        SINGLE(false,false,false,Corner.STRAIGHT),
        LEFT_END(false,true,false,Corner.STRAIGHT),
        RIGHT_END(true,false,false,Corner.STRAIGHT),
        MIDDLE(true,true,false,Corner.STRAIGHT),
        SUPPORTED_MIDDLE(true,true,true,Corner.STRAIGHT),
        INNER_LEFT_SINGLE(false,false,false,Corner.INNER_LEFT),
        INNER_LEFT_LEFT_END(false,true,false,Corner.INNER_LEFT),
        INNER_LEFT_RIGHT_END(true,false,false,Corner.INNER_LEFT),
        INNER_LEFT_MIDDLE(true,true,false,Corner.INNER_LEFT),
        INNER_RIGHT_SINGLE(false,false,false,Corner.INNER_RIGHT),
        INNER_RIGHT_LEFT_END(false,true,false,Corner.INNER_RIGHT),
        INNER_RIGHT_RIGHT_END(true,false,false,Corner.INNER_RIGHT),
        INNER_RIGHT_MIDDLE(true,true,false,Corner.INNER_RIGHT),
        OUTER_LEFT_SINGLE(false,false,false,Corner.OUTER_LEFT),
        OUTER_LEFT_LEFT_END(false,true,false,Corner.OUTER_LEFT),
        OUTER_LEFT_RIGHT_END(true,false,false,Corner.OUTER_LEFT),
        OUTER_LEFT_MIDDLE(true,true,false,Corner.OUTER_LEFT),
        OUTER_RIGHT_SINGLE(false,false,false,Corner.OUTER_RIGHT),
        OUTER_RIGHT_LEFT_END(false,true,false,Corner.OUTER_RIGHT),
        OUTER_RIGHT_RIGHT_END(true,false,false,Corner.OUTER_RIGHT),
        OUTER_RIGHT_MIDDLE(true,true,false,Corner.OUTER_RIGHT);
        public final boolean left, right, support;
        public final Corner corner;
        private static final Connection[][] BY_CORNER = indexConnections();
        Connection(boolean left, boolean right, boolean support, Corner corner) {
            this.left=left; this.right=right; this.support=support; this.corner=corner;
        }
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
        public Direction leftPort(Direction facing) {
            return switch(corner) {
                case INNER_LEFT -> facing;
                case OUTER_RIGHT -> facing.getOpposite();
                default -> facing.getCounterClockWise();
            };
        }
        public Direction rightPort(Direction facing) {
            return switch(corner) {
                case INNER_RIGHT -> facing;
                case OUTER_LEFT -> facing.getOpposite();
                default -> facing.getClockWise();
            };
        }
        static Connection of(Corner corner, boolean left, boolean right) {
            return BY_CORNER[corner.ordinal()][(left ? 1 : 0) | (right ? 2 : 0)];
        }
        private static Connection[][] indexConnections() {
            Connection[][] result = new Connection[Corner.values().length][4];
            for (Connection c : values()) if (!c.support)
                result[c.corner.ordinal()][(c.left ? 1 : 0) | (c.right ? 2 : 0)] = c;
            return result;
        }
    }
    public static final EnumProperty<Connection> CONNECTION = EnumProperty.create("connection", Connection.class);
    //? if >=1.21 {
    public static final MapCodec<ConnectedRestaurantSeatBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            StringRepresentable.fromEnum(Design::values).fieldOf("design").forGetter((ConnectedRestaurantSeatBlock b) -> b.design),
            propertiesCodec()).apply(instance, ConnectedRestaurantSeatBlock::new));
    //?}

    public ConnectedRestaurantSeatBlock(Design design, Properties properties) {
        super(design, properties);
        if (!design.connected()) throw new IllegalArgumentException("Connected seating design required");
        registerDefaultState(defaultBlockState().setValue(CONNECTION, Connection.SINGLE));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(CONNECTION);
    }

    private BlockState matching(LevelReader level, BlockPos pos, BlockPos removed) {
        if(pos.equals(removed) || !level.hasChunkAt(pos)) return null;
        BlockState state=level.getBlockState(pos);
        return state.is(this) ? state : null;
    }

    private boolean joins(LevelReader level, BlockPos pos, Direction facing, BlockPos removed) {
        BlockState state=matching(level,pos,removed);
        return state!=null && state.getValue(FACING)==facing;
    }

    // Like stair corners: only perpendicular facings turn. Block competing three-way branches.
    private Corner corner(LevelReader level, BlockPos pos, Direction facing, BlockPos removed) {
        BlockState behind=matching(level,pos.relative(facing.getOpposite()),removed);
        if(behind!=null) {
            Direction other=behind.getValue(FACING);
            if(other.getAxis()!=facing.getAxis() && !joins(level,pos.relative(other),facing,removed))
                return other==facing.getCounterClockWise() ? Corner.OUTER_RIGHT : Corner.OUTER_LEFT;
        }
        BlockState front=matching(level,pos.relative(facing),removed);
        if(front!=null) {
            Direction other=front.getValue(FACING);
            if(other.getAxis()!=facing.getAxis() && !joins(level,pos.relative(other.getOpposite()),facing,removed))
                return other==facing.getCounterClockWise() ? Corner.INNER_RIGHT : Corner.INNER_LEFT;
        }
        return Corner.STRAIGHT;
    }

    private boolean edgeJoins(LevelReader level, BlockPos pos, Direction facing, Direction edge, BlockPos removed) {
        BlockPos adjacent=pos.relative(edge);
        BlockState other=matching(level,adjacent,removed);
        if(other==null) return false;
        Direction otherFacing=other.getValue(FACING);
        if(otherFacing==facing.getOpposite() || otherFacing==facing && edge.getAxis()==facing.getAxis()) return false;
        Connection ports=Connection.of(corner(level,adjacent,otherFacing,removed),false,false);
        return ports.leftPort(otherFacing)==edge.getOpposite() || ports.rightPort(otherFacing)==edge.getOpposite();
    }

    private int run(LevelReader level, BlockPos pos, Direction along, Direction facing, BlockPos removed) {
        for (int n=1;n<=4;n++) if (!joins(level,pos.relative(along,n),facing,removed)) return n-1;
        return 4;
    }

    public BlockState layout(BlockState state, LevelReader level, BlockPos pos, BlockPos removed) {
        Direction facing=state.getValue(FACING), left=facing.getCounterClockWise(), right=facing.getClockWise();
        Corner shape=corner(level,pos,facing,removed);
        Connection ports=Connection.of(shape,false,false);
        boolean a=edgeJoins(level,pos,facing,ports.leftPort(facing),removed), b=edgeJoins(level,pos,facing,ports.rightPort(facing),removed);
        Connection connection=Connection.of(shape,a,b);
        if (design == Design.BENCH && shape==Corner.STRAIGHT && a && b) {
            Direction negative = left.getAxis() == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
            int before=run(level,pos,negative,facing,removed), after=run(level,pos,negative.getOpposite(),facing,removed);
            if (before>=2 && after>=2) {
                int coordinate=negative.getAxis()==Direction.Axis.X ? pos.getX() : pos.getZ();
                boolean support=Math.floorMod(coordinate,3)==0;
                if (!support && before+after+1<7) {
                    boolean grid=false;
                    for(int n=2-before;n<=after-2;n++) grid |= Math.floorMod(coordinate+n,3)==0;
                    support=!grid && before==(before+after+1)/2;
                }
                if (support) connection=Connection.SUPPORTED_MIDDLE;
            }
        }
        return state.setValue(CONNECTION,connection);
    }

    private void refreshConnections(Level level, BlockPos changed, BlockPos removed) {
        if(level.isClientSide) return;
        java.util.Set<BlockPos> affected=new java.util.HashSet<>();
        // A facing change can affect a corner's adjacent segment too (radius two).
        for(int x=-2;x<=2;x++) for(int z=-2;z<=2;z++) {
            BlockPos pos=changed.offset(x,0,z);
            BlockState state=matching(level,pos,removed);
            if(state==null) continue;
            affected.add(pos);
            if(design==Design.BENCH) {
                Direction along=state.getValue(FACING).getClockWise();
                for(int n=-4;n<=4;n++) affected.add(pos.relative(along,n));
            }
        }
        for(BlockPos pos:affected) {
            BlockState old=matching(level,pos,removed);
            if(old==null) continue;
            BlockState next=layout(old,level,pos,removed);
            if(old!=next) level.setBlock(pos,next,Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state=super.getStateForPlacement(context);
        return state==null ? null : layout(state,context.getLevel(),context.getClickedPos(),null);
    }

    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!old.is(this) || old.getValue(FACING)!=state.getValue(FACING)) refreshConnections(level,pos,null);
    }

    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!replacement.is(this) || replacement.getValue(FACING)!=state.getValue(FACING)) refreshConnections(level,pos,pos);
        super.onRemove(state,level,pos,replacement,moving);
    }

    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                            BlockPos pos, BlockPos neighborPos) {
        BlockState supported=super.updateShape(state,direction,neighbor,level,pos,neighborPos);
        if (supported.isAir() || !direction.getAxis().isHorizontal()) return supported;
        return layout(state,level,pos,null);
    }

    @Override public boolean skipRendering(BlockState state, BlockState neighbor, Direction direction) {
        if(neighbor.is(this)) {
            Connection c=state.getValue(CONNECTION), n=neighbor.getValue(CONNECTION);
            Direction f=state.getValue(FACING), nf=neighbor.getValue(FACING);
            boolean ours=c.left && c.leftPort(f)==direction || c.right && c.rightPort(f)==direction;
            boolean theirs=n.left && n.leftPort(nf)==direction.getOpposite() || n.right && n.rightPort(nf)==direction.getOpposite();
            if(ours && theirs) return true;
        }
        return super.skipRendering(state,neighbor,direction);
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result=super.mirror(state,mirror);
        if (mirror==Mirror.NONE) return result;
        Connection c=state.getValue(CONNECTION);
        Corner opposite=switch(c.corner) {
            case INNER_LEFT -> Corner.INNER_RIGHT; case INNER_RIGHT -> Corner.INNER_LEFT;
            case OUTER_LEFT -> Corner.OUTER_RIGHT; case OUTER_RIGHT -> Corner.OUTER_LEFT;
            default -> Corner.STRAIGHT;
        };
        return result.setValue(CONNECTION,c.support ? c : Connection.of(opposite,c.right,c.left));
    }

    //? if >=1.21 {
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec() { return CODEC; }
    //?}
}
