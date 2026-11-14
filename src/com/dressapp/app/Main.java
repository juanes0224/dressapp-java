package com.dressapp.app;

import com.dressapp.datos.ClosetEjemplo;
import com.dressapp.datos.Repositorio;
import com.dressapp.estructuras.ArbolCategorias;
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
import com.dressapp.motor.Config;
import com.dressapp.motor.ContextoRecomendacion;
import com.dressapp.motor.Puntuacion;
import com.dressapp.motor.Recomendador;
import com.dressapp.util.Texto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DressApp en terminal: calculador de outfits por puntaje para cada evento.
 * Sin interfaz gráfica y sin IA; toda la decisión la toma el motor de reglas y grafos.
 */
public class Main {

    private final Consola consola = new Consola(System.in);
    private final ArbolCategorias arbol = new ArbolCategorias();
    private final Repositorio repo = new Repositorio(arbol);
    private final Recomendador recomendador = new Recomendador();
    private final Map<String, PesoAprendido> pesos = new LinkedHashMap<>();
    private final List<Outfit> favoritos = new ArrayList<>();
    private List<Outfit> ultimos = new ArrayList<>();
    private Perfil perfil;

    public static void main(String[] args) {
        new Main().ejecutar();
    }

    private void ejecutar() {
        ClosetEjemplo.cargar(repo);
        Vista.titulo("DressApp - calculador de outfits por puntaje (versión terminal)");
        System.out.println("  Se cargó un closet de ejemplo con " + repo.tamano() + " prendas.");
        System.out.println("  Consejo: empieza por la opción 15 (demostración guiada) o la 8 (recomendar).");

        try {
            boolean seguir = true;
            while (seguir) {
                mostrarMenu();
                int opcion = consola.entero("Opción: ", 0, 15);
                try {
                    seguir = atender(opcion);
                } catch (CapaVaciaException e) {
                    System.out.println("\n  No se puede armar el outfit: " + e.getMessage());
                    System.out.println("  Agrega una prenda de esa capa (opción 3) o marca una existente como apta (opción 4).");
                } catch (IllegalArgumentException e) {
                    System.out.println("\n  Dato inválido: " + e.getMessage());
                }
            }
        } catch (Consola.FinDeEntrada e) {
            System.out.println();
        }
        System.out.println("  Hasta luego.");
    }

    private void mostrarMenu() {
        System.out.println();
        System.out.println("  PERFIL Y CLOSET");
        System.out.println("   1. Configurar perfil" + (perfil == null ? "" : " (ya configurado)"));
        System.out.println("   2. Ver closet");
        System.out.println("   3. Agregar prenda");
        System.out.println("   4. Editar prenda");
        System.out.println("   5. Eliminar prenda");
        System.out.println("   6. Buscar prenda por nombre (autocompletado con trie)");
        System.out.println("   7. Filtrar prendas");
        System.out.println("  RECOMENDACIÓN");
        System.out.println("   8. Recomendar outfits para un evento");
        System.out.println("   9. Completar un outfit a partir de una prenda");
        System.out.println("  10. Calificar un outfit recomendado");
        System.out.println("  11. Favoritos");
        System.out.println("  ESTRUCTURAS Y VERIFICACIÓN");
        System.out.println("  12. Ver pesos aprendidos");
        System.out.println("  13. Ver árbol de categorías");
        System.out.println("  14. Ejecutar autoverificación");
        System.out.println("  15. Demostración guiada");
        System.out.println("   0. Salir");
    }

    private boolean atender(int opcion) {
        switch (opcion) {
            case 1: configurarPerfil(); break;
            case 2: verCloset(); break;
            case 3: agregarPrenda(); break;
            case 4: editarPrenda(); break;
            case 5: eliminarPrenda(); break;
            case 6: buscarPorNombre(); break;
            case 7: filtrar(); break;
            case 8: recomendar(); break;
            case 9: completar(); break;
            case 10: calificar(); break;
            case 11: favoritos(); break;
            case 12: verPesos(); break;
            case 13: verArbol(); break;
            case 14: new Pruebas().ejecutar(); break;
            case 15: new Demo(consola).ejecutar(); break;
            case 0: return false;
            default: break;
        }
        return true;
    }

    // ================================================================== perfil

