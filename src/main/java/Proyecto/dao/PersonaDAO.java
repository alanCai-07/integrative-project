package Proyecto.dao;

import Proyecto.Model.Cliente;
import Proyecto.util.conexionBD;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PersonaDAO {

    public List<Cliente> buscarClientes(String query) {
        String sql = "SELECT p.id_persona, " +
                "p.nombres AS nombre, p.apellidos AS apellido, " +
                "p.documento, p.telefono, p.email, p.direccion, " +
                "c.contrasena_hash " +
                "FROM persona p " +
                "JOIN cliente c ON p.id_persona = c.id_persona " +
                "WHERE p.activo = 1 " +
                "  AND p.tipo = 'CLIENTE' " +
                "  AND (p.nombres   LIKE ? " +
                "    OR p.apellidos LIKE ? " +
                "    OR p.email     LIKE ?) " +
                "ORDER BY p.nombres ASC " +
                "LIMIT 10";

        List<Cliente> resultados = new ArrayList<>();
        String patron = "%" + query.trim() + "%";

        try (Connection conn = conexionBD.obtenerConexion();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, patron);
            ps.setString(2, patron);
            ps.setString(3, patron);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                resultados.add(mapearCliente(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar clientes: " + e.getMessage());
        }
        return resultados;
    }

    public Cliente obtenerEmpleadoPorEmail(String email) {
        String sql = "SELECT p.id_persona, " +
                "p.nombres AS nombre, p.apellidos AS apellido, " +
                "p.documento, p.telefono, p.email, p.direccion, " +
                "e.contrasena_hash, " +
                "UPPER(c.nombre) AS nombre_cargo " +
                "FROM persona p " +
                "JOIN empleado e ON p.id_persona = e.id_persona " +
                "JOIN cargo    c ON e.id_cargo   = c.id_cargo " +
                "WHERE p.email = ? AND p.tipo = 'EMPLEADO' AND p.activo = 1";

        try (Connection conn = conexionBD.obtenerConexion();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Cliente empleado = mapearCliente(rs);
                String cargo = rs.getString("nombre_cargo");
                empleado.setRol(cargo != null ? cargo : "EMPLEADO");
                return empleado;
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener empleado por email: " + e.getMessage());
        }
        return null;
    }

    // ── Crear cliente ──────────────────────────────────────────────────────────
    public boolean crearCliente(Cliente cliente) {
        String sqlPersona = "INSERT INTO persona " +
                "(tipo, nombres, apellidos, documento, telefono, email, direccion, activo) " +
                "VALUES ('CLIENTE', ?, ?, ?, ?, ?, ?, 1)";
        String sqlCliente = "INSERT INTO cliente (id_persona, contrasena_hash) VALUES (?, ?)";

        Connection conexion = null;
        try {
            conexion = conexionBD.obtenerConexion();
            conexion.setAutoCommit(false);

            int idPersona;
            try (PreparedStatement ps = conexion.prepareStatement(
                    sqlPersona, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, cliente.getNombre());
                ps.setString(2, cliente.getApellido());
                String doc = (cliente.getDocumento() != null && !cliente.getDocumento().isEmpty())
                        ? cliente.getDocumento()
                        : "CLI-" + System.currentTimeMillis();
                ps.setString(3, doc);
                ps.setString(4, cliente.getTelefono());
                ps.setString(5, cliente.getEmail());
                ps.setString(6, cliente.getDireccion());
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (!rs.next()) {
                    conexion.rollback();
                    return false;
                }
                idPersona = rs.getInt(1);
            }

            try (PreparedStatement ps2 = conexion.prepareStatement(sqlCliente)) {
                ps2.setInt(1, idPersona);
                ps2.setString(2, cliente.getPasswordHash());
                ps2.executeUpdate();
            }

            conexion.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Error al crear cliente: " + e.getMessage());
            try {
                if (conexion != null)
                    conexion.rollback();
            } catch (SQLException ex) {
                /* ignore */ }
        } finally {
            try {
                if (conexion != null) {
                    conexion.setAutoCommit(true);
                    conexion.close();
                }
            } catch (SQLException ex) {
                /* ignore */ }
        }
        return false;
    }

    // ── Crear empleado ─────────────────────────────────────────────────────────
    /**
     * Registra un nuevo empleado en las tablas persona y empleado.
     * El idCargo corresponde al ID del cargo en la tabla cargo:
     * 1=Administrador, 2=Comprador, 3=Vendedor, 4=Cajero, 5=Bodeguero
     */
    public boolean crearEmpleado(String nombre, String apellido, String email,
            String telefono, String documento,
            int idCargo, String passwordHash, double salario) {
        String sqlPersona = "INSERT INTO persona " +
                "(tipo, nombres, apellidos, documento, telefono, email, activo) " +
                "VALUES ('EMPLEADO', ?, ?, ?, ?, ?, 1)";
        String sqlEmpleado = "INSERT INTO empleado (id_persona, id_cargo, fecha_ingreso, contrasena_hash, salario) " +
                "VALUES (?, ?, CURDATE(), ?, ?)";

        Connection conexion = null;
        try {
            conexion = conexionBD.obtenerConexion();
            conexion.setAutoCommit(false);

            int idPersona;
            try (PreparedStatement ps = conexion.prepareStatement(
                    sqlPersona, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, nombre.trim());
                ps.setString(2, apellido.trim());
                ps.setString(3, documento.trim());
                ps.setString(4, telefono != null ? telefono.trim() : null);
                ps.setString(5, email.toLowerCase().trim());
                ps.executeUpdate();
                ResultSet rs = ps.getGeneratedKeys();
                if (!rs.next()) {
                    conexion.rollback();
                    return false;
                }
                idPersona = rs.getInt(1);
            }

            try (PreparedStatement ps2 = conexion.prepareStatement(sqlEmpleado)) {
                ps2.setInt(1, idPersona);
                ps2.setInt(2, idCargo);
                ps2.setString(3, passwordHash);
                ps2.setDouble(4, salario);
                ps2.executeUpdate();
            }

            conexion.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Error al crear empleado: " + e.getMessage());
            try {
                if (conexion != null)
                    conexion.rollback();
            } catch (SQLException ex) {
                /* ignore */ }
        } finally {
            try {
                if (conexion != null) {
                    conexion.setAutoCommit(true);
                    conexion.close();
                }
            } catch (SQLException ex) {
                /* ignore */ }
        }
        return false;
    }

    // ── Actualizar empleado ────────────────────────────────────────────────────
    public boolean actualizarEmpleado(int idPersona, String nombre, String apellido,
            String telefono, int idCargo,
            double salario, boolean activo) {
        String sqlPersona = "UPDATE persona SET nombres = ?, apellidos = ?, telefono = ?, activo = ? " +
                "WHERE id_persona = ? AND tipo = 'EMPLEADO'";
        String sqlEmpleado = "UPDATE empleado SET id_cargo = ?, salario = ? WHERE id_persona = ?";

        Connection conexion = null;
        try {
            conexion = conexionBD.obtenerConexion();
            conexion.setAutoCommit(false);

            try (PreparedStatement ps = conexion.prepareStatement(sqlPersona)) {
                ps.setString(1, nombre.trim());
                ps.setString(2, apellido.trim());
                ps.setString(3, telefono != null ? telefono.trim() : null);
                ps.setBoolean(4, activo);
                ps.setInt(5, idPersona);
                ps.executeUpdate();
            }

            try (PreparedStatement ps2 = conexion.prepareStatement(sqlEmpleado)) {
                ps2.setInt(1, idCargo);
                ps2.setDouble(2, salario);
                ps2.setInt(3, idPersona);
                ps2.executeUpdate();
            }

            conexion.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("Error al actualizar empleado: " + e.getMessage());
            try {
                if (conexion != null)
                    conexion.rollback();
            } catch (SQLException ex) {
                /* ignore */ }
        } finally {
            try {
                if (conexion != null) {
                    conexion.setAutoCommit(true);
                    conexion.close();
                }
            } catch (SQLException ex) {
                /* ignore */ }
        }
        return false;
    }

    // ── Obtener cliente por ID ─────────────────────────────────────────────────
    public Cliente obtenerClientePorId(int idPersona) {
        String sql = "SELECT p.id_persona, " +
                "p.nombres AS nombre, p.apellidos AS apellido, " +
                "p.documento, p.telefono, p.email, p.direccion, " +
                "c.contrasena_hash " +
                "FROM persona p " +
                "JOIN cliente c ON p.id_persona = c.id_persona " +
                "WHERE p.id_persona = ? AND p.tipo = 'CLIENTE'";

        try (Connection conn = conexionBD.obtenerConexion();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPersona);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapearCliente(rs);
        } catch (SQLException e) {
            System.err.println("Error al obtener cliente: " + e.getMessage());
        }
        return null;
    }

    public Cliente obtenerClientePorEmail(String email) {
        String sql = "SELECT * FROM vista_persona_cliente WHERE email = ?";
        try (Connection conn = conexionBD.obtenerConexion();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return mapearCliente(rs);
        } catch (SQLException e) {
            System.err.println("Error al obtener cliente por email: " + e.getMessage());
        }
        return null;
    }

    public boolean emailExiste(String email) {
        String sql = "SELECT COUNT(*) FROM persona WHERE email = ?";
        try (Connection conn = conexionBD.obtenerConexion();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("Error al verificar email: " + e.getMessage());
        }
        return false;
    }

    public List<Cliente> obtenerTodosLosClientes() {
        String sql = "SELECT * FROM vista_persona_cliente";
        List<Cliente> clientes = new ArrayList<>();
        try (Connection conn = conexionBD.obtenerConexion();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next())
                clientes.add(mapearCliente(rs));
        } catch (SQLException e) {
            System.err.println("Error al obtener clientes: " + e.getMessage());
        }
        return clientes;
    }

    public boolean actualizarCliente(Cliente cliente) {
        String sql = "UPDATE persona SET nombres = ?, apellidos = ?, documento = ?, " +
                "telefono = ?, email = ?, direccion = ? WHERE id_persona = ?";
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

    public boolean actualizarPassword(int idPersona, String passwordHash) {
        String sql = "UPDATE cliente SET contrasena_hash = ? WHERE id_persona = ?";
        try (Connection conn = conexionBD.obtenerConexion();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, idPersona);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar contrasena: " + e.getMessage());
        }
        return false;
    }

    public boolean eliminarCliente(int idPersona) {
        String sql = "UPDATE persona SET activo = false WHERE id_persona = ?";
        try (Connection conn = conexionBD.obtenerConexion();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPersona);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar cliente: " + e.getMessage());
        }
        return false;
    }

    private Cliente mapearCliente(ResultSet rs) throws SQLException {
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
}