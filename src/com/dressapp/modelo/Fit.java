package com.dressapp.modelo;

/** Ajuste de la prenda. El volumen va de 1 (slim) a 4 (ultra baggy). */
public enum Fit {
    SLIM(1),
    REGULAR(2),
    BAGGY(3),
    ULTRA_BAGGY(4);

    private final int volumen;

    Fit(int volumen) {
        this.volumen = volumen;
    }

    public int getVolumen() {
        return volumen;
    }

    public String etiqueta() {
        return name().toLowerCase().replace('_', ' ');
    }
}
