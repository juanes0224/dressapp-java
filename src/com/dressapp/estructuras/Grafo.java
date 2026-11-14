package com.dressapp.estructuras;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Grafo dirigido ponderado con lista de adyacencia. Solo acepta pesos >= 0, que es la
 * condición para que Dijkstra sea correcto.
 *
 * Complejidad: agregarNodo / agregarArista O(1) amortizado; vecinos O(1);
 * dijkstra O((V + E) log V) con el MinHeap propio (versión con borrado perezoso).
 */
public class Grafo {

    public static final class Arista {
        public final String destino;
        public final double peso;

        Arista(String destino, double peso) {
            this.destino = destino;
            this.peso = peso;
        }
    }

    /** Resultado de una búsqueda de camino. Si no hay camino, la lista está vacía y el costo es infinito. */
    public static final class Camino {
        public final double costo;
        public final List<String> nodos;

        public Camino(double costo, List<String> nodos) {
            this.costo = costo;
            this.nodos = Collections.unmodifiableList(new ArrayList<>(nodos));
        }

        public boolean existe() {
            return !nodos.isEmpty();
        }

        static Camino inexistente() {
            return new Camino(Double.POSITIVE_INFINITY, Collections.emptyList());
        }
    }

    private final Map<String, List<Arista>> adyacencia = new LinkedHashMap<>();
    private int numeroAristas;

    public void agregarNodo(String id) {
        adyacencia.computeIfAbsent(id, k -> new ArrayList<>());
    }

    public void agregarArista(String origen, String destino, double peso) {
        if (Double.isNaN(peso) || peso < 0) {
            throw new IllegalArgumentException("Peso negativo o inválido (" + peso + "): Dijkstra exige pesos >= 0.");
        }
        agregarNodo(origen);
        agregarNodo(destino);
        adyacencia.get(origen).add(new Arista(destino, peso));
        numeroAristas++;
    }

    public List<Arista> vecinos(String nodo) {
        List<Arista> lista = adyacencia.get(nodo);
        return lista == null ? Collections.emptyList() : Collections.unmodifiableList(lista);
    }

    public boolean contieneNodo(String nodo) {
        return adyacencia.containsKey(nodo);
    }

    public int numeroNodos() {
        return adyacencia.size();
    }

    public int numeroAristas() {
        return numeroAristas;
    }

    /** Peso de la arista origen -> destino, o NaN si no existe. */
    public double peso(String origen, String destino) {
        for (Arista a : vecinos(origen)) {
            if (a.destino.equals(destino)) return a.peso;
        }
        return Double.NaN;
    }

    /** Suma de los pesos de las aristas consecutivas del camino. */
    public double costoDe(List<String> nodos) {
        double total = 0;
        for (int i = 0; i + 1 < nodos.size(); i++) {
            total += peso(nodos.get(i), nodos.get(i + 1));
        }
        return total;
    }

    public static String claveArista(String origen, String destino) {
        return origen + "->" + destino;
    }

    public Camino dijkstra(String origen, String destino) {
        return dijkstra(origen, destino, Collections.emptySet(), Collections.emptySet());
    }

    /**
     * Dijkstra ignorando ciertos nodos y aristas (lo necesita el algoritmo de Yen para buscar
     * desvíos sin copiar el grafo).
     */
    public Camino dijkstra(String origen, String destino, Set<String> nodosExcluidos, Set<String> aristasExcluidas) {
        if (!contieneNodo(origen) || !contieneNodo(destino) || nodosExcluidos.contains(origen)) {
            return Camino.inexistente();
        }
        Map<String, Double> distancia = new HashMap<>();
        Map<String, String> anterior = new HashMap<>();
        Set<String> cerrados = new HashSet<>();
        MinHeap<String> cola = new MinHeap<>();

        distancia.put(origen, 0.0);
        cola.push(origen, 0.0);

        while (!cola.estaVacio()) {
            String actual = cola.pop();
            if (!cerrados.add(actual)) continue; // entrada vieja en el heap
            if (actual.equals(destino)) break;

            double distActual = distancia.get(actual);
            for (Arista a : adyacencia.get(actual)) {
                if (nodosExcluidos.contains(a.destino) || cerrados.contains(a.destino)) continue;
                if (aristasExcluidas.contains(claveArista(actual, a.destino))) continue;
                double nueva = distActual + a.peso;
                Double conocida = distancia.get(a.destino);
                if (conocida == null || nueva < conocida) {
                    distancia.put(a.destino, nueva);
                    anterior.put(a.destino, actual);
                    cola.push(a.destino, nueva);
                }
            }
        }

        if (!cerrados.contains(destino)) return Camino.inexistente();
        List<String> nodos = new ArrayList<>();
        for (String n = destino; n != null; n = anterior.get(n)) {
            nodos.add(n);
        }
        Collections.reverse(nodos);
        return new Camino(distancia.get(destino), nodos);
    }
}
