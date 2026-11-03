package inventario.estructuras;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Comparator;
import org.junit.jupiter.api.Test;

class ListaEnlazadaTest {

    private static String contenido(ListaEnlazada<Integer> lista) {
        StringBuilder sb = new StringBuilder();
        lista.recorrer(n -> sb.append(n).append(' '));
        return sb.toString().trim();
    }

    private static ListaEnlazada<Integer> listaCon(int... numeros) {
        ListaEnlazada<Integer> lista = new ListaEnlazada<>();
        for (int n : numeros) {
            lista.agregar(n);
        }
        return lista;
    }

    @Test
    void listaVacia() {
        ListaEnlazada<Integer> lista = new ListaEnlazada<>();
        assertTrue(lista.estaVacia());
        assertEquals(0, lista.tamano());
        assertNull(lista.buscar(n -> n == 1));
        assertFalse(lista.reemplazar(n -> n == 1, 2));
        assertFalse(lista.eliminar(n -> n == 1));
        assertEquals("", contenido(lista));
    }

    @Test
    void unSoloElemento() {
        ListaEnlazada<Integer> lista = listaCon(7);
        assertEquals(1, lista.tamano());
        assertEquals(7, lista.buscar(n -> n == 7));
        assertTrue(lista.eliminar(n -> n == 7));
        assertTrue(lista.estaVacia());
        lista.agregar(8); // la cola debe haber quedado bien después de vaciarla
        assertEquals("8", contenido(lista));
    }

    @Test
    void agregarConservaElOrdenDeInsercion() {
        assertEquals("1 2 3 4", contenido(listaCon(1, 2, 3, 4)));
    }

    @Test
    void elementoInexistente() {
        ListaEnlazada<Integer> lista = listaCon(1, 2, 3);
        assertNull(lista.buscar(n -> n == 99));
        assertFalse(lista.reemplazar(n -> n == 99, 0));
        assertFalse(lista.eliminar(n -> n == 99));
        assertEquals(3, lista.tamano());
    }

    @Test
    void eliminarCabezaColaYMedio() {
        ListaEnlazada<Integer> lista = listaCon(1, 2, 3, 4, 5);
        assertTrue(lista.eliminar(n -> n == 1)); // cabeza
        assertTrue(lista.eliminar(n -> n == 5)); // cola
        assertTrue(lista.eliminar(n -> n == 3)); // medio
        assertEquals("2 4", contenido(lista));
        lista.agregar(6); // tras borrar la cola, agregar debe seguir funcionando
        assertEquals("2 4 6", contenido(lista));
        assertEquals(3, lista.tamano());
    }

    @Test
    void reemplazarModificaElElemento() {
        ListaEnlazada<Integer> lista = listaCon(1, 2, 3);
        assertTrue(lista.reemplazar(n -> n == 2, 20));
        assertEquals("1 20 3", contenido(lista));
    }

    @Test
    void insertarOrdenadoAlInicioEnMedioYAlFinal() {
        ListaEnlazada<Integer> lista = new ListaEnlazada<>();
        lista.insertarOrdenado(5, Comparator.naturalOrder()); // lista vacía
        lista.insertarOrdenado(1, Comparator.naturalOrder()); // inicio
        lista.insertarOrdenado(9, Comparator.naturalOrder()); // final
        lista.insertarOrdenado(7, Comparator.naturalOrder()); // medio
        lista.insertarOrdenado(3, Comparator.naturalOrder()); // medio
        assertEquals("1 3 5 7 9", contenido(lista));
        assertEquals(5, lista.tamano());
        lista.agregar(10); // la cola sigue siendo la correcta
        assertEquals("1 3 5 7 9 10", contenido(lista));
    }

    @Test
    void filtrarDevuelveSoloLosQueCumplen() {
        ListaEnlazada<Integer> pares = listaCon(1, 2, 3, 4, 5, 6).filtrar(n -> n % 2 == 0);
        assertEquals("2 4 6", contenido(pares));
        assertTrue(listaCon(1, 3).filtrar(n -> n % 2 == 0).estaVacia());
    }
}
