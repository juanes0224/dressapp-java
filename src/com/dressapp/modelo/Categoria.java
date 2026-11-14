package com.dressapp.modelo;

/**
 * Categorías de prenda. El orden de declaración es el orden de las capas del outfit:
 * superior -> exterior -> inferior -> calzado -> accesorio.
 */
public enum Categoria {
    SUPERIOR(false),
    EXTERIOR(true),
    INFERIOR(false),
    CALZADO(false),
    ACCESORIO(true);

    private final boolean opcional;

    Categoria(boolean opcional) {
        this.opcional = opcional;
    }

    /** Una capa opcional puede omitirse. La exterior deja de serlo cuando el clima exige abrigo. */
    public boolean esOpcional() {
        return opcional;
    }

    public String etiqueta() {
        return name().toLowerCase();
    }
}
