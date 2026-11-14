package com.dressapp.datos;

import com.dressapp.estructuras.ArbolCategorias;
import com.dressapp.estructuras.Trie;
import com.dressapp.modelo.Categoria;
import com.dressapp.modelo.Evento;
import com.dressapp.modelo.Fit;
import com.dressapp.modelo.Prenda;
import com.dressapp.util.Texto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Closet en memoria. Tabla hash por id, trie por nombre y tablas hash por etiqueta
 * (categoría, color, fit, evento). Todos los índices se actualizan en cada alta, edición y baja,
 * así el autocompletado y los filtros reflejan los cambios al instante.
 *
 * Complejidad: obtener O(1); agregar / eliminar O(L + eventos); filtrar O(tamaño del índice más chico).
 */
public class Repositorio {

    private final ArbolCategorias arbol;
    private final Map<Integer, Prenda> porId = new LinkedHashMap<>();
    private final Trie trieNombres = new Trie();
    private final Map<Categoria, Set<Integer>> porCategoria = new EnumMap<>(Categoria.class);
    private final Map<String, Set<Integer>> porColor = new HashMap<>();
    private final Map<Fit, Set<Integer>> porFit = new EnumMap<>(Fit.class);
    private final Map<Evento, Set<Integer>> porEvento = new EnumMap<>(Evento.class);
    private int siguienteId = 1;

    public Repositorio(ArbolCategorias arbol) {
        this.arbol = arbol;
    }

    public ArbolCategorias getArbol() {
        return arbol;
    }

    public Prenda agregar(String nombre, Categoria categoria, String subtipo, String colorHex, String colorNombre,
                          Fit fit, int formalidad, int abrigo, Collection<Evento> eventosAptos) {
        if (!arbol.esSubtipoDe(subtipo, categoria)) {
            throw new IllegalArgumentException("El subtipo '" + subtipo + "' no pertenece a la categoría "
                    + categoria.etiqueta() + ". Opciones: " + arbol.subtipos(categoria));
        }
        Prenda p = new Prenda(siguienteId, nombre, categoria, subtipo, colorHex, colorNombre,
                fit, formalidad, abrigo, eventosAptos);
        siguienteId++;
        porId.put(p.getId(), p);
        indexar(p);
        return p;
    }

    /** Aplica un cambio a la prenda y reconstruye sus índices. Si el cambio es inválido, no se modifica nada. */
    public Prenda editar(int id, Consumer<Prenda> cambio) {
        Prenda p = porId.get(id);
        if (p == null) throw new IllegalArgumentException("No existe una prenda con id " + id + ".");
        Prenda respaldo = copiar(p);
        desindexar(p);
        try {
            cambio.accept(p);
        } catch (RuntimeException e) {
            restaurar(p, respaldo);
            indexar(p);
            throw e;
        }
        indexar(p);
        return p;
    }

    public boolean eliminar(int id) {
        Prenda p = porId.remove(id);
        if (p == null) return false;
        desindexar(p);
        return true;
    }

    public Prenda obtener(int id) {
        return porId.get(id);
    }

    public List<Prenda> listar() {
        return new ArrayList<>(porId.values());
    }

    public int tamano() {
        return porId.size();
    }

    public List<Prenda> buscarPorNombre(String prefijo, int limite) {
        List<Prenda> resultado = new ArrayList<>();
        for (int id : trieNombres.buscarPrefijo(prefijo, limite)) {
            resultado.add(porId.get(id));
        }
        return resultado;
    }

    /** Intersección de los índices pedidos. Un filtro en null no restringe. */
    public List<Prenda> filtrar(Categoria categoria, String color, Fit fit, Evento evento) {
        Set<Integer> ids = new LinkedHashSet<>(porId.keySet());
        if (categoria != null) ids.retainAll(porCategoria.getOrDefault(categoria, new LinkedHashSet<>()));
        if (color != null) ids.retainAll(porColor.getOrDefault(Texto.aClave(color), new LinkedHashSet<>()));
        if (fit != null) ids.retainAll(porFit.getOrDefault(fit, new LinkedHashSet<>()));
        if (evento != null) ids.retainAll(porEvento.getOrDefault(evento, new LinkedHashSet<>()));
        List<Prenda> resultado = new ArrayList<>();
        for (int id : ids) resultado.add(porId.get(id));
        return resultado;
    }

    public List<String> coloresRegistrados() {
        return new ArrayList<>(porColor.keySet());
    }

    // ------------------------------------------------------------------ índices

    private void indexar(Prenda p) {
        trieNombres.insertar(p.getNombre(), p.getId());
        porCategoria.computeIfAbsent(p.getCategoria(), k -> new LinkedHashSet<>()).add(p.getId());
        porColor.computeIfAbsent(p.getColorNombre(), k -> new LinkedHashSet<>()).add(p.getId());
        porFit.computeIfAbsent(p.getFit(), k -> new LinkedHashSet<>()).add(p.getId());
        for (Evento e : p.getEventosAptos()) {
            porEvento.computeIfAbsent(e, k -> new LinkedHashSet<>()).add(p.getId());
        }
    }

    private void desindexar(Prenda p) {
        trieNombres.eliminar(p.getNombre(), p.getId());
        quitar(porCategoria, p.getCategoria(), p.getId());
        quitar(porColor, p.getColorNombre(), p.getId());
        quitar(porFit, p.getFit(), p.getId());
        for (Evento e : p.getEventosAptos()) quitar(porEvento, e, p.getId());
    }

    private static <K> void quitar(Map<K, Set<Integer>> indice, K clave, int id) {
        Set<Integer> ids = indice.get(clave);
        if (ids == null) return;
        ids.remove(id);
        if (ids.isEmpty()) indice.remove(clave);
    }

    private static Prenda copiar(Prenda p) {
        return new Prenda(p.getId(), p.getNombre(), p.getCategoria(), p.getSubtipo(), p.getColorHex(),
                p.getColorNombre(), p.getFit(), p.getFormalidad(), p.getAbrigo(), p.getEventosAptos());
    }

    private static void restaurar(Prenda p, Prenda respaldo) {
        p.setNombre(respaldo.getNombre());
        p.setColor(respaldo.getColorHex(), respaldo.getColorNombre());
        p.setFit(respaldo.getFit());
        p.setFormalidad(respaldo.getFormalidad());
        p.setAbrigo(respaldo.getAbrigo());
        p.setEventosAptos(respaldo.getEventosAptos());
    }
}
