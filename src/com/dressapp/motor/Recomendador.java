package com.dressapp.motor;

import com.dressapp.estructuras.Grafo;
import com.dressapp.estructuras.MinHeap;
import com.dressapp.modelo.CapaVaciaException;
import com.dressapp.modelo.Categoria;
import com.dressapp.modelo.Outfit;
import com.dressapp.modelo.Prenda;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Recomendador de la sección 2.5. Nada aquí usa IA: es un grafo por capas con costos
 * deterministas, sobre el que corren Dijkstra, Yen y un backtracking con poda.
 *
 * Flujo de recomendar(evento):
 *   1. Filtro duro por capa.
 *   2. Grafo por capas INICIO -> superior -> [exterior] -> inferior -> calzado -> [accesorio] -> FIN.
 *   3. Yen obtiene los K caminos de menor costo (cada uno con el Dijkstra propio).
 *   4. Re-ranking por G (que mira TODOS los pares, no solo los consecutivos) con el MinHeap.
 *   5. Regla de diversidad: se descarta un candidato que comparta más de 2 prendas con uno elegido.
 */
public class Recomendador {

    public static final String INICIO = "INICIO";
    public static final String FIN = "FIN";

    /** Grafo por capas más el mapeo nodo -> prenda. */
    public static final class GrafoCapas {
        public final Grafo grafo;
        private final Map<String, Prenda> prendaDeNodo;

        GrafoCapas(Grafo grafo, Map<String, Prenda> prendaDeNodo) {
            this.grafo = grafo;
            this.prendaDeNodo = prendaDeNodo;
        }

        public List<Prenda> prendasDe(List<String> camino) {
            List<Prenda> prendas = new ArrayList<>();
            for (String nodo : camino) {
                Prenda p = prendaDeNodo.get(nodo);
                if (p != null) prendas.add(p);
            }
            return prendas;
        }
    }

    /** Resultado del backtracking, con estadísticas para mostrar el efecto de la poda. */
    public static final class ResultadoBacktracking {
        public final List<Outfit> outfits;
        public final long nodosExplorados;
        public final long ramasPodadas;

        ResultadoBacktracking(List<Outfit> outfits, long nodosExplorados, long ramasPodadas) {
            this.outfits = outfits;
            this.nodosExplorados = nodosExplorados;
            this.ramasPodadas = ramasPodadas;
        }
    }

    // ================================================================== filtro duro y capas

    /** Se descarta la prenda si form(p,e) < 0.5 o si el evento no está entre sus eventos aptos. */
    public static boolean pasaFiltroDuro(Prenda p, ContextoRecomendacion ctx) {
        return Puntuacion.form(p, ctx.getEvento()) >= Config.FORM_MINIMA
                && p.getEventosAptos().contains(ctx.getEvento());
    }

    public static boolean esObligatoria(Categoria categoria, ContextoRecomendacion ctx) {
        return !categoria.esOpcional() || (categoria == Categoria.EXTERIOR && ctx.getClima().exigeExterior());
    }

    /** Prendas aptas por capa. Lanza CapaVaciaException si una capa obligatoria queda vacía. */
    public Map<Categoria, List<Prenda>> candidatosPorCapa(List<Prenda> closet, ContextoRecomendacion ctx) {
        Map<Categoria, List<Prenda>> capas = new EnumMap<>(Categoria.class);
        for (Categoria c : Categoria.values()) capas.put(c, new ArrayList<>());
        for (Prenda p : closet) {
            if (pasaFiltroDuro(p, ctx)) capas.get(p.getCategoria()).add(p);
        }
        for (Categoria c : Categoria.values()) {
            if (esObligatoria(c, ctx) && capas.get(c).isEmpty()) throw new CapaVaciaException(c, ctx.getEvento());
        }
        return capas;
    }

    // ================================================================== grafo por capas

    private static String nodoDe(Prenda p) {
        return p.getCategoria().name() + "#" + p.getId();
    }

