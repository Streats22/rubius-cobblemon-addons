package nl.streats1.rubiusaddons.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import nl.streats1.rubiusaddons.block.entity.CreatePoweredHealingMachineBlockEntity;
import nl.streats1.rubiusaddons.block.entity.ModBlockEntities;
import org.jetbrains.annotations.NotNull;

public class CreatePoweredHealingMachineBlock extends Block implements EntityBlock {
    
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty HEALING = BlockStateProperties.POWERED; // Reuse POWERED property for healing state (active model)
    // Power state from RPM: 0 = default Cobblemon-like (blue), 1 = 12–32 RPM (yellow), 2 = 32+ RPM (red). Light: 5/8/12.
    public static final IntegerProperty POWER_STATE = BlockStateProperties.LEVEL; // Reuse LEVEL property (0, 1, 2)
    
    // Custom VoxelShapes matching Cobblemon's HealingMachineBlock to prevent clipping
    // These define the collision/rendering boundaries
    private static final VoxelShape NORTH_SOUTH_SHAPE = Shapes.or(
        Shapes.box(0.0, 0.0, 0.0, 1.0, 0.625, 1.0),           // Base
        Shapes.box(0.0625, 0.625, 0.0, 0.9375, 0.875, 0.125),  // Front edge
        Shapes.box(0.0625, 0.625, 0.875, 0.9375, 0.875, 1.0),  // Back edge
        Shapes.box(0.0625, 0.625, 0.125, 0.1875, 0.75, 0.875), // Left side
        Shapes.box(0.8125, 0.625, 0.125, 0.9375, 0.75, 0.875), // Right side
        Shapes.box(0.1875, 0.625, 0.125, 0.8125, 0.6875, 0.875) // Top tray
    );
    
    private static final VoxelShape WEST_EAST_SHAPE = Shapes.or(
        Shapes.box(0.0, 0.0, 0.0, 1.0, 0.625, 1.0),           // Base
        Shapes.box(0.875, 0.625, 0.0625, 1.0, 0.875, 0.9375), // Front edge
        Shapes.box(0.0, 0.625, 0.0625, 0.125, 0.875, 0.9375),  // Back edge
        Shapes.box(0.125, 0.625, 0.0625, 0.875, 0.75, 0.1875), // Left side
        Shapes.box(0.125, 0.625, 0.8125, 0.875, 0.75, 0.9375), // Right side
        Shapes.box(0.125, 0.625, 0.1875, 0.875, 0.6875, 0.8125) // Top tray
    );
    
    public CreatePoweredHealingMachineBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(HEALING, false)
            .setValue(POWER_STATE, 0)); // 0 = no power (blue, like normal Cobblemon)
    }
    
    @Override
    protected void createBlockStateDefinition(@NotNull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HEALING, POWER_STATE);
    }
    
    @Override
    public @NotNull BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    
    @Override
    public @NotNull BlockState rotate(@NotNull BlockState state, @NotNull Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }
    
    /**
     * Returns the VoxelShape for rendering and collision.
     * Matches Cobblemon's HealingMachineBlock to prevent clipping issues.
     */
    @Override
    public @NotNull VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return switch (facing) {
            case WEST, EAST -> WEST_EAST_SHAPE;
            default -> NORTH_SOUTH_SHAPE; // NORTH, SOUTH, UP, DOWN
        };
    }

    @Override
    public @NotNull BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new CreatePoweredHealingMachineBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        // Server-side ticker handles healing logic
        // Client-side doesn't need a ticker - renderer will check block entity state directly
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.CREATE_POWERED_HEALING_MACHINE.get(), CreatePoweredHealingMachineBlockEntity::tick);
    }
    
    // Handle player interaction using NeoForge event system
    // This will be handled via RightClickBlock event in the main mod class

    @SuppressWarnings("unchecked")
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> type, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == type ? (BlockEntityTicker<A>) ticker : null;
    }
}
