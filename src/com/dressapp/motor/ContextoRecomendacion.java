package com.dressapp.motor;

import com.dressapp.modelo.ContextoClima;
import com.dressapp.modelo.Evento;
import com.dressapp.modelo.Perfil;
import com.dressapp.modelo.PesoAprendido;

import java.util.Collections;
import java.util.Map;

/** Todo lo que la puntuación necesita saber además de las prendas: evento, clima, perfil y pesos. */
public final class ContextoRecomendacion {

    private final Evento evento;
    private final ContextoClima clima;
    private final Perfil perfil;
    private final Map<String, PesoAprendido> pesos;

    public ContextoRecomendacion(Evento evento, ContextoClima clima, Perfil perfil, Map<String, PesoAprendido> pesos) {
        if (evento == null) throw new IllegalArgumentException("El evento es obligatorio.");
        this.evento = evento;
        this.clima = clima == null ? ContextoClima.sinClima() : clima;
        this.perfil = perfil;
        this.pesos = pesos == null ? Collections.emptyMap() : pesos;
    }

    /** Contexto mínimo: sin clima, sin perfil y sin calificaciones. */
    public static ContextoRecomendacion basico(Evento evento) {
        return new ContextoRecomendacion(evento, null, null, null);
    }

    public Evento getEvento() { return evento; }
    public ContextoClima getClima() { return clima; }
    public Perfil getPerfil() { return perfil; }
    public Map<String, PesoAprendido> getPesos() { return pesos; }
}
