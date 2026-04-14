package com.noozy.missionodyssey.registry;

import com.noozy.missionodyssey.MissionOdyssey;
import com.noozy.missionodyssey.entity.SpaceshipEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MissionOdyssey.MODID);

    public static final Supplier<EntityType<SpaceshipEntity>> SPACESHIP = ENTITIES.register("spaceship",
            () -> EntityType.Builder.<SpaceshipEntity>of(SpaceshipEntity::new, MobCategory.MISC)
                    .sized(3.5f, 1.8f)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build("spaceship")
    );

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