    /**
     * Costos (todos >= 0 y dependientes solo de los dos extremos de la arista, así Dijkstra es válido):
     *   INICIO -> q          BETA*(1 - u(q))
     *   p -> q               ALFA*(1 - c(p,q)) + BETA*(1 - u(q))     (incluye el salto superior -> inferior)
     *   accesorio -> FIN     0
     *   calzado -> FIN       GAMMA                                    (outfit sin accesorio)
     * Las capas opcionales se omiten con aristas de salto. Si el clima exige abrigo, la capa
     * exterior es obligatoria y no se crean saltos sobre ella.
     */
    public GrafoCapas construirGrafo(List<Prenda> closet, ContextoRecomendacion ctx) {
        Map<Categoria, List<Prenda>> capas = candidatosPorCapa(closet, ctx);
        Grafo grafo = new Grafo();
        Map<String, Prenda> prendaDeNodo = new HashMap<>();
        Map<Integer, Double> uCache = new HashMap<>();
        grafo.agregarNodo(INICIO);
        grafo.agregarNodo(FIN);
        for (List<Prenda> capa : capas.values()) {
            for (Prenda p : capa) {
                grafo.agregarNodo(nodoDe(p));
                prendaDeNodo.put(nodoDe(p), p);
                uCache.put(p.getId(), Puntuacion.u(p, ctx));
            }
        }

        Categoria[] orden = Categoria.values();
        for (int i = -1; i < orden.length; i++) {
            List<Prenda> origenes = i < 0 ? Collections.singletonList(null) : capas.get(orden[i]);
            for (Prenda p : origenes) {
                String nodoOrigen = p == null ? INICIO : nodoDe(p);
                boolean saltoCapaOpcional = false;
                for (int j = i + 1; j <= orden.length; j++) {
                    if (j == orden.length) {
                        grafo.agregarArista(nodoOrigen, FIN, saltoCapaOpcional ? Config.GAMMA : 0.0);
                        break;
                    }
                    Categoria destino = orden[j];
                    for (Prenda q : capas.get(destino)) {
                        double costoU = Config.BETA * (1 - uCache.get(q.getId()));
                        double costo = p == null ? costoU : Config.ALFA * (1 - Puntuacion.c(p, q, ctx)) + costoU;
                        grafo.agregarArista(nodoOrigen, nodoDe(q), costo);
                    }
                    if (esObligatoria(destino, ctx)) break;
                    saltoCapaOpcional = true; // se puede saltar esta capa y seguir a la siguiente
                }
            }
        }
        return new GrafoCapas(grafo, prendaDeNodo);
    }

    private Outfit crearOutfit(List<Prenda> prendas, ContextoRecomendacion ctx, double costoCamino) {
        Map<String, Double> desglose = Puntuacion.desglose(prendas, ctx);
        if (!Double.isNaN(costoCamino)) desglose.put("costo_camino", costoCamino);
        return new Outfit(ctx.getEvento(), prendas, desglose.get(Puntuacion.G), desglose);
    }

    private static List<Outfit> conExplicacion(List<Outfit> outfits, ContextoRecomendacion ctx) {
        for (Outfit o : outfits) o.setExplicacion(Explicador.explicar(o, ctx));
        return outfits;
    }

    // ================================================================== Dijkstra y Yen

    /** Camino de menor costo INICIO -> FIN: el mejor outfit según compatibilidad entre capas consecutivas. */
    public Outfit mejorOutfit(List<Prenda> closet, ContextoRecomendacion ctx) {
        GrafoCapas gc = construirGrafo(closet, ctx);
        Grafo.Camino camino = gc.grafo.dijkstra(INICIO, FIN);
        Outfit outfit = crearOutfit(gc.prendasDe(camino.nodos), ctx, camino.costo);
        outfit.setExplicacion(Explicador.explicar(outfit, ctx));
        return outfit;
    }

    /**
     * Algoritmo de Yen: los k caminos sin ciclos de menor costo, en orden no decreciente.
     * Para cada nodo del último camino aceptado se busca un desvío (spur) con Dijkstra,
     * excluyendo las aristas ya usadas por caminos con la misma raíz y los nodos de la raíz.
     * Complejidad: O(k * V * (V + E) log V).
     */
    public List<Grafo.Camino> kMejoresCaminos(Grafo grafo, String origen, String destino, int k) {
        List<Grafo.Camino> aceptados = new ArrayList<>();
        Grafo.Camino primero = grafo.dijkstra(origen, destino);
        if (!primero.existe() || k <= 0) return aceptados;
        aceptados.add(primero);

        MinHeap<Grafo.Camino> candidatos = new MinHeap<>();
        Set<List<String>> vistos = new HashSet<>();
        vistos.add(primero.nodos);

        while (aceptados.size() < k) {
            List<String> ultimo = aceptados.get(aceptados.size() - 1).nodos;
            for (int i = 0; i < ultimo.size() - 1; i++) {
                String nodoDesvio = ultimo.get(i);
                List<String> raiz = ultimo.subList(0, i + 1);

                Set<String> aristasExcluidas = new HashSet<>();
                for (Grafo.Camino c : aceptados) {
                    if (c.nodos.size() > i + 1 && c.nodos.subList(0, i + 1).equals(raiz)) {
                        aristasExcluidas.add(Grafo.claveArista(c.nodos.get(i), c.nodos.get(i + 1)));
                    }
                }
                Set<String> nodosExcluidos = new HashSet<>(raiz.subList(0, i));

                Grafo.Camino desvio = grafo.dijkstra(nodoDesvio, destino, nodosExcluidos, aristasExcluidas);
                if (!desvio.existe()) continue;

                List<String> completo = new ArrayList<>(raiz.subList(0, i));
                completo.addAll(desvio.nodos);
                if (vistos.add(completo)) {
                    double costo = grafo.costoDe(completo);
                    candidatos.push(new Grafo.Camino(costo, completo), costo);
                }
            }
            if (candidatos.estaVacio()) break;
            aceptados.add(candidatos.pop());
        }
        return aceptados;
    }

