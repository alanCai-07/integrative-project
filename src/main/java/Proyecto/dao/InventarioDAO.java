package Proyecto.dao;

import Proyecto.util.conexionBD;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventarioDAO {

    /**
     * Registra un movimiento de inventario.
     *
     * @param idDocumento ID del documento padre (venta, compra, ajuste)
     * @param idProducto  ID del producto
     * @param idEmpleado  ID del empleado responsable (0 o negativo = NULL)
     * @param cantidad    Unidades del movimiento
     * @param subtotal    Subtotal total de la linea (se usa para calcular
     *                    precio_unitario)
     */
    public boolean registrarMovimiento(int idDocumento, int idProducto,
            int idEmpleado, int cantidad, double subtotal) {
        // subtotal_linea es columna GENERATED — se calcula automaticamente en MySQL
        // precio_unitario = subtotal / cantidad para que MySQL calcule subtotal_linea
        double precioUnitario = (cantidad > 0) ? (subtotal / cantidad) : 0.0;

        // CORRECCION: no se incluye subtotal_linea en el INSERT
        String sql = "INSERT INTO movimiento_inventario " +
                "(id_documento, id_producto, id_empleado, cantidad, precio_unitario, fecha_movimiento) " +
                "VALUES (?, ?, ?, ?, ?, NOW())";

        try (Connection conexion = conexionBD.obtenerConexion();
                PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, idDocumento);
            pstmt.setInt(2, idProducto);

            // CORRECCION: idEmpleado <= 0 se inserta como NULL
            if (idEmpleado > 0) {
                pstmt.setInt(3, idEmpleado);
            } else {
                pstmt.setNull(3, Types.INTEGER);
            }

            pstmt.setInt(4, cantidad);
            pstmt.setDouble(5, precioUnitario);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("InventarioDAO.registrarMovimiento: " + e.getMessage());
        }
        return false;
    }

    public boolean registrarMovimientoConPrecio(int idDocumento, int idProducto,
            int idEmpleado, int cantidad,
            double precioUnitario) {
        String sql = "INSERT INTO movimiento_inventario " +
                "(id_documento, id_producto, id_empleado, cantidad, precio_unitario, fecha_movimiento) " +
                "VALUES (?, ?, ?, ?, ?, NOW())";

        try (Connection conexion = conexionBD.obtenerConexion();
                PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, idDocumento);
            pstmt.setInt(2, idProducto);
            if (idEmpleado > 0) {
                pstmt.setInt(3, idEmpleado);
            } else {
                pstmt.setNull(3, Types.INTEGER);
            }
            pstmt.setInt(4, cantidad);
            pstmt.setDouble(5, precioUnitario);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("InventarioDAO.registrarMovimientoConPrecio: " + e.getMessage());
        }
        return false;
    }

    public List<Map<String, Object>> obtenerMovimientosPorDocumento(int idDocumento) {
        String sql = "SELECT m.id_movimiento, m.id_producto, p.nombre AS nombre_producto, " +
                "m.cantidad, m.precio_unitario, m.subtotal_linea, m.fecha_movimiento " +
                "FROM movimiento_inventario m " +
                "JOIN producto p ON m.id_producto = p.id_producto " +
                "WHERE m.id_documento = ? " +
                "ORDER BY m.id_movimiento";

        List<Map<String, Object>> movimientos = new ArrayList<>();

        try (Connection conexion = conexionBD.obtenerConexion();
                PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, idDocumento);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("idMovimiento", rs.getInt("id_movimiento"));
                m.put("idProducto", rs.getInt("id_producto"));
                m.put("producto", rs.getString("nombre_producto"));
                m.put("cantidad", rs.getInt("cantidad"));
                m.put("precioUnitario", rs.getDouble("precio_unitario"));
                m.put("subtotal", rs.getDouble("subtotal_linea"));
                m.put("fecha", rs.getTimestamp("fecha_movimiento"));
                movimientos.add(m);
            }
        } catch (SQLException e) {
            System.err.println("InventarioDAO.obtenerMovimientosPorDocumento: " + e.getMessage());
        }
        return movimientos;
    }

    public List<Map<String, Object>> obtenerMovimientosPorProducto(int idProducto) {
        String sql = "SELECT m.id_movimiento, m.cantidad, m.precio_unitario, " +
                "m.subtotal_linea, m.fecha_movimiento, td.descripcion AS tipo " +
                "FROM movimiento_inventario m " +
                "JOIN documento d ON m.id_documento = d.id_documento " +
                "JOIN tipo_documento td ON d.id_tipo_documento = td.id_tipo_documento " +
                "WHERE m.id_producto = ? " +
                "ORDER BY m.fecha_movimiento DESC LIMIT 50";

        List<Map<String, Object>> movimientos = new ArrayList<>();

        try (Connection conexion = conexionBD.obtenerConexion();
                PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, idProducto);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("idMovimiento", rs.getInt("id_movimiento"));
                m.put("tipo", rs.getString("tipo"));
                m.put("cantidad", rs.getInt("cantidad"));
                m.put("subtotal", rs.getDouble("subtotal_linea"));
                m.put("fecha", rs.getTimestamp("fecha_movimiento"));
                movimientos.add(m);
            }
        } catch (SQLException e) {
            System.err.println("InventarioDAO.obtenerMovimientosPorProducto: " + e.getMessage());
        }
        return movimientos;
    }

    public int obtenerStockActual(int idProducto) {
        String sql = "SELECT stock_actual FROM producto WHERE id_producto = ?";
        try (Connection conexion = conexionBD.obtenerConexion();
                PreparedStatement pstmt = conexion.prepareStatement(sql)) {
            pstmt.setInt(1, idProducto);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next())
                return rs.getInt("stock_actual");
        } catch (SQLException e) {
            System.err.println("InventarioDAO.obtenerStockActual: " + e.getMessage());
        }
        return 0;
    }

    public boolean actualizarStock(int idProducto, int cantidad) {
        String sql = "UPDATE producto SET stock_actual = stock_actual + ? WHERE id_producto = ?";
        try (Connection conexion = conexionBD.obtenerConexion();
                PreparedStatement pstmt = conexion.prepareStatement(sql)) {
            pstmt.setInt(1, cantidad);
            pstmt.setInt(2, idProducto);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("InventarioDAO.actualizarStock: " + e.getMessage());
        }
        return false;
    }

    /**
     * CORRECCION: incluye nombre de categoria para mostrar en las alertas del
     * dashboard.
     */
    public List<Map<String, Object>> obtenerProductosConStockBajo() {
        String sql = "SELECT p.id_producto, p.nombre, c.nombre AS categoria, " +
                "p.stock_actual, p.stock_minimo, " +
                "(p.stock_minimo - p.stock_actual) AS deficit " +
                "FROM producto p " +
                "JOIN categoria c ON p.id_categoria = c.id_categoria " +
                "WHERE p.stock_actual <= p.stock_minimo AND p.activo = 1 " +
                "ORDER BY deficit DESC";

        List<Map<String, Object>> productos = new ArrayList<>();

        try (Connection conexion = conexionBD.obtenerConexion();
                Statement stmt = conexion.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> p = new HashMap<>();
                p.put("idProducto", rs.getInt("id_producto"));
                p.put("nombre", rs.getString("nombre"));
                p.put("categoria", rs.getString("categoria"));
                p.put("stockActual", rs.getInt("stock_actual"));
                p.put("stockMinimo", rs.getInt("stock_minimo"));
                p.put("deficit", rs.getInt("deficit"));
                productos.add(p);
            }
        } catch (SQLException e) {
            System.err.println("InventarioDAO.obtenerProductosConStockBajo: " + e.getMessage());
        }
        return productos;
    }

    /**
     * KPIs del dashboard en una sola consulta para mejor rendimiento.
     * Evita multiples roundtrips a la BD desde el dashboard.
     */
    public Map<String, Object> obtenerKpisDashboard() {
        String sql = "SELECT " +
                "(SELECT COUNT(*) FROM producto WHERE activo = 1) AS total_productos, " +
                "(SELECT COUNT(*) FROM producto WHERE stock_actual <= stock_minimo AND activo = 1) AS bajo_stock, " +
                "(SELECT COUNT(*) FROM documento WHERE id_tipo_documento = 1 AND DATE(fecha_documento) = CURDATE()) AS ventas_hoy, "
                +
                "(SELECT IFNULL(SUM(total),0) FROM documento WHERE id_tipo_documento = 1 " +
                "  AND MONTH(fecha_documento)=MONTH(NOW()) AND YEAR(fecha_documento)=YEAR(NOW()) AND estado='COMPLETADA') AS ingresos_mes, "
                +
                "(SELECT IFNULL(SUM(total),0) FROM documento WHERE id_tipo_documento = 1 " +
                "  AND MONTH(fecha_documento)=MONTH(NOW()-INTERVAL 1 MONTH) AND YEAR(fecha_documento)=YEAR(NOW()-INTERVAL 1 MONTH) AND estado='COMPLETADA') AS ingresos_mes_anterior, "
                +
                "(SELECT COUNT(DISTINCT id_persona) FROM documento WHERE id_tipo_documento = 1 " +
                "  AND MONTH(fecha_documento)=MONTH(NOW()) AND YEAR(fecha_documento)=YEAR(NOW())) AS clientes_activos_mes, "
                +
                "(SELECT COUNT(*) FROM persona WHERE tipo='CLIENTE' AND activo=1) AS total_clientes";

        Map<String, Object> kpis = new HashMap<>();
        try (Connection conexion = conexionBD.obtenerConexion();
                Statement stmt = conexion.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                kpis.put("totalProductos", rs.getInt("total_productos"));
                kpis.put("bajoStock", rs.getInt("bajo_stock"));
                kpis.put("ventasHoy", rs.getInt("ventas_hoy"));
                kpis.put("ingresosMes", rs.getDouble("ingresos_mes"));
                kpis.put("ingresosMesAnterior", rs.getDouble("ingresos_mes_anterior"));
                kpis.put("clientesActivosMes", rs.getInt("clientes_activos_mes"));
                kpis.put("totalClientes", rs.getInt("total_clientes"));
            }
        } catch (SQLException e) {
            System.err.println("InventarioDAO.obtenerKpisDashboard: " + e.getMessage());
        }
        return kpis;
    }

    /**
     * Ventas de los ultimos N meses agrupadas por mes para el grafico de barras.
     */
    public List<Map<String, Object>> obtenerVentasPorMes(int meses) {
        String sql = "SELECT DATE_FORMAT(fecha_documento, '%Y-%m') AS periodo, " +
                "DATE_FORMAT(fecha_documento, '%b %Y') AS etiqueta, " +
                "COUNT(*) AS cantidad_ventas, " +
                "IFNULL(SUM(total), 0) AS total_ventas " +
                "FROM documento " +
                "WHERE id_tipo_documento = 1 " +
                "  AND estado = 'COMPLETADA' " +
                "  AND fecha_documento >= DATE_SUB(NOW(), INTERVAL ? MONTH) " +
                "GROUP BY periodo, etiqueta " +
                "ORDER BY periodo ASC";

        List<Map<String, Object>> resultado = new ArrayList<>();
        try (Connection conexion = conexionBD.obtenerConexion();
                PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, meses);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> fila = new HashMap<>();
                fila.put("periodo", rs.getString("periodo"));
                fila.put("etiqueta", rs.getString("etiqueta"));
                fila.put("cantidadVentas", rs.getInt("cantidad_ventas"));
                fila.put("totalVentas", rs.getDouble("total_ventas"));
                resultado.add(fila);
            }
        } catch (SQLException e) {
            System.err.println("InventarioDAO.obtenerVentasPorMes: " + e.getMessage());
        }
        return resultado;
    }
}