    private void configurarPerfil() {
        Vista.titulo("Configurar perfil");
        if (perfil != null) {
            System.out.println("  Perfil actual:");
            Vista.perfil(perfil);
            if (!consola.siNo("\n  ¿Quieres reemplazarlo?")) return;
        }
        Perfil nuevo = new Perfil();
        nuevo.setCiudad(consola.linea("  Ciudad (opcional): "));
        nuevo.setEstilos(consola.elegirVarios("  Estilos que te gustan:", Perfil.ESTILOS_DISPONIBLES, Texto::legible,
                new ArrayList<>()));
        System.out.println("  Colores registrados en tu closet: " + legibles(repo.coloresRegistrados()));
        nuevo.setColoresPreferidos(consola.listaTexto("  Colores preferidos, separados por coma (Enter = ninguno): "));
        nuevo.setColoresEvitados(consola.listaTexto("  Colores que prefieres evitar (Enter = ninguno): "));
        nuevo.setFitPreferido(consola.elegirOpcional("  Fit preferido:", Arrays.asList(Fit.values()), Fit::etiqueta,
                "sin preferencia"));
        nuevo.setEventosFrecuentes(consola.elegirVarios("  Eventos a los que vas con frecuencia:",
                Arrays.asList(Evento.values()), Evento::etiqueta, new ArrayList<>()));
        perfil = nuevo;
        System.out.println("\n  Perfil guardado. Desde ahora perfil(p) usa tus colores y tu fit preferido.");
    }

    private static String legibles(List<String> claves) {
        List<String> out = new ArrayList<>();
        for (String c : claves) out.add(Texto.legible(c));
        return String.join(", ", out);
    }

    // ================================================================== closet

    private void verCloset() {
        Vista.titulo("Mi closet");
        Vista.tablaPrendas(repo.listar());
    }

    private void agregarPrenda() {
        Vista.titulo("Agregar prenda");
        String nombre = consola.texto("  Nombre (p. ej. Camisa de lino): ");
        Categoria categoria = consola.elegir("  Capa:", Arrays.asList(Categoria.values()), Categoria::etiqueta);
        String subtipo = consola.elegir("  Subtipo:", arbol.subtipos(categoria), Texto::legible);
        String colorNombre = consola.texto("  Color (nombre, p. ej. negro, azul marino, rojo): ");
        String hex = leerHex("  Color en hex (p. ej. #1B1F3B): ");
        Fit fit = consola.elegir("  Fit:", Arrays.asList(Fit.values()), Fit::etiqueta);
        int formalidad = consola.entero("  Formalidad de 1 (muy informal) a 5 (muy formal): ", 1, 5);
        int abrigo = consola.entero("  Abrigo de 1 (muy fresca) a 5 (muy abrigada): ", 1, 5);
        List<Evento> sugeridos = eventosSugeridos(formalidad);
        List<Evento> eventos = consola.elegirVarios("  Eventos para los que sirve:", Arrays.asList(Evento.values()),
                e -> e.etiqueta() + " (formalidad objetivo " + e.getFormalidadObjetivo() + ")", sugeridos);

        Prenda p = repo.agregar(nombre, categoria, subtipo, hex, colorNombre, fit, formalidad, abrigo, eventos);
        System.out.println("\n  Prenda agregada: " + p);
        if (eventos.isEmpty()) System.out.println("  Ojo: sin eventos aptos, esta prenda nunca se recomendará.");
    }

    /** Eventos cuya formalidad objetivo está a 2 niveles o menos (los que pasarían el filtro duro). */
    private static List<Evento> eventosSugeridos(int formalidad) {
        List<Evento> sugeridos = new ArrayList<>();
        for (Evento e : Evento.values()) {
            if (Math.abs(formalidad - e.getFormalidadObjetivo()) <= 2) sugeridos.add(e);
        }
        return sugeridos;
    }

    private String leerHex(String mensaje) {
        while (true) {
            String hex = consola.texto(mensaje);
            if (!hex.startsWith("#")) hex = "#" + hex;
            if (hex.matches("#[0-9A-Fa-f]{6}")) return hex;
            System.out.println("  Formato esperado: # seguido de 6 dígitos hexadecimales (p. ej. #FF0000).");
        }
    }

    private Prenda pedirPrenda(String mensaje) {
        int id = consola.entero(mensaje, 1, Integer.MAX_VALUE);
        Prenda p = repo.obtener(id);
        if (p == null) System.out.println("  No existe una prenda con id " + id + ". Mira los ids en la opción 2.");
        return p;
    }

