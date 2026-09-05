package Proyecto.dao;

import Proyecto.Model.ItemCarrito;
import Proyecto.Model.Producto;
import Proyecto.util.conexionBD;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ItemCarritoDAO — CORRECCIONES APLICADAS:
 *
 * BUG CRÍTICO en agregarItemAlCarrito():
 *   CarritoServices llama: itemCarritoDAO.agregarItemAlCarrito(carrito.getCliente().getId(), ...)
 *   es decir, pasa el ID del CLIENTE, no el ID del carrito.
 *   Pero el INSERT usaba ese valor como id_carrito directamente.
 *   Resultado: INSERT fallaba con FK violation porque no existe carrito con id = id_cliente.
 *
 *   FIX: el método ahora busca el id_carrito real a partir del id_cliente
 *   antes de hacer el INSERT. Si no existe carrito lo crea.
 *
 * BUG en obtenerItemsDelCarrito():
 *   Mismo problema — recibía id_cliente pero buscaba en carrito.id_carrito.
 *   FIX: hace JOIN para resolver id_cliente → id_carrito.
 *
 * BUG en limpiarCarrito():
 *   Mismo problema.
 *   FIX: resuelve id_carrito desde id_cliente antes del DELETE.
 */
public class ItemCarritoDAO {

    // ── Agregar item al carrito ────────────────────────────────────────────────
    // PARÁMETRO: idCliente (no idCarrito — CarritoServices pasa cliente.getId())
    public boolean agregarItemAlCarrito(int idCliente, int idProducto, int cantidad) {

        // 1. Resolver el id_carrito real desde el id_cliente
        int idCarrito = obtenerOCrearCarritoPorCliente(idCliente);
        if (idCarrito == -1) {
            System.err.println("ItemCarritoDAO: no se pudo obtener/crear carrito para cliente " + idCliente);
            return false;
        }

        // 2. Si el producto ya existe en el carrito, actualizar cantidad
        String sqlCheck = "SELECT id_item, cantidad FROM item_carrito "
                + "WHERE id_carrito = ? AND id_producto = ?";

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement ps = conexion.prepareStatement(sqlCheck)) {

            ps.setInt(1, idCarrito);
            ps.setInt(2, idProducto);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // Ya existe: suma la cantidad
                int idItem       = rs.getInt("id_item");
                int cantActual   = rs.getInt("cantidad");
                int cantNueva    = cantActual + cantidad;
                return actualizarCantidadItem(idItem, cantNueva);
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar item en carrito: " + e.getMessage());
            return false;
        }

        // 3. Producto nuevo: INSERT
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

    // ── Obtener items del carrito ──────────────────────────────────────────────
    // PARÁMETRO: idCliente (CarritoServices pasa cliente.getId())
    public List<ItemCarrito> obtenerItemsDelCarrito(int idCliente) {
        // Resuelve id_carrito desde id_cliente mediante JOIN
        String sql = "SELECT ic.id_item, ic.cantidad, "
                + "p.id_producto, p.nombre, p.descripcion, p.precio_compra, "
                + "p.precio_venta, p.stock_actual, p.stock_minimo, p.activo, p.id_categoria "
                + "FROM item_carrito ic "
                + "JOIN carrito ca  ON ic.id_carrito = ca.id_carrito "
                + "JOIN producto p  ON ic.id_producto = p.id_producto "
                + "WHERE ca.id_cliente = ? "
                + "ORDER BY ic.id_item";

        List<ItemCarrito> items = new ArrayList<>();

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, idCliente);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) items.add(mapearItemCarrito(rs));

        } catch (SQLException e) {
            System.err.println("Error al obtener items del carrito: " + e.getMessage());
        }
        return items;
    }

    // ── Actualizar cantidad ────────────────────────────────────────────────────
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

    // ── Eliminar item ──────────────────────────────────────────────────────────
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

    // ── Limpiar carrito ───────────────────────────────────────────────────────
    // PARÁMETRO: idCliente (CarritoServices pasa cliente.getId())
    public boolean limpiarCarrito(int idCliente) {
        // Resuelve id_carrito desde id_cliente
        String sql = "DELETE ic FROM item_carrito ic "
                + "JOIN carrito ca ON ic.id_carrito = ca.id_carrito "
                + "WHERE ca.id_cliente = ?";

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, idCliente);
            pstmt.executeUpdate(); // puede ser 0 filas si el carrito estaba vacío
            return true;

        } catch (SQLException e) {
            System.err.println("Error al limpiar carrito: " + e.getMessage());
        }
        return false;
    }

    // ── Helpers internos ──────────────────────────────────────────────────────

    /**
     * Busca el carrito activo del cliente. Si no existe, lo crea.
     * Devuelve el id_carrito o -1 si falla.
     */
    private int obtenerOCrearCarritoPorCliente(int idCliente) {
        // Buscar carrito existente (el más reciente)
        String sqlSelect = "SELECT id_carrito FROM carrito "
                + "WHERE id_cliente = ? ORDER BY creado_en DESC LIMIT 1";

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement ps = conexion.prepareStatement(sqlSelect)) {

            ps.setInt(1, idCliente);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id_carrito");

        } catch (SQLException e) {
            System.err.println("Error al buscar carrito: " + e.getMessage());
            return -1;
        }

        // No existe → crear uno nuevo
        String sqlInsert = "INSERT INTO carrito (id_cliente, creado_en) VALUES (?, NOW())";

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement ps = conexion.prepareStatement(sqlInsert,
                     Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, idCliente);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);

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
