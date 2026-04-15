package com.noozy.missionodyssey.util;

import com.noozy.missionodyssey.registry.ModDimensions;
import net.minecraft.world.phys.Vec3;

/**
 * Calcula as posições orbitais dos planetas em tempo real.
 *
 * <p>Cada planeta orbita o Sol (fixo em 0, 0) no plano XZ com:
 * <ul>
 *   <li>{@code AnguloAtual = START_ANGLE + (gameTime * GLOBAL_ORBIT_SPEED * velocidadeRelativa)}</li>
 *   <li>{@code X = SolX + ORBIT_RADIUS * cos(AnguloAtual)}</li>
 *   <li>{@code Z = SolZ + ORBIT_RADIUS * sin(AnguloAtual)}</li>
 * </ul>
 *
 * <p>A velocidade relativa é derivada da Terceira Lei de Kepler simplificada:
 * planetas mais próximos do Sol orbitam mais rápido. Usamos a Terra como
 * referência (velocidade relativa = 1.0) e calculamos os demais com
 * {@code speedMultiplier = (EARTH_ORBIT_RADIUS / PLANET_ORBIT_RADIUS)^1.5}.
 */
public final class OrbitalMathHelper {

    // ───────────────────── Velocidades Relativas (Kepler simplificado) ─────────────────────
    // Terra é a referência: speed = 1.0
    // Velocidade ∝ (raioTerra / raioPlaneta)^1.5
    public static final double EARTH_SPEED_MULTIPLIER = 1.0;

    public static final double MARS_SPEED_MULTIPLIER = Math.pow(
            ModDimensions.EARTH_ORBIT_RADIUS / ModDimensions.MARS_ORBIT_RADIUS, 1.5
    );

    public static final double SATURN_SPEED_MULTIPLIER = Math.pow(
            ModDimensions.EARTH_ORBIT_RADIUS / ModDimensions.SATURN_ORBIT_RADIUS, 1.5
    );

    private OrbitalMathHelper() {}

    // ───────────────────── Cálculos Genéricos ─────────────────────────────────────────────

    /**
     * Calcula o ângulo orbital atual (em radianos) de um corpo celeste.
     *
     * @param gameTime       Tempo de jogo contínuo (level.getGameTime() + partialTick)
     * @param startAngle     Ângulo inicial em radianos
     * @param speedMultiplier Multiplicador de velocidade relativa do planeta
     * @return Ângulo atual em radianos
     */
    public static double getCurrentAngle(double gameTime, double startAngle, double speedMultiplier) {
        return startAngle + (gameTime * ModDimensions.GLOBAL_ORBIT_SPEED * speedMultiplier);
    }

    /**
     * Calcula a posição orbital atual de um corpo celeste.
     *
     * @param gameTime       Tempo de jogo contínuo (level.getGameTime() + partialTick)
     * @param orbitRadius    Raio da órbita em blocos
     * @param startAngle     Ângulo inicial em radianos
     * @param y              Altura fixa (eixo Y)
     * @param speedMultiplier Multiplicador de velocidade relativa do planeta
     * @return Vec3 com a posição atual (X, Y, Z) no espaço do mundo
     */
    public static Vec3 getOrbitalPosition(double gameTime, double orbitRadius,
                                           double startAngle, double y,
                                           double speedMultiplier) {
        double angle = getCurrentAngle(gameTime, startAngle, speedMultiplier);
        double x = ModDimensions.SUN_X + (orbitRadius * Math.cos(angle));
        double z = ModDimensions.SUN_Z + (orbitRadius * Math.sin(angle));
        return new Vec3(x, y, z);
    }

    // ───────────────────── Atalhos por Planeta ────────────────────────────────────────────

    /**
     * Retorna a posição atual da Terra no espaço.
     *
     * @param gameTime Tempo de jogo contínuo
     */
    public static Vec3 getEarthPosition(double gameTime) {
        return getOrbitalPosition(
                gameTime,
                ModDimensions.EARTH_ORBIT_RADIUS,
                ModDimensions.EARTH_START_ANGLE,
                ModDimensions.EARTH_Y,
                EARTH_SPEED_MULTIPLIER
        );
    }

    /**
     * Retorna a posição atual de Marte no espaço.
     *
     * @param gameTime Tempo de jogo contínuo
     */
    public static Vec3 getMarsPosition(double gameTime) {
        return getOrbitalPosition(
                gameTime,
                ModDimensions.MARS_ORBIT_RADIUS,
                ModDimensions.MARS_START_ANGLE,
                ModDimensions.MARS_Y,
                MARS_SPEED_MULTIPLIER
        );
    }

    /**
     * Retorna a posição atual de Saturno no espaço.
     *
     * @param gameTime Tempo de jogo contínuo
     */
    public static Vec3 getSaturnPosition(double gameTime) {
        return getOrbitalPosition(
                gameTime,
                ModDimensions.SATURN_ORBIT_RADIUS,
                ModDimensions.SATURN_START_ANGLE,
                ModDimensions.SATURN_Y,
                SATURN_SPEED_MULTIPLIER
        );
    }

    /**
     * Calcula a direção do Sol vista a partir de uma posição orbital.
     * Útil para iluminação direcional nos shaders de atmosfera.
     *
     * @param planetPos Posição atual do planeta
     * @return Vetor unitário apontando do planeta para o Sol
     */
    public static org.joml.Vector3f getSunDirection(Vec3 planetPos) {
        return new org.joml.Vector3f(
                (float) (ModDimensions.SUN_X - planetPos.x),
                (float) (ModDimensions.SUN_Y - planetPos.y),
                (float) (ModDimensions.SUN_Z - planetPos.z)
        ).normalize();
    }
}
