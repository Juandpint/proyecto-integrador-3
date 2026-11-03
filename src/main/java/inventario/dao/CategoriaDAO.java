package inventario.dao;

import inventario.estructuras.ListaEnlazada;
import inventario.modelo.Categoria;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Acceso a la tabla categoria (solo lectura: las categorías vienen de 02_dml.sql). */
public class CategoriaDAO {

    public ListaEnlazada<Categoria> listar() throws SQLException {
        ListaEnlazada<Categoria> categorias = new ListaEnlazada<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement("SELECT id, nombre FROM categoria ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                categorias.agregar(new Categoria(rs.getInt("id"), rs.getString("nombre")));
            }
        }
        return categorias;
    }
}
