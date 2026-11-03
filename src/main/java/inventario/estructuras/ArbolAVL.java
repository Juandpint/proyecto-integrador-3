package inventario.estructuras;

import inventario.modelo.Producto;

/**
 * Árbol AVL propio que guarda productos ordenados por código.
 * Después de cada inserción o eliminación se rebalancea con rotaciones,
 * así que buscar, insertar y eliminar son O(log n) aunque los códigos lleguen en orden.
 */
public class ArbolAVL {

    private static class Nodo {
        Producto dato;
        Nodo izquierdo;
        Nodo derecho;
        int altura = 1; // un nodo sin hijos (hoja) tiene altura 1

        Nodo(Producto dato) {
            this.dato = dato;
        }
    }

    private Nodo raiz;
    private int tamano;

    public boolean estaVacio() {
        return raiz == null;
    }

    public int tamano() {
        return tamano;
    }

    public int altura() {
        return altura(raiz);
    }

    // ---------------------------------------------------------------- búsqueda

    /** Devuelve el producto con ese código, o null si no existe. O(log n). */
    public Producto buscar(String codigo) {
        Nodo actual = raiz;
        while (actual != null) {
            int cmp = comparar(codigo, actual.dato.getCodigo());
            if (cmp == 0) {
                return actual.dato;
            }
            actual = cmp < 0 ? actual.izquierdo : actual.derecho;
        }
        return null;
    }

    // ---------------------------------------------------------------- inserción

    /** Inserta el producto. Devuelve false si ya existe uno con ese código. O(log n). */
    public boolean insertar(Producto producto) {
        if (buscar(producto.getCodigo()) != null) {
            return false;
        }
        raiz = insertar(raiz, producto);
        tamano++;
        return true;
    }

    private Nodo insertar(Nodo nodo, Producto producto) {
        if (nodo == null) {
            return new Nodo(producto);
        }
        if (comparar(producto.getCodigo(), nodo.dato.getCodigo()) < 0) {
            nodo.izquierdo = insertar(nodo.izquierdo, producto);
        } else {
            nodo.derecho = insertar(nodo.derecho, producto);
        }
        return balancear(nodo);
    }

    // ---------------------------------------------------------------- modificación

    /**
     * Reemplaza los datos del producto que tiene el mismo código. Devuelve false si no existe.
     * Como el código (la llave) no cambia, el árbol no necesita rebalancearse. O(log n).
     */
    public boolean modificar(Producto producto) {
        Nodo actual = raiz;
        while (actual != null) {
            int cmp = comparar(producto.getCodigo(), actual.dato.getCodigo());
            if (cmp == 0) {
                actual.dato = producto;
                return true;
            }
            actual = cmp < 0 ? actual.izquierdo : actual.derecho;
        }
        return false;
    }

    // ---------------------------------------------------------------- eliminación

    /** Elimina el producto con ese código. Devuelve false si no existe. O(log n). */
    public boolean eliminar(String codigo) {
        if (buscar(codigo) == null) {
            return false;
        }
        raiz = eliminar(raiz, codigo);
        tamano--;
        return true;
    }

    private Nodo eliminar(Nodo nodo, String codigo) {
        int cmp = comparar(codigo, nodo.dato.getCodigo());
        if (cmp < 0) {
            nodo.izquierdo = eliminar(nodo.izquierdo, codigo);
        } else if (cmp > 0) {
            nodo.derecho = eliminar(nodo.derecho, codigo);
        } else {
            // Caso 1 y 2: sin hijos o con un solo hijo -> el hijo toma su lugar
            if (nodo.izquierdo == null) {
                return nodo.derecho;
            }
            if (nodo.derecho == null) {
                return nodo.izquierdo;
            }
            // Caso 3: dos hijos -> se copia el sucesor inorden (el menor del subárbol derecho)
            // y luego se elimina ese sucesor del subárbol derecho
            Nodo sucesor = nodo.derecho;
            while (sucesor.izquierdo != null) {
                sucesor = sucesor.izquierdo;
            }
            nodo.dato = sucesor.dato;
            nodo.derecho = eliminar(nodo.derecho, sucesor.dato.getCodigo());
        }
        return balancear(nodo);
    }

    // ---------------------------------------------------------------- recorridos

    /** Recorrido inorden (izquierdo, raíz, derecho): devuelve los productos ordenados por código. O(n). */
    public ListaEnlazada<Producto> recorridoInorden() {
        ListaEnlazada<Producto> resultado = new ListaEnlazada<>();
        inorden(raiz, resultado);
        return resultado;
    }

