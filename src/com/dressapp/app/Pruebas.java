package com.dressapp.app;

import com.dressapp.datos.ClosetEjemplo;
import com.dressapp.datos.Repositorio;
import com.dressapp.estructuras.ArbolCategorias;
import com.dressapp.estructuras.Grafo;
import com.dressapp.estructuras.MinHeap;
import com.dressapp.estructuras.Trie;
import com.dressapp.modelo.CapaVaciaException;
import com.dressapp.modelo.Categoria;
import com.dressapp.modelo.ContextoClima;
import com.dressapp.modelo.Evento;
import com.dressapp.modelo.Fit;
import com.dressapp.modelo.Outfit;
import com.dressapp.modelo.Perfil;
import com.dressapp.modelo.PesoAprendido;
import com.dressapp.modelo.Prenda;
import com.dressapp.motor.Aprendizaje;
import com.dressapp.motor.ContextoRecomendacion;
import com.dressapp.motor.Puntuacion;
import com.dressapp.motor.Recomendador;
import com.dressapp.util.Texto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Autoverificación en consola de los criterios de aceptación del plan: casos borde de las
 * estructuras, propiedades del motor y equivalencias con fuerza bruta. Sin frameworks: cada
 * chequeo imprime [ OK ] o [FALLA].
 */
public class Pruebas {

    private static final double EPS = 1e-9;
    private static final ContextoClima[] CLIMAS = {
            ContextoClima.sinClima(), ContextoClima.de(30, false), ContextoClima.de(15, true), ContextoClima.de(8, true), ContextoClima.de(2, false)};

    private final ArbolCategorias arbol = new ArbolCategorias();
    private final Recomendador recomendador = new Recomendador();
    private final List<String> fallas = new ArrayList<>();
    private int total;

    public boolean ejecutar() {
        Vista.titulo("Autoverificación");
        minHeap();
        trie();
        arbolYValidaciones();
        grafoYDijkstra();
        puntuacion();
        recomendadorGrafo();
        backtracking();
        aprendizaje();
        explicaciones();
        rendimiento();

        System.out.println();
        System.out.println("  " + (total - fallas.size()) + "/" + total + " chequeos superados.");
        for (String f : fallas) System.out.println("  FALLÓ: " + f);
        return fallas.isEmpty();
    }

    private void seccion(String nombre) {
        System.out.println("\n  " + nombre);
    }

    private void verificar(String descripcion, boolean condicion) {
        total++;
        if (!condicion) fallas.add(descripcion);
        System.out.println("    " + (condicion ? "[ OK ] " : "[FALLA] ") + descripcion);
    }

    private interface Accion {
        void correr();
    }

    private boolean lanza(Class<? extends Exception> tipo, Accion accion) {
        try {
            accion.correr();
            return false;
        } catch (Exception e) {
            return tipo.isInstance(e);
        }
    }

    // ================================================================== estructuras

    private void minHeap() {
        seccion("MinHeap");
        Random rnd = new Random(1);
        MinHeap<Integer> heap = new MinHeap<>();
        List<Integer> valores = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            int v = rnd.nextInt(50);
            valores.add(v);
            heap.push(v, v);
        }
        Collections.sort(valores);
        List<Integer> salida = new ArrayList<>();
        while (!heap.estaVacio()) salida.add(heap.pop());
        verificar("Ordena igual que Collections.sort (500 valores, con repetidos)", salida.equals(valores));

        MinHeap<String> estable = new MinHeap<>();
        estable.push("a", 1);
        estable.push("b", 1);
        estable.push("z", 0);
        estable.push("c", 1);
        String orden = estable.pop() + estable.pop() + estable.pop() + estable.pop();
        verificar("Desempate estable: con igual prioridad sale primero el que entró primero (zabc)", orden.equals("zabc"));

