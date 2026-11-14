package com.dressapp.estructuras;

import com.dressapp.modelo.Categoria;
import com.dressapp.util.Texto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Árbol de categorías de tres niveles: raíz "prenda" -> categoría -> subtipo.
 * Cada nodo guarda sus hijos en una tabla hash, así bajar un nivel cuesta O(1).
 *
 * Complejidad: esSubtipoDe O(profundidad) = O(1); categoriaDe O(número de categorías);
 * subtipos O(hijos de la categoría); recorridoDFS O(número de nodos).
 */
public class ArbolCategorias {

    private static final class Nodo {
        final String nombre;
        final Map<String, Nodo> hijos = new LinkedHashMap<>();

        Nodo(String nombre) {
            this.nombre = nombre;
        }
    }

    private final Nodo raiz = new Nodo("prenda");

    public ArbolCategorias() {
        agregar(Categoria.SUPERIOR, "camiseta", "camisa", "polo", "buzo", "blusa");
        agregar(Categoria.EXTERIOR, "chaqueta", "blazer", "abrigo", "chaleco");
        agregar(Categoria.INFERIOR, "jean", "pantalon", "sudadera", "short", "falda");
        agregar(Categoria.CALZADO, "tenis", "zapato_formal", "bota", "sandalia");
        agregar(Categoria.ACCESORIO, "reloj", "gorra", "cinturon", "collar", "bolso");
    }

    private void agregar(Categoria categoria, String... subtipos) {
        Nodo nodoCategoria = raiz.hijos.computeIfAbsent(categoria.name(), Nodo::new);
        for (String s : subtipos) {
            String clave = Texto.aClave(s);
            nodoCategoria.hijos.put(clave, new Nodo(clave));
        }
    }

    public boolean esSubtipoDe(String subtipo, Categoria categoria) {
        Nodo nodoCategoria = raiz.hijos.get(categoria.name());
        return nodoCategoria != null && nodoCategoria.hijos.containsKey(Texto.aClave(subtipo));
    }

    /** Categoría a la que pertenece el subtipo, o null si no existe. */
    public Categoria categoriaDe(String subtipo) {
        String clave = Texto.aClave(subtipo);
        for (Nodo nodoCategoria : raiz.hijos.values()) {
            if (nodoCategoria.hijos.containsKey(clave)) return Categoria.valueOf(nodoCategoria.nombre);
        }
        return null;
    }

    public List<String> subtipos(Categoria categoria) {
        Nodo nodoCategoria = raiz.hijos.get(categoria.name());
        return nodoCategoria == null ? new ArrayList<>() : new ArrayList<>(nodoCategoria.hijos.keySet());
    }

    public int cantidadSubtipos() {
        int total = 0;
        for (Nodo c : raiz.hijos.values()) total += c.hijos.size();
        return total;
    }

    /** Recorrido en profundidad (preorden). Cada línea va sangrada según su nivel. */
    public List<String> recorridoDFS() {
        List<String> lineas = new ArrayList<>();
        dfs(raiz, 0, lineas);
        return lineas;
    }

    private void dfs(Nodo nodo, int nivel, List<String> lineas) {
        StringBuilder sangria = new StringBuilder();
        for (int i = 0; i < nivel; i++) sangria.append("    ");
        String nombre = nivel == 1 ? nodo.nombre.toLowerCase() : Texto.legible(nodo.nombre);
        lineas.add(sangria + (nivel == 0 ? "" : "|- ") + nombre);
        for (Nodo hijo : nodo.hijos.values()) {
            dfs(hijo, nivel + 1, lineas);
        }
    }
}
