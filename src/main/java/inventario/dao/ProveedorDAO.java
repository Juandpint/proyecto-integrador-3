package inventario.dao;

import inventario.estructuras.ListaEnlazada;
import inventario.modelo.Proveedor;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Acceso a la tabla proveedor (solo lectura: los proveedores vienen de 02_dml.sql). */
public class ProveedorDAO {

    public ListaEnlazada<Proveedor> listar() throws SQLException {
        ListaEnlazada<Proveedor> proveedores = new ListaEnlazada<>();
        String sql = "SELECT id, nombre, telefono, tiempo_entrega_dias FROM proveedor ORDER BY id";
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                proveedores.agregar(new Proveedor(rs.getInt("id"), rs.getString("nombre"), rs.getString("telefono"),
                        rs.getInt("tiempo_entrega_dias")));
            }
        }
        return proveedores;
    }
}