    // ================================================================== recomendar

    /**
     * Hasta n outfits diversos. Arranca con K = 10 candidatos; si la regla de diversidad deja
     * menos de n, duplica K (hasta K_MAXIMO) mientras el grafo tenga más caminos.
     */
    public List<Outfit> recomendar(List<Prenda> closet, ContextoRecomendacion ctx, int n) {
        GrafoCapas gc = construirGrafo(closet, ctx);
        int k = Config.K_CANDIDATOS;
        while (true) {
            List<Grafo.Camino> caminos = kMejoresCaminos(gc.grafo, INICIO, FIN, k);
            List<Outfit> elegidos = reRankearConDiversidad(caminos, gc, ctx, n);
            if (elegidos.size() >= n || caminos.size() < k || k >= Config.K_MAXIMO) return conExplicacion(elegidos, ctx);
            k = Math.min(2 * k, Config.K_MAXIMO);
        }
    }

    private List<Outfit> reRankearConDiversidad(List<Grafo.Camino> caminos, GrafoCapas gc,
                                                ContextoRecomendacion ctx, int n) {
        MinHeap<Outfit> porG = new MinHeap<>();
        for (Grafo.Camino camino : caminos) {
            Outfit o = crearOutfit(gc.prendasDe(camino.nodos), ctx, camino.costo);
            porG.push(o, -o.getPuntaje()); // prioridad negativa: sale primero el mayor G
        }
        List<Outfit> elegidos = new ArrayList<>();
        while (!porG.estaVacio() && elegidos.size() < n) {
            Outfit candidato = porG.pop();
            if (esDiverso(candidato, elegidos)) elegidos.add(candidato);
        }
        return elegidos;
    }

    public static boolean esDiverso(Outfit candidato, List<Outfit> elegidos) {
        for (Outfit o : elegidos) {
            if (candidato.prendasCompartidasCon(o) > Config.MAX_PRENDAS_COMPARTIDAS) return false;
        }
        return true;
    }

    // ================================================================== completar con prenda fija

    private static final class Capa {
        final List<Prenda> opciones;
        final boolean obligatoria;

        Capa(List<Prenda> opciones, boolean obligatoria) {
            this.opciones = opciones;
            this.obligatoria = obligatoria;
        }
    }

    private List<Capa> capasParaCompletar(Prenda fija, List<Prenda> closet, ContextoRecomendacion ctx) {
        List<Capa> capas = new ArrayList<>();
        for (Categoria categoria : Categoria.values()) {
            if (categoria == fija.getCategoria()) {
                capas.add(new Capa(Collections.singletonList(fija), true));
                continue;
            }
            List<Prenda> opciones = new ArrayList<>();
            for (Prenda p : closet) {
                if (p.getCategoria() == categoria && pasaFiltroDuro(p, ctx)) opciones.add(p);
            }
            boolean obligatoria = esObligatoria(categoria, ctx);
            if (opciones.isEmpty()) {
                if (obligatoria) throw new CapaVaciaException(categoria, ctx.getEvento());
                continue; // capa opcional sin prendas aptas: no hay nada que decidir
            }
            capas.add(new Capa(opciones, obligatoria));
        }
        return capas;
    }

    /**
     * Backtracking: recorre las capas en profundidad eligiendo una prenda por capa (o ninguna,
     * si la capa es opcional) y poda una rama cuando su cota optimista no supera al peor
     * outfit del top-n actual. Devuelve los n mejores por G.
     */
    public ResultadoBacktracking completarOutfit(Prenda fija, List<Prenda> closet, ContextoRecomendacion ctx, int n) {
        List<Capa> capas = capasParaCompletar(fija, closet, ctx);
        Busqueda busqueda = new Busqueda(capas, ctx, n, true);
        busqueda.explorar(0, 0.0, 0.0);
        return new ResultadoBacktracking(conExplicacion(busqueda.top, ctx), busqueda.nodos, busqueda.podas);
    }

    /** Misma búsqueda sin poda. Solo sirve para verificar que la poda no cambia el resultado. */
    public ResultadoBacktracking completarOutfitExhaustivo(Prenda fija, List<Prenda> closet, ContextoRecomendacion ctx, int n) {
        List<Capa> capas = capasParaCompletar(fija, closet, ctx);
        Busqueda busqueda = new Busqueda(capas, ctx, n, false);
        busqueda.explorar(0, 0.0, 0.0);
        return new ResultadoBacktracking(busqueda.top, busqueda.nodos, busqueda.podas);
    }