    private void editarPrenda() {
        Vista.titulo("Editar prenda");
        Prenda p = pedirPrenda("  Id de la prenda: ");
        if (p == null) return;
        System.out.println("  Actual: " + p + ", formalidad " + p.getFormalidad() + ", abrigo " + p.getAbrigo());
        List<String> campos = Arrays.asList("nombre", "color", "fit", "formalidad", "abrigo", "eventos aptos");
        String campo = consola.elegirOpcional("  ¿Qué quieres cambiar?", campos, s -> s, "cancelar");
        if (campo == null) return;
        switch (campo) {
            case "nombre": {
                String nombre = consola.texto("  Nuevo nombre: ");
                repo.editar(p.getId(), x -> x.setNombre(nombre));
                break;
            }
            case "color": {
                String nombre = consola.texto("  Nuevo color (nombre): ");
                String hex = leerHex("  Nuevo color en hex: ");
                repo.editar(p.getId(), x -> x.setColor(hex, nombre));
                break;
            }
            case "fit": {
                Fit fit = consola.elegir("  Nuevo fit:", Arrays.asList(Fit.values()), Fit::etiqueta);
                repo.editar(p.getId(), x -> x.setFit(fit));
                break;
            }
            case "formalidad": {
                int f = consola.entero("  Nueva formalidad (1-5): ", 1, 5);
                repo.editar(p.getId(), x -> x.setFormalidad(f));
                break;
            }
            case "abrigo": {
                int a = consola.entero("  Nuevo abrigo (1-5): ", 1, 5);
                repo.editar(p.getId(), x -> x.setAbrigo(a));
                break;
            }
            default: {
                List<Evento> eventos = consola.elegirVarios("  Eventos aptos:", Arrays.asList(Evento.values()),
                        Evento::etiqueta, new ArrayList<>(p.getEventosAptos()));
                repo.editar(p.getId(), x -> x.setEventosAptos(eventos));
                break;
            }
        }
        System.out.println("  Prenda actualizada: " + p);
    }

    private void eliminarPrenda() {
        Vista.titulo("Eliminar prenda");
        Prenda p = pedirPrenda("  Id de la prenda: ");
        if (p == null) return;
        if (consola.siNo("  ¿Eliminar " + p.getNombre() + "?")) {
            repo.eliminar(p.getId());
            System.out.println("  Prenda eliminada. El trie y los índices ya no la incluyen.");
        }
    }

    private void buscarPorNombre() {
        Vista.titulo("Buscar por nombre");
        String prefijo = consola.linea("  Escribe el comienzo del nombre (sin importar tildes ni mayúsculas): ");
        List<Prenda> resultado = repo.buscarPorNombre(prefijo, 20);
        if (resultado.isEmpty()) System.out.println("  Ninguna prenda empieza por \"" + prefijo + "\".");
        else Vista.tablaPrendas(resultado);
    }

    private void filtrar() {
        Vista.titulo("Filtrar prendas");
        Categoria categoria = consola.elegirOpcional("  Capa:", Arrays.asList(Categoria.values()), Categoria::etiqueta, "cualquiera");
        String color = consola.elegirOpcional("  Color:", repo.coloresRegistrados(), Texto::legible, "cualquiera");
        Fit fit = consola.elegirOpcional("  Fit:", Arrays.asList(Fit.values()), Fit::etiqueta, "cualquiera");
        Evento evento = consola.elegirOpcional("  Evento apto:", Arrays.asList(Evento.values()), Evento::etiqueta, "cualquiera");
        Vista.tablaPrendas(repo.filtrar(categoria, color, fit, evento));
    }

    // ================================================================== recomendación

    private ContextoRecomendacion pedirContexto() {
        Evento evento = consola.elegir("  Evento:", Arrays.asList(Evento.values()),
                e -> e.etiqueta() + " (formalidad objetivo " + e.getFormalidadObjetivo() + ")");
        ContextoClima clima = ContextoClima.sinClima();
        if (consola.siNo("  ¿Quieres tener en cuenta el clima?")) {
            double t = consola.decimal("  Temperatura en °C: ", -30, 50);
            boolean lluvia = consola.siNo("  ¿La probabilidad de lluvia es de 60 % o más?");
            clima = ContextoClima.de(t, lluvia);
        }
        return new ContextoRecomendacion(evento, clima, perfil, pesos);
    }

    private void recomendar() {
        Vista.titulo("Recomendar outfits");
        ContextoRecomendacion ctx = pedirContexto();
        long inicio = System.nanoTime();
        List<Outfit> outfits = recomendador.recomendar(repo.listar(), ctx, Config.N_RECOMENDACIONES);
        long ms = (System.nanoTime() - inicio) / 1_000_000;

        Vista.titulo("Mejores outfits para " + ctx.getEvento().enFrase() + " (" + ctx.getClima() + ")");
        if (ctx.getClima().exigeExterior()) System.out.println("  Con este frío la capa exterior es obligatoria.");
        Vista.outfits(outfits);
        System.out.println("\n  Calculado en " + ms + " ms (grafo por capas + Yen + re-ranking por G + diversidad).");
        if (outfits.size() < Config.N_RECOMENDACIONES) {
            System.out.println("  Solo hay " + outfits.size() + " combinación(es) suficientemente distintas en tu closet.");
        }
        ultimos = outfits;
    }

