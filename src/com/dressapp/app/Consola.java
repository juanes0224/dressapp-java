package com.dressapp.app;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

/** Lectura validada desde la terminal. Ningún dato mal escrito hace caer el programa. */
public class Consola {

    /** La entrada se terminó (por ejemplo, al redirigir un archivo). El menú sale limpio. */
    public static class FinDeEntrada extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public FinDeEntrada() {
            super("Fin de la entrada.");
        }
    }

    private final Scanner sc;

    public Consola(InputStream entrada) {
        this.sc = new Scanner(entrada);
    }

    public String linea(String mensaje) {
        System.out.print(mensaje);
        if (!sc.hasNextLine()) throw new FinDeEntrada();
        return sc.nextLine().trim();
    }

    public String texto(String mensaje) {
        while (true) {
            String t = linea(mensaje);
            if (!t.isEmpty()) return t;
            System.out.println("  Este dato es obligatorio.");
        }
    }

    public int entero(String mensaje, int min, int max) {
        while (true) {
            String t = linea(mensaje);
            try {
                int v = Integer.parseInt(t);
                if (v >= min && v <= max) return v;
                System.out.println("  Escribe un número entre " + min + " y " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  Escribe un número entero.");
            }
        }
    }

    public double decimal(String mensaje, double min, double max) {
        while (true) {
            String t = linea(mensaje).replace(',', '.');
            try {
                double v = Double.parseDouble(t);
                if (v >= min && v <= max) return v;
                System.out.println("  Escribe un valor entre " + min + " y " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  Escribe un número (por ejemplo 14 o 8,5).");
            }
        }
    }

    public boolean siNo(String mensaje) {
        while (true) {
            String t = linea(mensaje + " (s/n): ").toLowerCase();
            if (t.equals("s") || t.equals("si") || t.equals("sí")) return true;
            if (t.equals("n") || t.equals("no")) return false;
            System.out.println("  Responde s o n.");
        }
    }

    public <E> E elegir(String titulo, List<E> opciones, Function<E, String> etiqueta) {
        System.out.println(titulo);
        for (int i = 0; i < opciones.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, etiqueta.apply(opciones.get(i)));
        }
        return opciones.get(entero("Elige una opción: ", 1, opciones.size()) - 1);
    }

    /** Como elegir, pero con una opción 0 que devuelve null. */
    public <E> E elegirOpcional(String titulo, List<E> opciones, Function<E, String> etiqueta, String textoCero) {
        System.out.println(titulo);
        System.out.printf("  0. %s%n", textoCero);
        for (int i = 0; i < opciones.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, etiqueta.apply(opciones.get(i)));
        }
        int n = entero("Elige una opción: ", 0, opciones.size());
        return n == 0 ? null : opciones.get(n - 1);
    }

    /**
     * Selección múltiple con números separados por coma ("1,3,4").
     * Enter vacío devuelve la selección por defecto; "0" devuelve una lista vacía.
     */
    public <E> List<E> elegirVarios(String titulo, List<E> opciones, Function<E, String> etiqueta, List<E> porDefecto) {
        System.out.println(titulo);
        for (int i = 0; i < opciones.size(); i++) {
            String marca = porDefecto.contains(opciones.get(i)) ? " *" : "";
            System.out.printf("  %d. %s%s%n", i + 1, etiqueta.apply(opciones.get(i)), marca);
        }
        String ayuda = porDefecto.isEmpty()
                ? "Números separados por coma (0 = ninguno): "
                : "Números separados por coma (Enter = los marcados con *, 0 = ninguno): ";
        while (true) {
            String t = linea(ayuda);
            if (t.isEmpty()) return new ArrayList<>(porDefecto);
            if (t.equals("0")) return new ArrayList<>();
            List<E> elegidos = new ArrayList<>();
            boolean valido = true;
            for (String parte : t.split("[,\\s]+")) {
                if (parte.isEmpty()) continue;
                try {
                    int n = Integer.parseInt(parte);
                    if (n < 1 || n > opciones.size()) {
                        valido = false;
                        break;
                    }
                    E opcion = opciones.get(n - 1);
                    if (!elegidos.contains(opcion)) elegidos.add(opcion);
                } catch (NumberFormatException e) {
                    valido = false;
                    break;
                }
            }
            if (valido) return elegidos;
            System.out.println("  Usa números entre 1 y " + opciones.size() + ", separados por coma.");
        }
    }

    /** Lista libre separada por comas ("negro, azul marino"). Enter vacío = lista vacía. */
    public List<String> listaTexto(String mensaje) {
        List<String> lista = new ArrayList<>();
        for (String parte : linea(mensaje).split(",")) {
            if (!parte.trim().isEmpty()) lista.add(parte.trim());
        }
        return lista;
    }

    public void pausa() {
        linea("\n  (Enter para continuar) ");
    }
}
