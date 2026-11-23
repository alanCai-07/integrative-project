package Proyecto.dao;

import Proyecto.Model.Categoria;
import Proyecto.Model.Producto;
import Proyecto.util.conexionBD;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    public boolean crearProducto(Producto producto) {
        String sql = "INSERT INTO producto (id_categoria, nombre, descripcion, precio_compra, precio_venta, " +
                "stock_actual, stock_minimo, imagen_url, activo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, TRUE) RETURNING id_producto";

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, producto.getCategoria().getId());
            pstmt.setString(2, producto.getNombre().trim());
            pstmt.setString(3, producto.getDescripcion());
            pstmt.setDouble(4, producto.getPrecioCompra());
            pstmt.setDouble(5, producto.getPrecioVenta());
            pstmt.setInt(6, Math.max(0, producto.getStockActual()));
            pstmt.setInt(7, Math.max(0, producto.getStockMinimo()));
            pstmt.setString(8, producto.getImagenUrl());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    producto.setIdProducto(rs.getInt(1));
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al crear producto: " + e.getMessage());
        }
        return false;
    }

    public Producto obtenerProductoporId(int idProducto) {
        String sql = "SELECT p.id_producto, p.id_categoria, c.nombre AS categoria_nombre, " +
                "c.descripcion AS categoria_descripcion, p.nombre, p.descripcion, " +
                "p.precio_compra, p.precio_venta, p.stock_actual, p.stock_minimo, p.imagen_url, p.activo " +
                "FROM producto p JOIN categoria c ON c.id_categoria = p.id_categoria " +
                "WHERE p.id_producto = ?";

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, idProducto);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapearProducto(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener producto: " + e.getMessage());
        }
        return null;
    }

    public List<Producto> obtenerTodosProductos() {
        String sql = "SELECT p.id_producto, p.id_categoria, c.nombre AS categoria_nombre, " +
                "c.descripcion AS categoria_descripcion, p.nombre, p.descripcion, " +
                "p.precio_compra, p.precio_venta, p.stock_actual, p.stock_minimo, p.imagen_url, p.activo " +
                "FROM producto p JOIN categoria c ON c.id_categoria = p.id_categoria " +
                "WHERE p.activo = TRUE ORDER BY p.nombre";
        List<Producto> productos = new ArrayList<>();

        try (Connection conexion = conexionBD.obtenerConexion();
             Statement stmt = conexion.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                productos.add(mapearProducto(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener productos: " + e.getMessage());
        }
        return productos;
    }

    public List<Producto> obtenerProductosPorCategoria(int idCategoria) {
        String sql = "SELECT p.id_producto, p.id_categoria, c.nombre AS categoria_nombre, " +
                "c.descripcion AS categoria_descripcion, p.nombre, p.descripcion, " +
                "p.precio_compra, p.precio_venta, p.stock_actual, p.stock_minimo, p.imagen_url, p.activo " +
                "FROM producto p JOIN categoria c ON c.id_categoria = p.id_categoria " +
                "WHERE p.id_categoria = ? AND p.activo = TRUE ORDER BY p.nombre";
        List<Producto> productos = new ArrayList<>();

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, idCategoria);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    productos.add(mapearProducto(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener productos por categoria: " + e.getMessage());
        }
        return productos;
    }

    public List<Producto> buscarProductosPorNombre(String nombre) {
        String sql = "SELECT p.id_producto, p.id_categoria, c.nombre AS categoria_nombre, " +
                "c.descripcion AS categoria_descripcion, p.nombre, p.descripcion, " +
                "p.precio_compra, p.precio_venta, p.stock_actual, p.stock_minimo, p.imagen_url, p.activo " +
                "FROM producto p JOIN categoria c ON c.id_categoria = p.id_categoria " +
                "WHERE p.activo = TRUE AND (p.nombre ILIKE ? OR CAST(p.id_producto AS TEXT) = ?) " +
                "ORDER BY p.nombre";
        List<Producto> productos = new ArrayList<>();
        String criterio = nombre == null ? "" : nombre.trim();

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setString(1, "%" + criterio + "%");
            pstmt.setString(2, criterio);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    productos.add(mapearProducto(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar productos: " + e.getMessage());
        }
        return productos;
    }

    // RF-02 / RNF-07: No modifica stock_actual directamente
    public boolean actualizarProducto(Producto producto) {
        String sql = "UPDATE producto SET id_categoria = ?, nombre = ?, descripcion = ?, " +
                "precio_compra = ?, precio_venta = ?, stock_minimo = ?, imagen_url = ? " +
                "WHERE id_producto = ?";

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, producto.getCategoria().getId());
            pstmt.setString(2, producto.getNombre().trim());
            pstmt.setString(3, producto.getDescripcion());
            pstmt.setDouble(4, producto.getPrecioCompra());
            pstmt.setDouble(5, producto.getPrecioVenta());
            pstmt.setInt(6, Math.max(0, producto.getStockMinimo()));
            pstmt.setString(7, producto.getImagenUrl());
            pstmt.setInt(8, producto.getIdProducto());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar producto: " + e.getMessage());
        }
        return false;
    }

    // RF-12: Actualización de umbral sugerido
    public boolean actualizarStockMinimo(int idProducto, int nuevoStockMinimo) {
        if (nuevoStockMinimo < 0) {
            return false;
        }
        String sql = "UPDATE producto SET stock_minimo = ? WHERE id_producto = ?";
        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {
            pstmt.setInt(1, nuevoStockMinimo);
            pstmt.setInt(2, idProducto);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar stock minimo: " + e.getMessage());
        }
        return false;
    }

    public boolean cambiarEstadoProducto(int idProducto, boolean activo) {
        String sql = "UPDATE producto SET activo = ? WHERE id_producto = ?";
        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {
            pstmt.setBoolean(1, activo);
            pstmt.setInt(2, idProducto);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al cambiar estado del producto: " + e.getMessage());
        }
        return false;
    }

    public boolean eliminarProducto(int idProducto) {
        return cambiarEstadoProducto(idProducto, false);
    }

    private Producto mapearProducto(ResultSet rs) throws SQLException {
        String nombreCategoria = rs.getString("categoria_nombre");
        Categoria categoria = new Categoria(
                rs.getInt("id_categoria"),
                nombreCategoria == null ? "" : nombreCategoria,
                rs.getString("categoria_descripcion")
        );

        Producto producto = new Producto();
        producto.setIdProducto(rs.getInt("id_producto"));
        producto.setCategoria(categoria);
        producto.setNombre(rs.getString("nombre"));
        producto.setDescripcion(rs.getString("descripcion"));
        producto.setPrecioCompra(rs.getDouble("precio_compra"));
        producto.setPrecioVenta(rs.getDouble("precio_venta"));
        producto.setCantidad(rs.getInt("stock_actual"));
        producto.setStockMinimo(rs.getInt("stock_minimo"));
        producto.setImagenUrl(rs.getString("imagen_url"));
        producto.setActivo(rs.getBoolean("activo"));

        return producto;
    }
}