package com.noozy.missionodyssey.block;

import com.mojang.serialization.MapCodec;
import com.noozy.missionodyssey.block.entity.TitaniumBlastFurnaceBlockEntity;
import com.noozy.missionodyssey.block.entity.TitaniumBlastFurnacePartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class TitaniumBlastFurnacePart extends BaseEntityBlock {
    public static final MapCodec<TitaniumBlastFurnacePart> CODEC = simpleCodec(TitaniumBlastFurnacePart::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public TitaniumBlastFurnacePart(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public float getShadeBrightness(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TitaniumBlastFurnacePartBlockEntity part) {
            BlockPos controllerPos = part.getControllerPos();
            if (controllerPos != null) {
                BlockEntity controllerBE = level.getBlockEntity(controllerPos);
                if (controllerBE instanceof TitaniumBlastFurnaceBlockEntity controller) {
                    if (stack.is(com.noozy.missionodyssey.registry.ModItems.WRENCH.get())) {
                        if (hitResult.getDirection() == level.getBlockState(controllerPos).getValue(HorizontalDirectionalBlock.FACING) && pos.equals(controllerPos)) {
                             return ItemInteractionResult.FAIL;
                        }

                        if (!level.isClientSide) {
                            controller.togglePort(pos, hitResult.getDirection());
                        }
                        return ItemInteractionResult.sidedSuccess(level.isClientSide);
                    }
                }
                BlockState controllerState = level.getBlockState(controllerPos);
                return controllerState.useItemOn(stack, level, player, hand, hitResult.withPosition(controllerPos));
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TitaniumBlastFurnacePartBlockEntity part) {
            BlockPos controllerPos = part.getControllerPos();
            if (controllerPos != null) {
                BlockState controllerState = level.getBlockState(controllerPos);
                return controllerState.useWithoutItem(level, player, hitResult.withPosition(controllerPos));
            }
        }
        return super.useWithoutItem(state, level, pos, player, hitResult);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TitaniumBlastFurnacePartBlockEntity part) {
                part.breakMultiblock();
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TitaniumBlastFurnacePartBlockEntity(pos, state);
    }
}
