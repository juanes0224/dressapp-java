package com.dressapp.app;

import com.dressapp.datos.ClosetEjemplo;
import com.dressapp.datos.Repositorio;
import com.dressapp.estructuras.ArbolCategorias;
import com.dressapp.modelo.ContextoClima;
import com.dressapp.modelo.Evento;
import com.dressapp.modelo.Fit;
import com.dressapp.modelo.Outfit;
import com.dressapp.modelo.Perfil;
import com.dressapp.modelo.PesoAprendido;
import com.dressapp.modelo.Prenda;
import com.dressapp.motor.Aprendizaje;
import com.dressapp.motor.ContextoRecomendacion;
import com.dressapp.motor.Recomendador;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Guion de demostración de punta a punta (perfil -> closet -> recomendación para dos eventos ->
 * calificación -> cambio en la recomendación). Usa un estado propio, así no altera lo que el
 * usuario haya hecho en el menú.
 */
public class Demo {

    private final Consola consola;
    private final Repositorio repo = new Repositorio(new ArbolCategorias());
    private final Recomendador recomendador = new Recomendador();
    private final Map<String, PesoAprendido> pesos = new LinkedHashMap<>();

    public Demo(Consola consola) {
        this.consola = consola;
    }

    public void ejecutar() {
        ClosetEjemplo.cargar(repo);

        Vista.titulo("Demo 1/6 - Perfil inicial");
        Perfil perfil = new Perfil();
        perfil.setCiudad("Bogotá");
        perfil.setEstilos(Arrays.asList("casual", "urbano"));
        perfil.setColoresPreferidos(Arrays.asList("negro", "blanco"));
        perfil.setColoresEvitados(Arrays.asList("rojo"));
        perfil.setFitPreferido(Fit.REGULAR);
        perfil.setEventosFrecuentes(Arrays.asList(Evento.CASUAL, Evento.MEDIO_FORMAL));
        Vista.perfil(perfil);
        System.out.println("\n  Closet de ejemplo: " + repo.tamano() + " prendas.");
        consola.pausa();

        Vista.titulo("Demo 2/6 - Recomendación para un evento casual, sin clima");
        ContextoRecomendacion casual = new ContextoRecomendacion(Evento.CASUAL, null, perfil, pesos);
        List<Outfit> antes = recomendador.recomendar(repo.listar(), casual, 3);
        Vista.outfits(antes);
        consola.pausa();

        Vista.titulo("Demo 3/6 - Medio formal con 8 °C y lluvia probable");
        ContextoRecomendacion frio = new ContextoRecomendacion(Evento.MEDIO_FORMAL, ContextoClima.de(8, true), perfil, pesos);
        System.out.println("  A 8 °C el abrigo requerido es 4: la capa exterior se vuelve obligatoria.");
        System.out.println("  Con lluvia probable, el calzado cerrado suma y la sandalia resta en clima(p).");
        Vista.outfits(recomendador.recomendar(repo.listar(), frio, 3));
        consola.pausa();

        Vista.titulo("Demo 4/6 - Aprendizaje: el usuario califica con 1 la opción 1 casual, cinco veces");
        Outfit rechazado = antes.get(0);
        System.out.println("  Outfit calificado: " + rechazado);
        for (int i = 0; i < 5; i++) Aprendizaje.registrarCalificacion(rechazado, 1, pesos);
        System.out.println();
        Vista.pesos(pesos, repo);
        System.out.println("\n  Con n = 5, lambda = 5/8 = 0,625: ahora el peso aprendido manda más que la regla.");
        consola.pausa();

        Vista.titulo("Demo 5/6 - Nueva recomendación casual");
        List<Outfit> despues = recomendador.recomendar(repo.listar(), casual, 3);
        Vista.outfits(despues);
        boolean sigue = false;
        for (Outfit o : despues) {
            if (o.claveCombinacion().equals(rechazado.claveCombinacion())) sigue = true;
        }
        System.out.println("\n  " + (sigue
                ? "El outfit rechazado sigue en el top 3 (bajó su puntaje, pero no lo suficiente)."
                : "El outfit rechazado ya no aparece en el top 3: la recomendación cambió con la calificación."));
        consola.pausa();

        Vista.titulo("Demo 6/6 - Completar un outfit alrededor de la blusa roja, para discoteca");
        Prenda blusa = repo.buscarPorNombre("blusa", 1).get(0);
        ContextoRecomendacion disco = new ContextoRecomendacion(Evento.DISCOTECA, null, perfil, pesos);
        Recomendador.ResultadoBacktracking conPoda = recomendador.completarOutfit(blusa, repo.listar(), disco, 3);
        Recomendador.ResultadoBacktracking sinPoda = recomendador.completarOutfitExhaustivo(blusa, repo.listar(), disco, 3);
        Vista.outfits(conPoda.outfits);
        System.out.println("\n  Con poda:  " + conPoda.nodosExplorados + " nodos explorados, " + conPoda.ramasPodadas + " ramas podadas.");
        boolean iguales = conPoda.outfits.size() == sinPoda.outfits.size();
        for (int i = 0; iguales && i < conPoda.outfits.size(); i++) {
            iguales = Math.abs(conPoda.outfits.get(i).getPuntaje() - sinPoda.outfits.get(i).getPuntaje()) < 1e-9;
        }
        System.out.println("  Sin poda:  " + sinPoda.nodosExplorados + " nodos explorados. "
                + (iguales ? "Mismos puntajes en el top 3: la poda no perdió ninguna solución."
                           : "ATENCIÓN: los puntajes difieren."));
        System.out.println("  (La blusa es roja, un color evitado en el perfil: por eso su perfil(p) es 0.)");
        System.out.println("\n  Fin de la demostración.");
    }
}
