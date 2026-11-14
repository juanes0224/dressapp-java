package com.dressapp.motor;

import com.dressapp.modelo.Categoria;
import com.dressapp.modelo.ContextoClima;
import com.dressapp.modelo.Outfit;
import com.dressapp.modelo.Perfil;
import com.dressapp.modelo.Prenda;
import com.dressapp.util.Texto;

import java.util.ArrayList;
import java.util.List;

/**
 * Explicación por plantilla, construida solo con el desglose del motor y con las prendas del
 * propio outfit (nunca menciona una prenda que no esté en él). No usa ningún modelo de lenguaje.
 */
public final class Explicador {

    private Explicador() {}

    public static String explicar(Outfit outfit, ContextoRecomendacion ctx) {
        List<Prenda> prendas = outfit.getPrendas();
        List<String> frases = new ArrayList<>();
        Prenda superior = buscar(prendas, Categoria.SUPERIOR);
        Prenda inferior = buscar(prendas, Categoria.INFERIOR);

        fraseColor(prendas, superior, inferior, frases);
        fraseFit(superior, inferior, ctx, frases);
        fraseFormalidad(outfit, ctx, frases);
        fraseClima(prendas, ctx.getClima(), frases);
        frasePerfil(prendas, ctx.getPerfil(), frases);

        return String.join(". ", frases) + ".";
    }

    private static void fraseColor(List<Prenda> prendas, Prenda superior, Prenda inferior, List<String> frases) {
        for (Prenda p : prendas) {
            if (Colores.esNeutro(p)) {
                frases.add("El " + color(p) + " de tu " + subtipo(p) + " es neutro y combina con todo");
                return;
            }
        }
        if (superior != null && inferior != null) {
            Colores.Relacion r = Colores.relacion(superior, inferior);
            frases.add("Tu " + subtipo(superior) + " " + color(superior) + " y tu " + subtipo(inferior) + " "
                    + color(inferior) + ": " + r.descripcion);
        }
    }

    private static void fraseFit(Prenda superior, Prenda inferior, ContextoRecomendacion ctx, List<String> frases) {
        if (superior == null || inferior == null) return;
        int vs = superior.getFit().getVolumen();
        int vi = inferior.getFit().getVolumen();
        String par = subtipo(superior) + " " + superior.getFit().etiqueta() + " con " + subtipo(inferior) + " " + inferior.getFit().etiqueta();
        if (vs == 1 && vi == 1) {
            frases.add("Silueta ajustada: " + par);
        } else if (vs >= 3 && vi >= 3) {
            frases.add(ctx.getEvento().esRelajado()
                    ? "Dos prendas holgadas (" + par + "), un look relajado que encaja en el evento"
                    : "Dos prendas holgadas (" + par + ") restan formalidad al conjunto");
        } else if (vs == vi) {
            frases.add("Proporciones parejas: " + subtipo(superior) + " y " + subtipo(inferior) + " con fit " + superior.getFit().etiqueta());
        } else if (Math.abs(vs - vi) == 1) {
            frases.add("Contraste de fit equilibrado: " + par);
        }
    }

    private static void fraseFormalidad(Outfit outfit, ContextoRecomendacion ctx, List<String> frases) {
        double form = outfit.valorDesglose(Puntuacion.FORMALIDAD);
        String evento = ctx.getEvento().enFrase();
        if (form >= 0.9) frases.add("Formalidad adecuada para " + evento);
        else if (form >= 0.75) frases.add("Formalidad cercana a la ideal para " + evento);
        else frases.add("Formalidad aceptable, aunque no ideal, para " + evento);
    }

    private static void fraseClima(List<Prenda> prendas, ContextoClima clima, List<String> frases) {
        if (!clima.estaActivo()) return;
        Prenda exterior = buscar(prendas, Categoria.EXTERIOR);
        String grados = clima.temperaturaTexto();
        if (exterior != null && clima.exigeExterior()) {
            frases.add("Incluye " + subtipo(exterior) + " porque hacen " + grados);
        } else if (exterior != null) {
            frases.add("Suma " + subtipo(exterior) + " como capa extra con " + grados);
        }
        Prenda calzado = buscar(prendas, Categoria.CALZADO);
        if (clima.esLluviaProbable() && calzado != null && !Puntuacion.esCalzadoAbierto(calzado)) {
            frases.add("Calzado cerrado (" + subtipo(calzado) + ") por la probabilidad de lluvia");
        }
    }

    private static void frasePerfil(List<Prenda> prendas, Perfil perfil, List<String> frases) {
        if (perfil == null) return;
        for (Prenda p : prendas) {
            if (perfil.prefiereColor(p.getColorNombre())) {
                frases.add("Incluye uno de tus colores preferidos (" + color(p) + ")");
                return;
            }
        }
    }

    private static Prenda buscar(List<Prenda> prendas, Categoria categoria) {
        for (Prenda p : prendas) {
            if (p.getCategoria() == categoria) return p;
        }
        return null;
    }

    private static String subtipo(Prenda p) {
        return Texto.legible(p.getSubtipo());
    }

    private static String color(Prenda p) {
        return Texto.legible(p.getColorNombre());
    }
}
