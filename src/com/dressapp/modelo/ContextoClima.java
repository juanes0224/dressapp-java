package com.dressapp.modelo;

/**
 * Clima del momento de la recomendación. No consulta ninguna API: la temperatura y la lluvia
 * las escribe el usuario. Sin clima, clima(p) vale 1.0 para todas las prendas.
 */
public final class ContextoClima {

    private static final ContextoClima SIN_CLIMA = new ContextoClima(false, 0, false);

    private final boolean activo;
    private final double temperaturaC;
    private final boolean lluviaProbable;

    private ContextoClima(boolean activo, double temperaturaC, boolean lluviaProbable) {
        this.activo = activo;
        this.temperaturaC = temperaturaC;
        this.lluviaProbable = lluviaProbable;
    }

    public static ContextoClima sinClima() {
        return SIN_CLIMA;
    }

    /** @param lluviaProbable true si la probabilidad de lluvia es de 60 % o más. */
    public static ContextoClima de(double temperaturaC, boolean lluviaProbable) {
        return new ContextoClima(true, temperaturaC, lluviaProbable);
    }

    public boolean estaActivo() { return activo; }
    public double getTemperaturaC() { return temperaturaC; }
    public boolean esLluviaProbable() { return activo && lluviaProbable; }

    /** abrigo_requerido(T): T >= 24 -> 1 | 18-24 -> 2 | 12-18 -> 3 | 6-12 -> 4 | < 6 -> 5 */
    public int abrigoRequerido() {
        if (temperaturaC >= 24) return 1;
        if (temperaturaC >= 18) return 2;
        if (temperaturaC >= 12) return 3;
        if (temperaturaC >= 6) return 4;
        return 5;
    }

    /** Con abrigo requerido >= 4 la capa exterior se vuelve obligatoria. */
    public boolean exigeExterior() {
        return activo && abrigoRequerido() >= 4;
    }

    /** "8 °C" o "8,5 °C": sin decimales cuando la temperatura es entera. */
    public String temperaturaTexto() {
        return temperaturaC == Math.rint(temperaturaC)
                ? String.format("%.0f °C", temperaturaC)
                : String.format("%.1f °C", temperaturaC);
    }

    @Override
    public String toString() {
        if (!activo) return "sin clima";
        return temperaturaTexto() + (lluviaProbable ? ", lluvia probable" : "");
    }
}