    private void inorden(Nodo nodo, ListaEnlazada<Producto> resultado) {
        if (nodo != null) {
            inorden(nodo.izquierdo, resultado);
            resultado.agregar(nodo.dato);
            inorden(nodo.derecho, resultado);
        }
    }

    /**
     * Dibuja el árbol "acostado": la raíz queda a la izquierda, el subárbol derecho arriba
     * y el izquierdo abajo. Junto a cada código se muestra su factor de balance (fb).
     */
    public String dibujar() {
        if (estaVacio()) {
            return "(árbol vacío)";
        }
        StringBuilder sb = new StringBuilder();
        dibujar(raiz, 0, sb);
        return sb.toString();
    }

    private void dibujar(Nodo nodo, int nivel, StringBuilder sb) {
        if (nodo == null) {
            return;
        }
        dibujar(nodo.derecho, nivel + 1, sb);
        sb.append("        ".repeat(nivel))
          .append(nodo.dato.getCodigo())
          .append(" (fb ").append(factorBalance(nodo)).append(")\n");
        dibujar(nodo.izquierdo, nivel + 1, sb);
    }

    // ---------------------------------------------------------------- balanceo

    private int altura(Nodo nodo) {
        return nodo == null ? 0 : nodo.altura;
    }

    /** Factor de balance = altura izquierda - altura derecha. En un AVL debe ser -1, 0 o 1. */
    private int factorBalance(Nodo nodo) {
        return altura(nodo.izquierdo) - altura(nodo.derecho);
    }

    private void actualizarAltura(Nodo nodo) {
        nodo.altura = 1 + Math.max(altura(nodo.izquierdo), altura(nodo.derecho));
    }

    /*
     *       y                x
     *      / \              / \
     *     x   C    -->     A   y
     *    / \                  / \
     *   A   B                B   C
     */
    private Nodo rotarDerecha(Nodo y) {
        Nodo x = y.izquierdo;
        y.izquierdo = x.derecho;
        x.derecho = y;
        actualizarAltura(y);
        actualizarAltura(x);
        return x;
    }

    /*
     *     x                    y
     *    / \                  / \
     *   A   y      -->       x   C
     *      / \              / \
     *     B   C            A   B
     */
    private Nodo rotarIzquierda(Nodo x) {
        Nodo y = x.derecho;
        x.derecho = y.izquierdo;
        y.izquierdo = x;
        actualizarAltura(x);
        actualizarAltura(y);
        return y;
    }

    /** Actualiza la altura del nodo y, si quedó desbalanceado, aplica la rotación que corresponde. */
    private Nodo balancear(Nodo nodo) {
        actualizarAltura(nodo);
        int fb = factorBalance(nodo);
        if (fb > 1) { // cargado a la izquierda
            if (factorBalance(nodo.izquierdo) < 0) {
                nodo.izquierdo = rotarIzquierda(nodo.izquierdo); // caso izquierda-derecha
            }
            return rotarDerecha(nodo); // caso izquierda-izquierda
        }
        if (fb < -1) { // cargado a la derecha
            if (factorBalance(nodo.derecho) > 0) {
                nodo.derecho = rotarDerecha(nodo.derecho); // caso derecha-izquierda
            }
            return rotarIzquierda(nodo); // caso derecha-derecha
        }
        return nodo;
    }

    /**
     * Compara dos códigos. Los códigos son números guardados como texto, así que el más corto
     * va primero ("5" antes que "1001"); con la misma longitud se comparan como texto.
     */
    private static int comparar(String a, String b) {
        if (a.length() != b.length()) {
            return Integer.compare(a.length(), b.length());
        }
        return a.compareTo(b);
    }

    // ---------------------------------------------------------------- solo para las pruebas

    /** true si todos los nodos tienen factor de balance entre -1 y 1 y su altura guardada es correcta. */
    boolean estaBalanceado() {
        return revisarBalance(raiz) >= 0;
    }

    /** Devuelve la altura real del subárbol, o -1 si encontró un nodo desbalanceado o mal calculado. */
    private int revisarBalance(Nodo nodo) {
        if (nodo == null) {
            return 0;
        }
        int izq = revisarBalance(nodo.izquierdo);
        int der = revisarBalance(nodo.derecho);
        if (izq < 0 || der < 0 || Math.abs(izq - der) > 1 || nodo.altura != 1 + Math.max(izq, der)) {
            return -1;
        }
        return nodo.altura;
    }
}
