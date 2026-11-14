package com.dressapp.modelo;

/** No quedó ninguna prenda apta en una capa obligatoria después del filtro duro. */
public class CapaVaciaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Categoria categoria;

    public CapaVaciaException(Categoria categoria, Evento evento) {
        super("Te falta una prenda de la capa " + categoria.etiqueta() + " apta para " + evento.enFrase() + ".");
        this.categoria = categoria;
    }

    public Categoria getCategoria() {
        return categoria;
    }
}
