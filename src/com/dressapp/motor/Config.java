package com.dressapp.motor;

/**
 * Constantes del motor (secciones 2.4 y 2.5 del plan). Son valores iniciales ajustables,
 * no verdades fijas.
 */
public final class Config {

    private Config() {}

    // ---- u(p,e) = 0.6*form + 0.2*clima + 0.2*perfil
    public static final double PESO_FORM = 0.6;
    public static final double PESO_CLIMA = 0.2;
    public static final double PESO_PERFIL = 0.2;

    // ---- Filtro duro: se descarta la prenda si form(p,e) < 0.5 (más de 2 niveles de diferencia)
    public static final double FORM_MINIMA = 0.5;

    // ---- Regla de lluvia (probabilidad >= 60 %)
    public static final double PENALIZACION_SANDALIA_LLUVIA = 0.5;
    public static final double BONO_CALZADO_CERRADO_LLUVIA = 0.1;

    // ---- c_reglas(p,q) = 0.6*color + 0.4*fit
    public static final double PESO_COLOR = 0.6;
    public static final double PESO_FIT = 0.4;

    // ---- Rueda de color (diferencia de matiz en grados) y sus puntajes
    public static final double UMBRAL_MISMO_MATIZ = 15;
    public static final double UMBRAL_ANALOGOS = 45;
    public static final double TOLERANCIA_COMPLEMENTARIOS = 20;
    public static final double SATURACION_NEUTRA = 0.10; // por debajo, el color se trata como neutro (gris)
    public static final double COLOR_NEUTRO = 1.00;
    public static final double COLOR_MISMO_MATIZ = 0.80;
    public static final double COLOR_ANALOGOS = 0.85;
    public static final double COLOR_COMPLEMENTARIOS = 0.70;
    public static final double COLOR_OTRO = 0.40;

    // ---- Compatibilidad de fit entre superior e inferior
    public static final double FIT_EQUILIBRADO = 1.0;
    public static final double FIT_AMBOS_HOLGADOS_RELAJADO = 0.9;
    public static final double FIT_AMBOS_HOLGADOS_FORMAL = 0.4;
    public static final double FIT_AMBOS_SLIM = 0.8;
    public static final double FIT_OTRO = 0.6; // no fijado en el plan: decisión de diseño

    // ---- Costos del grafo por capas
    public static final double ALFA = 0.5;   // peso de (1 - c) en cada arista
    public static final double BETA = 0.5;   // peso de (1 - u) del nodo destino
    public static final double GAMMA = 0.10; // costo de calzado -> FIN (outfit sin accesorio)

    // ---- G(outfit) = DELTA*promedio(u) + EPSILON*promedio(c)
    public static final double DELTA = 0.5;
    public static final double EPSILON = 0.5;

    // ---- Yen, re-ranking y diversidad
    public static final int K_CANDIDATOS = 10;
    public static final int K_MAXIMO = 80;
    public static final int MAX_PRENDAS_COMPARTIDAS = 2;
    public static final int N_RECOMENDACIONES = 3;

    // ---- Aprendizaje: w <- (1 - TASA)*w + TASA*r_norm ; lambda = n / (n + CONFIANZA)
    public static final double TASA_APRENDIZAJE = 0.3;
    public static final double CONFIANZA = 3.0;
}
