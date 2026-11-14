package com.dressapp.estructuras;

import com.dressapp.util.Texto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Trie para autocompletar nombres de prendas. Normaliza a minúsculas y sin tildes, así
 * "pantalon", "PANTALÓN" y "Pantalón" llegan al mismo nodo. Varias prendas pueden compartir
 * nombre: cada nodo terminal guarda la lista de ids que terminan ahí.
 *
 * Los hijos se guardan en un TreeMap para que el autocompletado salga en orden alfabético.
 * Complejidad (L = largo del texto, k = resultados, σ = tamaño del alfabeto):
 *   insertar / eliminar  O(L log σ)
 *   buscarPrefijo        O(L log σ + tamaño del subárbol recorrido), cortando al llegar a k
 */
public class Trie {

    private static final class Nodo {
        final Map<Character, Nodo> hijos = new TreeMap<>();
        final List<Integer> ids = new ArrayList<>();
    }

    private final Nodo raiz = new Nodo();
    private int cantidad;

    public void insertar(String palabra, int id) {
        Nodo actual = raiz;
        for (char c : Texto.normalizar(palabra).toCharArray()) {
            actual = actual.hijos.computeIfAbsent(c, k -> new Nodo());
        }
        if (!actual.ids.contains(id)) {
            actual.ids.add(id);
            cantidad++;
        }
    }

    /** Quita solo el id indicado; si el nodo queda sin ids ni hijos, se poda hacia la raíz. */
    public boolean eliminar(String palabra, int id) {
        String norm = Texto.normalizar(palabra);
        List<Nodo> camino = new ArrayList<>();
        Nodo actual = raiz;
        camino.add(actual);
        for (char c : norm.toCharArray()) {
            actual = actual.hijos.get(c);
            if (actual == null) return false;
            camino.add(actual);
        }
        if (!actual.ids.remove(Integer.valueOf(id))) return false;
        cantidad--;

        for (int i = camino.size() - 1; i > 0; i--) {
            Nodo nodo = camino.get(i);
            if (!nodo.ids.isEmpty() || !nodo.hijos.isEmpty()) break;
            camino.get(i - 1).hijos.remove(norm.charAt(i - 1));
        }
        return true;
    }

    /** Hasta `limite` ids cuyo nombre empieza por `prefijo`, en orden alfabético. */
    public List<Integer> buscarPrefijo(String prefijo, int limite) {
        List<Integer> resultado = new ArrayList<>();
        if (limite <= 0) return resultado;
        Nodo actual = raiz;
        for (char c : Texto.normalizar(prefijo).toCharArray()) {
            actual = actual.hijos.get(c);
            if (actual == null) return resultado;
        }
        recolectar(actual, resultado, limite);
        return resultado;
    }

    private void recolectar(Nodo nodo, List<Integer> resultado, int limite) {
        for (int id : nodo.ids) {
            if (resultado.size() >= limite) return;
            resultado.add(id);
        }
        for (Nodo hijo : nodo.hijos.values()) {
            if (resultado.size() >= limite) return;
            recolectar(hijo, resultado, limite);
        }
    }

    /** Cantidad de pares (palabra, id) guardados. */
    public int tamano() {
        return cantidad;
    }

    public boolean estaVacio() {
        return cantidad == 0;
    }
}
