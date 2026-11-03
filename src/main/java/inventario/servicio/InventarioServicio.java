package inventario.servicio;

import inventario.dao.CategoriaDAO;
import inventario.dao.MovimientoDAO;
import inventario.dao.ProductoDAO;
import inventario.dao.ProveedorDAO;
import inventario.estructuras.ArbolAVL;
import inventario.estructuras.ListaEnlazada;
import inventario.estructuras.Pila;
import inventario.modelo.Accion;
import inventario.modelo.Categoria;
import inventario.modelo.Movimiento;
import inventario.modelo.Producto;
import inventario.modelo.Proveedor;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.function.Consumer;

/**
 * Lógica del inventario. Une las estructuras en memoria con la base de datos.
 *
 * Regla: primero se guarda en la BD y, solo si eso funcionó, se actualiza la estructura.
 * Así la memoria nunca tiene algo que la BD no tenga.
 *
 * Los errores de reglas del negocio (código repetido, stock insuficiente, etc.) se lanzan
 * como IllegalArgumentException con un mensaje para mostrar al usuario.
 */
public class InventarioServicio {

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final MovimientoDAO movimientoDAO = new MovimientoDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();

    private final ArbolAVL productos = new ArbolAVL();
    private final ListaEnlazada<Movimiento> movimientos = new ListaEnlazada<>();
    private final Pila<Accion> acciones = new Pila<>();
    private ListaEnlazada<Categoria> categorias = new ListaEnlazada<>();
    private ListaEnlazada<Proveedor> proveedores = new ListaEnlazada<>();

    /** Al iniciar el programa: pasa los registros de la BD a las estructuras. */
    public void cargarDesdeBD() throws SQLException {
        productoDAO.listar().recorrer(productos::insertar);
        movimientoDAO.listar().recorrer(movimientos::agregar);
        categorias = categoriaDAO.listar();
        proveedores = proveedorDAO.listar();
    }

    // =================================================================== productos (árbol AVL)

    public int cantidadProductos() {
        return productos.tamano();
    }

    public Producto buscarProducto(String codigo) {
        return productos.buscar(codigo);
    }

    public ListaEnlazada<Producto> listarProductos() {
        return productos.recorridoInorden();
    }

    public ListaEnlazada<Producto> productosEnMinimo() {
        return productos.recorridoInorden().filtrar(Producto::estaEnMinimo);
    }

    /** Revisa que el código tenga solo números y que no esté repetido. */
    public void validarCodigoNuevo(String codigo) {
        if (codigo == null || !codigo.matches("\\d{1,20}")) {
            throw new IllegalArgumentException("El código debe tener solo números (máximo 20), por ejemplo 1013.");
        }
        if (productos.buscar(codigo) != null) {
            throw new IllegalArgumentException("Ya existe un producto con el código " + codigo + ".");
        }
    }

    public void crearProducto(Producto p) throws SQLException {
        validarCodigoNuevo(p.getCodigo());
        if (p.getCantidad() < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa.");
        }
        validarDatos(p);
        insertarProducto(p);
        acciones.apilar(new Accion(Accion.Tipo.CREAR_PRODUCTO,
                "Crear producto " + p.getCodigo() + " (" + p.getNombre() + ")", p.copia(), null));
    }

    /** Cambia nombre, mínimo, precio, categoría y proveedor. El código y la cantidad no se tocan aquí. */
    public void modificarProducto(Producto cambios) throws SQLException {
        Producto actual = exigirProducto(cambios.getCodigo());
        validarDatos(cambios);
        cambios.setCantidad(actual.getCantidad()); // la cantidad solo cambia con entradas y salidas
        Producto antes = actual.copia();
        actualizarProducto(cambios);
        acciones.apilar(new Accion(Accion.Tipo.MODIFICAR_PRODUCTO,
                "Modificar producto " + antes.getCodigo() + " (" + antes.getNombre() + ")", antes, null));
    }

    public void eliminarProducto(String codigo) throws SQLException {
        Producto actual = exigirProducto(codigo);
        if (movimientos.buscar(m -> m.getCodigoProducto().equals(codigo)) != null) {
            throw new IllegalArgumentException("No se puede eliminar el producto " + codigo
                    + " porque tiene movimientos en el historial.");
        }
        borrarProducto(codigo);
        acciones.apilar(new Accion(Accion.Tipo.ELIMINAR_PRODUCTO,
                "Eliminar producto " + codigo + " (" + actual.getNombre() + ")", actual.copia(), null));
    }

    // =================================================================== movimientos (lista enlazada)

    public int cantidadMovimientos() {
        return movimientos.tamano();
    }

    public void recorrerMovimientos(Consumer<Movimiento> accion) {
        movimientos.recorrer(accion);
    }

