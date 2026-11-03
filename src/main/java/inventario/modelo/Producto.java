package inventario.modelo;

import java.math.BigDecimal;

/** Un producto del almacén (tabla producto). Se guarda en el árbol AVL usando su código como llave. */
public class Producto {

    private final String codigo; // llave del AVL: por eso no se puede modificar
    private String nombre;
    private int cantidad;
    private int minimo;
    private BigDecimal precio;
    private int idCategoria;
    private int idProveedor;

    public Producto(String codigo, String nombre, int cantidad, int minimo, BigDecimal precio,
                    int idCategoria, int idProveedor) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.minimo = minimo;
        this.precio = precio;
        this.idCategoria = idCategoria;
        this.idProveedor = idProveedor;
    }

    /** Copia independiente: la usa la pila para recordar cómo estaba el producto antes de un cambio. */
    public Producto copia() {
        return new Producto(codigo, nombre, cantidad, minimo, precio, idCategoria, idProveedor);
    }

    /** true si hay que reponer: la cantidad es igual o menor que la existencia mínima. */
    public boolean estaEnMinimo() {
        return cantidad <= minimo;
    }

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public int getCantidad() { return cantidad; }
    public int getMinimo() { return minimo; }
    public BigDecimal getPrecio() { return precio; }
    public int getIdCategoria() { return idCategoria; }
    public int getIdProveedor() { return idProveedor; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public void setMinimo(int minimo) { this.minimo = minimo; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public void setIdCategoria(int idCategoria) { this.idCategoria = idCategoria; }
    public void setIdProveedor(int idProveedor) { this.idProveedor = idProveedor; }

    @Override
    public String toString() {
        return String.format("%-6s %-30s cant: %4d  mín: %4d  $%,12.2f%s",
                codigo, nombre, cantidad, minimo, precio, estaEnMinimo() ? "  << REPONER" : "");
    }
}
