package com.noozy.missionodyssey.registry;

import com.noozy.missionodyssey.MissionOdyssey;
import com.noozy.missionodyssey.block.entity.TitaniumBlastFurnaceBlockEntity;
import com.noozy.missionodyssey.block.entity.TitaniumBlastFurnacePartBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MissionOdyssey.MODID);

    public static final Supplier<BlockEntityType<TitaniumBlastFurnaceBlockEntity>> TITANIUM_BLAST_FURNACE =
            BLOCK_ENTITIES.register("titanium_blast_furnace",
                    () -> BlockEntityType.Builder.of(TitaniumBlastFurnaceBlockEntity::new, ModBlocks.TITANIUM_BLAST_FURNACE.get()).build(null));

    public static final Supplier<BlockEntityType<TitaniumBlastFurnacePartBlockEntity>> TITANIUM_BLAST_FURNACE_PART =
            BLOCK_ENTITIES.register("titanium_blast_furnace_part",
                    () -> BlockEntityType.Builder.of(TitaniumBlastFurnacePartBlockEntity::new, ModBlocks.TITANIUM_BLAST_FURNACE_PART.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
