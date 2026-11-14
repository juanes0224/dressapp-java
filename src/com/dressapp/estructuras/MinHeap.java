package com.dressapp.estructuras;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Cola de prioridad mínima: heap binario guardado en un arreglo, donde los hijos del índice i
 * están en 2i+1 y 2i+2. Si dos elementos tienen la misma prioridad sale primero el que entró
 * primero (desempate estable con un contador de inserción).
 *
 * Complejidad: push O(log n), pop O(log n), peek O(1).
 */
public class MinHeap<T> {

    private static final class Entrada<T> {
        final T valor;
        final double prioridad;
        final long orden;

        Entrada(T valor, double prioridad, long orden) {
            this.valor = valor;
            this.prioridad = prioridad;
            this.orden = orden;
        }

        boolean menorQue(Entrada<T> otra) {
            if (prioridad != otra.prioridad) return prioridad < otra.prioridad;
            return orden < otra.orden;
        }
    }

    private final List<Entrada<T>> datos = new ArrayList<>();
    private long contador;

    public void push(T valor, double prioridad) {
        if (Double.isNaN(prioridad)) throw new IllegalArgumentException("La prioridad no puede ser NaN.");
        datos.add(new Entrada<>(valor, prioridad, contador++));
        subir(datos.size() - 1);
    }

    public T pop() {
        T valor = peek();
        Entrada<T> ultima = datos.remove(datos.size() - 1);
        if (!datos.isEmpty()) {
            datos.set(0, ultima);
            bajar(0);
        }
        return valor;
    }

    public T peek() {
        if (datos.isEmpty()) throw new NoSuchElementException("El heap está vacío.");
        return datos.get(0).valor;
    }

    public double peekPrioridad() {
        if (datos.isEmpty()) throw new NoSuchElementException("El heap está vacío.");
        return datos.get(0).prioridad;
    }

    public int size() {
        return datos.size();
    }

    public boolean estaVacio() {
        return datos.isEmpty();
    }

    private void subir(int i) {
        while (i > 0) {
            int padre = (i - 1) / 2;
            if (!datos.get(i).menorQue(datos.get(padre))) break;
            intercambiar(i, padre);
            i = padre;
        }
    }

    private void bajar(int i) {
        int n = datos.size();
        while (true) {
            int menor = i;
            int izq = 2 * i + 1;
            int der = 2 * i + 2;
            if (izq < n && datos.get(izq).menorQue(datos.get(menor))) menor = izq;
            if (der < n && datos.get(der).menorQue(datos.get(menor))) menor = der;
            if (menor == i) return;
            intercambiar(i, menor);
            i = menor;
        }
    }

    private void intercambiar(int i, int j) {
        Entrada<T> tmp = datos.get(i);
        datos.set(i, datos.get(j));
        datos.set(j, tmp);
    }
}