        MinHeap<String> vacio = new MinHeap<>();
        vacio.push("x", 3);
        verificar("peek no extrae el elemento", vacio.peek().equals("x") && vacio.size() == 1);
        vacio.pop();
        verificar("pop en un heap vacío lanza NoSuchElementException", lanza(NoSuchElementException.class, vacio::pop));
    }

    private void trie() {
        seccion("Trie");
        Trie t = new Trie();
        verificar("Trie vacío: cualquier prefijo devuelve lista vacía", t.buscarPrefijo("a", 10).isEmpty() && t.estaVacio());
        t.insertar("Camiseta blanca", 1);
        t.insertar("Camisa blanca", 2);
        t.insertar("Camiseta negra", 3);
        t.insertar("Chaqueta", 4);
        t.insertar("Pantalón Gris", 5);
        verificar("Prefijo 'cami' encuentra las 3 camisas/camisetas", t.buscarPrefijo("cami", 10).size() == 3);
        verificar("Prefijo 'camise' encuentra solo las 2 camisetas", t.buscarPrefijo("camise", 10).size() == 2);
        verificar("Ignora tildes y mayúsculas: 'PANTALON' encuentra 'Pantalón Gris'", t.buscarPrefijo("PANTALON", 10).contains(5));
        verificar("Prefijo inexistente devuelve lista vacía", t.buscarPrefijo("xyz", 10).isEmpty());
        verificar("Respeta el límite de resultados", t.buscarPrefijo("c", 2).size() == 2);
        t.insertar("Jean azul", 6);
        t.insertar("Jean azul", 7);
        t.eliminar("Jean azul", 6);
        verificar("Eliminar una de dos prendas con el mismo nombre deja la otra", t.buscarPrefijo("jean", 10).equals(Collections.singletonList(7)));
        verificar("Eliminar algo que no existe devuelve false", !t.eliminar("sombrero", 99));
        t.eliminar("Jean azul", 7);
        verificar("Tras borrar la última, el prefijo deja de existir", t.buscarPrefijo("je", 10).isEmpty());
    }

    private void arbolYValidaciones() {
        seccion("Árbol de categorías y validaciones");
        verificar("'camiseta' es subtipo de superior", arbol.esSubtipoDe("camiseta", Categoria.SUPERIOR));
        verificar("'camiseta' no es subtipo de calzado", !arbol.esSubtipoDe("camiseta", Categoria.CALZADO));
        verificar("'Zapato formal' (con espacio y mayúscula) es subtipo de calzado", arbol.esSubtipoDe("Zapato formal", Categoria.CALZADO));
        verificar("categoriaDe('Pantalón') = inferior", arbol.categoriaDe("Pantalón") == Categoria.INFERIOR);
        verificar("categoriaDe de un subtipo desconocido = null", arbol.categoriaDe("sombrero") == null);
        verificar("El árbol tiene 23 subtipos y el DFS recorre 1 + 5 + 23 nodos",
                arbol.cantidadSubtipos() == 23 && arbol.recorridoDFS().size() == 29);

        Repositorio repo = new Repositorio(arbol);
        verificar("El repositorio rechaza un subtipo de otra categoría (tenis como superior)", lanza(IllegalArgumentException.class,
                () -> repo.agregar("x", Categoria.SUPERIOR, "tenis", "#000000", "negro", Fit.REGULAR, 3, 2, Arrays.asList(Evento.CASUAL))));
        verificar("Se rechaza formalidad 7", lanza(IllegalArgumentException.class,
                () -> repo.agregar("x", Categoria.SUPERIOR, "camisa", "#000000", "negro", Fit.REGULAR, 7, 2, Arrays.asList(Evento.CASUAL))));
        verificar("Se rechaza un color hex inválido", lanza(IllegalArgumentException.class,
                () -> repo.agregar("x", Categoria.SUPERIOR, "camisa", "#12345", "negro", Fit.REGULAR, 3, 2, Arrays.asList(Evento.CASUAL))));

        Prenda p = repo.agregar("Camisa azul", Categoria.SUPERIOR, "camisa", "#2255CC", "azul", Fit.REGULAR, 3, 2, Arrays.asList(Evento.CASUAL));
        repo.editar(p.getId(), x -> x.setNombre("Blusa verde"));
        verificar("Al renombrar, el trie encuentra el nombre nuevo y no el viejo",
                repo.buscarPorNombre("blusa", 5).size() == 1 && repo.buscarPorNombre("camisa", 5).isEmpty());
        boolean rechazo = lanza(IllegalArgumentException.class, () -> repo.editar(p.getId(), x -> x.setFormalidad(9)));
        verificar("Una edición inválida no modifica la prenda ni sus índices",
                rechazo && p.getFormalidad() == 3 && repo.filtrar(Categoria.SUPERIOR, "azul", null, Evento.CASUAL).size() == 1);
    }

    private void grafoYDijkstra() {
        seccion("Grafo y Dijkstra");
        Grafo g = new Grafo();
        verificar("Un peso negativo se rechaza", lanza(IllegalArgumentException.class, () -> g.agregarArista("a", "b", -1)));
        g.agregarNodo("a");
        g.agregarNodo("b");
        verificar("Sin camino: Dijkstra informa que no existe", !g.dijkstra("a", "b").existe());
        Grafo.Camino mismo = g.dijkstra("a", "a");
        verificar("Un solo nodo (origen = destino): costo 0", mismo.existe() && mismo.costo == 0 && mismo.nodos.size() == 1);

        Grafo empate = new Grafo();
        empate.agregarArista("a", "b", 1);
        empate.agregarArista("a", "c", 1);
        empate.agregarArista("b", "d", 1);
        empate.agregarArista("c", "d", 1);
        verificar("Con empates encuentra un camino de costo mínimo (2)", Math.abs(empate.dijkstra("a", "d").costo - 2) < EPS);

        Random rnd = new Random(7);
        boolean coincide = true;
        for (int prueba = 0; prueba < 40; prueba++) {
            int n = 3 + rnd.nextInt(6);
            Grafo aleatorio = new Grafo();
            double[][] peso = new double[n][n];
            for (double[] fila : peso) Arrays.fill(fila, Double.NaN);
            for (int i = 0; i < n; i++) {
                aleatorio.agregarNodo("n" + i);
                for (int j = 0; j < n; j++) {
                    if (i != j && rnd.nextDouble() < 0.35) {
                        peso[i][j] = rnd.nextInt(10);
                        aleatorio.agregarArista("n" + i, "n" + j, peso[i][j]);
                    }
                }
            }
            double esperado = fuerzaBruta(peso, 0, n - 1, new boolean[n]);
            double obtenido = aleatorio.dijkstra("n0", "n" + (n - 1)).costo;
            if (!(Double.isInfinite(esperado) && Double.isInfinite(obtenido)) && Math.abs(esperado - obtenido) > EPS) coincide = false;
        }
        verificar("Dijkstra coincide con fuerza bruta en 40 grafos aleatorios (pesos >= 0, incluido 0)", coincide);
    }

    private double fuerzaBruta(double[][] peso, int actual, int destino, boolean[] visitado) {
        if (actual == destino) return 0;
        visitado[actual] = true;
        double mejor = Double.POSITIVE_INFINITY;
        for (int j = 0; j < peso.length; j++) {
            if (!visitado[j] && !Double.isNaN(peso[actual][j])) {
                mejor = Math.min(mejor, peso[actual][j] + fuerzaBruta(peso, j, destino, visitado));
            }
        }
        visitado[actual] = false;
        return mejor;
    }

    // ================================================================== motor

    private Repositorio closetEjemplo() {
        Repositorio r = new Repositorio(new ArbolCategorias());
        ClosetEjemplo.cargar(r);
        return r;
    }

    private Perfil perfilEjemplo() {
        Perfil p = new Perfil();
        p.setColoresPreferidos(Arrays.asList("negro"));
        p.setColoresEvitados(Arrays.asList("rojo"));
        p.setFitPreferido(Fit.REGULAR);
        return p;
    }

    private Prenda prenda(int id, Categoria c, String subtipo, String hex, String color, Fit fit, int form, int abrigo) {
        return new Prenda(id, c.etiqueta() + " " + id, c, subtipo, hex, color, fit, form, abrigo, Arrays.asList(Evento.values()));
    }

    private void puntuacion() {
        seccion("Motor de puntuación");
        List<Prenda> closet = closetEjemplo().listar();
        Perfil perfil = perfilEjemplo();
        boolean enRango = true;
        boolean simetrica = true;
        for (Evento e : Evento.values()) {
            for (ContextoClima clima : CLIMAS) {
                ContextoRecomendacion ctx = new ContextoRecomendacion(e, clima, perfil, null);
                for (Prenda p : closet) {
                    double u = Puntuacion.u(p, ctx);
                    if (u < 0 || u > 1) enRango = false;
                    for (Prenda q : closet) {
                        if (p == q) continue;
                        double c = Puntuacion.c(p, q, ctx);
                        if (c < 0 || c > 1) enRango = false;
                        if (Math.abs(c - Puntuacion.c(q, p, ctx)) > EPS) simetrica = false;
                    }
                }
            }
        }
        verificar("u(p,e) y c(p,q) siempre en [0,1] (todos los eventos y climas)", enRango);
        verificar("c es simétrica: c(p,q) = c(q,p)", simetrica);

        // Caso calculado a mano (ver README):
        // camisa blanca slim (form 5) + pantalón azul marino slim (form 5), muy formal, sin clima ni perfil:
        //   u = 0.6*1 + 0.2*1 + 0.2*0.5 = 0.90 ; c = 0.6*1.0 (neutro) + 0.4*0.8 (ambos slim) = 0.92 ; G = 0.91
        // buzo gris ultra baggy (form 1) + sudadera gris ultra baggy (form 1), muy formal:
        //   u = 0.6*0 + 0.2 + 0.1 = 0.30 ; c = 0.6*1.0 + 0.4*0.4 (holgados en formal) = 0.76 ; G = 0.53
        ContextoRecomendacion muyFormal = ContextoRecomendacion.basico(Evento.MUY_FORMAL);
        Prenda camisa = prenda(1, Categoria.SUPERIOR, "camisa", "#FFFFFF", "blanco", Fit.SLIM, 5, 2);
        Prenda pantalon = prenda(2, Categoria.INFERIOR, "pantalon", "#1B1F3B", "azul marino", Fit.SLIM, 5, 2);
        Prenda buzo = prenda(3, Categoria.SUPERIOR, "buzo", "#808080", "gris", Fit.ULTRA_BAGGY, 1, 4);
        Prenda sudadera = prenda(4, Categoria.INFERIOR, "sudadera", "#808080", "gris", Fit.ULTRA_BAGGY, 1, 3);
        double gFormal = Puntuacion.g(Arrays.asList(camisa, pantalon), muyFormal);
        double gInformal = Puntuacion.g(Arrays.asList(buzo, sudadera), muyFormal);
        verificar("Caso a mano: camisa blanca + pantalón azul marino en muy formal da G = 0,91", Math.abs(gFormal - 0.91) < EPS);
        verificar("Caso a mano: buzo ultra baggy + sudadera en muy formal da G = 0,53", Math.abs(gInformal - 0.53) < EPS);
        verificar("Camisa + pantalón > buzo + sudadera en muy formal", gFormal > gInformal);

        Prenda roja = prenda(5, Categoria.SUPERIOR, "blusa", "#C0392B", "rojo", Fit.SLIM, 3, 2);
        Prenda negra = prenda(6, Categoria.SUPERIOR, "camiseta", "#111111", "negro", Fit.SLIM, 3, 2);
        verificar("Un color evitado baja perfil(p) de 0,5 a 0,0", Puntuacion.perfil(roja, null) == 0.5 && Puntuacion.perfil(roja, perfil) == 0.0);
        verificar("Un color preferido sube perfil(p) a 1,0", Puntuacion.perfil(negra, perfil) == 1.0);

        Prenda sandalia = prenda(7, Categoria.CALZADO, "sandalia", "#E8DCC5", "beige", Fit.REGULAR, 1, 1);
        Prenda tenis = prenda(8, Categoria.CALZADO, "tenis", "#FFFFFF", "blanco", Fit.REGULAR, 1, 1);
        ContextoClima lluvia = ContextoClima.de(26, true);
        verificar("Con lluvia, la sandalia puntúa peor en clima(p) que un calzado cerrado igual de abrigado",
                Puntuacion.clima(sandalia, lluvia) < Puntuacion.clima(tenis, lluvia));
        verificar("abrigo_requerido: 30 °C -> 1, 20 -> 2, 15 -> 3, 8 -> 4, 2 -> 5",
                ContextoClima.de(30, false).abrigoRequerido() == 1 && ContextoClima.de(20, false).abrigoRequerido() == 2
                        && ContextoClima.de(15, false).abrigoRequerido() == 3 && ContextoClima.de(8, false).abrigoRequerido() == 4
                        && ContextoClima.de(2, false).abrigoRequerido() == 5);
    }

    /** Closet aleatorio pequeño: todas las prendas pasan el filtro duro para el evento. */
    private List<Prenda> closetAleatorio(Random rnd, Evento evento, int maxPorCapa, boolean conExterior) {
        String[][] colores = {{"#000000", "negro"}, {"#FFFFFF", "blanco"}, {"#808080", "gris"}, {"#C0392B", "rojo"},
                {"#2255CC", "azul"}, {"#27AE60", "verde"}, {"#F1C40F", "amarillo"}, {"#8E44AD", "morado"}, {"#E67E22", "naranja"}};
        List<Prenda> closet = new ArrayList<>();
        int id = 1;
        for (Categoria c : Categoria.values()) {
            int minimo = (c.esOpcional() && !(c == Categoria.EXTERIOR && conExterior)) ? 0 : 1;
            int cantidad = minimo + rnd.nextInt(maxPorCapa - minimo + 1);
            List<String> subtipos = arbol.subtipos(c);
            for (int i = 0; i < cantidad; i++) {
                String[] color = colores[rnd.nextInt(colores.length)];
                int form = Math.max(1, Math.min(5, evento.getFormalidadObjetivo() + rnd.nextInt(5) - 2));
                closet.add(new Prenda(id++, c.etiqueta() + " " + i, c, subtipos.get(rnd.nextInt(subtipos.size())),
                        color[0], color[1], Fit.values()[rnd.nextInt(4)], form, 1 + rnd.nextInt(5), Arrays.asList(Evento.values())));
            }
        }
        return closet;
    }

    private ContextoRecomendacion contextoAleatorio(Random rnd, Evento evento) {
        return new ContextoRecomendacion(evento, CLIMAS[rnd.nextInt(CLIMAS.length)], rnd.nextBoolean() ? perfilEjemplo() : null, null);
    }

    private void costosDeTodosLosCaminos(Grafo g, String nodo, double costo, List<Double> salida) {
        if (nodo.equals(Recomendador.FIN)) {
            salida.add(costo);
            return;
        }
        for (Grafo.Arista a : g.vecinos(nodo)) costosDeTodosLosCaminos(g, a.destino, costo + a.peso, salida);
    }

    private void recomendadorGrafo() {
        seccion("Recomendador: grafo por capas, Dijkstra, Yen y diversidad");
        Random rnd = new Random(11);
        boolean dijkstraOk = true;
        boolean yenOk = true;
        boolean pesosNoNegativos = true;
        for (int prueba = 0; prueba < 30; prueba++) {
            Evento evento = Evento.values()[rnd.nextInt(Evento.values().length)];
            List<Prenda> closet = closetAleatorio(rnd, evento, 5, true);
            ContextoRecomendacion ctx = contextoAleatorio(rnd, evento);
            Recomendador.GrafoCapas gc;
            try {
                gc = recomendador.construirGrafo(closet, ctx); // Grafo rechaza cualquier peso negativo
            } catch (IllegalArgumentException ex) {
                pesosNoNegativos = false;
                continue;
            }
            double dijkstra = gc.grafo.dijkstra(Recomendador.INICIO, Recomendador.FIN).costo;
            if (Math.abs(dijkstra - recomendador.costoMinimoExhaustivo(closet, ctx)) > EPS) dijkstraOk = false;

            List<Double> todos = new ArrayList<>();
            costosDeTodosLosCaminos(gc.grafo, Recomendador.INICIO, 0, todos);
            Collections.sort(todos);
            int k = 12;
            List<Grafo.Camino> yen = recomendador.kMejoresCaminos(gc.grafo, Recomendador.INICIO, Recomendador.FIN, k);
            if (yen.size() != Math.min(k, todos.size())) yenOk = false;
            for (int i = 0; yenOk && i < yen.size(); i++) {
                if (Math.abs(yen.get(i).costo - todos.get(i)) > EPS) yenOk = false;
            }
        }
        verificar("Dijkstra = mínimo por enumeración exhaustiva, en 30 closets aleatorios (<= 5 prendas por capa)", dijkstraOk && pesosNoNegativos);
        verificar("Yen devuelve exactamente los k costos más bajos de la enumeración exhaustiva", yenOk);

        Repositorio repo = closetEjemplo();
        ContextoRecomendacion casual = ContextoRecomendacion.basico(Evento.CASUAL);
        List<Grafo.Camino> caminos = recomendador.kMejoresCaminos(recomendador.construirGrafo(repo.listar(), casual).grafo,
                Recomendador.INICIO, Recomendador.FIN, 15);
        boolean distintos = new HashSet<>(nodosDe(caminos)).size() == caminos.size();
        boolean noDecreciente = true;
        for (int i = 1; i < caminos.size(); i++) if (caminos.get(i).costo < caminos.get(i - 1).costo - EPS) noDecreciente = false;
        verificar("Yen sobre el closet de ejemplo: 15 caminos distintos en orden de costo no decreciente",
                caminos.size() == 15 && distintos && noDecreciente);

        boolean diversidad = true;
        boolean tres = true;
        for (Evento e : Evento.values()) {
            for (ContextoClima clima : CLIMAS) {
                List<Outfit> outs = recomendador.recomendar(repo.listar(), new ContextoRecomendacion(e, clima, null, null), 3);
                if (outs.size() != 3) tres = false;
                for (int i = 0; i < outs.size(); i++)
                    for (int j = i + 1; j < outs.size(); j++)
                        if (outs.get(i).prendasCompartidasCon(outs.get(j)) > 2) diversidad = false;
            }
        }
        verificar("Para cada evento y clima se devuelven 3 outfits", tres);
        verificar("Los 3 outfits cumplen la diversidad: ningún par comparte más de 2 prendas", diversidad);

        boolean exteriorConFrio = true;
        for (Evento e : Evento.values()) {
            Outfit top = recomendador.recomendar(repo.listar(), new ContextoRecomendacion(e, ContextoClima.de(8, false), null, null), 1).get(0);
            boolean tiene = false;
            for (Prenda p : top.getPrendas()) if (p.getCategoria() == Categoria.EXTERIOR) tiene = true;
            if (!tiene) exteriorConFrio = false;
        }
        verificar("Con 8 °C el top 1 de cada evento incluye una prenda exterior", exteriorConFrio);

        List<Prenda> sinCalzado = new ArrayList<>();
        for (Prenda p : repo.listar()) if (p.getCategoria() != Categoria.CALZADO) sinCalzado.add(p);
        boolean capaVacia = false;
        try {
            recomendador.recomendar(sinCalzado, casual, 3);
        } catch (CapaVaciaException ex) {
            capaVacia = ex.getCategoria() == Categoria.CALZADO;
        }
        verificar("Sin calzado apto se lanza CapaVaciaException(calzado) con un mensaje claro", capaVacia);
    }

    private List<List<String>> nodosDe(List<Grafo.Camino> caminos) {
        List<List<String>> lista = new ArrayList<>();
        for (Grafo.Camino c : caminos) lista.add(c.nodos);
        return lista;
    }

    private void backtracking() {
        seccion("Backtracking con poda (completar outfit)");
        Random rnd = new Random(23);
        boolean iguales = true;
        boolean incluyeFija = true;
        boolean podaAyuda = true;
        long nodosConPoda = 0;
        long nodosSinPoda = 0;
        for (int prueba = 0; prueba < 40; prueba++) {
            Evento evento = Evento.values()[rnd.nextInt(Evento.values().length)];
            List<Prenda> closet = closetAleatorio(rnd, evento, 4, true);
            ContextoRecomendacion ctx = contextoAleatorio(rnd, evento);
            Prenda fija = closet.get(rnd.nextInt(closet.size()));
            Recomendador.ResultadoBacktracking con = recomendador.completarOutfit(fija, closet, ctx, 3);
            Recomendador.ResultadoBacktracking sin = recomendador.completarOutfitExhaustivo(fija, closet, ctx, 3);
            nodosConPoda += con.nodosExplorados;
            nodosSinPoda += sin.nodosExplorados;
            if (con.nodosExplorados > sin.nodosExplorados) podaAyuda = false;
            if (con.outfits.size() != sin.outfits.size()) iguales = false;
            for (int i = 0; iguales && i < con.outfits.size(); i++) {
                if (Math.abs(con.outfits.get(i).getPuntaje() - sin.outfits.get(i).getPuntaje()) > EPS) iguales = false;
            }
            for (Outfit o : con.outfits) if (!o.getPrendas().contains(fija)) incluyeFija = false;
        }
        verificar("Con poda = exhaustivo sin poda (mismos puntajes del top 3) en 40 closets aleatorios", iguales);
        verificar("Todos los resultados incluyen la prenda fija", incluyeFija);
        verificar("La poda nunca explora más nodos (" + nodosConPoda + " con poda vs " + nodosSinPoda + " sin poda)", podaAyuda && nodosConPoda < nodosSinPoda);

        Repositorio repo = closetEjemplo();
        Prenda jean = repo.buscarPorNombre("jean azul", 1).get(0);
        Recomendador.ResultadoBacktracking frio = recomendador.completarOutfit(jean, repo.listar(),
                new ContextoRecomendacion(Evento.CASUAL, ContextoClima.de(5, false), null, null), 3);
        boolean todosConExterior = !frio.outfits.isEmpty();
        for (Outfit o : frio.outfits) {
            boolean tiene = false;
            for (Prenda p : o.getPrendas()) if (p.getCategoria() == Categoria.EXTERIOR) tiene = true;
            if (!tiene) todosConExterior = false;
        }
        verificar("Con 5 °C todos los outfits completados incluyen capa exterior", todosConExterior);
    }

    // ================================================================== aprendizaje

    private void aprendizaje() {
        seccion("Perfil y aprendizaje por calificaciones");
        Repositorio repo = closetEjemplo();
        List<Prenda> closet = repo.listar();

        boolean arranqueFrio = true;
        ContextoRecomendacion vacio = new ContextoRecomendacion(Evento.CASUAL, null, null, new LinkedHashMap<>());
        for (Prenda p : closet)
            for (Prenda q : closet)
                if (p != q && Math.abs(Puntuacion.c(p, q, vacio) - Puntuacion.cReglas(p, q, Evento.CASUAL)) > EPS) arranqueFrio = false;
        verificar("Arranque en frío: sin calificaciones c(p,q) = c_reglas(p,q) (lambda = 0)", arranqueFrio);
        verificar("Sin calificaciones la recomendación es idéntica a la del motor sin pesos",
                claves(recomendador.recomendar(closet, vacio, 3)).equals(claves(recomendador.recomendar(closet, ContextoRecomendacion.basico(Evento.CASUAL), 3))));

        // Tabla del plan (etapa 5): c_reglas = 0.5 y calificaciones de 5.
        Map<String, PesoAprendido> tabla = new LinkedHashMap<>();
        Prenda a = closet.get(0);
        Prenda b = closet.get(11);
        tabla.put(PesoAprendido.clave(a.getId(), b.getId(), Evento.CASUAL),
                new PesoAprendido(Math.min(a.getId(), b.getId()), Math.max(a.getId(), b.getId()), Evento.CASUAL, 0.5));
        Outfit par = new Outfit(Evento.CASUAL, Arrays.asList(a, b), 0, new LinkedHashMap<>());
        double[] esperado = {0.65, 0.755, 0.8285, 0.87995, 0.915965};
        boolean tablaOk = true;
        for (double w : esperado) {
            Aprendizaje.registrarCalificacion(par, 5, tabla);
            if (Math.abs(tabla.values().iterator().next().getValor() - w) > EPS) tablaOk = false;
        }
        PesoAprendido peso = tabla.values().iterator().next();
        verificar("Evolución de w con calificaciones de 5: 0,65 -> 0,755 -> 0,8285 -> 0,87995 -> 0,915965", tablaOk);
        verificar("Tras 5 calificaciones lambda = 5/8 = 0,625", Math.abs(peso.lambda() - 0.625) < EPS);

        Map<String, PesoAprendido> pesos = new LinkedHashMap<>();
        ContextoRecomendacion ctx = new ContextoRecomendacion(Evento.CASUAL, null, null, pesos);
        Outfit top = recomendador.recomendar(closet, ctx, 3).get(0);
        for (int i = 0; i < 5; i++) Aprendizaje.registrarCalificacion(top, 1, pesos);
        verificar("Calificar 5 veces con 1 el top 1 lo saca del top 3",
                !claves(recomendador.recomendar(closet, ctx, 3)).contains(top.claveCombinacion()));

        Map<String, PesoAprendido> pesosAltos = new LinkedHashMap<>();
        ContextoRecomendacion ctxAltos = new ContextoRecomendacion(Evento.CASUAL, null, null, pesosAltos);
        Outfit favorito = recomendador.recomendar(closet, ctxAltos, 3).get(0);
        for (int i = 0; i < 5; i++) Aprendizaje.registrarCalificacion(favorito, 5, pesosAltos);
        verificar("Calificar 5 veces con 5 el top 1 lo mantiene como top 1",
                recomendador.recomendar(closet, ctxAltos, 3).get(0).claveCombinacion().equals(favorito.claveCombinacion()));

        Random rnd = new Random(5);
        Map<String, PesoAprendido> azar = new LinkedHashMap<>();
        for (int i = 0; i < 200; i++) {
            Evento e = Evento.values()[rnd.nextInt(Evento.values().length)];
            List<Outfit> outs = recomendador.recomendar(closet, new ContextoRecomendacion(e, null, null, azar), 3);
            Aprendizaje.registrarCalificacion(outs.get(rnd.nextInt(outs.size())), 1 + rnd.nextInt(5), azar);
        }
        boolean wEnRango = true;
        for (PesoAprendido w : azar.values()) {
            if (w.getValor() < 0 || w.getValor() > 1) wEnRango = false;
            if (Math.abs(w.lambda() - w.getCalificaciones() / (w.getCalificaciones() + 3.0)) > EPS) wEnRango = false;
        }
        verificar("Tras 200 calificaciones al azar, todo w queda en [0,1] y lambda = n/(n+3)", wEnRango && !azar.isEmpty());
    }

    private List<String> claves(List<Outfit> outfits) {
        List<String> lista = new ArrayList<>();
        for (Outfit o : outfits) lista.add(o.claveCombinacion());
        return lista;
    }

    // ================================================================== explicaciones y rendimiento

    private void explicaciones() {
        seccion("Explicaciones por plantilla");
        Repositorio repo = closetEjemplo();
        Perfil perfil = perfilEjemplo();
        boolean soloPrendasPropias = true;
        boolean bienFormadas = true;
        boolean mencionaFrio = true;
        for (Evento e : Evento.values()) {
            for (ContextoClima clima : CLIMAS) {
                for (Outfit o : recomendador.recomendar(repo.listar(), new ContextoRecomendacion(e, clima, perfil, null), 3)) {
                    String texto = o.getExplicacion();
                    if (texto.isEmpty() || !texto.endsWith(".")) bienFormadas = false;
                    Set<String> propios = new HashSet<>();
                    for (Prenda p : o.getPrendas()) propios.add(Texto.legible(p.getSubtipo()));
                    for (Categoria c : Categoria.values()) {
                        for (String s : arbol.subtipos(c)) {
                            String palabra = Texto.legible(s);
                            Pattern patron = Pattern.compile("\\b" + Pattern.quote(palabra) + "\\b", Pattern.UNICODE_CHARACTER_CLASS);
                            if (!propios.contains(palabra) && patron.matcher(texto).find()) soloPrendasPropias = false;
                        }
                    }
                    if (clima.exigeExterior() && !texto.contains("porque hacen")) mencionaFrio = false;
                }
            }
        }
        verificar("Ninguna explicación menciona un tipo de prenda que no esté en su outfit", soloPrendasPropias);
        verificar("Toda explicación tiene texto y termina en punto", bienFormadas);
        verificar("Con frío, la explicación dice por qué incluye la prenda exterior", mencionaFrio);
    }

    private void rendimiento() {
        seccion("Rendimiento");
        List<Prenda> closet = closetEjemplo().listar();
        ContextoRecomendacion ctx = ContextoRecomendacion.basico(Evento.CASUAL);
        for (int i = 0; i < 3; i++) recomendador.recomendar(closet, ctx, 3); // calentamiento de la JVM
        int repeticiones = 10;
        long inicio = System.nanoTime();
        for (int i = 0; i < repeticiones; i++) recomendador.recomendar(closet, ctx, 3);
        double ms = (System.nanoTime() - inicio) / 1e6 / repeticiones;
        verificar(String.format("recomendar con el closet de ejemplo tarda menos de 300 ms (promedio %.1f ms)", ms), ms < 300);
    }
}
