package inventario.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Mantiene la única conexión JDBC del programa. */
public class ConexionBD {

    private static Connection conexion;

    /** Un bloque de trabajo con la BD que puede lanzar SQLException. */
    public interface TrabajoSQL {
        void ejecutar(Connection conexion) throws SQLException;
    }

    public static void abrir(String url, String usuario, String clave) throws SQLException {
        conexion = DriverManager.getConnection(url, usuario, clave);
    }

    public static Connection obtener() {
        return conexion;
    }

    /**
     * Ejecuta varias sentencias como una sola transacción:
     * o se guardan todas (commit) o no se guarda ninguna (rollback).
     */
    public static void enTransaccion(TrabajoSQL trabajo) throws SQLException {
        conexion.setAutoCommit(false);
        try {
            trabajo.ejecutar(conexion);
            conexion.commit();
        } catch (SQLException e) {
            conexion.rollback();
            throw e;
        } finally {
            conexion.setAutoCommit(true);
        }
    }

    public static void cerrar() {
        try {
            if (conexion != null) {
                conexion.close();
            }
        } catch (SQLException e) {
            System.out.println("Aviso: no se pudo cerrar la conexión: " + e.getMessage());
        }
    }
}
