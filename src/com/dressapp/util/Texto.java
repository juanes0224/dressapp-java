package com.dressapp.util;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

/** Normalización de texto compartida por el trie, el árbol de categorías y los colores. */
public final class Texto {

    private Texto() {}

    /** Minúsculas, sin tildes ni diéresis y con espacios simples. "  Pantalón  Azul " -> "pantalon azul". */
    public static String normalizar(String texto) {
        if (texto == null) return "";
        String sinTildes = Normalizer.normalize(texto.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}", "");
        return sinTildes.trim().replaceAll("\\s+", " ");
    }

    /** Forma de clave interna: normalizada y con guion bajo en lugar de espacios. "Azul marino" -> "azul_marino". */
    public static String aClave(String texto) {
        return normalizar(texto).replace(' ', '_');
    }

    /** Palabras que se guardan sin tilde como clave pero se muestran con ella. */
    private static final Map<String, String> CON_TILDE = new HashMap<>();

    static {
        CON_TILDE.put("pantalon", "pantalón");
        CON_TILDE.put("cinturon", "cinturón");
        CON_TILDE.put("marron", "marrón");
        CON_TILDE.put("cafe", "café");
        CON_TILDE.put("clasico", "clásico");
    }

    /** Inversa visual de aClave, para mostrar en frases. "azul_marino" -> "azul marino", "pantalon" -> "pantalón". */
    public static String legible(String clave) {
        if (clave == null) return "";
        String conEspacios = clave.replace('_', ' ');
        return CON_TILDE.getOrDefault(conEspacios, conEspacios);
    }
}
