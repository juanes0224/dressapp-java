package com.dressapp.motor;

import com.dressapp.modelo.Outfit;
import com.dressapp.modelo.PesoAprendido;
import com.dressapp.modelo.Prenda;

import java.util.List;
import java.util.Map;

/**
 * Aprendizaje por calificaciones (sección 2.4):
 *   r_norm = (valor - 1) / 4                  1 -> 0.0 ... 5 -> 1.0
 *   para cada par (p,q) del outfit:
 *     w(p,q,e) <- (1 - 0.3)*w(p,q,e) + 0.3*r_norm     (w inicial = c_reglas)
 *     n(p,q,e) <- n + 1
 * Como c(p,q) usa lambda = n/(n+3), con pocas calificaciones sigue mandando la regla.
 */
public final class Aprendizaje {

    private Aprendizaje() {}

    /** Actualiza los pesos de todos los pares del outfit. Devuelve cuántos pares se actualizaron. */
    public static int registrarCalificacion(Outfit outfit, int valor, Map<String, PesoAprendido> pesos) {
        if (valor < 1 || valor > 5) throw new IllegalArgumentException("La calificación debe estar entre 1 y 5.");
        double rNorm = (valor - 1) / 4.0;
        List<Prenda> prendas = outfit.getPrendas();
        int actualizados = 0;
        for (int i = 0; i < prendas.size(); i++) {
            for (int j = i + 1; j < prendas.size(); j++) {
                actualizarPar(prendas.get(i), prendas.get(j), outfit, rNorm, pesos);
                actualizados++;
            }
        }
        outfit.agregarCalificacion(valor);
        return actualizados;
    }

    private static void actualizarPar(Prenda p, Prenda q, Outfit outfit, double rNorm, Map<String, PesoAprendido> pesos) {
        String clave = PesoAprendido.clave(p.getId(), q.getId(), outfit.getEvento());
        PesoAprendido peso = pesos.get(clave);
        if (peso == null) {
            double inicial = Puntuacion.cReglas(p, q, outfit.getEvento());
            peso = new PesoAprendido(Math.min(p.getId(), q.getId()), Math.max(p.getId(), q.getId()), outfit.getEvento(), inicial);
            pesos.put(clave, peso);
        }
        peso.setValor((1 - Config.TASA_APRENDIZAJE) * peso.getValor() + Config.TASA_APRENDIZAJE * rNorm);
        peso.sumarCalificacion();
    }
}
