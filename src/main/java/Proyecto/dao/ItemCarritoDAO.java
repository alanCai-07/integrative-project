package Proyecto.dao;

import Proyecto.Model.ItemCarrito;
import Proyecto.Model.Producto;
import Proyecto.util.conexionBD;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemCarritoDAO {

    public boolean agregarItemAlCarrito(int idCliente, int idProducto, int cantidad) {
        int idCarrito = obtenerOCrearCarritoPorCliente(idCliente);
        if (idCarrito == -1) {
            System.err.println("ItemCarritoDAO: no se pudo obtener/crear carrito para cliente " + idCliente);
            return false;
        }

        String sqlCheck = "SELECT id_item, cantidad FROM item_carrito "
                + "WHERE id_carrito = ? AND id_producto = ?";

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement ps = conexion.prepareStatement(sqlCheck)) {

            ps.setInt(1, idCarrito);
            ps.setInt(2, idProducto);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int idItem = rs.getInt("id_item");
                int cantActual = rs.getInt("cantidad");
                int cantNueva = cantActual + cantidad;
                return actualizarCantidadItem(idItem, cantNueva);
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar item en carrito: " + e.getMessage());
            return false;
        }

        String sqlInsert = "INSERT INTO item_carrito (id_carrito, id_producto, cantidad) "
                + "VALUES (?, ?, ?)";

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(sqlInsert)) {

            pstmt.setInt(1, idCarrito);
            pstmt.setInt(2, idProducto);
            pstmt.setInt(3, cantidad);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al agregar item al carrito: " + e.getMessage());
        }
        return false;
    }

    public List<ItemCarrito> obtenerItemsDelCarrito(int idCliente) {
        String sql = "SELECT ic.id_item, ic.cantidad, "
                + "p.id_producto, p.nombre, p.descripcion, p.precio_compra, "
                + "p.precio_venta, p.stock_actual, p.stock_minimo, p.activo, p.id_categoria "
                + "FROM item_carrito ic "
                + "JOIN carrito ca ON ic.id_carrito = ca.id_carrito "
                + "JOIN producto p ON ic.id_producto = p.id_producto "
                + "WHERE ca.id_cliente = ? "
                + "AND ca.estado = 'ABIERTO' "  
                + "ORDER BY ic.id_item";            

        List<ItemCarrito> items = new ArrayList<>();

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, idCliente);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                items.add(mapearItemCarrito(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener items del carrito: " + e.getMessage());
        }
        return items;
    }

    public boolean actualizarCantidadItem(int idItem, int cantidad) {
        String sql = "UPDATE item_carrito SET cantidad = ? WHERE id_item = ?";

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, cantidad);
            pstmt.setInt(2, idItem);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al actualizar cantidad: " + e.getMessage());
        }
        return false;
    }

    public boolean eliminarItemDelCarrito(int idItem) {
        String sql = "DELETE FROM item_carrito WHERE id_item = ?";

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, idItem);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al eliminar item: " + e.getMessage());
        }
        return false;
    }

    public boolean limpiarCarrito(int idCliente) {
        String sql = "DELETE FROM item_carrito "
                + "WHERE id_carrito IN (SELECT id_carrito FROM carrito WHERE id_cliente = ? AND estado = 'ABIERTO')";

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, idCliente);
            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Error al limpiar carrito: " + e.getMessage());
        }
        return false;
    }

    private int obtenerOCrearCarritoPorCliente(int idCliente) {
        String sqlSelect = "SELECT id_carrito FROM carrito "
                + "WHERE id_cliente = ? ORDER BY creado_en DESC LIMIT 1";

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement ps = conexion.prepareStatement(sqlSelect)) {

            ps.setInt(1, idCliente);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id_carrito");
            }

        } catch (SQLException e) {
            System.err.println("Error al buscar carrito: " + e.getMessage());
            return -1;
        }

        String sqlInsert = "INSERT INTO carrito (id_cliente, creado_en) VALUES (?, NOW())";

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement ps = conexion.prepareStatement(sqlInsert,
                     Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, idCliente);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("Error al crear carrito: " + e.getMessage());
        }
        return -1;
    }

    private ItemCarrito mapearItemCarrito(ResultSet rs) throws SQLException {
        Producto producto = new Producto();
        producto.setIdProducto(rs.getInt("id_producto"));
        producto.setNombre(rs.getString("nombre"));
        producto.setPrecioVenta(rs.getDouble("precio_venta"));
        producto.setCantidad(rs.getInt("stock_actual"));
        producto.setActivo(rs.getBoolean("activo"));

        ItemCarrito item = new ItemCarrito(producto, rs.getInt("cantidad"));
        item.setIdItem(rs.getInt("id_item"));
        return item;
    }
}