package Proyecto.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Gestiona las conexiones JDBC a PostgreSQL en Neon.
 *
 * La configuración se lee desde config.properties. El archivo se busca en el
 * directorio desde el que se inicia la aplicación, que es la ubicación usada
 * actualmente por Maven y por el ejecutable del proyecto.
 */
public final class conexionBD {

    private static final String RUTA_CONFIG = "config.properties";
    private static final Properties PROPIEDADES = cargarPropiedades();
    private static final HikariDataSource FUENTE_DATOS = crearFuenteDatos();

    private conexionBD() {
    }

    private static Properties cargarPropiedades() {
        Properties propiedades = new Properties();

        try (FileInputStream archivo = new FileInputStream(RUTA_CONFIG)) {
            propiedades.load(archivo);
            return propiedades;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo leer " + RUTA_CONFIG
                            + ". Cree el archivo con las credenciales de Neon.",
                    e);
        }
    }

    /**
     * Obtiene una conexión del pool de Neon.
     *
     * Se admite una URL completa (db.url) o la configuración desglosada
     * existente en el proyecto (db.host, db.puerto y db.nombre).
     *
     * @return conexión JDBC activa, o null si PostgreSQL rechaza la conexión
     */
    public static Connection obtenerConexion() {
        try {
            return FUENTE_DATOS.getConnection();
        } catch (SQLException e) {
            System.err.println("[ConexionBD] Error al conectar con Neon: " + e.getMessage());
            return null;
        }
    }

    private static HikariDataSource crearFuenteDatos() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(construirUrl());
        config.setUsername(obtenerPropiedadRequerida("db.usuario", "db.user"));
        config.setPassword(obtenerPropiedadRequerida(
                "db.contrasena", "db.password", "db.pass"));
        config.setMaximumPoolSize(Integer.parseInt(obtenerPropiedad("db.conexiones_max", "10")));
        config.setMinimumIdle(Integer.parseInt(obtenerPropiedad("db.conexiones_min", "1")));
        config.setConnectionTimeout(Long.parseLong(obtenerPropiedad("db.timeout_conexion", "30")) * 1000L);
        config.setValidationTimeout(5000);
        config.setPoolName("TechZone-Neon");
        config.setConnectionInitSql("SET search_path TO \"" + obtenerEsquema() + "\", public");
        return new HikariDataSource(config);
    }

    private static String obtenerEsquema() {
        String esquema = obtenerPropiedad("db.schema", "public");
        if (!esquema.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalStateException("Nombre de esquema inválido: " + esquema);
        }
        return esquema;
    }

    private static String construirUrl() {
        String url = PROPIEDADES.getProperty("db.url");
        if (url != null && !url.isBlank()) {
            return url.trim();
        }

        String host = obtenerPropiedadRequerida("db.host");
        String puerto = obtenerPropiedad("db.puerto", "5432");
        String nombre = obtenerPropiedadRequerida("db.nombre", "db.name");
        String esquema = obtenerPropiedad("db.schema", "public");
        boolean ssl = Boolean.parseBoolean(obtenerPropiedad("db.ssl", "true"));
        String timeout = obtenerPropiedad("db.timeout_conexion", "30");

        return String.format(
                "jdbc:postgresql://%s:%s/%s?sslmode=%s&currentSchema=%s&connectTimeout=%s",
                host,
                puerto,
                nombre,
                ssl ? "require" : "disable",
                esquema,
                timeout);
    }

    private static String obtenerPropiedadRequerida(String... claves) {
        for (String clave : claves) {
            String valor = PROPIEDADES.getProperty(clave);
            if (valor != null && !valor.isBlank()) {
                return valor.trim();
            }
        }
        throw new IllegalStateException(
                "Falta una propiedad de conexión requerida: " + String.join(" o ", claves));
    }

    private static String obtenerPropiedad(String clave, String valorPorDefecto) {
        String valor = PROPIEDADES.getProperty(clave);
        return valor == null || valor.isBlank() ? valorPorDefecto : valor.trim();
    }

    /**
     * Cierra de forma segura una conexión abierta.
     *
     * @param conexion conexión a cerrar
     */
    public static void cerrarConexion(Connection conexion) {
        if (conexion == null) {
            return;
        }

        try {
            conexion.close();
        } catch (SQLException e) {
            System.err.println("[ConexionBD] Error al cerrar la conexion: " + e.getMessage());
        }
    }

    /**
     * Abre una conexión en segundo plano para que la primera pantalla no pague
     * la latencia inicial de Neon.
     */
    public static void precalentar() {
        try (Connection conexion = obtenerConexion()) {
            if (conexion == null) {
                System.err.println("[ConexionBD] No se pudo precalentar el pool.");
            }
        } catch (SQLException e) {
            System.err.println("[ConexionBD] Error al precalentar el pool: " + e.getMessage());
        }
    }

    public static void cerrarPool() {
        if (!FUENTE_DATOS.isClosed()) {
            FUENTE_DATOS.close();
        }
    }
}
