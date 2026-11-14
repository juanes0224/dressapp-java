package com.dressapp.modelo;

import com.dressapp.util.Texto;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Una prenda del closet. Valida sus propios rangos; la coherencia categoría-subtipo la valida
 * el repositorio con el árbol de categorías.
 */
public final class Prenda {

    private static final Pattern HEX = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private final int id;
    private final Categoria categoria;
    private final String subtipo;
    private String nombre;
    private String colorHex;
    private String colorNombre;
    private Fit fit;
    private int formalidad;
    private int abrigo;
    private EnumSet<Evento> eventosAptos;

    public Prenda(int id, String nombre, Categoria categoria, String subtipo, String colorHex,
                  String colorNombre, Fit fit, int formalidad, int abrigo, Collection<Evento> eventosAptos) {
        if (categoria == null) throw new IllegalArgumentException("La categoría es obligatoria.");
        if (subtipo == null || subtipo.trim().isEmpty()) throw new IllegalArgumentException("El subtipo es obligatorio.");
        this.id = id;
        this.categoria = categoria;
        this.subtipo = Texto.aClave(subtipo);
        setNombre(nombre);
        setColor(colorHex, colorNombre);
        setFit(fit);
        setFormalidad(formalidad);
        setAbrigo(abrigo);
        setEventosAptos(eventosAptos);
    }

    public int getId() { return id; }
    public Categoria getCategoria() { return categoria; }
    public String getSubtipo() { return subtipo; }
    public String getNombre() { return nombre; }
    public String getColorHex() { return colorHex; }
    public String getColorNombre() { return colorNombre; }
    public Fit getFit() { return fit; }
    public int getFormalidad() { return formalidad; }
    public int getAbrigo() { return abrigo; }
    public Set<Evento> getEventosAptos() { return Collections.unmodifiableSet(eventosAptos); }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) throw new IllegalArgumentException("El nombre no puede estar vacío.");
        this.nombre = nombre.trim();
    }

    public void setColor(String colorHex, String colorNombre) {
        if (colorHex == null || !HEX.matcher(colorHex.trim()).matches()) {
            throw new IllegalArgumentException("Color hex inválido: '" + colorHex + "' (formato esperado #RRGGBB).");
        }
        if (colorNombre == null || colorNombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del color es obligatorio.");
        }
        this.colorHex = colorHex.trim().toUpperCase();
        this.colorNombre = Texto.aClave(colorNombre);
    }

    public void setFit(Fit fit) {
        if (fit == null) throw new IllegalArgumentException("El fit es obligatorio.");
        this.fit = fit;
    }

    public void setFormalidad(int formalidad) {
        this.formalidad = validarRango("formalidad", formalidad);
    }

    public void setAbrigo(int abrigo) {
        this.abrigo = validarRango("abrigo", abrigo);
    }

    public void setEventosAptos(Collection<Evento> eventos) {
        this.eventosAptos = (eventos == null || eventos.isEmpty())
                ? EnumSet.noneOf(Evento.class)
                : EnumSet.copyOf(eventos);
    }

    private static int validarRango(String campo, int valor) {
        if (valor < 1 || valor > 5) {
            throw new IllegalArgumentException("La " + campo + " debe estar entre 1 y 5 (llegó " + valor + ").");
        }
        return valor;
    }

    /** Descripción corta: "Camiseta blanca (camiseta, blanco, regular)". */
    public String descripcionCorta() {
        return nombre + " (" + Texto.legible(subtipo) + ", " + Texto.legible(colorNombre) + ", " + fit.etiqueta() + ")";
    }

    @Override
    public String toString() {
        return "#" + id + " " + descripcionCorta();
    }
}
