package com.noozy.missionodyssey.registry;

import com.noozy.missionodyssey.block.entity.TitaniumBlastFurnaceBlockEntity;
import com.noozy.missionodyssey.block.entity.TitaniumBlastFurnacePartBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class ModCapabilities {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModCapabilities::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {


        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.TITANIUM_BLAST_FURNACE.get(),
                (be, side) -> {
                    BlockState state = be.getBlockState();
                    if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) return null;
                    Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
                    return be.getItemHandler(side, facing);
                }
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.TITANIUM_BLAST_FURNACE.get(),
                (be, side) -> {
                    BlockState state = be.getBlockState();
                    if (!state.hasProperty(HorizontalDirectionalBlock.FACING)) return null;
                    Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
                    return be.getEnergyHandler(side, facing);
                }
        );


        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.TITANIUM_BLAST_FURNACE_PART.get(),
                (be, side) -> {
                    if (be.getControllerPos() == null || be.getLevel() == null) return null;
                    BlockEntity controllerBE = be.getLevel().getBlockEntity(be.getControllerPos());
                    if (!(controllerBE instanceof TitaniumBlastFurnaceBlockEntity controller)) return null;
                    BlockState controllerState = be.getLevel().getBlockState(be.getControllerPos());
                    if (!controllerState.hasProperty(HorizontalDirectionalBlock.FACING)) return null;
                    Direction facing = controllerState.getValue(HorizontalDirectionalBlock.FACING);
                    return controller.getItemHandler(side, facing);
                }
        );

        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.TITANIUM_BLAST_FURNACE_PART.get(),
                (be, side) -> {
                    if (be.getControllerPos() == null || be.getLevel() == null) return null;
                    BlockEntity controllerBE = be.getLevel().getBlockEntity(be.getControllerPos());
                    if (!(controllerBE instanceof TitaniumBlastFurnaceBlockEntity controller)) return null;
                    BlockState controllerState = be.getLevel().getBlockState(be.getControllerPos());
                    if (!controllerState.hasProperty(HorizontalDirectionalBlock.FACING)) return null;
                    Direction facing = controllerState.getValue(HorizontalDirectionalBlock.FACING);
                    return controller.getEnergyHandler(side, facing);
                }
        );
    }
}
