package com.noozy.missionodyssey.block;

import com.mojang.serialization.MapCodec;
import com.noozy.missionodyssey.block.entity.TitaniumBlastFurnaceBlockEntity;
import com.noozy.missionodyssey.block.entity.TitaniumBlastFurnacePartBlockEntity;
import com.noozy.missionodyssey.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class TitaniumBlastFurnaceBlock extends BaseEntityBlock {
    public static final MapCodec<TitaniumBlastFurnaceBlock> CODEC = simpleCodec(TitaniumBlastFurnaceBlock::new);
    public static final BooleanProperty ASSEMBLED = BooleanProperty.create("assembled");

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public TitaniumBlastFurnaceBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH).setValue(ASSEMBLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING, ASSEMBLED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {

        return this.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(ASSEMBLED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            if (!state.getValue(ASSEMBLED)) {
                if (tryAssemble(level, pos, state)) {
                    return InteractionResult.SUCCESS;
                }
            } else {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof TitaniumBlastFurnaceBlockEntity furnace) {
                    player.openMenu(furnace, pos);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private boolean tryAssemble(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);







        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = 0; dz < 3; dz++) {
                if (dx == 0 && dz == 0) continue;


                BlockPos targetPos = getRelativePos(pos, dx, 0, dz, facing);
                BlockState targetState = level.getBlockState(targetPos);

                if (!targetState.is(ModBlocks.MACHINE_FRAME.get())) {
                    return false;
                }
            }
        }


        level.setBlock(pos, state.setValue(ASSEMBLED, true), 3);
        for (int dy = 0; dy < 3; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = 0; dz < 3; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos targetPos = getRelativePos(pos, dx, dy, dz, facing);


                    level.setBlock(targetPos, ModBlocks.TITANIUM_BLAST_FURNACE_PART.get().defaultBlockState(), 3);
                    BlockEntity be = level.getBlockEntity(targetPos);
                    if (be instanceof TitaniumBlastFurnacePartBlockEntity part) {
                        part.setControllerPos(pos);
                    }
                }
            }
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TitaniumBlastFurnaceBlockEntity controller) {
            controller.onAssembled();
        }

        return true;
    }

    public static BlockPos getRelativePos(BlockPos pos, int x, int y, int z, Direction facing) {


        return switch (facing) {
            case NORTH -> pos.offset(-x, y, z);
            case SOUTH -> pos.offset(x, y, -z);
            case WEST -> pos.offset(z, y, x);
            case EAST -> pos.offset(-z, y, -x);
            default -> pos.offset(x, y, z);
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return createTickerHelper(type, com.noozy.missionodyssey.registry.ModBlockEntities.TITANIUM_BLAST_FURNACE.get(), TitaniumBlastFurnaceBlockEntity::tick);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TitaniumBlastFurnaceBlockEntity(pos, state);
    }
}
