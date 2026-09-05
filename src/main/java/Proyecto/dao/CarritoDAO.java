package Proyecto.dao;

import Proyecto.Model.Carrito;
import Proyecto.Model.Cliente;
import Proyecto.util.conexionBD;
import java.sql.*;

public class CarritoDAO {

    // Crear carrito
    public int crearCarrito(int idCliente) {
        String sql = "INSERT INTO carrito (id_cliente, creado_en) VALUES (?, NOW())";

        try (Connection conexion = conexionBD.obtenerConexion();
                PreparedStatement pstmt = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, idCliente);
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error al crear carrito: " + e.getMessage());
        }
        return -1;
    }

    // Obtener carrito por ID
    public Carrito obtenerCarritoPorId(int idCarrito) {
        String sql = "SELECT * FROM carrito WHERE id_carrito = ?";

        try (Connection conexion = conexionBD.obtenerConexion();
                PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, idCarrito);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapearCarrito(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener carrito: " + e.getMessage());
        }
        return null;
    }

    // Obtener carrito activo del cliente
    public Carrito obtenerCarritoActivoDelCliente(int idCliente) {
        String sql = "SELECT * FROM carrito WHERE id_cliente = ? ORDER BY creado_en DESC LIMIT 1";

        try (Connection conexion = conexionBD.obtenerConexion();
                PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, idCliente);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapearCarrito(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener carrito activo: " + e.getMessage());
        }
        return null;
    }

    // Eliminar carrito
    public boolean eliminarCarrito(int idCarrito) {
        String sql = "DELETE FROM carrito WHERE id_carrito = ?";

        try (Connection conexion = conexionBD.obtenerConexion();
                PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, idCarrito);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar carrito: " + e.getMessage());
        }
        return false;
    }

    private Carrito mapearCarrito(ResultSet rs) throws SQLException {
        Cliente cliente = new Cliente();
        cliente.setId(rs.getInt("id_cliente"));

        Carrito carrito = new Carrito();
        carrito.setIdCarrito(rs.getInt("id_carrito"));
        carrito.setCliente(cliente);
        carrito.setCreadoEn(rs.getTimestamp("creado_en").toLocalDateTime());

        return carrito;
    }
}
