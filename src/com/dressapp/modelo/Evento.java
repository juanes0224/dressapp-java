package com.dressapp.modelo;

/** Catálogo fijo de eventos. El número es la formalidad objetivo (1-5). */
public enum Evento {
    MUY_FORMAL(5),
    MEDIO_FORMAL(4),
    NOCTURNO(3),
    DISCOTECA(3),
    CASUAL(2);

    private final int formalidadObjetivo;

    Evento(int formalidadObjetivo) {
        this.formalidadObjetivo = formalidadObjetivo;
    }

    public int getFormalidadObjetivo() {
        return formalidadObjetivo;
    }

    /** Eventos donde dos prendas holgadas juntas se ven bien (afecta la compatibilidad de fit). */
    public boolean esRelajado() {
        return this == CASUAL || this == NOCTURNO || this == DISCOTECA;
    }

    public String etiqueta() {
        return name().toLowerCase().replace('_', ' ');
    }

    /** Para usar dentro de una frase: "ideal para " + enFrase(). */
    public String enFrase() {
        switch (this) {
            case MUY_FORMAL: return "un evento muy formal";
            case MEDIO_FORMAL: return "un evento medio formal";
            case NOCTURNO: return "un plan nocturno";
            case DISCOTECA: return "ir a una discoteca";
            default: return "un plan casual";
        }
    }
}
