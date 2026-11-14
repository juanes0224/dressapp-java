package com.dressapp.app;

import com.dressapp.datos.Repositorio;
import com.dressapp.modelo.Evento;
import com.dressapp.modelo.Outfit;
import com.dressapp.modelo.Perfil;
import com.dressapp.modelo.PesoAprendido;
import com.dressapp.modelo.Prenda;
import com.dressapp.motor.Puntuacion;
import com.dressapp.util.Texto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Formato de salida en la terminal. No tiene lógica de negocio. */
public final class Vista {

    private Vista() {}

    private static final String LINEA = "------------------------------------------------------------------------";

    public static void titulo(String texto) {
        System.out.println();
        System.out.println(LINEA);
        System.out.println("  " + texto);
        System.out.println(LINEA);
    }

    public static String num(double v) {
        return String.format("%.3f", v);
    }

    public static void tablaPrendas(List<Prenda> prendas) {
        if (prendas.isEmpty()) {
            System.out.println("  (sin prendas)");
            return;
        }
        System.out.printf("  %-4s %-22s %-10s %-14s %-12s %-12s %4s %6s  %s%n",
                "ID", "Nombre", "Capa", "Subtipo", "Color", "Fit", "Form", "Abrigo", "Eventos aptos");
        for (Prenda p : prendas) {
            System.out.printf("  %-4d %-22s %-10s %-14s %-12s %-12s %4d %6d  %s%n",
                    p.getId(), recortar(p.getNombre(), 22), p.getCategoria().etiqueta(),
                    Texto.legible(p.getSubtipo()), Texto.legible(p.getColorNombre()), p.getFit().etiqueta(),
                    p.getFormalidad(), p.getAbrigo(), eventos(p));
        }
        System.out.println("  " + prendas.size() + " prenda(s).");
    }

    private static String eventos(Prenda p) {
        if (p.getEventosAptos().isEmpty()) return "(ninguno)";
        return p.getEventosAptos().stream().map(Evento::etiqueta).collect(Collectors.joining(", "));
    }

    public static void outfit(Outfit o, int numero) {
        System.out.println();
        System.out.println("  Opción " + numero + "   G = " + num(o.getPuntaje())
                + (o.isFavorito() ? "   [favorito]" : "")
                + (o.getCalificaciones().isEmpty() ? "" : "   calificaciones: " + o.getCalificaciones()));
        for (Prenda p : o.getPrendas()) {
            System.out.printf("    %-10s #%-3d %s%n", p.getCategoria().etiqueta(), p.getId(), p.descripcionCorta());
        }
        System.out.printf("    Desglose:  u promedio %s (formalidad %s, clima %s, perfil %s)  |  c promedio %s%n",
                num(o.valorDesglose(Puntuacion.U)), num(o.valorDesglose(Puntuacion.FORMALIDAD)),
                num(o.valorDesglose(Puntuacion.CLIMA)), num(o.valorDesglose(Puntuacion.PERFIL)),
                num(o.valorDesglose(Puntuacion.C)));
        Double costo = o.getDesglose().get("costo_camino");
        if (costo != null) System.out.println("    Costo del camino en el grafo: " + num(costo));
        System.out.println("    Por qué:   " + o.getExplicacion());
    }

    public static void outfits(List<Outfit> outfits) {
        for (int i = 0; i < outfits.size(); i++) outfit(outfits.get(i), i + 1);
    }

    public static void listaCorta(List<Outfit> outfits) {
        for (int i = 0; i < outfits.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + outfits.get(i));
        }
    }

    public static void pesos(Map<String, PesoAprendido> pesos, Repositorio repo) {
        if (pesos.isEmpty()) {
            System.out.println("  Todavía no hay calificaciones: c(p,q) es igual a c_reglas (arranque en frío).");
            return;
        }
        List<PesoAprendido> lista = new ArrayList<>(pesos.values());
        lista.sort(Comparator.comparing((PesoAprendido w) -> w.getEvento())
                .thenComparingInt(PesoAprendido::getIdPrendaA)
                .thenComparingInt(PesoAprendido::getIdPrendaB));
        System.out.printf("  %-14s %-22s %-22s %7s %4s %7s%n", "Evento", "Prenda A", "Prenda B", "w", "n", "lambda");
        for (PesoAprendido w : lista) {
            System.out.printf("  %-14s %-22s %-22s %7s %4d %7s%n", w.getEvento().etiqueta(),
                    nombre(repo, w.getIdPrendaA()), nombre(repo, w.getIdPrendaB()),
                    num(w.getValor()), w.getCalificaciones(), num(w.lambda()));
        }
    }

    private static String nombre(Repositorio repo, int id) {
        Prenda p = repo.obtener(id);
        return recortar(p == null ? "#" + id + " (eliminada)" : "#" + id + " " + p.getNombre(), 22);
    }

    public static void perfil(Perfil perfil) {
        if (perfil == null) {
            System.out.println("  Sin perfil configurado: perfil(p) vale 0,5 para todas las prendas.");
            return;
        }
        System.out.println("  Ciudad:             " + (perfil.getCiudad().isEmpty() ? "-" : perfil.getCiudad()));
        System.out.println("  Estilos:            " + lista(perfil.getEstilos()));
        System.out.println("  Colores preferidos: " + lista(perfil.getColoresPreferidos()));
        System.out.println("  Colores evitados:   " + lista(perfil.getColoresEvitados()));
        System.out.println("  Fit preferido:      " + (perfil.getFitPreferido() == null ? "sin preferencia" : perfil.getFitPreferido().etiqueta()));
        System.out.println("  Eventos frecuentes: " + (perfil.getEventosFrecuentes().isEmpty() ? "-"
                : perfil.getEventosFrecuentes().stream().map(Evento::etiqueta).collect(Collectors.joining(", "))));
    }

    private static String lista(List<String> valores) {
        return valores.isEmpty() ? "-" : valores.stream().map(Texto::legible).collect(Collectors.joining(", "));
    }

    private static String recortar(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + ".";
    }
}
