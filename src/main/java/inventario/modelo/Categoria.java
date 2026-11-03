package inventario.modelo;

/** Categoría de productos (tabla categoria). */
public class Categoria {

    private final int id;
    private final String nombre;

    public Categoria(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }

    @Override
    public String toString() {
        return id + ". " + nombre;
    }
}
