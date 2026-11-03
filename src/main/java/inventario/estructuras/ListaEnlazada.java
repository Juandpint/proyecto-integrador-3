package inventario.estructuras;

import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Lista simplemente enlazada propia. Guarda referencia a la cabeza y a la cola,
 * así que agregar al final es O(1). Se usa para el historial de movimientos.
 */
public class ListaEnlazada<T> {

    private static class Nodo<T> {
        T dato;
        Nodo<T> siguiente;

        Nodo(T dato) {
            this.dato = dato;
        }
    }

    private Nodo<T> cabeza;
    private Nodo<T> cola;
    private int tamano;

    public boolean estaVacia() {
        return cabeza == null;
    }

    public int tamano() {
        return tamano;
    }

    /** Inserta al final. O(1) gracias a la referencia a la cola. */
    public void agregar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (estaVacia()) {
            cabeza = nuevo;
        } else {
            cola.siguiente = nuevo;
        }
        cola = nuevo;
        tamano++;
    }

    /** Inserta el dato antes del primer elemento que sea mayor según "orden". O(n). */
    public void insertarOrdenado(T dato, Comparator<T> orden) {
        if (estaVacia() || orden.compare(dato, cola.dato) >= 0) {
            agregar(dato); // va al final
            return;
        }
        Nodo<T> nuevo = new Nodo<>(dato);
        if (orden.compare(dato, cabeza.dato) < 0) {
            nuevo.siguiente = cabeza; // va al inicio
            cabeza = nuevo;
        } else {
            Nodo<T> actual = cabeza;
            while (orden.compare(dato, actual.siguiente.dato) >= 0) {
                actual = actual.siguiente;
            }
            nuevo.siguiente = actual.siguiente; // va en medio
            actual.siguiente = nuevo;
        }
        tamano++;
    }

    /** Devuelve el primer elemento que cumple la condición, o null si no hay ninguno. O(n). */
    public T buscar(Predicate<T> condicion) {
        for (Nodo<T> actual = cabeza; actual != null; actual = actual.siguiente) {
            if (condicion.test(actual.dato)) {
                return actual.dato;
            }
        }
        return null;
    }

    /** Devuelve una lista nueva con todos los elementos que cumplen la condición. O(n). */
    public ListaEnlazada<T> filtrar(Predicate<T> condicion) {
        ListaEnlazada<T> resultado = new ListaEnlazada<>();
        recorrer(dato -> {
            if (condicion.test(dato)) {
                resultado.agregar(dato);
            }
        });
        return resultado;
    }

    /** Reemplaza el primer elemento que cumple la condición. false si no lo encontró. O(n). */
    public boolean reemplazar(Predicate<T> condicion, T nuevoDato) {
        for (Nodo<T> actual = cabeza; actual != null; actual = actual.siguiente) {
            if (condicion.test(actual.dato)) {
                actual.dato = nuevoDato;
                return true;
            }
        }
        return false;
    }

    /** Elimina el primer elemento que cumple la condición. false si no lo encontró. O(n). */
    public boolean eliminar(Predicate<T> condicion) {
        Nodo<T> anterior = null;
        for (Nodo<T> actual = cabeza; actual != null; anterior = actual, actual = actual.siguiente) {
            if (condicion.test(actual.dato)) {
                if (anterior == null) {
                    cabeza = actual.siguiente; // era la cabeza
                } else {
                    anterior.siguiente = actual.siguiente;
                }
                if (actual == cola) {
                    cola = anterior; // era la cola (si la lista quedó vacía, cola queda null)
                }
                tamano--;
                return true;
            }
        }
        return false;
    }

    /** Recorre la lista de la cabeza a la cola (orden de inserción). O(n). */
    public void recorrer(Consumer<T> accion) {
        for (Nodo<T> actual = cabeza; actual != null; actual = actual.siguiente) {
            accion.accept(actual.dato);
        }
    }
}
