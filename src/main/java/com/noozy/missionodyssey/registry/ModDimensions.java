package com.noozy.missionodyssey.registry;

import com.noozy.missionodyssey.MissionOdyssey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class ModDimensions {

    public static final ResourceKey<Level> SPACE_KEY = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "space")
    );

    public static final ResourceKey<Level> MARS_KEY = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(MissionOdyssey.MODID, "mars")
    );

    // SATURNO: Gigante gasoso (4x o tamanho da Terra no jogo). Longe e em diagonal.
    public static final double SATURN_X = -25_000.0;
    public static final double SATURN_Y = 64.0;
    public static final double SATURN_Z = 20_000.0;
    public static final double SATURN_RADIUS_BLOCKS = 1200.0;
    public static final double SATURN_MODEL_RADIUS = 1.5;

    // MARTE: Metade do tamanho da Terra. O vizinho mais próximo.
    public static final double MARS_X = 12_000.0;
    public static final double MARS_Y = 64.0;
    public static final double MARS_Z = -5_000.0;
    public static final double MARS_RADIUS_BLOCKS = 150.0;
    public static final double MARS_MODEL_RADIUS = 1.5;

    // TERRA: O ponto zero e a nossa referência de tamanho.
    public static final double EARTH_X = 0.0;
    public static final double EARTH_Y = 64.0;
    public static final double EARTH_Z = 0.0;
    public static final double EARTH_RADIUS_BLOCKS = 300.0;
    public static final double EARTH_MODEL_RADIUS = 1.5;

    // SOL: Colossal (10x a Terra), posicionado bem longe no centro do sistema.
    // Vai dominar o horizonte, mas não demora 50 horas para chegar.
    public static final double SUN_X = 0.0;
    public static final double SUN_Y = 64.0;
    public static final double SUN_Z = -50_000.0;
    public static final double SUN_RADIUS_BLOCKS = 3000.0;
    public static final double SUN_MODEL_RADIUS = 1.5;

    // BURACO NEGRO: Singularidade compacta com disco de acreção.
    // Posição de teste em 10000, 64, 10000.
    // MODEL_RADIUS é o raio do cubo unitário do shader (1.5 como os outros planetas).
    // BH_RADIUS_BLOCKS define o tamanho angular no espaço (menor que a Terra para parecer distante).
    public static final double BLACK_HOLE_X = 10_000.0;
    public static final double BLACK_HOLE_Y = 64.0;
    public static final double BLACK_HOLE_Z = 10_000.0;
    public static final double BLACK_HOLE_RADIUS_BLOCKS = 200.0;
    public static final double BLACK_HOLE_MODEL_RADIUS = 1.5;
}