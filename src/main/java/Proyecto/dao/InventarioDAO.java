package Proyecto.dao;

import Proyecto.util.conexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Acceso a datos para consultas de inventario, alertas, los 6 reportes de RF-06
 * y el cálculo dinámico de reposición sugerida (RF-12).
 *
 * @author Allan Matthew Caicedo Montaño
 * @author German Astudillo Astudillo
 */
public class InventarioDAO {

    public boolean registrarMovimiento(int idDocumento, int idProducto,
            int idEmpleado, int cantidad, double subtotal) {
        double precioUnitario = (cantidad > 0) ? (subtotal / cantidad) : 0.0;
        return registrarMovimientoConPrecio(idDocumento, idProducto, idEmpleado, cantidad, precioUnitario);
    }

    public boolean registrarMovimientoConPrecio(int idDocumento, int idProducto,
            int idEmpleado, int cantidad, double precioUnitario) {
        String sql = "INSERT INTO movimiento_inventario " +
                "(id_documento, id_producto, id_empleado, cantidad, costo_unitario, precio_unitario, fecha_movimiento) " +
                "VALUES (?, ?, ?, ?, COALESCE((SELECT precio_compra FROM producto WHERE id_producto = ?), 0), ?, NOW())";

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
            pstmt.setInt(5, idProducto);
            pstmt.setDouble(6, precioUnitario);

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
            try (ResultSet rs = pstmt.executeQuery()) {
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
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("idMovimiento", rs.getInt("id_movimiento"));
                    m.put("tipo", rs.getString("tipo"));
                    m.put("cantidad", rs.getInt("cantidad"));
                    m.put("subtotal", rs.getDouble("subtotal_linea"));
                    m.put("fecha", rs.getTimestamp("fecha_movimiento"));
                    movimientos.add(m);
                }
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
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("stock_actual");
                }
            }
        } catch (SQLException e) {
            System.err.println("InventarioDAO.obtenerStockActual: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Método puente de compatibilidad: el stock en PostgreSQL ya se actualiza
     * automáticamente mediante el trigger trg_movimiento_stock al insertar en
     * movimiento_inventario (RNF-07 / SCE2.14). No ejecuta UPDATE duplicado.
     */
  
    public boolean actualizarStock(int idProducto, int cantidad) {
        return true;
    }

    /**
     * Reporte 3 (RF-06): Productos con stock igual o inferior al mínimo.
     */
    public List<Map<String, Object>> obtenerProductosConStockBajo() {
        String sql = "SELECT p.id_producto, p.nombre, c.nombre AS categoria, " +
                "p.stock_actual, p.stock_minimo, " +
                "(p.stock_minimo - p.stock_actual) AS deficit " +
                "FROM producto p " +
                "JOIN categoria c ON p.id_categoria = c.id_categoria " +
                "WHERE p.stock_actual <= p.stock_minimo AND p.activo = TRUE " +
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
     * Reporte 1 (RF-06): Ventas por rango de fechas.
     */
    public List<Map<String, Object>> reporteVentasPorPeriodo(String fechaDesde, String fechaHasta) {
        String sql = "SELECT d.id_documento, d.numero_comprobante, d.fecha_documento, " +
                "CONCAT(p.nombres, ' ', p.apellidos) AS cliente, " +
                "COALESCE(mp.nombre, 'N/A') AS metodo_pago, d.subtotal, d.iva, d.total, d.estado " +
                "FROM documento d " +
                "JOIN persona p ON p.id_persona = d.id_persona " +
                "LEFT JOIN metodo_pago mp ON mp.id_metodo_pago = d.id_metodo_pago " +
                "WHERE d.id_tipo_documento = 1 " +
                "  AND d.fecha_documento >= ?::date " +
                "  AND d.fecha_documento < (?::date + INTERVAL '1 day') " +
                "ORDER BY d.fecha_documento DESC";

        List<Map<String, Object>> lista = new ArrayList<>();
        try (Connection conn = conexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fechaDesde);
            ps.setString(2, fechaHasta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> fila = new HashMap<>();
                    fila.put("idDocumento", rs.getInt("id_documento"));
                    fila.put("numeroComprobante", rs.getInt("numero_comprobante"));
                    fila.put("fecha", rs.getTimestamp("fecha_documento"));
                    fila.put("cliente", rs.getString("cliente"));
                    fila.put("metodoPago", rs.getString("metodo_pago"));
                    fila.put("subtotal", rs.getDouble("subtotal"));
                    fila.put("iva", rs.getDouble("iva"));
                    fila.put("total", rs.getDouble("total"));
                    fila.put("estado", rs.getString("estado"));
                    lista.add(fila);
                }
            }
        } catch (SQLException e) {
            System.err.println("InventarioDAO.reporteVentasPorPeriodo: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Reporte 2 (RF-06): Inventario actual valorizado.
     */
    public List<Map<String, Object>> reporteInventarioActual() {
        String sql = "SELECT p.id_producto, p.nombre, c.nombre AS categoria, " +
                "p.stock_actual, p.stock_minimo, p.precio_compra, p.precio_venta, " +
                "(p.stock_actual * p.precio_compra) AS valor_costo, " +
                "(p.stock_actual * p.precio_venta) AS valor_venta " +
                "FROM producto p JOIN categoria c ON p.id_categoria = c.id_categoria " +
                "WHERE p.activo = TRUE ORDER BY c.nombre, p.nombre";

        List<Map<String, Object>> lista = new ArrayList<>();
        try (Connection conn = conexionBD.obtenerConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> fila = new HashMap<>();
                fila.put("idProducto", rs.getInt("id_producto"));
                fila.put("nombre", rs.getString("nombre"));
                fila.put("categoria", rs.getString("categoria"));
                fila.put("stockActual", rs.getInt("stock_actual"));
                fila.put("stockMinimo", rs.getInt("stock_minimo"));
                fila.put("precioCompra", rs.getDouble("precio_compra"));
                fila.put("precioVenta", rs.getDouble("precio_venta"));
                fila.put("valorCosto", rs.getDouble("valor_costo"));
                fila.put("valorVenta", rs.getDouble("valor_venta"));
                lista.add(fila);
            }
        } catch (SQLException e) {
            System.err.println("InventarioDAO.reporteInventarioActual: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Reporte 4 (RF-06): Clientes frecuentes por periodo.
     */
    public List<Map<String, Object>> reporteClientesFrecuentes(String fechaDesde, String fechaHasta) {
        String sql = "SELECT p.id_persona, CONCAT(p.nombres, ' ', p.apellidos) AS cliente, " +
                "p.documento, COUNT(d.id_documento) AS total_compras, COALESCE(SUM(d.total), 0) AS monto_total " +
                "FROM persona p " +
                "JOIN documento d ON d.id_persona = p.id_persona " +
                "WHERE p.tipo = 'CLIENTE' AND d.id_tipo_documento = 1 AND d.estado = 'COMPLETADA' " +
                "  AND d.fecha_documento >= ?::date " +
                "  AND d.fecha_documento < (?::date + INTERVAL '1 day') " +
                "GROUP BY p.id_persona, p.nombres, p.apellidos, p.documento " +
                "ORDER BY total_compras DESC, monto_total DESC LIMIT 20";

        List<Map<String, Object>> lista = new ArrayList<>();
        try (Connection conn = conexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fechaDesde);
            ps.setString(2, fechaHasta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> fila = new HashMap<>();
                    fila.put("idCliente", rs.getInt("id_persona"));
                    fila.put("cliente", rs.getString("cliente"));
                    fila.put("documento", rs.getString("documento"));
                    fila.put("totalCompras", rs.getInt("total_compras"));
                    fila.put("montoTotal", rs.getDouble("monto_total"));
                    lista.add(fila);
                }
            }
        } catch (SQLException e) {
            System.err.println("InventarioDAO.reporteClientesFrecuentes: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Reporte 5 (RF-06 / SCE2.15): Rotación real filtrando únicamente ventas completadas.
     */
    public List<Map<String, Object>> reporteRotacionProductos(String fechaDesde, String fechaHasta) {
        String sql = "SELECT p.id_producto, p.nombre, SUM(m.cantidad) AS unidades_vendidas, " +
                "SUM(m.subtotal_linea) AS ingresos " +
                "FROM movimiento_inventario m " +
                "JOIN documento d ON d.id_documento = m.id_documento " +
                "JOIN producto p ON p.id_producto = m.id_producto " +
                "WHERE d.id_tipo_documento = 1 AND d.estado = 'COMPLETADA' " +
                "  AND d.fecha_documento >= ?::date " +
                "  AND d.fecha_documento < (?::date + INTERVAL '1 day') " +
                "GROUP BY p.id_producto, p.nombre " +
                "ORDER BY unidades_vendidas DESC";

        List<Map<String, Object>> lista = new ArrayList<>();
        try (Connection conn = conexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fechaDesde);
            ps.setString(2, fechaHasta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> fila = new HashMap<>();
                    fila.put("idProducto", rs.getInt("id_producto"));
                    fila.put("nombre", rs.getString("nombre"));
                    fila.put("unidadesVendidas", rs.getInt("unidades_vendidas"));
                    fila.put("ingresos", rs.getDouble("ingresos"));
                    lista.add(fila);
                }
            }
        } catch (SQLException e) {
            System.err.println("InventarioDAO.reporteRotacionProductos: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Reporte 6 (RF-06 / SCE2.15): Rentabilidad histórica por categoría usando m.costo_unitario.
     */
    public List<Map<String, Object>> reporteRentabilidadPorCategoria(String fechaDesde, String fechaHasta) {
        String sql = "SELECT c.nombre AS categoria, " +
                "SUM(m.subtotal_linea) AS ingresos, " +
                "SUM(m.cantidad * m.costo_unitario) AS costos, " +
                "SUM(m.cantidad * (m.precio_unitario - m.costo_unitario)) AS utilidad " +
                "FROM movimiento_inventario m " +
                "JOIN documento d ON d.id_documento = m.id_documento " +
                "JOIN producto p ON p.id_producto = m.id_producto " +
                "JOIN categoria c ON c.id_categoria = p.id_categoria " +
                "WHERE d.id_tipo_documento = 1 AND d.estado = 'COMPLETADA' " +
                "  AND d.fecha_documento >= ?::date " +
                "  AND d.fecha_documento < (?::date + INTERVAL '1 day') " +
                "GROUP BY c.id_categoria, c.nombre " +
                "ORDER BY utilidad DESC";

        List<Map<String, Object>> lista = new ArrayList<>();
        try (Connection conn = conexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fechaDesde);
            ps.setString(2, fechaHasta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> fila = new HashMap<>();
                    fila.put("categoria", rs.getString("categoria"));
                    fila.put("ingresos", rs.getDouble("ingresos"));
                    fila.put("costos", rs.getDouble("costos"));
                    fila.put("utilidad", rs.getDouble("utilidad"));
                    lista.add(fila);
                }
            }
        } catch (SQLException e) {
            System.err.println("InventarioDAO.reporteRentabilidadPorCategoria: " + e.getMessage());
        }
        return lista;
    }

    /**
     * RF-12 / SCE2.15: Reposición sugerida leyendo L y S dinámicamente desde la tabla parametro.
     */
    public List<Map<String, Object>> obtenerReposicionSugerida() {
        String sql = "WITH params AS (" +
                "  SELECT COALESCE((SELECT valor FROM parametro WHERE clave = 'TIEMPO_REPOSICION_L'), 10) AS l_dias, " +
                "         COALESCE((SELECT valor FROM parametro WHERE clave = 'STOCK_SEGURIDAD_S'), 3) AS s_dias " +
                "), ventas_28d AS (" +
                "  SELECT m.id_producto, SUM(m.cantidad) AS unidades_28d " +
                "  FROM movimiento_inventario m " +
                "  JOIN documento d ON d.id_documento = m.id_documento " +
                "  WHERE d.id_tipo_documento = 1 AND d.estado = 'COMPLETADA' " +
                "    AND m.fecha_movimiento >= CURRENT_DATE - INTERVAL '28 days' " +
                "  GROUP BY m.id_producto" +
                ") " +
                "SELECT p.id_producto, p.nombre, c.nombre AS categoria, " +
                "  p.stock_actual, p.stock_minimo, " +
                "  ROUND(COALESCE(v.unidades_28d, 0) / 28.0, 2) AS demanda_diaria, " +
                "  CEIL((COALESCE(v.unidades_28d, 0) / 28.0) * (pa.l_dias + pa.s_dias))::int AS minimo_sugerido, " +
                "  GREATEST(p.stock_minimo - p.stock_actual, 0) AS deficit " +
                "FROM producto p " +
                "JOIN categoria c ON c.id_categoria = p.id_categoria " +
                "CROSS JOIN params pa " +
                "LEFT JOIN ventas_28d v ON v.id_producto = p.id_producto " +
                "WHERE p.activo = TRUE " +
                "ORDER BY (p.stock_actual <= p.stock_minimo) DESC, deficit DESC, p.nombre ASC";

        List<Map<String, Object>> lista = new ArrayList<>();
        try (Connection conn = conexionBD.obtenerConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> fila = new HashMap<>();
                fila.put("idProducto", rs.getInt("id_producto"));
                fila.put("nombre", rs.getString("nombre"));
                fila.put("categoria", rs.getString("categoria"));
                fila.put("stockActual", rs.getInt("stock_actual"));
                fila.put("stockMinimo", rs.getInt("stock_minimo"));
                fila.put("demandaDiaria", rs.getDouble("demanda_diaria"));
                fila.put("minimoSugerido", rs.getInt("minimo_sugerido"));
                fila.put("deficit", rs.getInt("deficit"));
                lista.add(fila);
            }
        } catch (SQLException e) {
            System.err.println("InventarioDAO.obtenerReposicionSugerida: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Permite al administrador actualizar los parámetros L o S de reposición sugerida (Tabla 9).
     */
    public boolean actualizarParametro(String clave, double nuevoValor) {
        String sql = "UPDATE parametro SET valor = ? WHERE clave = ?";
        try (Connection conn = conexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, nuevoValor);
            ps.setString(2, clave);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("InventarioDAO.actualizarParametro: " + e.getMessage());
        }
        return false;
    }

    public Map<String, Object> obtenerKpisDashboard() {
        String sql = "SELECT " +
                "(SELECT COUNT(*) FROM producto WHERE activo = TRUE) AS total_productos, " +
                "(SELECT COUNT(*) FROM producto WHERE stock_actual <= stock_minimo AND activo = TRUE) AS bajo_stock, " +
                "(SELECT COUNT(*) FROM documento WHERE id_tipo_documento = 1 AND fecha_documento::date = CURRENT_DATE) AS ventas_hoy, " +
                "(SELECT COALESCE(SUM(total),0) FROM documento WHERE id_tipo_documento = 1 " +
                "  AND DATE_TRUNC('month', fecha_documento) = DATE_TRUNC('month', CURRENT_TIMESTAMP) AND estado='COMPLETADA') AS ingresos_mes, " +
                "(SELECT COALESCE(SUM(total),0) FROM documento WHERE id_tipo_documento = 1 " +
                "  AND DATE_TRUNC('month', fecha_documento) = DATE_TRUNC('month', CURRENT_TIMESTAMP - INTERVAL '1 month') AND estado='COMPLETADA') AS ingresos_mes_anterior, " +
                "(SELECT COUNT(DISTINCT id_persona) FROM documento WHERE id_tipo_documento = 1 " +
                "  AND DATE_TRUNC('month', fecha_documento) = DATE_TRUNC('month', CURRENT_TIMESTAMP)) AS clientes_activos_mes, " +
                "(SELECT COUNT(*) FROM persona WHERE tipo='CLIENTE' AND activo=TRUE) AS total_clientes";

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

    public List<Map<String, Object>> obtenerVentasPorMes(int meses) {
        String sql = "SELECT TO_CHAR(fecha_documento, 'YYYY-MM') AS periodo, " +
                "TO_CHAR(fecha_documento, 'Mon YYYY') AS etiqueta, " +
                "COUNT(*) AS cantidad_ventas, " +
                "COALESCE(SUM(total), 0) AS total_ventas " +
                "FROM documento " +
                "WHERE id_tipo_documento = 1 " +
                "  AND estado = 'COMPLETADA' " +
                "  AND fecha_documento >= CURRENT_TIMESTAMP - (? * INTERVAL '1 month') " +
                "GROUP BY periodo, etiqueta " +
                "ORDER BY periodo ASC";

        List<Map<String, Object>> resultado = new ArrayList<>();
        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, meses);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> fila = new HashMap<>();
                    fila.put("periodo", rs.getString("periodo"));
                    fila.put("etiqueta", rs.getString("etiqueta"));
                    fila.put("cantidadVentas", rs.getInt("cantidad_ventas"));
                    fila.put("totalVentas", rs.getDouble("total_ventas"));
                    resultado.add(fila);
                }
            }
        } catch (SQLException e) {
            System.err.println("InventarioDAO.obtenerVentasPorMes: " + e.getMessage());
        }
        return resultado;
    }
}