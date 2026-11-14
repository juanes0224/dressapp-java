package com.dressapp.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Un outfit propuesto: prendas en orden de capa, puntaje G, desglose y explicación. */
public class Outfit {

    private static int siguienteId = 1;

    private final int id;
    private final Evento evento;
    private final List<Prenda> prendas;
    private final double puntaje;
    private final Map<String, Double> desglose;
    private final List<Integer> calificaciones = new ArrayList<>();
    private String explicacion = "";
    private boolean favorito;

    public Outfit(Evento evento, List<Prenda> prendas, double puntaje, Map<String, Double> desglose) {
        this.id = siguienteId++;
        this.evento = evento;
        this.prendas = Collections.unmodifiableList(new ArrayList<>(prendas));
        this.puntaje = puntaje;
        this.desglose = Collections.unmodifiableMap(new LinkedHashMap<>(desglose));
    }

    public int getId() { return id; }
    public Evento getEvento() { return evento; }
    public List<Prenda> getPrendas() { return prendas; }
    public double getPuntaje() { return puntaje; }
    public Map<String, Double> getDesglose() { return desglose; }
    public List<Integer> getCalificaciones() { return Collections.unmodifiableList(calificaciones); }
    public String getExplicacion() { return explicacion; }
    public void setExplicacion(String explicacion) { this.explicacion = explicacion; }
    public boolean isFavorito() { return favorito; }
    public void setFavorito(boolean favorito) { this.favorito = favorito; }

    public void agregarCalificacion(int valor) {
        calificaciones.add(valor);
    }

    public double valorDesglose(String clave) {
        Double v = desglose.get(clave);
        return v == null ? Double.NaN : v;
    }

    /** Cuántas prendas tiene en común con otro outfit (regla de diversidad). */
    public int prendasCompartidasCon(Outfit otro) {
        int compartidas = 0;
        for (Prenda p : prendas) {
            for (Prenda q : otro.prendas) {
                if (p.getId() == q.getId()) {
                    compartidas++;
                    break;
                }
            }
        }
        return compartidas;
    }

    /** Identifica la combinación de prendas sin importar el id del outfit. "1-11-21" */
    public String claveCombinacion() {
        return prendas.stream().map(p -> String.valueOf(p.getId())).sorted().collect(Collectors.joining("-"));
    }

    @Override
    public String toString() {
        return "[" + evento.etiqueta() + "] G=" + String.format("%.3f", puntaje) + ": "
                + prendas.stream().map(Prenda::getNombre).collect(Collectors.joining(" + "));
    }
}
