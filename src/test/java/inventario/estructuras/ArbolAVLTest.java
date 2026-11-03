package inventario.estructuras;

import static org.junit.jupiter.api.Assertions.*;

import inventario.modelo.Producto;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ArbolAVLTest {

    private static Producto producto(String codigo) {
        return new Producto(codigo, "Producto " + codigo, 10, 2, BigDecimal.ONE, 1, 1);
    }

    private static String codigosInorden(ArbolAVL arbol) {
        StringBuilder sb = new StringBuilder();
        arbol.recorridoInorden().recorrer(p -> sb.append(p.getCodigo()).append(' '));
        return sb.toString().trim();
    }

    @Test
    void arbolVacio() {
        ArbolAVL arbol = new ArbolAVL();
        assertTrue(arbol.estaVacio());
        assertEquals(0, arbol.tamano());
        assertEquals(0, arbol.altura());
        assertNull(arbol.buscar("1001"));
        assertFalse(arbol.modificar(producto("1001")));
        assertFalse(arbol.eliminar("1001"));
        assertTrue(arbol.recorridoInorden().estaVacia());
        assertEquals("(árbol vacío)", arbol.dibujar());
    }

    @Test
    void unSoloElementoSeInsertaBuscaYEliminaComoRaiz() {
        ArbolAVL arbol = new ArbolAVL();
        assertTrue(arbol.insertar(producto("1001")));
        assertEquals(1, arbol.tamano());
        assertEquals(1, arbol.altura());
        assertEquals("1001", arbol.buscar("1001").getCodigo());

        assertTrue(arbol.eliminar("1001"));
        assertTrue(arbol.estaVacio());
        assertNull(arbol.buscar("1001"));
    }

    @Test
    void elementoInexistente() {
        ArbolAVL arbol = new ArbolAVL();
        arbol.insertar(producto("1001"));
        arbol.insertar(producto("1002"));
        assertNull(arbol.buscar("9999"));
        assertFalse(arbol.modificar(producto("9999")));
        assertFalse(arbol.eliminar("9999"));
        assertEquals(2, arbol.tamano());
    }

    @Test
    void codigoDuplicadoSeRechaza() {
        ArbolAVL arbol = new ArbolAVL();
        assertTrue(arbol.insertar(producto("1001")));
        assertFalse(arbol.insertar(producto("1001")));
        assertEquals(1, arbol.tamano());
    }

    @Test
    void codigosEnOrdenAscendenteMantienenElArbolBalanceado() {
        ArbolAVL arbol = new ArbolAVL();
        for (int i = 1; i <= 1000; i++) {
            arbol.insertar(producto(String.valueOf(i)));
            assertTrue(arbol.estaBalanceado(), "desbalanceado al insertar " + i);
        }
        assertEquals(1000, arbol.tamano());
        // Un BST normal tendría altura 1000; un AVL de 1000 nodos no pasa de 14
        assertTrue(arbol.altura() <= 14, "altura " + arbol.altura());
        assertNotNull(arbol.buscar("1"));
        assertNotNull(arbol.buscar("1000"));
    }

    @Test
    void inordenDevuelveLosCodigosOrdenados() {
        ArbolAVL arbol = new ArbolAVL();
        for (String c : new String[] {"1005", "1002", "1009", "5", "1001", "1007", "20"}) {
            arbol.insertar(producto(c));
        }
        assertEquals("5 20 1001 1002 1005 1007 1009", codigosInorden(arbol));
    }

    @Test
    void eliminarHojaNodoConUnHijoNodoConDosHijosYRaiz() {
        ArbolAVL arbol = new ArbolAVL();
        // Queda: raíz 1004, con 1002 (hijos 1001, 1003) y 1006 (hijos 1005, 1007)
        for (String c : new String[] {"1004", "1002", "1006", "1001", "1003", "1005", "1007"}) {
            arbol.insertar(producto(c));
        }
        assertTrue(arbol.eliminar("1001")); // hoja
        assertTrue(arbol.estaBalanceado());
        assertTrue(arbol.eliminar("1002")); // ahora tiene un solo hijo (1003)
        assertTrue(arbol.estaBalanceado());
        assertTrue(arbol.eliminar("1006")); // dos hijos: lo reemplaza su sucesor 1007
        assertTrue(arbol.estaBalanceado());
        assertTrue(arbol.eliminar("1004")); // la raíz
        assertTrue(arbol.estaBalanceado());
        assertEquals("1003 1005 1007", codigosInorden(arbol));
        assertEquals(3, arbol.tamano());
    }

    @Test
    void eliminarMuchosMantieneElBalance() {
        ArbolAVL arbol = new ArbolAVL();
        for (int i = 1; i <= 200; i++) {
            arbol.insertar(producto(String.valueOf(i)));
        }
        for (int i = 1; i <= 200; i += 2) { // se borran los impares
            assertTrue(arbol.eliminar(String.valueOf(i)));
            assertTrue(arbol.estaBalanceado(), "desbalanceado al eliminar " + i);
        }
        assertEquals(100, arbol.tamano());
        assertNull(arbol.buscar("1"));
        assertNotNull(arbol.buscar("2"));
    }

    @Test
    void modificarReemplazaLosDatos() {
        ArbolAVL arbol = new ArbolAVL();
        arbol.insertar(producto("1001"));
        Producto cambiado = new Producto("1001", "Martillo nuevo", 3, 1, BigDecimal.TEN, 2, 2);
        assertTrue(arbol.modificar(cambiado));
        assertEquals("Martillo nuevo", arbol.buscar("1001").getNombre());
        assertEquals(1, arbol.tamano());
    }
}
