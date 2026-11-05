    package inventario.dao;

import inventario.estructuras.ListaEnlazada;
import inventario.modelo.Producto;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Acceso a la tabla producto. Todo el SQL va con sentencias preparadas (?) para evitar inyección SQL. */
public class ProductoDAO {

    public ListaEnlazada<Producto> listar() throws SQLException {
        String sql = "SELECT codigo, nombre, cantidad, minimo, precio, id_categoria, id_proveedor FROM producto";
        ListaEnlazada<Producto> productos = new ListaEnlazada<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                productos.agregar(new Producto(
                        rs.getString("codigo"),
                        rs.getString("nombre"),
                        rs.getInt("cantidad"),
                        rs.getInt("minimo"),
                        rs.getBigDecimal("precio"),
                        rs.getInt("id_categoria"),
                        rs.getInt("id_proveedor")));
            }
        }
        return productos;
    }

    public void insertar(Producto p) throws SQLException {
        String sql = "INSERT INTO producto (codigo, nombre, cantidad, minimo, precio, id_categoria, id_proveedor) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setString(1, p.getCodigo());
            ps.setString(2, p.getNombre());
            ps.setInt(3, p.getCantidad());
            ps.setInt(4, p.getMinimo());
            ps.setBigDecimal(5, p.getPrecio());
            ps.setInt(6, p.getIdCategoria());
            ps.setInt(7, p.getIdProveedor());
            ps.executeUpdate();
        }
    }

    /** Actualiza todos los datos menos el código, que es la llave. */
    public void actualizar(Producto p) throws SQLException {
        String sql = "UPDATE producto SET nombre = ?, cantidad = ?, minimo = ?, precio = ?, "
                   + "id_categoria = ?, id_proveedor = ? WHERE codigo = ?";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql)) {
            ps.setString(1, p.getNombre());
            ps.setInt(2, p.getCantidad());
            ps.setInt(3, p.getMinimo());
            ps.setBigDecimal(4, p.getPrecio());
            ps.setInt(5, p.getIdCategoria());
            ps.setInt(6, p.getIdProveedor());
            ps.setString(7, p.getCodigo());
            ps.executeUpdate();
        }
    }

    public void eliminar(String codigo) throws SQLException {
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement("DELETE FROM producto WHERE codigo = ?")) {
            ps.setString(1, codigo);
            ps.executeUpdate();
        }
    }
}
