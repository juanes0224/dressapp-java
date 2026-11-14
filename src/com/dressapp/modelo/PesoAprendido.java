package com.dressapp.modelo;

/**
 * Peso aprendido w(a, b, evento) en [0, 1] a partir de las calificaciones del usuario.
 * Clave única (a, b, evento) con a < b, así el par es el mismo sin importar el orden.
 */
public final class PesoAprendido {

    private final int idPrendaA;
    private final int idPrendaB;
    private final Evento evento;
    private double valor;
    private int calificaciones;

    public PesoAprendido(int idPrendaA, int idPrendaB, Evento evento, double valorInicial) {
        if (idPrendaA >= idPrendaB) {
            throw new IllegalArgumentException("Se requiere idPrendaA < idPrendaB.");
        }
        this.idPrendaA = idPrendaA;
        this.idPrendaB = idPrendaB;
        this.evento = evento;
        setValor(valorInicial);
    }

    public static String clave(int idA, int idB, Evento evento) {
        return Math.min(idA, idB) + "|" + Math.max(idA, idB) + "|" + evento.name();
    }

    public String clave() {
        return clave(idPrendaA, idPrendaB, evento);
    }

    public int getIdPrendaA() { return idPrendaA; }
    public int getIdPrendaB() { return idPrendaB; }
    public Evento getEvento() { return evento; }
    public double getValor() { return valor; }
    public int getCalificaciones() { return calificaciones; }

    public void setValor(double valor) {
        this.valor = Math.max(0.0, Math.min(1.0, valor));
    }

    public void sumarCalificacion() {
        calificaciones++;
    }

    /** lambda = n / (n + 3). Con n = 0 manda la regla (arranque en frío); con más datos, el usuario. */
    public double lambda() {
        return calificaciones / (calificaciones + 3.0);
    }
}
