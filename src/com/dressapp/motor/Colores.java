package com.dressapp.motor;

import com.dressapp.modelo.Prenda;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Rueda de color HSL y compatibilidad de color entre dos prendas. Funciones puras. */
public final class Colores {

    private Colores() {}

    /** Relación de color entre dos prendas, con el puntaje que le asigna el plan. */
    public enum Relacion {
        NEUTRO(Config.COLOR_NEUTRO, "un color neutro combina con todo"),
        MISMO_MATIZ(Config.COLOR_MISMO_MATIZ, "comparten el mismo matiz"),
        ANALOGOS(Config.COLOR_ANALOGOS, "son colores análogos en la rueda"),
        COMPLEMENTARIOS(Config.COLOR_COMPLEMENTARIOS, "son colores complementarios"),
        OTRO(Config.COLOR_OTRO, "sus colores no tienen una relación clara");

        public final double valor;
        public final String descripcion;

        Relacion(double valor, String descripcion) {
            this.valor = valor;
            this.descripcion = descripcion;
        }
    }

    private static final Set<String> NEUTROS = new HashSet<>(Arrays.asList(
            "negro", "blanco", "gris", "beige", "azul_marino"));

    /** {matiz en [0,360), saturación en [0,1], luminosidad en [0,1]} */
    public static double[] hexAHsl(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        double r = Integer.parseInt(h.substring(0, 2), 16) / 255.0;
        double g = Integer.parseInt(h.substring(2, 4), 16) / 255.0;
        double b = Integer.parseInt(h.substring(4, 6), 16) / 255.0;
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double l = (max + min) / 2;
        double d = max - min;
        if (d == 0) return new double[]{0, 0, l};
        double s = d / (1 - Math.abs(2 * l - 1));
        double matiz;
        if (max == r) matiz = 60 * (((g - b) / d) % 6);
        else if (max == g) matiz = 60 * (((b - r) / d) + 2);
        else matiz = 60 * (((r - g) / d) + 4);
        if (matiz < 0) matiz += 360;
        return new double[]{matiz, s, l};
    }

    /** Distancia angular mínima entre dos matices, en [0, 180]. */
    public static double deltaMatiz(double h1, double h2) {
        double d = Math.abs(h1 - h2) % 360;
        return Math.min(d, 360 - d);
    }

    /** Neutro por nombre (negro, blanco, gris, beige, azul marino) o por tener saturación casi nula. */
    public static boolean esNeutro(Prenda p) {
        return NEUTROS.contains(p.getColorNombre()) || hexAHsl(p.getColorHex())[1] < Config.SATURACION_NEUTRA;
    }

    public static Relacion relacion(Prenda p, Prenda q) {
        if (esNeutro(p) || esNeutro(q)) return Relacion.NEUTRO;
        double delta = deltaMatiz(hexAHsl(p.getColorHex())[0], hexAHsl(q.getColorHex())[0]);
        if (delta <= Config.UMBRAL_MISMO_MATIZ) return Relacion.MISMO_MATIZ;
        if (delta <= Config.UMBRAL_ANALOGOS) return Relacion.ANALOGOS;
        if (Math.abs(delta - 180) <= Config.TOLERANCIA_COMPLEMENTARIOS) return Relacion.COMPLEMENTARIOS;
        return Relacion.OTRO;
    }

    /** color(p,q) en [0,1]. Es simétrica porque deltaMatiz y esNeutro lo son. */
    public static double compatibilidad(Prenda p, Prenda q) {
        return relacion(p, q).valor;
    }
}
