package com.dressapp.modelo;

import com.dressapp.util.Texto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Cuestionario inicial del usuario. Alimenta el componente perfil(p) de la puntuación. */
public class Perfil {

    /** Lista cerrada de estilos que ofrece el cuestionario. */
    public static final List<String> ESTILOS_DISPONIBLES = Collections.unmodifiableList(Arrays.asList(
            "clasico", "casual", "deportivo", "elegante", "urbano", "minimalista"));

    private String ciudad = "";
    private List<String> estilos = new ArrayList<>();
    private List<String> coloresPreferidos = new ArrayList<>();
    private List<String> coloresEvitados = new ArrayList<>();
    private Fit fitPreferido; // puede ser null: sin preferencia
    private List<Evento> eventosFrecuentes = new ArrayList<>();

    public String getCiudad() { return ciudad; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad == null ? "" : ciudad.trim(); }

    public List<String> getEstilos() { return estilos; }
    public void setEstilos(List<String> estilos) { this.estilos = new ArrayList<>(estilos); }

    public List<String> getColoresPreferidos() { return coloresPreferidos; }
    public void setColoresPreferidos(List<String> colores) { this.coloresPreferidos = normalizarColores(colores); }

    public List<String> getColoresEvitados() { return coloresEvitados; }
    public void setColoresEvitados(List<String> colores) { this.coloresEvitados = normalizarColores(colores); }

    public Fit getFitPreferido() { return fitPreferido; }
    public void setFitPreferido(Fit fitPreferido) { this.fitPreferido = fitPreferido; }

    public List<Evento> getEventosFrecuentes() { return eventosFrecuentes; }
    public void setEventosFrecuentes(List<Evento> eventos) { this.eventosFrecuentes = new ArrayList<>(eventos); }

    public boolean prefiereColor(String colorNombre) {
        return coloresPreferidos.contains(Texto.aClave(colorNombre));
    }

    public boolean evitaColor(String colorNombre) {
        return coloresEvitados.contains(Texto.aClave(colorNombre));
    }

    private static List<String> normalizarColores(List<String> colores) {
        List<String> resultado = new ArrayList<>();
        for (String c : colores) {
            String clave = Texto.aClave(c);
            if (!clave.isEmpty() && !resultado.contains(clave)) resultado.add(clave);
        }
        return resultado;
    }
}
