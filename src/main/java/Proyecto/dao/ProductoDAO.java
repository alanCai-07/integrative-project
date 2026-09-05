package Proyecto.dao;

import Proyecto.Model.Categoria;
import Proyecto.Model.Producto;
import Proyecto.util.conexionBD;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    // Crear producto
    public boolean crearProducto(Producto producto) {
        String sql = "INSERT INTO producto (id_categoria, nombre, descripcion, precio_compra, precio_venta, stock_actual, stock_minimo, activo) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, producto.getCategoria().getId());
            pstmt.setString(2, producto.getNombre());
            pstmt.setString(3, producto.getDescripcion());
            pstmt.setDouble(4, producto.getPrecioCompra());
            pstmt.setDouble(5, producto.getPrecioVenta());
            pstmt.setInt(6, producto.getCantidad());
            pstmt.setInt(7, 5); // Stock mínimo por defecto
            pstmt.setBoolean(8, true);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al crear producto: " + e.getMessage());
        }
        return false;
    }

    // Obtener producto por ID
    public Producto obtenerProductoporId(int idProducto) {
        String sql = "SELECT * FROM producto WHERE id_producto = ?";

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, idProducto);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapearProducto(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener producto: " + e.getMessage());
        }
        return null;
    }

    // Obtener todos los productos
    public List<Producto> obtenerTodosProductos() {
        String sql = "SELECT * FROM producto WHERE activo = true";
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

    // Obtener productos por categoría
    public List<Producto> obtenerProductosPorCategoria(int idCategoria) {
        String sql = "SELECT * FROM producto WHERE id_categoria = ? AND activo = true";
        List<Producto> productos = new ArrayList<>();

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, idCategoria);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                productos.add(mapearProducto(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener productos por categoría: " + e.getMessage());
        }
        return productos;
    }

    // Buscar productos por nombre
    public List<Producto> buscarProductosPorNombre(String nombre) {
        String sql = "SELECT * FROM producto WHERE nombre LIKE ? AND activo = true";
        List<Producto> productos = new ArrayList<>();

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setString(1, "%" + nombre + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                productos.add(mapearProducto(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar productos: " + e.getMessage());
        }
        return productos;
    }

    // Actualizar producto
    public boolean actualizarProducto(Producto producto) {
        String sql = "UPDATE producto SET id_categoria = ?, nombre = ?, descripcion = ?, " +
                     "precio_compra = ?, precio_venta = ?, stock_actual = ? WHERE id_producto = ?";

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, producto.getCategoria().getId());
            pstmt.setString(2, producto.getNombre());
            pstmt.setString(3, producto.getDescripcion());
            pstmt.setDouble(4, producto.getPrecioCompra());
            pstmt.setDouble(5, producto.getPrecioVenta());
            pstmt.setInt(6, producto.getCantidad());
            pstmt.setInt(7, producto.getIdProducto());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar producto: " + e.getMessage());
        }
        return false;
    }

    // Eliminar producto (soft delete)
    public boolean eliminarProducto(int idProducto) {
        String sql = "UPDATE producto SET activo = false WHERE id_producto = ?";

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, idProducto);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar producto: " + e.getMessage());
        }
        return false;
    }

    // Mapear ResultSet a Producto
    private Producto mapearProducto(ResultSet rs) throws SQLException {
        Categoria categoria = new Categoria(
            rs.getInt("id_categoria"),
            "",
            ""
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
        producto.setActivo(rs.getBoolean("activo"));

        return producto;
    }
}