    private final class Busqueda {
        final List<Capa> capas;
        final ContextoRecomendacion ctx;
        final int n;
        final boolean conPoda;
        final int[] obligatoriasDesde; // obligatoriasDesde[i] = capas obligatorias en [i, fin)
        final Map<Integer, Double> uCache = new HashMap<>();
        final List<Prenda> elegidas = new ArrayList<>();
        final List<Outfit> top = new ArrayList<>();
        long nodos;
        long podas;

        Busqueda(List<Capa> capas, ContextoRecomendacion ctx, int n, boolean conPoda) {
            this.capas = capas;
            this.ctx = ctx;
            this.n = n;
            this.conPoda = conPoda;
            this.obligatoriasDesde = new int[capas.size() + 1];
            for (int i = capas.size() - 1; i >= 0; i--) {
                obligatoriasDesde[i] = obligatoriasDesde[i + 1] + (capas.get(i).obligatoria ? 1 : 0);
            }
        }

        double u(Prenda p) {
            return uCache.computeIfAbsent(p.getId(), id -> Puntuacion.u(p, ctx));
        }

        void explorar(int i, double sumaU, double sumaC) {
            nodos++;
            if (i == capas.size()) {
                registrar();
                return;
            }
            if (conPoda && top.size() == n) {
                double cota = cotaOptimista(elegidas.size(), sumaU, sumaC,
                        obligatoriasDesde[i], capas.size() - i);
                if (cota <= top.get(top.size() - 1).getPuntaje()) {
                    podas++;
                    return;
                }
            }
            Capa capa = capas.get(i);
            for (Prenda p : capa.opciones) {
                double nuevaC = sumaC;
                for (Prenda q : elegidas) nuevaC += Puntuacion.c(q, p, ctx);
                elegidas.add(p);
                explorar(i + 1, sumaU + u(p), nuevaC);
                elegidas.remove(elegidas.size() - 1);
            }
            if (!capa.obligatoria) {
                explorar(i + 1, sumaU, sumaC); // omitir esta capa opcional
            }
        }

        void registrar() {
            Outfit outfit = crearOutfit(elegidas, ctx, Double.NaN);
            int pos = top.size();
            while (pos > 0 && top.get(pos - 1).getPuntaje() < outfit.getPuntaje()) pos--;
            top.add(pos, outfit);
            if (top.size() > n) top.remove(top.size() - 1);
        }
    }

    /**
     * Cota optimista de G para cualquier forma de completar la rama. Si faltan r prendas más,
     * cada u nueva y cada c de un par nuevo valen como mucho 1, así que
     *   G <= DELTA*(sumaU + r)/(k + r) + EPSILON*(sumaC + paresNuevos)/paresTotales.
     * Como r no se conoce (las capas opcionales pueden omitirse), se toma el máximo para
     * r entre las capas obligatorias que faltan y todas las que faltan.
     */
    static double cotaOptimista(int k, double sumaU, double sumaC, int rMin, int rMax) {
        double mejor = Double.NEGATIVE_INFINITY;
        for (int r = rMin; r <= rMax; r++) {
            int total = k + r;
            if (total == 0) continue;
            int paresTotales = total * (total - 1) / 2;
            int paresNuevos = paresTotales - k * (k - 1) / 2;
            double promU = (sumaU + r) / total;
            double promC = paresTotales == 0 ? 1.0 : (sumaC + paresNuevos) / paresTotales;
            mejor = Math.max(mejor, Puntuacion.combinar(promU, promC));
        }
        return mejor;
    }

    // ================================================================== verificación

    /** Mínimo costo INICIO -> FIN enumerando TODOS los caminos (sin Dijkstra). Solo para pruebas. */
    public double costoMinimoExhaustivo(List<Prenda> closet, ContextoRecomendacion ctx) {
        GrafoCapas gc = construirGrafo(closet, ctx);
        double[] mejor = {Double.POSITIVE_INFINITY};
        enumerar(gc.grafo, INICIO, 0.0, mejor, new LinkedHashMap<>());
        return mejor[0];
    }

    private void enumerar(Grafo grafo, String nodo, double costo, double[] mejor, Map<String, Boolean> enCamino) {
        if (nodo.equals(FIN)) {
            mejor[0] = Math.min(mejor[0], costo);
            return;
        }
        enCamino.put(nodo, true);
        for (Grafo.Arista a : grafo.vecinos(nodo)) {
            if (!enCamino.containsKey(a.destino)) enumerar(grafo, a.destino, costo + a.peso, mejor, enCamino);
        }
        enCamino.remove(nodo);
    }
}
