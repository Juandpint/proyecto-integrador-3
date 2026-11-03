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
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement("SELECT id, nombre, telefono FROM proveedor ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                proveedores.agregar(new Proveedor(rs.getInt("id"), rs.getString("nombre"), rs.getString("telefono")));
            }
        }
        return proveedores;
    }
}
