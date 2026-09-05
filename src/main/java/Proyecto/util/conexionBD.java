package Proyecto.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * ConexionBD - Gestiona la conexion a la base de datos MySQL.
 * Las credenciales se leen desde config.properties usando FileInputStream,
 * evitando que la contrasena quede expuesta en el codigo fuente.
 */
public class conexionBD {

    // Ruta al archivo de configuracion (relativa al proyecto)
    private static final String RUTA_CONFIG = "config.properties";

    private static String url;
    private static String usuario;
    private static String contrasena;

    // Bloque estatico: carga las propiedades una sola vez al iniciar la clase
    static {
        cargarPropiedades();
    }

    /**
     * Lee el archivo config.properties y carga las credenciales.
     */
    private static void cargarPropiedades() {
        Properties props = new Properties();

        try (FileInputStream fis = new FileInputStream(RUTA_CONFIG)) {
            props.load(fis);

            url       = props.getProperty("db.url");
            usuario   = props.getProperty("db.user");
            contrasena = props.getProperty("db.password");

            // Cargar el driver JDBC de MySQL
            Class.forName(props.getProperty("db.driver"));

        } catch (IOException e) {
            System.err.println("[ConexionBD] Error al leer config.properties: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("[ConexionBD] Driver MySQL no encontrado: " + e.getMessage());
        }
    }

    /**
     * Abre y retorna una conexion activa a la base de datos.
     * @return Connection - objeto de conexion JDBC
     */
    public static Connection obtenerConexion() {
        Connection conexion = null;
        try {
            conexion = DriverManager.getConnection(url, usuario, contrasena);
            System.out.println("[ConexionBD] Conexion exitosa a la base de datos.");
        } catch (SQLException e) {
            System.err.println("[ConexionBD] Error al conectar: " + e.getMessage());
        }
        return conexion;
    }

    /**
     * Cierra de forma segura una conexion abierta.
     * @param conexion - la conexion a cerrar
     */
    public static void cerrarConexion(Connection conexion) {
        if (conexion != null) {
            try {
                conexion.close();
                System.out.println("[ConexionBD] Conexion cerrada correctamente.");
            } catch (SQLException e) {
                System.err.println("[ConexionBD] Error al cerrar la conexion: " + e.getMessage());
            }
        }
    }
}
