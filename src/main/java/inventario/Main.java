package inventario;

import inventario.dao.ConexionBD;
import inventario.servicio.InventarioServicio;
import inventario.ui.MenuConsola;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Properties;

public class Main {

    public static void main(String[] args) {
        // 1. Leer config.properties (está en .gitignore porque tiene la clave de MySQL)
        Path archivo = Path.of("config.properties");
        if (!Files.exists(archivo)) {
            System.out.println("No se encontró " + archivo.toAbsolutePath());
            System.out.println("Copie config.example.properties con el nombre config.properties"
                    + " y escriba su usuario y clave de MySQL.");
            return;
        }
        Properties config = new Properties();
        try (Reader lector = Files.newBufferedReader(archivo)) {
            config.load(lector);
        } catch (IOException e) {
            System.out.println("No se pudo leer config.properties: " + e.getMessage());
            return;
        }

        // 2. Conectar a MySQL
        try {
            ConexionBD.abrir(config.getProperty("db.url"), config.getProperty("db.usuario"),
                    config.getProperty("db.clave", ""));
        } catch (SQLException e) {
            System.out.println("No se pudo conectar a MySQL: " + e.getMessage());
            System.out.println("Revise que MySQL esté encendido, que ejecutó sql/01_ddl.sql"
                    + " y que el usuario y la clave de config.properties son correctos.");
            return;
        }

        // 3. Cargar los registros de la BD en las estructuras y abrir el menú
        try {
            InventarioServicio servicio = new InventarioServicio();
            servicio.cargarDesdeBD();
            System.out.println("Cargados " + servicio.cantidadProductos() + " productos (árbol AVL) y "
                    + servicio.cantidadMovimientos() + " movimientos (lista enlazada) desde la base de datos.");
            int enMinimo = servicio.productosEnMinimo().tamano();
            if (enMinimo > 0) {
                System.out.println("[ALERTA] Hay " + enMinimo + " productos en existencia mínima (opción 6).");
            }
            new MenuConsola(servicio).iniciar();
        } catch (SQLException e) {
            System.out.println("Error al cargar los datos: " + e.getMessage());
        } finally {
            ConexionBD.cerrar();
        }
    }
}
