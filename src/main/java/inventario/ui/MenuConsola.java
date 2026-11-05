package inventario.ui;

import inventario.estructuras.ListaEnlazada;
import inventario.modelo.Accion;
import inventario.modelo.Movimiento;
import inventario.modelo.Producto;
import inventario.servicio.CalculoIntegral;
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
                    case 7 -> registrarMovimiento();
                    case 8 -> verHistorial();
                    case 9 -> deshacer();
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
        System.out.println(" MOVIMIENTOS (lista enlazada)");
        System.out.println("   7. Registrar entrada o salida");
        System.out.println("   8. Ver historial de movimientos");
        System.out.println(" DESHACER (pila)");
        System.out.println("   9. Deshacer última acción");
        System.out.println("   0. Salir");
    }

    // =================================================================== productos

    private void registrarProducto() throws SQLException {
        System.out.println("--- Registrar producto ---");
        String codigo = leerTexto("Código (solo números, ej. 1013): ");
        servicio.validarCodigoNuevo(codigo); // avisa antes de pedir el resto de datos
        String nombre = leerTexto("Nombre: ");
        int cantidad = leerEntero("Cantidad inicial: ");
        int minimo = leerEntero("Existencia mínima manual (se usa si no hay historial): ");
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
        int minimo = leerEnteroOpcional("Existencia mínima manual [" + actual.getMinimo() + "]: ", actual.getMinimo());
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
        System.out.println("Producto eliminado. (Se puede deshacer con la opción 9)");
    }

    private void listarProductos() {
        ListaEnlazada<Producto> lista = servicio.listarProductos();
        if (lista.estaVacia()) {
            System.out.println("No hay productos registrados (el árbol está vacío).");
            return;
        }
        System.out.println("--- Productos ordenados por código (" + lista.tamano() + ") ---");
        lista.recorrer(p -> System.out.println(linea(p)));
    }

    /** Una línea por producto: sus datos, el mínimo calculado (o "-" si no hay historial) y la alerta. */
    private String linea(Producto p) {
        CalculoIntegral.Resultado calculo = servicio.calcularMinimo(p);
        return p + String.format("  calculado: %4s", calculo == null ? "-" : calculo.minimo())
                + (servicio.estaEnMinimo(p) ? "  << REPONER" : "");
    }

    private void productosEnMinimo() {
        ListaEnlazada<Producto> lista = servicio.productosEnMinimo();
        if (lista.estaVacia()) {
            System.out.println("Ningún producto está en su existencia mínima.");
            return;
        }
        System.out.println("--- Productos que hay que reponer (" + lista.tamano() + ") ---");
        lista.recorrer(p -> System.out.println(linea(p) + "  proveedor: " + servicio.nombreProveedor(p.getIdProveedor())));
    }

    private void mostrarDetalle(Producto p) {
        System.out.println("Código:     " + p.getCodigo());
        System.out.println("Nombre:     " + p.getNombre());
        System.out.println("Cantidad:   " + p.getCantidad());
        System.out.println("Precio:     $" + p.getPrecio());
        System.out.println("Categoría:  " + servicio.nombreCategoria(p.getIdCategoria()));
        System.out.println("Proveedor:  " + servicio.nombreProveedor(p.getIdProveedor()));
        mostrarCalculoMinimo(p);
        avisarSiEstaEnMinimo(p);
    }

    /** Muestra paso a paso cómo se calculó la existencia mínima. */
    private void mostrarCalculoMinimo(Producto p) {
        CalculoIntegral.Resultado c = servicio.calcularMinimo(p);
        if (c == null) {
            System.out.println("Mínimo:     " + p.getMinimo() + " (manual: no hay salidas recientes para calcularlo)");
            return;
        }
        StringBuilder salidas = new StringBuilder();
        for (double y : c.salidasPorDia()) {
            salidas.append(salidas.length() == 0 ? "" : ", ").append((int) y);
        }
        System.out.println("Mínimo:     " + c.minimo() + " calculado (el manual es " + p.getMinimo() + ")");
        System.out.printf("  1. Simpson 1/3, salidas de los últimos %d días (h = 1): [%s]%n", c.diasVentana(), salidas);
        System.out.printf("     consumo mientras llega el pedido = %.2f unidades%n", c.consumoSimpson());
        System.out.printf("  2. Fracciones parciales, demanda promedio %.2f und/día:%n", c.demandaPromedio());
        System.out.printf("     pedidos acumulados en un pico = %.2f unidades%n", c.colaPedidos());
        System.out.printf("  mínimo = techo(%.2f + %.2f) = %d%n", c.consumoSimpson(), c.colaPedidos(), c.minimo());
    }

    private void avisarSiEstaEnMinimo(Producto p) {
        if (p != null && servicio.estaEnMinimo(p)) {
            System.out.println("[ALERTA] El producto " + p.getCodigo() + " (" + p.getNombre() + ") tiene "
                    + p.getCantidad() + " unidades y su mínimo es " + servicio.minimoEfectivo(p) + ". Hay que reponer.");
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

    // =================================================================== pila

    private void deshacer() throws SQLException {
        Accion accion = servicio.deshacer();
        if (accion == null) {
            System.out.println("No hay acciones para deshacer (la pila está vacía).");
        } else {
            System.out.println("Deshecho: " + accion.getDescripcion());
        }
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
