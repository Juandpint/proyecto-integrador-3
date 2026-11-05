package inventario.modelo;

/** Proveedor de productos (tabla proveedor). */
public class Proveedor {

    private final int id;
    private final String nombre;
    private final String telefono;
    private final int tiempoEntregaDias; // L: días que tarda en traer un pedido (para la existencia mínima)

    public Proveedor(int id, String nombre, String telefono, int tiempoEntregaDias) {
        this.id = id;
        this.nombre = nombre;
        this.telefono = telefono;
        this.tiempoEntregaDias = tiempoEntregaDias;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getTelefono() { return telefono; }
    public int getTiempoEntregaDias() { return tiempoEntregaDias; }

    @Override
    public String toString() {
        return id + ". " + nombre + " (tel. " + telefono + ", entrega en " + tiempoEntregaDias + " días)";
    }
}
