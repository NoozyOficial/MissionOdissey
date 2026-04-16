package com.noozy.missionodyssey.block.entity;

import com.noozy.missionodyssey.block.TitaniumBlastFurnaceBlock;
import com.noozy.missionodyssey.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TitaniumBlastFurnacePartBlockEntity extends BlockEntity {
    private BlockPos controllerPos;

    public TitaniumBlastFurnacePartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TITANIUM_BLAST_FURNACE_PART.get(), pos, state);
    }

    public void setControllerPos(BlockPos controllerPos) {
        this.controllerPos = controllerPos;
        setChanged();
    }

    public BlockPos getControllerPos() {
        return controllerPos;
    }

    public void breakMultiblock() {
        if (controllerPos != null && level != null) {
            BlockState controllerState = level.getBlockState(controllerPos);
            if (controllerState.getBlock() instanceof TitaniumBlastFurnaceBlock && controllerState.getValue(TitaniumBlastFurnaceBlock.ASSEMBLED)) {
                level.setBlock(controllerPos, controllerState.setValue(TitaniumBlastFurnaceBlock.ASSEMBLED, false), 3);
                Direction facing = controllerState.getValue(HorizontalDirectionalBlock.FACING);

                for (int dy = 0; dy < 3; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = 0; dz < 3; dz++) {
                            BlockPos targetPos = TitaniumBlastFurnaceBlock.getRelativePos(controllerPos, dx, dy, dz, facing);
                            if (targetPos.equals(this.getBlockPos())) continue;
                            if (dx == 0 && dy == 0 && dz == 0) continue;

                            if (level.getBlockState(targetPos).is(this.getBlockState().getBlock())) {
                                level.destroyBlock(targetPos, false);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (controllerPos != null) {
            tag.put("controller", NbtUtils.writeBlockPos(controllerPos));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("controller")) {
            controllerPos = NbtUtils.readBlockPos(tag, "controller").orElse(null);
        }
    }
}
