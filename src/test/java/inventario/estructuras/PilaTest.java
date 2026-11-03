package inventario.estructuras;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PilaTest {

    @Test
    void pilaVacia() {
        Pila<String> pila = new Pila<>();
        assertTrue(pila.estaVacia());
        assertEquals(0, pila.tamano());
        assertNull(pila.verTope());
        assertNull(pila.desapilar());
    }

    @Test
    void unSoloElemento() {
        Pila<String> pila = new Pila<>();
        pila.apilar("a");
        assertEquals("a", pila.verTope());
        assertEquals(1, pila.tamano());
        assertEquals("a", pila.desapilar());
        assertTrue(pila.estaVacia());
        assertNull(pila.desapilar());
    }

    @Test
    void loUltimoQueEntraEsLoPrimeroQueSale() {
        Pila<String> pila = new Pila<>();
        pila.apilar("a");
        pila.apilar("b");
        pila.apilar("c");
        assertEquals("c", pila.verTope()); // ver el tope no lo saca
        assertEquals(3, pila.tamano());
        assertEquals("c", pila.desapilar());
        assertEquals("b", pila.desapilar());
        assertEquals("a", pila.desapilar());
        assertTrue(pila.estaVacia());
    }

    @Test
    void recorrerVaDelTopeAlFondo() {
        Pila<String> pila = new Pila<>();
        pila.apilar("a");
        pila.apilar("b");
        pila.apilar("c");
        StringBuilder sb = new StringBuilder();
        pila.recorrer(sb::append);
        assertEquals("cba", sb.toString());
        assertEquals(3, pila.tamano()); // recorrer no saca nada
    }
}
