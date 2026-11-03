package inventario.dao;

import inventario.estructuras.ListaEnlazada;
import inventario.modelo.Movimiento;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

/**
 * Acceso a la tabla movimiento. Cada cambio en un movimiento también cambia la cantidad
 * del producto, así que las dos sentencias se ejecutan en una sola transacción.
 */
public class MovimientoDAO {

    /** Devuelve todos los movimientos en orden cronológico (el id sigue el orden de registro). */
    public ListaEnlazada<Movimiento> listar() throws SQLException {
        String sql = "SELECT id, codigo_producto, tipo, cantidad, fecha, observacion FROM movimiento ORDER BY id";
        ListaEnlazada<Movimiento> movimientos = new ListaEnlazada<>();
        try (PreparedStatement ps = ConexionBD.obtener().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                movimientos.agregar(new Movimiento(
                        rs.getInt("id"),
                        rs.getString("codigo_producto"),
                        Movimiento.Tipo.valueOf(rs.getString("tipo")),
                        rs.getInt("cantidad"),
                        rs.getObject("fecha", LocalDateTime.class),
                        rs.getString("observacion")));
            }
        }
        return movimientos;
    }

    /** Inserta el movimiento y deja el stock del producto en "nuevoStock". MySQL le asigna el id, que queda en m. */
    public void insertar(Movimiento m, int nuevoStock) throws SQLException {
        String sql = "INSERT INTO movimiento (codigo_producto, tipo, cantidad, fecha, observacion) "
                   + "VALUES (?, ?, ?, ?, ?)";
        ConexionBD.enTransaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, m.getCodigoProducto());
                ps.setString(2, m.getTipo().name());
                ps.setInt(3, m.getCantidad());
                ps.setObject(4, m.getFecha());
                ps.setString(5, m.getObservacion());
                ps.executeUpdate();
                try (ResultSet llaves = ps.getGeneratedKeys()) {
                    if (llaves.next()) {
                        m.setId(llaves.getInt(1));
                    }
                }
            }
            actualizarStock(c, m.getCodigoProducto(), nuevoStock);
        });
    }

    /** Borra el movimiento (al deshacer su registro) y deja el stock del producto en "nuevoStock". */
    public void eliminar(Movimiento m, int nuevoStock) throws SQLException {
        ConexionBD.enTransaccion(c -> {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM movimiento WHERE id = ?")) {
                ps.setInt(1, m.getId());
                ps.executeUpdate();
            }
            actualizarStock(c, m.getCodigoProducto(), nuevoStock);
        });
    }

    private void actualizarStock(Connection c, String codigo, int nuevoStock) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE producto SET cantidad = ? WHERE codigo = ?")) {
            ps.setInt(1, nuevoStock);
            ps.setString(2, codigo);
            ps.executeUpdate();
        }
    }
}