    private void completar() {
        Vista.titulo("Completar un outfit a partir de una prenda");
        Prenda fija = pedirPrenda("  Id de la prenda que quieres usar sí o sí: ");
        if (fija == null) return;
        ContextoRecomendacion ctx = pedirContexto();
        if (!Recomendador.pasaFiltroDuro(fija, ctx)) {
            System.out.println("  Aviso: " + fija.getNombre() + " no es ideal para " + ctx.getEvento().enFrase()
                    + " (formalidad " + fija.getFormalidad() + "), pero se usa igual porque la elegiste.");
        }
        Recomendador.ResultadoBacktracking r = recomendador.completarOutfit(fija, repo.listar(), ctx, Config.N_RECOMENDACIONES);

        Vista.titulo("Outfits con " + fija.getNombre() + " para " + ctx.getEvento().enFrase());
        Vista.outfits(r.outfits);
        System.out.println("\n  Backtracking: " + r.nodosExplorados + " nodos explorados, " + r.ramasPodadas
                + " ramas podadas por la cota optimista.");
        ultimos = r.outfits;
    }

    private Outfit elegirDeUltimos(String accion) {
        if (ultimos.isEmpty()) {
            System.out.println("  Primero pide una recomendación (opción 8 o 9).");
            return null;
        }
        System.out.println("  Últimos outfits recomendados:");
        Vista.listaCorta(ultimos);
        return ultimos.get(consola.entero("  ¿Cuál quieres " + accion + "? ", 1, ultimos.size()) - 1);
    }

    private void calificar() {
        Vista.titulo("Calificar un outfit");
        Outfit o = elegirDeUltimos("calificar");
        if (o == null) return;
        int valor = consola.entero("  Calificación de 1 (no me gusta) a 5 (me encanta): ", 1, 5);
        double antes = promedioC(o);
        int pares = Aprendizaje.registrarCalificacion(o, valor, pesos);
        double despues = promedioC(o);
        Prenda a = o.getPrendas().get(0);
        Prenda b = o.getPrendas().get(1);
        PesoAprendido ejemplo = pesos.get(PesoAprendido.clave(a.getId(), b.getId(), o.getEvento()));
        System.out.println("  Se actualizaron los " + pares + " pares de prendas de este outfit (evento " + o.getEvento().etiqueta() + ").");
        System.out.println("  Ejemplo, " + a.getNombre() + " + " + b.getNombre() + ": w = " + Vista.num(ejemplo.getValor())
                + ", n = " + ejemplo.getCalificaciones() + ", lambda = " + Vista.num(ejemplo.lambda()));
        if (Math.abs(antes - despues) < 1e-9) {
            System.out.println("  Su compatibilidad promedio sigue en " + Vista.num(despues)
                    + ": la calificación confirma lo que ya decían las reglas.");
        } else {
            System.out.println("  Compatibilidad promedio de este outfit: " + Vista.num(antes) + " -> " + Vista.num(despues));
        }
        System.out.println("  La próxima recomendación para ese evento ya tiene en cuenta tu calificación (opción 12 para ver los pesos).");
    }

    private double promedioC(Outfit o) {
        ContextoRecomendacion ctx = new ContextoRecomendacion(o.getEvento(), null, perfil, pesos);
        return Puntuacion.desglose(o.getPrendas(), ctx).get(Puntuacion.C);
    }

    private void favoritos() {
        Vista.titulo("Favoritos");
        List<String> opciones = Arrays.asList("marcar un outfit reciente como favorito", "ver favoritos", "quitar un favorito");
        String opcion = consola.elegirOpcional("  ¿Qué quieres hacer?", opciones, s -> s, "volver");
        if (opcion == null) return;
        if (opcion.startsWith("marcar")) {
            Outfit o = elegirDeUltimos("marcar como favorito");
            if (o == null) return;
            o.setFavorito(true);
            if (!favoritos.contains(o)) favoritos.add(o);
            System.out.println("  Guardado en favoritos.");
        } else if (opcion.startsWith("ver")) {
            if (favoritos.isEmpty()) System.out.println("  Todavía no tienes favoritos.");
            else Vista.outfits(favoritos);
        } else {
            if (favoritos.isEmpty()) {
                System.out.println("  Todavía no tienes favoritos.");
                return;
            }
            Vista.listaCorta(favoritos);
            Outfit o = favoritos.remove(consola.entero("  ¿Cuál quitas? ", 1, favoritos.size()) - 1);
            o.setFavorito(false);
            System.out.println("  Quitado de favoritos.");
        }
    }

    // ================================================================== estructuras

    private void verPesos() {
        Vista.titulo("Pesos aprendidos w(p,q,evento)");
        Vista.pesos(pesos, repo);
        System.out.println("\n  c(p,q) = (1 - lambda) * c_reglas + lambda * w, con lambda = n / (n + 3).");
    }

    private void verArbol() {
        Vista.titulo("Árbol de categorías (recorrido DFS)");
        for (String linea : arbol.recorridoDFS()) System.out.println("  " + linea);
        System.out.println("\n  " + Categoria.values().length + " categorías, " + arbol.cantidadSubtipos() + " subtipos.");
    }
}
