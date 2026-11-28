package Proyecto.dao;

import Proyecto.Model.Cliente;
import Proyecto.util.conexionBD;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PersonaDAO {

    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(10));
    }

    public static boolean verificarPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }
        if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$")) {
            return BCrypt.checkpw(plainPassword, storedHash);
        }
        return false;
    }

    public List<Cliente> buscarClientes(String query) {
        String sql = "SELECT p.id_persona, p.nombres AS nombre, p.apellidos AS apellido, " +
                "p.documento, p.telefono, p.email, p.direccion " +
                "FROM persona p " +
                "JOIN cliente c ON p.id_persona = c.id_persona " +
                "WHERE p.activo = TRUE AND p.tipo = 'CLIENTE' " +
                "  AND (p.documento ILIKE ? OR p.nombres ILIKE ? OR p.apellidos ILIKE ? " +
                "       OR p.email ILIKE ? OR CONCAT(p.nombres, ' ', p.apellidos) ILIKE ?) " +
                "ORDER BY p.nombres ASC LIMIT 15";

        List<Cliente> resultados = new ArrayList<>();
        String patron = "%" + (query == null ? "" : query.trim()) + "%";

        try (Connection conn = conexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) {
                ps.setString(i, patron);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultados.add(mapearClienteSinHash(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar clientes: " + e.getMessage());
        }
        return resultados;
    }

    public Cliente obtenerEmpleadoPorEmail(String email) {
        String sql = "SELECT p.id_persona, p.nombres AS nombre, p.apellidos AS apellido, " +
                "p.documento, p.telefono, p.email, p.direccion, " +
                "e.contrasena_hash, UPPER(c.nombre) AS nombre_cargo " +
                "FROM persona p " +
                "JOIN empleado e ON p.id_persona = e.id_persona " +
                "JOIN cargo c ON e.id_cargo = c.id_cargo " +
                "WHERE LOWER(p.email) = LOWER(?) AND p.tipo = 'EMPLEADO' AND p.activo = TRUE";

        try (Connection conn = obtenerConexionAutenticacion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Cliente empleado = mapearClienteConHash(rs);
                    String cargo = rs.getString("nombre_cargo");
                    empleado.setRol(cargo != null ? cargo : "EMPLEADO");
                    return empleado;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron consultar las credenciales del empleado.", e);
        }
        return null;
    }

    public Cliente obtenerCredencialesClientePorEmail(String email) {
        String sql = "SELECT p.id_persona, p.nombres AS nombre, p.apellidos AS apellido, " +
                "p.documento, p.telefono, p.email, p.direccion, c.contrasena_hash " +
                "FROM persona p " +
                "JOIN cliente c ON p.id_persona = c.id_persona " +
                "WHERE LOWER(p.email) = LOWER(?) AND p.tipo = 'CLIENTE' " +
                "  AND p.activo = TRUE AND c.contrasena_hash IS NOT NULL";

        try (Connection conn = obtenerConexionAutenticacion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Cliente cli = mapearClienteConHash(rs);
                    cli.setRol("CLIENTE");
                    return cli;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron consultar las credenciales del cliente.", e);
        }
        return null;
    }

    private Connection obtenerConexionAutenticacion() {
        Connection conexion = conexionBD.obtenerConexion();
        if (conexion == null) {
            throw new IllegalStateException(
                    "No se pudo conectar con la base de datos. Verifica config.properties y la conexión a internet.");
        }
        return conexion;
    }

    public Cliente obtenerClientePorEmail(String email) {
        String sql = "SELECT id_persona, nombre, apellido, documento, telefono, email, direccion, activo " +
                "FROM vista_persona_cliente WHERE LOWER(email) = LOWER(?) AND activo = TRUE";
        try (Connection conn = conexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearClienteSinHash(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener perfil cliente: " + e.getMessage());
        }
        return null;
    }

    public boolean crearCliente(Cliente cliente) {
        String sqlPersona = "INSERT INTO persona " +
                "(tipo, nombres, apellidos, documento, telefono, email, direccion, activo) " +
                "VALUES ('CLIENTE', ?, ?, ?, ?, ?, ?, TRUE) RETURNING id_persona";
        String sqlCliente = "INSERT INTO cliente (id_persona, tipo, contrasena_hash) VALUES (?, 'CLIENTE', ?)";

        Connection conexion = null;
        try {
            conexion = conexionBD.obtenerConexion();
            conexion.setAutoCommit(false);

            int idPersona;
            try (PreparedStatement ps = conexion.prepareStatement(sqlPersona)) {
                ps.setString(1, cliente.getNombre());
                ps.setString(2, cliente.getApellido());
                String doc = (cliente.getDocumento() != null && !cliente.getDocumento().isBlank())
                        ? cliente.getDocumento().trim()
                        : "CLI-" + System.currentTimeMillis();
                ps.setString(3, doc);
                ps.setString(4, cliente.getTelefono());
                ps.setString(5, cliente.getEmail());
                ps.setString(6, cliente.getDireccion());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conexion.rollback();
                        return false;
                    }
                    idPersona = rs.getInt(1);
                }
            }

            try (PreparedStatement ps2 = conexion.prepareStatement(sqlCliente)) {
                ps2.setInt(1, idPersona);
                String hash = cliente.getPasswordHash();
                if (hash != null && !hash.startsWith("$2")) {
                    hash = hashPassword(hash);
                }
                ps2.setString(2, hash);
                ps2.executeUpdate();
            }

            conexion.commit();
            cliente.setId(idPersona);
            return true;
        } catch (SQLException e) {
            System.err.println("Error al crear cliente: " + e.getMessage());
            rollbackSilencioso(conexion);
        } finally {
            cerrarTransaccion(conexion);
        }
        return false;
    }

    public boolean crearEmpleado(String nombre, String apellido, String email,
                                 String telefono, String documento,
                                 int idCargo, String passwordPlanoOrHash) {
        String sqlPersona = "INSERT INTO persona " +
                "(tipo, nombres, apellidos, documento, telefono, email, activo) " +
                "VALUES ('EMPLEADO', ?, ?, ?, ?, ?, TRUE) RETURNING id_persona";
        String sqlEmpleado = "INSERT INTO empleado (id_persona, tipo, id_cargo, fecha_ingreso, contrasena_hash) " +
                "VALUES (?, 'EMPLEADO', ?, CURRENT_DATE, ?)";

        Connection conexion = null;
        try {
            conexion = conexionBD.obtenerConexion();
            conexion.setAutoCommit(false);

            int idPersona;
            try (PreparedStatement ps = conexion.prepareStatement(sqlPersona)) {
                ps.setString(1, nombre.trim());
                ps.setString(2, apellido.trim());
                ps.setString(3, documento.trim());
                ps.setString(4, telefono != null && !telefono.isBlank() ? telefono.trim() : null);
                ps.setString(5, email.toLowerCase().trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conexion.rollback();
                        return false;
                    }
                    idPersona = rs.getInt(1);
                }
            }

            try (PreparedStatement ps2 = conexion.prepareStatement(sqlEmpleado)) {
                ps2.setInt(1, idPersona);
                ps2.setInt(2, idCargo);
                String hash = passwordPlanoOrHash.startsWith("$2")
                        ? passwordPlanoOrHash
                        : hashPassword(passwordPlanoOrHash);
                ps2.setString(3, hash);
                ps2.executeUpdate();
            }

            conexion.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error al crear empleado: " + e.getMessage());
            rollbackSilencioso(conexion);
        } finally {
            cerrarTransaccion(conexion);
        }
        return false;
    }

    public boolean actualizarEmpleado(int idPersona, String nombre, String apellido,
                                      String telefono, String email, String direccion,
                                      int idCargo, boolean activo) {
        String sqlPersona = "UPDATE persona SET nombres = ?, apellidos = ?, telefono = ?, " +
                "email = ?, direccion = ?, activo = ? WHERE id_persona = ? AND tipo = 'EMPLEADO'";
        String sqlEmpleado = "UPDATE empleado SET id_cargo = ? WHERE id_persona = ?";

        Connection conexion = null;
        try {
            conexion = conexionBD.obtenerConexion();
            conexion.setAutoCommit(false);

            try (PreparedStatement ps = conexion.prepareStatement(sqlPersona)) {
                ps.setString(1, nombre.trim());
                ps.setString(2, apellido.trim());
                ps.setString(3, telefono != null && !telefono.isBlank() ? telefono.trim() : null);
                ps.setString(4, email.toLowerCase().trim());
                ps.setString(5, direccion != null && !direccion.isBlank() ? direccion.trim() : null);
                ps.setBoolean(6, activo);
                ps.setInt(7, idPersona);
                ps.executeUpdate();
            }

            try (PreparedStatement ps2 = conexion.prepareStatement(sqlEmpleado)) {
                ps2.setInt(1, idCargo);
                ps2.setInt(2, idPersona);
                ps2.executeUpdate();
            }

            conexion.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error al actualizar empleado: " + e.getMessage());
            rollbackSilencioso(conexion);
        } finally {
            cerrarTransaccion(conexion);
        }
        return false;
    }

    public boolean actualizarEmpleado(int idPersona, String nombre, String apellido,
                                      String telefono, int idCargo, boolean activo) {
        String sqlPersona = "UPDATE persona SET nombres = ?, apellidos = ?, telefono = ?, activo = ? "
                + "WHERE id_persona = ? AND tipo = 'EMPLEADO'";
        String sqlEmpleado = "UPDATE empleado SET id_cargo = ? WHERE id_persona = ?";

        Connection conexion = null;
        try {
            conexion = conexionBD.obtenerConexion();
            conexion.setAutoCommit(false);

            try (PreparedStatement ps = conexion.prepareStatement(sqlPersona)) {
                ps.setString(1, nombre.trim());
                ps.setString(2, apellido.trim());
                ps.setString(3, telefono != null && !telefono.isBlank() ? telefono.trim() : null);
                ps.setBoolean(4, activo);
                ps.setInt(5, idPersona);
                if (ps.executeUpdate() == 0) {
                    conexion.rollback();
                    return false;
                }
            }

            try (PreparedStatement ps = conexion.prepareStatement(sqlEmpleado)) {
                ps.setInt(1, idCargo);
                ps.setInt(2, idPersona);
                ps.executeUpdate();
            }

            conexion.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error al actualizar empleado: " + e.getMessage());
            rollbackSilencioso(conexion);
            return false;
        } finally {
            cerrarTransaccion(conexion);
        }
    }

    public boolean crearProveedor(String empresa, String nit, String nombre, String apellido,
                                  String email, String telefono, String direccion) {
        String sqlPersona = "INSERT INTO persona " +
                "(tipo, nombres, apellidos, documento, telefono, email, direccion, activo) " +
                "VALUES ('PROVEEDOR', ?, ?, ?, ?, ?, ?, TRUE) RETURNING id_persona";
        String sqlProveedor = "INSERT INTO proveedor (id_persona, tipo, nombre_empresa, nit) " +
                "VALUES (?, 'PROVEEDOR', ?, ?)";

        Connection conexion = null;
        try {
            conexion = conexionBD.obtenerConexion();
            conexion.setAutoCommit(false);
            int idPersona;
            try (PreparedStatement ps = conexion.prepareStatement(sqlPersona)) {
                ps.setString(1, nombre.trim());
                ps.setString(2, apellido.trim());
                ps.setString(3, nit.trim());
                ps.setString(4, telefono);
                ps.setString(5, email);
                ps.setString(6, direccion);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conexion.rollback();
                        return false;
                    }
                    idPersona = rs.getInt(1);
                }
            }
            try (PreparedStatement ps = conexion.prepareStatement(sqlProveedor)) {
                ps.setInt(1, idPersona);
                ps.setString(2, empresa.trim());
                ps.setString(3, nit.trim());
                ps.executeUpdate();
            }
            conexion.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error al crear proveedor: " + e.getMessage());
            rollbackSilencioso(conexion);
            return false;
        } finally {
            cerrarTransaccion(conexion);
        }
    }

    public boolean actualizarProveedor(int idPersona, String empresa, String nit, String nombre,
                                       String apellido, String email, String telefono,
                                       String direccion, boolean activo) {
        String sqlPersona = "UPDATE persona SET nombres = ?, apellidos = ?, documento = ?, " +
                "telefono = ?, email = ?, direccion = ?, activo = ? " +
                "WHERE id_persona = ? AND tipo = 'PROVEEDOR'";
        String sqlProveedor = "UPDATE proveedor SET nombre_empresa = ?, nit = ? WHERE id_persona = ?";

        Connection conexion = null;
        try {
            conexion = conexionBD.obtenerConexion();
            conexion.setAutoCommit(false);
            try (PreparedStatement ps = conexion.prepareStatement(sqlPersona)) {
                ps.setString(1, nombre.trim());
                ps.setString(2, apellido.trim());
                ps.setString(3, nit.trim());
                ps.setString(4, telefono);
                ps.setString(5, email);
                ps.setString(6, direccion);
                ps.setBoolean(7, activo);
                ps.setInt(8, idPersona);
                if (ps.executeUpdate() == 0) {
                    conexion.rollback();
                    return false;
                }
            }
            try (PreparedStatement ps = conexion.prepareStatement(sqlProveedor)) {
                ps.setString(1, empresa.trim());
                ps.setString(2, nit.trim());
                ps.setInt(3, idPersona);
                if (ps.executeUpdate() == 0) {
                    conexion.rollback();
                    return false;
                }
            }
            conexion.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error al actualizar proveedor: " + e.getMessage());
            rollbackSilencioso(conexion);
            return false;
        } finally {
            cerrarTransaccion(conexion);
        }
    }

    public Cliente obtenerClientePorId(int idPersona) {
        String sql = "SELECT id_persona, nombre, apellido, documento, telefono, email, direccion, activo " +
                "FROM vista_persona_cliente WHERE id_persona = ?";
        try (Connection conn = conexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPersona);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearClienteSinHash(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener cliente: " + e.getMessage());
        }
        return null;
    }

    public boolean emailExiste(String email) {
        String sql = "SELECT 1 FROM persona WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = conexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar email: " + e.getMessage());
        }
        return false;
    }

    public List<Cliente> obtenerTodosLosClientes() {
        String sql = "SELECT id_persona, nombre, apellido, documento, telefono, email, direccion, activo " +
                "FROM vista_persona_cliente WHERE activo = TRUE ORDER BY nombre";
        List<Cliente> clientes = new ArrayList<>();
        try (Connection conn = conexionBD.obtenerConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                clientes.add(mapearClienteSinHash(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener clientes: " + e.getMessage());
        }
        return clientes;
    }

    public boolean actualizarCliente(Cliente cliente) {
        String sql = "UPDATE persona SET nombres = ?, apellidos = ?, documento = ?, " +
                "telefono = ?, email = ?, direccion = ? WHERE id_persona = ? AND tipo = 'CLIENTE'";
        try (Connection conn = conexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getApellido());
            ps.setString(3, cliente.getDocumento());
            ps.setString(4, cliente.getTelefono());
            ps.setString(5, cliente.getEmail());
            ps.setString(6, cliente.getDireccion());
            ps.setInt(7, cliente.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar cliente: " + e.getMessage());
        }
        return false;
    }

    public boolean actualizarPassword(int idPersona, String plainPassword) {
        String sql = "UPDATE cliente SET contrasena_hash = ? WHERE id_persona = ?";
        try (Connection conn = conexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hashPassword(plainPassword));
            ps.setInt(2, idPersona);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar contrasena: " + e.getMessage());
        }
        return false;
    }

    public boolean eliminarCliente(int idPersona) {
        String sql = "UPDATE persona SET activo = FALSE WHERE id_persona = ? AND tipo = 'CLIENTE'";
        try (Connection conn = conexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPersona);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar cliente: " + e.getMessage());
        }
        return false;
    }

    private Cliente mapearClienteConHash(ResultSet rs) throws SQLException {
        return new Cliente(
                rs.getInt("id_persona"),
                rs.getString("nombre"),
                rs.getString("apellido"),
                rs.getString("documento"),
                rs.getString("telefono"),
                rs.getString("email"),
                rs.getString("direccion"),
                rs.getString("contrasena_hash"));
    }

    private Cliente mapearClienteSinHash(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id_persona"));
        c.setNombre(rs.getString("nombre"));
        c.setApellido(rs.getString("apellido"));
        c.setDocumento(rs.getString("documento"));
        c.setTelefono(rs.getString("telefono"));
        c.setEmail(rs.getString("email"));
        c.setDireccion(rs.getString("direccion"));
        return c;
    }

    private void rollbackSilencioso(Connection conexion) {
        try {
            if (conexion != null) {
                conexion.rollback();
            }
        } catch (SQLException ignored) {
        }
    }

    private void cerrarTransaccion(Connection conexion) {
        if (conexion == null) {
            return;
        }
        try {
            conexion.setAutoCommit(true);
            conexion.close();
        } catch (SQLException e) {
            System.err.println("Error al cerrar conexion: " + e.getMessage());
        }
    }
}