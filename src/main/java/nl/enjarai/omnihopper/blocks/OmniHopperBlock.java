package nl.enjarai.omnihopper.blocks;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public abstract class OmniHopperBlock extends BlockWithEntity {
    public static final EnumProperty<Direction> POINTY_BIT;
    public static final EnumProperty<Direction> SUCKY_BIT;
    public static final BooleanProperty ENABLED;
    public static final VoxelShape[] SUCKY_AREA;
    private static final VoxelShape MIDDLE_SHAPE;
    private static final VoxelShape POINTY_SHAPE;
    private static final VoxelShape[][] SHAPES;
    private static final VoxelShape[][] SHAPES_RAYCAST;

    static {
        POINTY_BIT = DirectionProperty.of("pointy_bit", Direction.values());
        SUCKY_BIT = DirectionProperty.of("sucky_bit", Direction.values());
        ENABLED = Properties.ENABLED;

        var defaultShapes = new VoxelShape[]{
                Block.createCuboidShape(0, 0, 0, 16, 6, 16),
                Block.createCuboidShape(0, 10, 0, 16, 16, 16),

                Block.createCuboidShape(0, 0, 0, 16, 16, 6),
                Block.createCuboidShape(0, 0, 10, 16, 16, 16),

                Block.createCuboidShape(0, 0, 0, 6, 16, 16),
                Block.createCuboidShape(10, 0, 0, 16, 16, 16),
        };
        var insideShapes = new VoxelShape[]{
                Block.createCuboidShape(2, 0, 2, 14, 5, 14),
                Block.createCuboidShape(2, 11, 2, 14, 16, 14),

                Block.createCuboidShape(2, 2, 0, 14, 14, 5),
                Block.createCuboidShape(2, 2, 11, 14, 14, 16),

                Block.createCuboidShape(0, 2, 2, 5, 14, 14),
                Block.createCuboidShape(11, 2, 2, 16, 14, 14),
        };

        MIDDLE_SHAPE = Block.createCuboidShape(4.0D, 4.0D, 4.0D, 12.0D, 12.0D, 12.0D);
        POINTY_SHAPE = Block.createCuboidShape(6.0D, 6.0D, 6.0D, 10.0D, 10.0D, 10.0D);

        SHAPES = new VoxelShape[6][6];
        SHAPES_RAYCAST = new VoxelShape[6][6];

        SUCKY_AREA = new VoxelShape[6];
        for (var suckyDirection : Direction.values()) {
            var suckyI = suckyDirection.ordinal();

            var infrontSuckyArea =
                    Block.createCuboidShape(0, 0, 0, 16, 16, 16)
                            .offset(suckyDirection.getOffsetX(), suckyDirection.getOffsetY(), suckyDirection.getOffsetZ());

            SUCKY_AREA[suckyI] = VoxelShapes.union(insideShapes[suckyI], infrontSuckyArea);

            var mainShapeRaycast = VoxelShapes.union(defaultShapes[suckyI], MIDDLE_SHAPE);
            var mainShape = VoxelShapes.combineAndSimplify(
                    mainShapeRaycast, insideShapes[suckyI], BooleanBiFunction.ONLY_FIRST);

            for (var pointyDirection : Direction.values()) {

                var pX = pointyDirection.getOffsetX() * 0.375;
                var pY = pointyDirection.getOffsetY() * 0.375;
                var pZ = pointyDirection.getOffsetZ() * 0.375;
                if (!pointyDirection.getAxis().equals(suckyDirection.getAxis())) {
                    pX += suckyDirection.getOffsetX() * -0.125;
                    pY += suckyDirection.getOffsetY() * -0.125;
                    pZ += suckyDirection.getOffsetZ() * -0.125;
                }
                var pointyShape = POINTY_SHAPE.offset(pX, pY, pZ);

                SHAPES[pointyDirection.ordinal()][suckyI] = VoxelShapes.union(mainShape, pointyShape);
                SHAPES_RAYCAST[pointyDirection.ordinal()][suckyI] = VoxelShapes.union(mainShapeRaycast, pointyShape);
            }
        }
    }

    public OmniHopperBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES[state.get(POINTY_BIT).ordinal()][state.get(SUCKY_BIT).ordinal()];
    }

    @Override
    public VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
        return SHAPES_RAYCAST[state.get(POINTY_BIT).ordinal()][state.get(SUCKY_BIT).ordinal()];
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState()
                .with(POINTY_BIT, ctx.getSide().getOpposite())
                .with(SUCKY_BIT, ctx.getPlayerLookDirection().getOpposite())
                .with(ENABLED, true);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
//        // TODO this doesn't work right now because of a problem with the transfer api, so comparator outputs will have to wait
//        if (world.getBlockEntity(pos) instanceof OmniHopperBlockEntity<?> hopperBlockEntity) {
//            return StorageUtil.calculateComparatorOutput(hopperBlockEntity.getStorage(), null);
//        }
        return 0;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof OmniHopperBlockEntity<?> hopperBlockEntity) {
                hopperBlockEntity.tick(world, pos, state);
            }
        };
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state
                .with(POINTY_BIT, rotation.rotate(state.get(POINTY_BIT)))
                .with(SUCKY_BIT, rotation.rotate(state.get(SUCKY_BIT)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(POINTY_BIT)));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(POINTY_BIT).add(SUCKY_BIT).add(ENABLED);
    }

    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return false;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        } else {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof OmniHopperBlockEntity hopperBlockEntity) {
                player.openHandledScreen(hopperBlockEntity);
                player.incrementStat(Stats.INSPECT_HOPPER);
            }

            return ActionResult.CONSUME;
        }
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        if (itemStack.hasCustomName()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof OmniHopperBlockEntity hopperBlockEntity) {
                hopperBlockEntity.setCustomName(itemStack.getName());
            }
        }
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean notify) {
        this.updateEnabled(world, pos, state);
        super.neighborUpdate(state, world, pos, block, fromPos, notify);
    }

    private void updateEnabled(World world, BlockPos pos, BlockState state) {
        boolean bl = !world.isReceivingRedstonePower(pos);
        if (bl != state.get(ENABLED)) {
            world.setBlockState(pos, state.with(ENABLED, bl), 4);
        }
    }

    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        if (!oldState.isOf(state.getBlock())) {
            this.updateEnabled(world, pos, state);
        }
        super.onBlockAdded(state, world, pos, oldState, notify);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        world.updateComparators(pos, this);
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