    /** Registra una entrada o salida. Devuelve el producto ya actualizado (para revisar la existencia mínima). */
    public Producto registrarMovimiento(String codigo, Movimiento.Tipo tipo, int cantidad, String observacion)
            throws SQLException {
        exigirProducto(codigo);
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad del movimiento debe ser mayor que 0.");
        }
        Movimiento m = new Movimiento(0, codigo, tipo, cantidad, LocalDateTime.now().withNano(0), observacion);
        guardarMovimiento(m);
        acciones.apilar(new Accion(Accion.Tipo.REGISTRAR_MOVIMIENTO,
                "Registrar " + tipo + " #" + m.getId() + " de " + cantidad + " del producto " + codigo, null, m.copia()));
        return productos.buscar(codigo);
    }

    // =================================================================== deshacer (pila)

    /**
     * Deshace la última acción ejecutando la operación contraria (también en la BD).
     * Devuelve la acción deshecha, o null si la pila está vacía.
     * La acción solo se saca de la pila si se pudo deshacer.
     */
    public Accion deshacer() throws SQLException {
        Accion accion = acciones.verTope();
        if (accion == null) {
            return null;
        }
        Producto p = accion.getProducto();
        Movimiento m = accion.getMovimiento();
        switch (accion.getTipo()) {
            case CREAR_PRODUCTO -> borrarProducto(p.getCodigo());
            case MODIFICAR_PRODUCTO -> actualizarProducto(p.copia());
            case ELIMINAR_PRODUCTO -> insertarProducto(p.copia());
            case REGISTRAR_MOVIMIENTO -> borrarMovimiento(m.getId());
        }
        acciones.desapilar();
        return accion;
    }

    // =================================================================== catálogos

    public ListaEnlazada<Categoria> getCategorias() {
        return categorias;
    }

    public ListaEnlazada<Proveedor> getProveedores() {
        return proveedores;
    }

    public String nombreCategoria(int id) {
        Categoria c = categorias.buscar(x -> x.getId() == id);
        return c == null ? "?" : c.getNombre();
    }

    public String nombreProveedor(int id) {
        Proveedor p = proveedores.buscar(x -> x.getId() == id);
        return p == null ? "?" : p.getNombre();
    }

    // =================================================================== operaciones internas
    // Hacen el cambio en la BD y en la estructura, sin tocar la pila. Las usa también deshacer().

    private void insertarProducto(Producto p) throws SQLException {
        productoDAO.insertar(p);
        productos.insertar(p);
    }

    private void actualizarProducto(Producto p) throws SQLException {
        productoDAO.actualizar(p);
        productos.modificar(p);
    }

    private void borrarProducto(String codigo) throws SQLException {
        productoDAO.eliminar(codigo);
        productos.eliminar(codigo);
    }

    private void guardarMovimiento(Movimiento m) throws SQLException {
        Producto p = exigirProducto(m.getCodigoProducto());
        int nuevoStock = calcularStock(p, p.getCantidad() + m.efectoEnStock());
        movimientoDAO.insertar(m, nuevoStock);
        movimientos.agregar(m); // el más reciente va al final: O(1)
        p.setCantidad(nuevoStock);
    }

    private void borrarMovimiento(int id) throws SQLException {
        Movimiento actual = exigirMovimiento(id);
        Producto p = exigirProducto(actual.getCodigoProducto());
        int nuevoStock = calcularStock(p, p.getCantidad() - actual.efectoEnStock());
        movimientoDAO.eliminar(actual, nuevoStock);
        movimientos.eliminar(x -> x.getId() == id);
        p.setCantidad(nuevoStock);
    }

    // =================================================================== validaciones

    private Producto exigirProducto(String codigo) {
        Producto p = productos.buscar(codigo);
        if (p == null) {
            throw new IllegalArgumentException("No existe un producto con el código " + codigo + ".");
        }
        return p;
    }

    private Movimiento exigirMovimiento(int id) {
        Movimiento m = movimientos.buscar(x -> x.getId() == id);
        if (m == null) {
            throw new IllegalArgumentException("No existe un movimiento con el número " + id + ".");
        }
        return m;
    }

    /** El stock nunca puede quedar negativo. */
    private int calcularStock(Producto p, int nuevoStock) {
        if (nuevoStock < 0) {
            throw new IllegalArgumentException("No hay existencias suficientes del producto " + p.getCodigo()
                    + ": hay " + p.getCantidad() + " y la operación lo dejaría en " + nuevoStock + ".");
        }
        return nuevoStock;
    }

    private void validarDatos(Producto p) {
        if (p.getNombre() == null || p.getNombre().isBlank() || p.getNombre().length() > 100) {
            throw new IllegalArgumentException("El nombre es obligatorio (máximo 100 caracteres).");
        }
        if (p.getMinimo() < 0) {
            throw new IllegalArgumentException("La existencia mínima no puede ser negativa.");
        }
        if (p.getPrecio() == null || p.getPrecio().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo.");
        }
        if (categorias.buscar(c -> c.getId() == p.getIdCategoria()) == null) {
            throw new IllegalArgumentException("No existe la categoría " + p.getIdCategoria() + ".");
        }
        if (proveedores.buscar(x -> x.getId() == p.getIdProveedor()) == null) {
            throw new IllegalArgumentException("No existe el proveedor " + p.getIdProveedor() + ".");
        }
    }
}
