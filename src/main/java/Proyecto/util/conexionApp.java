package Proyecto.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.util.Properties;

/**
 * ConexionApp - Punto de entrada para verificar la conexion
 * y exponer metodos utilitarios de configuracion a la aplicacion.
 *
 * Usa FileInputStream para leer config.properties de forma segura,
 * sin exponer credenciales en el codigo fuente.
 */
public class conexionApp {

    private static final String RUTA_CONFIG = "config.properties";

    
      //Retorna el valor de una propiedad especifica del archivo config.properties.
      //Util para acceder a cualquier parametro desde otras capas de la app.

    ///@return String con el valor, o null si no existe
     
    public static String obtenerPropiedad(String clave) {
        Properties props = new Properties();

        try (FileInputStream fis = new FileInputStream(RUTA_CONFIG)) {
            props.load(fis);
            return props.getProperty(clave);

        } catch (IOException e) {
            System.err.println("[ConexionApp] Error al leer config.properties: " + e.getMessage());
            return null;
        }
    }

    //Verifica si la conexion a la base de datos esta disponible.
    ///@return true si la conexion es exitosa, false en caso contrario
    public static boolean verificarConexion() {
        Connection conexion = conexionBD.obtenerConexion();

        if (conexion != null) {
            System.out.println("[ConexionApp] La aplicacion se conecto correctamente a: "
                    + obtenerPropiedad("db.url"));
            conexionBD.cerrarConexion(conexion);
            return true;
        } else {
            System.err.println("[ConexionApp] No se pudo establecer la conexion.");
            return false;
        }
    }

    /**
     * Muestra en consola la configuracion actual (sin mostrar la contrasena).
     */
    public static void mostrarConfiguracion() {
        System.out.println("========================================");
        System.out.println("  Configuracion de la aplicacion");
        System.out.println("========================================");
        System.out.println("  Host    : " + obtenerPropiedad("db.host"));
        System.out.println("  Puerto  : " + obtenerPropiedad("db.port"));
        System.out.println("  Base BD : " + obtenerPropiedad("db.name"));
        System.out.println("  Usuario : " + obtenerPropiedad("db.user"));
        System.out.println("  Password: ********** (oculta)");
        System.out.println("========================================");
    }

    // --- Main de prueba ---
    public static void main(String[] args) {
        mostrarConfiguracion();
        verificarConexion();
    }
}
