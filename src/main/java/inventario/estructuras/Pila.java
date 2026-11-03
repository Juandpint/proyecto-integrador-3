package inventario.estructuras;

import java.util.function.Consumer;

/**
 * Pila propia (LIFO): lo último que entra es lo primero que sale.
 * Se usa para deshacer acciones. Vive solo en memoria: empieza vacía en cada ejecución.
 */
public class Pila<T> {

    private static class Nodo<T> {
        T dato;
        Nodo<T> debajo;

        Nodo(T dato, Nodo<T> debajo) {
            this.dato = dato;
            this.debajo = debajo;
        }
    }

    private Nodo<T> tope;
    private int tamano;

    public boolean estaVacia() {
        return tope == null;
    }

    public int tamano() {
        return tamano;
    }

    /** push: pone un elemento encima. O(1). */
    public void apilar(T dato) {
        tope = new Nodo<>(dato, tope);
        tamano++;
    }

    /** pop: saca y devuelve el elemento de encima, o null si la pila está vacía. O(1). */
    public T desapilar() {
        if (estaVacia()) {
            return null;
        }
        T dato = tope.dato;
        tope = tope.debajo;
        tamano--;
        return dato;
    }

    /** peek: devuelve el elemento de encima sin sacarlo, o null si la pila está vacía. O(1). */
    public T verTope() {
        return estaVacia() ? null : tope.dato;
    }

    /** Recorre del tope hacia el fondo (de la acción más reciente a la más antigua). O(n). */
    public void recorrer(Consumer<T> accion) {
        for (Nodo<T> actual = tope; actual != null; actual = actual.debajo) {
            accion.accept(actual.dato);
        }
    }
}
