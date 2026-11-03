package inventario.modelo;

/** Proveedor de productos (tabla proveedor). */
public class Proveedor {

    private final int id;
    private final String nombre;
    private final String telefono;

    public Proveedor(int id, String nombre, String telefono) {
        this.id = id;
        this.nombre = nombre;
        this.telefono = telefono;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getTelefono() { return telefono; }

    @Override
    public String toString() {
        return id + ". " + nombre + " (tel. " + telefono + ")";
    }
}
