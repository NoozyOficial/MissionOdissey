package com.noozy.missionodyssey.registry;

import com.noozy.missionodyssey.MissionOdyssey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class ModDimensions {

    /**
     * Velocidade global de órbita (radianos por tick).
     * Um pouco mais rápida por padrão para que o jogador consiga
     * perceber o movimento dos astros no céu durante uma sessão de jogo.
     */
    public static final double GLOBAL_ORBIT_SPEED = 0.0005;

    public static final ResourceKey<Level> SPACE_KEY = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "space")
    );

    public static final ResourceKey<Level> MARS_KEY = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "mars")
    );

    // SOL: Central. Muito grande, mas não quebra a perspectiva do jogador.
    public static final double SUN_X = 0.0;
    public static final double SUN_Y = 64.0;
    public static final double SUN_Z = 0.0;
    public static final double SUN_RADIUS_BLOCKS = 1500.0;
    public static final double SUN_MODEL_RADIUS = 1.5;

    // TERRA: Nossa referência.
    // Trazida para mais perto: 10.000 blocos de distância do Sol.
    public static final double EARTH_ORBIT_RADIUS = 10_000.0;
    public static final double EARTH_START_ANGLE = 0.0;
    public static final double EARTH_Y = 64.0;
    public static final double EARTH_RADIUS_BLOCKS = 300.0;
    public static final double EARTH_MODEL_RADIUS = 1.5;

    // MARTE:
    // Órbita bem próxima da Terra (15.000 blocos) para renderizar bonito no céu.
    public static final double MARS_ORBIT_RADIUS = 15_000.0;
    public static final double MARS_START_ANGLE = Math.PI / 4;
    public static final double MARS_Y = 64.0;
    public static final double MARS_RADIUS_BLOCKS = 160.0;
    public static final double MARS_MODEL_RADIUS = 1.5;

    // SATURNO:
    // Distância considerável (30.000 blocos), mas perto o suficiente para ver os anéis.
    public static final double SATURN_ORBIT_RADIUS = 30_000.0;
    public static final double SATURN_START_ANGLE = Math.PI;
    public static final double SATURN_Y = 64.0;
    public static final double SATURN_RADIUS_BLOCKS = 900.0;
    public static final double SATURN_MODEL_RADIUS = 1.5;

    // BURACO NEGRO:
    // Reduzido o Z para 100.000. Fica lá no fundo como um "easter egg" perigoso.
    public static final double BLACK_HOLE_X = 0.0;
    public static final double BLACK_HOLE_Y = 64.0;
    public static final double BLACK_HOLE_Z = 100_000.0;
    public static final double BLACK_HOLE_RADIUS_BLOCKS = 200.0;
    public static final double BLACK_HOLE_MODEL_RADIUS = 1.5;
}