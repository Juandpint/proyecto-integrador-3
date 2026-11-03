package inventario.ui;

import inventario.estructuras.ListaEnlazada;
import inventario.modelo.Accion;
import inventario.modelo.Movimiento;
import inventario.modelo.Producto;
import inventario.servicio.InventarioServicio;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Menú por consola para demostrar todas las operaciones. No tiene SQL ni lógica de estructuras. */
public class MenuConsola {

    private final InventarioServicio servicio;
    private final Scanner entrada = new Scanner(System.in);

    public MenuConsola(InventarioServicio servicio) {
        this.servicio = servicio;
    }

    public void iniciar() {
        int opcion = -1;
        while (opcion != 0) {
            mostrarMenu();
            try {
                opcion = leerEntero("Opción: ");
                switch (opcion) {
                    case 1 -> registrarProducto();
                    case 2 -> buscarProducto();
                    case 3 -> modificarProducto();
                    case 4 -> eliminarProducto();
                    case 5 -> listarProductos();
                    case 6 -> productosEnMinimo();
                    case 7 -> System.out.println(servicio.dibujarArbol());
                    case 8 -> registrarMovimiento();
                    case 9 -> verHistorial();
                    case 10 -> movimientosDeProducto();
                    case 11 -> modificarMovimiento();
                    case 12 -> eliminarMovimiento();
                    case 13 -> deshacer();
                    case 14 -> verPila();
                    case 0 -> System.out.println("Hasta luego.");
                    default -> System.out.println("Opción no válida.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("[!] " + e.getMessage());
            } catch (SQLException e) {
                System.out.println("[!] Error de base de datos: " + e.getMessage());
            } catch (NoSuchElementException e) {
                return; // se cerró la entrada (Ctrl+D / fin de archivo)
            }
        }
    }

    private void mostrarMenu() {
        System.out.println();
        System.out.println("========== INVENTARIO DE ALMACÉN ==========");
        System.out.println(" PRODUCTOS (árbol AVL)");
        System.out.println("   1. Registrar producto");
        System.out.println("   2. Buscar producto por código");
        System.out.println("   3. Modificar producto");
        System.out.println("   4. Eliminar producto");
        System.out.println("   5. Listar productos (recorrido inorden)");
        System.out.println("   6. Productos en existencia mínima");
        System.out.println("   7. Ver forma del árbol AVL");
        System.out.println(" MOVIMIENTOS (lista enlazada)");
        System.out.println("   8. Registrar entrada o salida");
        System.out.println("   9. Ver historial de movimientos");
        System.out.println("  10. Buscar movimientos de un producto");
        System.out.println("  11. Modificar cantidad de un movimiento");
        System.out.println("  12. Eliminar movimiento");
        System.out.println(" DESHACER (pila)");
        System.out.println("  13. Deshacer última acción");
        System.out.println("  14. Ver acciones que se pueden deshacer");
        System.out.println("   0. Salir");
    }

    // =================================================================== productos

    private void registrarProducto() throws SQLException {
        System.out.println("--- Registrar producto ---");
        String codigo = leerTexto("Código (solo números, ej. 1013): ");
        servicio.validarCodigoNuevo(codigo); // avisa antes de pedir el resto de datos
        String nombre = leerTexto("Nombre: ");
        int cantidad = leerEntero("Cantidad inicial: ");
        int minimo = leerEntero("Existencia mínima: ");
        BigDecimal precio = leerPrecio("Precio: ");
        System.out.println("Categorías:");
        servicio.getCategorias().recorrer(c -> System.out.println("   " + c));
        int idCategoria = leerEntero("Número de categoría: ");
        System.out.println("Proveedores:");
        servicio.getProveedores().recorrer(p -> System.out.println("   " + p));
        int idProveedor = leerEntero("Número de proveedor: ");

        Producto p = new Producto(codigo, nombre, cantidad, minimo, precio, idCategoria, idProveedor);
        servicio.crearProducto(p);
        System.out.println("Producto registrado: " + p);
        avisarSiEstaEnMinimo(p);
    }

    private void buscarProducto() {
        String codigo = leerTexto("Código a buscar: ");
        Producto p = servicio.buscarProducto(codigo);
        if (p == null) {
            System.out.println("No existe un producto con el código " + codigo + ".");
            return;
        }
        mostrarDetalle(p);
    }

    private void modificarProducto() throws SQLException {
        String codigo = leerTexto("Código del producto a modificar: ");
        Producto actual = servicio.buscarProducto(codigo);
        if (actual == null) {
            System.out.println("No existe un producto con el código " + codigo + ".");
            return;
        }
        mostrarDetalle(actual);
        System.out.println("Escriba el nuevo valor o presione Enter para dejarlo igual.");
        System.out.println("(El código no se puede cambiar y la cantidad cambia solo con entradas y salidas.)");
        String nombre = leerTextoOpcional("Nombre [" + actual.getNombre() + "]: ", actual.getNombre());
        int minimo = leerEnteroOpcional("Existencia mínima [" + actual.getMinimo() + "]: ", actual.getMinimo());
        BigDecimal precio = leerPrecioOpcional("Precio [" + actual.getPrecio() + "]: ", actual.getPrecio());
        int idCategoria = leerEnteroOpcional("Categoría [" + actual.getIdCategoria() + " - "
                + servicio.nombreCategoria(actual.getIdCategoria()) + "]: ", actual.getIdCategoria());
        int idProveedor = leerEnteroOpcional("Proveedor [" + actual.getIdProveedor() + " - "
                + servicio.nombreProveedor(actual.getIdProveedor()) + "]: ", actual.getIdProveedor());

        Producto cambios = new Producto(codigo, nombre, actual.getCantidad(), minimo, precio, idCategoria, idProveedor);
        servicio.modificarProducto(cambios);
        System.out.println("Producto modificado: " + servicio.buscarProducto(codigo));
    }

    private void eliminarProducto() throws SQLException {
        String codigo = leerTexto("Código del producto a eliminar: ");
        Producto p = servicio.buscarProducto(codigo);
        if (p == null) {
            System.out.println("No existe un producto con el código " + codigo + ".");
            return;
        }
        if (!confirmar("¿Eliminar " + p.getCodigo() + " - " + p.getNombre() + "? (s/n): ")) {
            System.out.println("Cancelado.");
            return;
        }
        servicio.eliminarProducto(codigo);
        System.out.println("Producto eliminado. (Se puede deshacer con la opción 13)");
    }

    private void listarProductos() {
        ListaEnlazada<Producto> lista = servicio.listarProductos();
        if (lista.estaVacia()) {
            System.out.println("No hay productos registrados (el árbol está vacío).");
            return;
        }
        System.out.println("--- Productos ordenados por código (" + lista.tamano() + ") ---");
        lista.recorrer(System.out::println);
    }

    private void productosEnMinimo() {
        ListaEnlazada<Producto> lista = servicio.productosEnMinimo();
        if (lista.estaVacia()) {
            System.out.println("Ningún producto está en su existencia mínima.");
            return;
        }
        System.out.println("--- Productos que hay que reponer (" + lista.tamano() + ") ---");
        lista.recorrer(p -> System.out.println(p + "  proveedor: " + servicio.nombreProveedor(p.getIdProveedor())));
    }

    private void mostrarDetalle(Producto p) {
        System.out.println("Código:     " + p.getCodigo());
        System.out.println("Nombre:     " + p.getNombre());
        System.out.println("Cantidad:   " + p.getCantidad());
        System.out.println("Mínimo:     " + p.getMinimo());
        System.out.println("Precio:     $" + p.getPrecio());
        System.out.println("Categoría:  " + servicio.nombreCategoria(p.getIdCategoria()));
        System.out.println("Proveedor:  " + servicio.nombreProveedor(p.getIdProveedor()));
        avisarSiEstaEnMinimo(p);
    }

    private void avisarSiEstaEnMinimo(Producto p) {
        if (p != null && p.estaEnMinimo()) {
            System.out.println("[ALERTA] El producto " + p.getCodigo() + " (" + p.getNombre() + ") tiene "
                    + p.getCantidad() + " unidades y su mínimo es " + p.getMinimo() + ". Hay que reponer.");
        }
    }

    // =================================================================== movimientos

    private void registrarMovimiento() throws SQLException {
        System.out.println("--- Registrar movimiento ---");
        String codigo = leerTexto("Código del producto: ");
        Producto p = servicio.buscarProducto(codigo);
        if (p == null) {
            System.out.println("No existe un producto con el código " + codigo + ".");
            return;
        }
        System.out.println(p.getNombre() + " - existencias actuales: " + p.getCantidad());
        int tipo = leerEntero("Tipo (1 = entrada, 2 = salida): ");
        if (tipo != 1 && tipo != 2) {
            System.out.println("Tipo no válido.");
            return;
        }
        int cantidad = leerEntero("Cantidad: ");
        String observacion = leerTextoOpcional("Observación (opcional): ", null);
        Producto actualizado = servicio.registrarMovimiento(codigo,
                tipo == 1 ? Movimiento.Tipo.ENTRADA : Movimiento.Tipo.SALIDA, cantidad, observacion);
        System.out.println("Movimiento registrado. Existencias de " + codigo + ": " + actualizado.getCantidad());
        avisarSiEstaEnMinimo(actualizado);
    }

    private void verHistorial() {
        if (servicio.cantidadMovimientos() == 0) {
            System.out.println("No hay movimientos registrados (la lista está vacía).");
            return;
        }
        System.out.println("--- Historial de movimientos (" + servicio.cantidadMovimientos() + ") ---");
        servicio.recorrerMovimientos(System.out::println);
    }

    private void movimientosDeProducto() {
        String codigo = leerTexto("Código del producto: ");
        ListaEnlazada<Movimiento> lista = servicio.movimientosDeProducto(codigo);
        if (lista.estaVacia()) {
            System.out.println("No hay movimientos del producto " + codigo + ".");
            return;
        }
        lista.recorrer(System.out::println);
    }

    private void modificarMovimiento() throws SQLException {
        int id = leerEntero("Número (#) del movimiento: ");
        Movimiento m = servicio.buscarMovimiento(id);
        if (m == null) {
            System.out.println("No existe un movimiento con el número " + id + ".");
            return;
        }
        System.out.println(m);
        int cantidad = leerEntero("Nueva cantidad: ");
        Producto actualizado = servicio.modificarMovimiento(id, cantidad);
        System.out.println("Movimiento modificado. Existencias de " + actualizado.getCodigo() + ": "
                + actualizado.getCantidad());
        avisarSiEstaEnMinimo(actualizado);
    }

    private void eliminarMovimiento() throws SQLException {
        int id = leerEntero("Número (#) del movimiento a eliminar: ");
        Movimiento m = servicio.buscarMovimiento(id);
        if (m == null) {
            System.out.println("No existe un movimiento con el número " + id + ".");
            return;
        }
        System.out.println(m);
        if (!confirmar("¿Eliminarlo? Se revertirá su efecto en las existencias (s/n): ")) {
            System.out.println("Cancelado.");
            return;
        }
        Producto actualizado = servicio.eliminarMovimiento(id);
        System.out.println("Movimiento eliminado. Existencias de " + actualizado.getCodigo() + ": "
                + actualizado.getCantidad());
        avisarSiEstaEnMinimo(actualizado);
    }

    // =================================================================== pila

    private void deshacer() throws SQLException {
        Accion accion = servicio.deshacer();
        if (accion == null) {
            System.out.println("No hay acciones para deshacer (la pila está vacía).");
        } else {
            System.out.println("Deshecho: " + accion.getDescripcion());
        }
    }

    private void verPila() {
        Accion tope = servicio.ultimaAccion();
        if (tope == null) {
            System.out.println("No hay acciones para deshacer (la pila está vacía).");
            return;
        }
        System.out.println("Tope de la pila (lo próximo que se deshace): " + tope);
        System.out.println("--- Pila, de la más reciente a la más antigua ---");
        servicio.recorrerAcciones(a -> System.out.println("   " + a));
    }

    // =================================================================== lectura de datos

    private String leerTexto(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String texto = entrada.nextLine().trim();
            if (!texto.isEmpty()) {
                return texto;
            }
            System.out.println("Este dato es obligatorio.");
        }
    }

    /** Si el usuario solo presiona Enter, devuelve el valor por defecto. */
    private String leerTextoOpcional(String mensaje, String porDefecto) {
        System.out.print(mensaje);
        String texto = entrada.nextLine().trim();
        return texto.isEmpty() ? porDefecto : texto;
    }

    private int leerEntero(String mensaje) {
        while (true) {
            String texto = leerTexto(mensaje);
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                System.out.println("Escriba un número entero.");
            }
        }
    }

    private int leerEnteroOpcional(String mensaje, int porDefecto) {
        while (true) {
            String texto = leerTextoOpcional(mensaje, null);
            if (texto == null) {
                return porDefecto;
            }
            try {
                return Integer.parseInt(texto);
            } catch (NumberFormatException e) {
                System.out.println("Escriba un número entero.");
            }
        }
    }

    private BigDecimal leerPrecio(String mensaje) {
        while (true) {
            String texto = leerTexto(mensaje);
            try {
                return new BigDecimal(texto);
            } catch (NumberFormatException e) {
                System.out.println("Escriba un número, por ejemplo 15000 o 15000.50");
            }
        }
    }

    private BigDecimal leerPrecioOpcional(String mensaje, BigDecimal porDefecto) {
        while (true) {
            String texto = leerTextoOpcional(mensaje, null);
            if (texto == null) {
                return porDefecto;
            }
            try {
                return new BigDecimal(texto);
            } catch (NumberFormatException e) {
                System.out.println("Escriba un número, por ejemplo 15000 o 15000.50");
            }
        }
    }

    private boolean confirmar(String mensaje) {
        return leerTexto(mensaje).equalsIgnoreCase("s");
    }
}
