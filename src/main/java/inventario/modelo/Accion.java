package inventario.modelo;

/**
 * Una acción que se puede deshacer. Se guarda en la pila.
 * Guarda una copia de los datos necesarios para revertirla:
 * - producto: cómo estaba el producto ANTES del cambio (o el producto creado).
 * - movimiento: el movimiento registrado.
 */
public class Accion {

    public enum Tipo {
        CREAR_PRODUCTO, MODIFICAR_PRODUCTO, ELIMINAR_PRODUCTO, REGISTRAR_MOVIMIENTO
    }

    private final Tipo tipo;
    private final String descripcion;
    private final Producto producto;
    private final Movimiento movimiento;

    public Accion(Tipo tipo, String descripcion, Producto producto, Movimiento movimiento) {
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.producto = producto;
        this.movimiento = movimiento;
    }

    public Tipo getTipo() { return tipo; }
    public String getDescripcion() { return descripcion; }
    public Producto getProducto() { return producto; }
    public Movimiento getMovimiento() { return movimiento; }

    @Override
    public String toString() {
        return descripcion;
    }
}
