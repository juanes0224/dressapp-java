package com.dressapp.motor;

import com.dressapp.modelo.Categoria;
import com.dressapp.modelo.ContextoClima;
import com.dressapp.modelo.Evento;
import com.dressapp.modelo.Perfil;
import com.dressapp.modelo.PesoAprendido;
import com.dressapp.modelo.Prenda;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Modelo de puntuación de la sección 2.4. Todas las funciones son puras, deterministas y
 * devuelven valores en [0, 1].
 *
 *   u(p,e)  idoneidad de una prenda para el evento
 *   c(p,q)  compatibilidad entre dos prendas (reglas + peso aprendido)
 *   G(o)    puntaje global de un outfit
 */
public final class Puntuacion {

    private Puntuacion() {}

    // Claves del desglose de G
    public static final String FORMALIDAD = "formalidad";
    public static final String CLIMA = "clima";
    public static final String PERFIL = "perfil";
    public static final String U = "u";
    public static final String C = "c";
    public static final String G = "G";

    // ------------------------------------------------------------------ u(p,e)

    /** form(p,e) = 1 - |formalidad(p) - formalidad_objetivo(e)| / 4 */
    public static double form(Prenda p, Evento e) {
        return 1 - Math.abs(p.getFormalidad() - e.getFormalidadObjetivo()) / 4.0;
    }

    /**
     * clima(p) = 1 - |abrigo(p) - abrigo_requerido(T)| / 4, o 1.0 si no hay clima.
     * Con lluvia probable se penaliza la sandalia y se bonifica el calzado cerrado.
     */
    public static double clima(Prenda p, ContextoClima clima) {
        if (!clima.estaActivo()) return 1.0;
        double valor = 1 - Math.abs(p.getAbrigo() - clima.abrigoRequerido()) / 4.0;
        if (clima.esLluviaProbable() && p.getCategoria() == Categoria.CALZADO) {
            valor += esCalzadoAbierto(p) ? -Config.PENALIZACION_SANDALIA_LLUVIA : Config.BONO_CALZADO_CERRADO_LLUVIA;
        }
        return recortar(valor);
    }

    public static boolean esCalzadoAbierto(Prenda p) {
        return p.getCategoria() == Categoria.CALZADO && p.getSubtipo().equals("sandalia");
    }

    /** perfil(p) = 0.5 + 0.5*[color preferido] - 0.5*[color evitado] + 0.25*[fit preferido], recortado a [0,1]. */
    public static double perfil(Prenda p, Perfil perfil) {
        double valor = 0.5;
        if (perfil != null) {
            if (perfil.prefiereColor(p.getColorNombre())) valor += 0.5;
            if (perfil.evitaColor(p.getColorNombre())) valor -= 0.5;
            if (perfil.getFitPreferido() == p.getFit()) valor += 0.25;
        }
        return recortar(valor);
    }

    /** u(p,e) = 0.6*form + 0.2*clima + 0.2*perfil */
    public static double u(Prenda p, ContextoRecomendacion ctx) {
        return Config.PESO_FORM * form(p, ctx.getEvento())
                + Config.PESO_CLIMA * clima(p, ctx.getClima())
                + Config.PESO_PERFIL * perfil(p, ctx.getPerfil());
    }

    // ------------------------------------------------------------------ c(p,q)

    public static boolean esParSuperiorInferior(Prenda p, Prenda q) {
        return (p.getCategoria() == Categoria.SUPERIOR && q.getCategoria() == Categoria.INFERIOR)
                || (p.getCategoria() == Categoria.INFERIOR && q.getCategoria() == Categoria.SUPERIOR);
    }

    /**
     * fit(p,q): solo se evalúa entre superior e inferior (en otro par vale 1.0).
     * |vol(p)-vol(q)| <= 1 -> 1.0 ; ambos >= 3 -> 0.9 en eventos relajados, 0.4 en formales ;
     * ambos slim -> 0.8 ; cualquier otro caso -> 0.6.
     */
    public static double fitPar(Prenda p, Prenda q, Evento e) {
        if (!esParSuperiorInferior(p, q)) return 1.0;
        int vp = p.getFit().getVolumen();
        int vq = q.getFit().getVolumen();
        if (vp == 1 && vq == 1) return Config.FIT_AMBOS_SLIM;
        if (vp >= 3 && vq >= 3) return e.esRelajado() ? Config.FIT_AMBOS_HOLGADOS_RELAJADO : Config.FIT_AMBOS_HOLGADOS_FORMAL;
        if (Math.abs(vp - vq) <= 1) return Config.FIT_EQUILIBRADO;
        return Config.FIT_OTRO;
    }

    /** c_reglas(p,q) = 0.6*color(p,q) + 0.4*fit(p,q) */
    public static double cReglas(Prenda p, Prenda q, Evento e) {
        return Config.PESO_COLOR * Colores.compatibilidad(p, q) + Config.PESO_FIT * fitPar(p, q, e);
    }

    /** c(p,q) = (1 - lambda)*c_reglas + lambda*w, con lambda = n/(n+3). Sin calificaciones, c = c_reglas. */
    public static double c(Prenda p, Prenda q, ContextoRecomendacion ctx) {
        double reglas = cReglas(p, q, ctx.getEvento());
        PesoAprendido peso = ctx.getPesos().get(PesoAprendido.clave(p.getId(), q.getId(), ctx.getEvento()));
        if (peso == null) return reglas;
        double lambda = peso.lambda();
        return (1 - lambda) * reglas + lambda * peso.getValor();
    }

    // ------------------------------------------------------------------ G(outfit)

    /** G(o) = 0.5*promedio(u) + 0.5*promedio(c) sobre TODOS los pares de prendas. */
    public static double g(List<Prenda> prendas, ContextoRecomendacion ctx) {
        return desglose(prendas, ctx).get(G);
    }

    public static Map<String, Double> desglose(List<Prenda> prendas, ContextoRecomendacion ctx) {
        double sumForm = 0, sumClima = 0, sumPerfil = 0, sumU = 0;
        for (Prenda p : prendas) {
            sumForm += form(p, ctx.getEvento());
            sumClima += clima(p, ctx.getClima());
            sumPerfil += perfil(p, ctx.getPerfil());
            sumU += u(p, ctx);
        }
        double sumC = 0;
        int pares = 0;
        for (int i = 0; i < prendas.size(); i++) {
            for (int j = i + 1; j < prendas.size(); j++) {
                sumC += c(prendas.get(i), prendas.get(j), ctx);
                pares++;
            }
        }
        int n = Math.max(1, prendas.size());
        double promU = prendas.isEmpty() ? 0 : sumU / n;
        double promC = pares == 0 ? 1.0 : sumC / pares;

        Map<String, Double> d = new LinkedHashMap<>();
        d.put(FORMALIDAD, sumForm / n);
        d.put(CLIMA, sumClima / n);
        d.put(PERFIL, sumPerfil / n);
        d.put(U, promU);
        d.put(C, promC);
        d.put(G, combinar(promU, promC));
        return d;
    }

    public static double combinar(double promedioU, double promedioC) {
        return Config.DELTA * promedioU + Config.EPSILON * promedioC;
    }

    private static double recortar(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
