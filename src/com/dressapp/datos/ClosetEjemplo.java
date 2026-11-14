package com.dressapp.datos;

import com.dressapp.modelo.Evento;

import java.util.Arrays;
import java.util.List;

import static com.dressapp.modelo.Categoria.ACCESORIO;
import static com.dressapp.modelo.Categoria.CALZADO;
import static com.dressapp.modelo.Categoria.EXTERIOR;
import static com.dressapp.modelo.Categoria.INFERIOR;
import static com.dressapp.modelo.Categoria.SUPERIOR;
import static com.dressapp.modelo.Evento.CASUAL;
import static com.dressapp.modelo.Evento.DISCOTECA;
import static com.dressapp.modelo.Evento.MEDIO_FORMAL;
import static com.dressapp.modelo.Evento.MUY_FORMAL;
import static com.dressapp.modelo.Evento.NOCTURNO;
import static com.dressapp.modelo.Fit.BAGGY;
import static com.dressapp.modelo.Fit.REGULAR;
import static com.dressapp.modelo.Fit.SLIM;
import static com.dressapp.modelo.Fit.ULTRA_BAGGY;

/**
 * Closet de muestra (31 prendas). Cubre todas las categorías y deja al menos dos opciones por
 * capa obligatoria en cada evento, para que la recomendación y la diversidad tengan con qué trabajar.
 */
public final class ClosetEjemplo {

    private ClosetEjemplo() {}

    public static void cargar(Repositorio r) {
        //        nombre                  categoría  subtipo          hex        color          fit          form abr eventos aptos
        r.agregar("Camiseta blanca",      SUPERIOR,  "camiseta",      "#FFFFFF", "blanco",      REGULAR,     2, 1, ev(CASUAL, NOCTURNO));
        r.agregar("Camiseta negra",       SUPERIOR,  "camiseta",      "#111111", "negro",       REGULAR,     2, 1, ev(CASUAL, DISCOTECA, NOCTURNO));
        r.agregar("Camisa blanca",        SUPERIOR,  "camisa",        "#FFFFFF", "blanco",      SLIM,        5, 2, ev(MUY_FORMAL, MEDIO_FORMAL));
        r.agregar("Camisa celeste",       SUPERIOR,  "camisa",        "#87CEEB", "celeste",     REGULAR,     4, 2, ev(MEDIO_FORMAL, MUY_FORMAL, NOCTURNO));
        r.agregar("Polo azul marino",     SUPERIOR,  "polo",          "#1B1F3B", "azul marino", SLIM,        3, 2, ev(MEDIO_FORMAL, CASUAL, NOCTURNO));
        r.agregar("Buzo gris",            SUPERIOR,  "buzo",          "#808080", "gris",        ULTRA_BAGGY, 1, 4, ev(CASUAL));
        r.agregar("Blusa roja",           SUPERIOR,  "blusa",         "#C0392B", "rojo",        SLIM,        3, 2, ev(NOCTURNO, DISCOTECA));

        r.agregar("Chaqueta de cuero",    EXTERIOR,  "chaqueta",      "#1A1A1A", "negro",       REGULAR,     3, 4, ev(CASUAL, NOCTURNO, DISCOTECA));
        r.agregar("Blazer azul marino",   EXTERIOR,  "blazer",        "#1B1F3B", "azul marino", REGULAR,     5, 3, ev(MUY_FORMAL, MEDIO_FORMAL));
        r.agregar("Abrigo gris",          EXTERIOR,  "abrigo",        "#7F8C8D", "gris",        BAGGY,       4, 5, ev(MEDIO_FORMAL, MUY_FORMAL, CASUAL));
        r.agregar("Chaleco beige",        EXTERIOR,  "chaleco",       "#E8DCC5", "beige",       REGULAR,     3, 3, ev(CASUAL, MEDIO_FORMAL));

        r.agregar("Jean azul",            INFERIOR,  "jean",          "#2255CC", "azul",        REGULAR,     2, 2, ev(CASUAL));
        r.agregar("Jean negro",           INFERIOR,  "jean",          "#222222", "negro",       SLIM,        3, 2, ev(CASUAL, NOCTURNO, DISCOTECA));
        r.agregar("Pantalón negro",       INFERIOR,  "pantalon",      "#000000", "negro",       SLIM,        5, 2, ev(MUY_FORMAL, MEDIO_FORMAL));
        r.agregar("Pantalón gris",        INFERIOR,  "pantalon",      "#6E6E6E", "gris",        SLIM,        5, 2, ev(MUY_FORMAL, MEDIO_FORMAL));
        r.agregar("Pantalón beige",       INFERIOR,  "pantalon",      "#E8DCC5", "beige",       REGULAR,     4, 2, ev(MEDIO_FORMAL, CASUAL));
        r.agregar("Sudadera gris",        INFERIOR,  "sudadera",      "#808080", "gris",        ULTRA_BAGGY, 1, 3, ev(CASUAL));
        r.agregar("Short de jean",        INFERIOR,  "short",         "#4A78C2", "azul",        REGULAR,     1, 1, ev(CASUAL));
        r.agregar("Falda negra",          INFERIOR,  "falda",         "#000000", "negro",       SLIM,        3, 1, ev(NOCTURNO, DISCOTECA));

        r.agregar("Tenis blancos",        CALZADO,   "tenis",         "#FFFFFF", "blanco",      REGULAR,     1, 1, ev(CASUAL));
        r.agregar("Tenis negros",         CALZADO,   "tenis",         "#111111", "negro",       REGULAR,     2, 1, ev(CASUAL, NOCTURNO, DISCOTECA));
        r.agregar("Zapato formal negro",  CALZADO,   "zapato_formal", "#000000", "negro",       SLIM,        5, 2, ev(MUY_FORMAL, MEDIO_FORMAL));
        r.agregar("Zapato formal café",   CALZADO,   "zapato_formal", "#5C3A21", "cafe",        SLIM,        5, 2, ev(MUY_FORMAL, MEDIO_FORMAL));
        r.agregar("Botín negro",          CALZADO,   "bota",          "#1C1C1C", "negro",       SLIM,        4, 3, ev(MEDIO_FORMAL, NOCTURNO, DISCOTECA));
        r.agregar("Bota marrón",          CALZADO,   "bota",          "#8B4513", "marron",      REGULAR,     3, 4, ev(CASUAL, MEDIO_FORMAL));
        r.agregar("Sandalia beige",       CALZADO,   "sandalia",      "#E8DCC5", "beige",       REGULAR,     1, 1, ev(CASUAL));

        r.agregar("Reloj plateado",       ACCESORIO, "reloj",         "#C0C0C0", "plateado",    REGULAR,     4, 1, ev(MUY_FORMAL, MEDIO_FORMAL, NOCTURNO));
        r.agregar("Cinturón negro",       ACCESORIO, "cinturon",      "#000000", "negro",       REGULAR,     4, 1, ev(MUY_FORMAL, MEDIO_FORMAL, NOCTURNO, DISCOTECA));
        r.agregar("Gorra azul",           ACCESORIO, "gorra",         "#2255CC", "azul",        REGULAR,     1, 1, ev(CASUAL));
        r.agregar("Collar dorado",        ACCESORIO, "collar",        "#D4AF37", "dorado",      REGULAR,     3, 1, ev(NOCTURNO, DISCOTECA));
        r.agregar("Bolso beige",          ACCESORIO, "bolso",         "#E8DCC5", "beige",       REGULAR,     3, 1, ev(CASUAL, MEDIO_FORMAL));
    }

    private static List<Evento> ev(Evento... eventos) {
        return Arrays.asList(eventos);
    }
}
