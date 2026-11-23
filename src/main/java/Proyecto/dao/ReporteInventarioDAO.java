package Proyecto.dao;

import Proyecto.util.conexionBD;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReporteInventarioDAO {

    public Map<String, Double> obtenerParametrosReposicion() {
        Map<String, Double> params = new HashMap<>();
        params.put("TIEMPO_REPOSICION_DIAS", 10.0);
        params.put("STOCK_SEGURIDAD_DIAS", 3.0);

        String sql = "SELECT clave, valor FROM parametro WHERE clave IN ('TIEMPO_REPOSICION_DIAS', 'STOCK_SEGURIDAD_DIAS')";
        try (Connection conn = conexionBD.obtenerConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                params.put(rs.getString("clave"), rs.getDouble("valor"));
            }
        } catch (SQLException e) {
            System.err.println("Error al leer parametros de reposicion: " + e.getMessage());
        }
        return params;
    }

    public boolean actualizarParametro(String clave, double valor) {
        if (valor < 0) {
            return false;
        }
        String sql = "UPDATE parametro SET valor = ? WHERE clave = ?";
        try (Connection conn = conexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, valor);
            ps.setString(2, clave);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar parametro: " + e.getMessage());
        }
        return false;
    }

    // RF-12: Reposición sugerida SM = ceil((U28 / 28) * (L + S))
    public List<Map<String, Object>> obtenerReposicionSugerida() {
        Map<String, Double> params = obtenerParametrosReposicion();
        double l = params.get("TIEMPO_REPOSICION_DIAS");
        double s = params.get("STOCK_SEGURIDAD_DIAS");

        String sql = "WITH ventas_28d AS (" +
                "  SELECT m.id_producto, SUM(m.cantidad) AS unidades_28d " +
                "  FROM movimiento_inventario m " +
                "  JOIN documento d ON d.id_documento = m.id_documento " +
                "  JOIN tipo_documento td ON td.id_tipo_documento = d.id_tipo_documento " +
                "  WHERE td.descripcion = 'Venta' AND d.estado = 'COMPLETADA' " +
                "    AND m.fecha_movimiento >= CURRENT_DATE - INTERVAL '28 days' " +
                "  GROUP BY m.id_producto" +
                ") " +
                "SELECT p.id_producto, p.nombre, c.nombre AS categoria, p.stock_actual, p.stock_minimo, " +
                "  ROUND(COALESCE(v.unidades_28d, 0) / 28.0, 2) AS demanda_diaria, " +
                "  CEIL((COALESCE(v.unidades_28d, 0) / 28.0) * (? + ?))::int AS minimo_sugerido, " +
                "  (p.stock_minimo - p.stock_actual) AS deficit " +
                "FROM producto p " +
                "JOIN categoria c ON c.id_categoria = p.id_categoria " +
                "LEFT JOIN ventas_28d v ON v.id_producto = p.id_producto " +
                "WHERE p.activo = TRUE " +
                "ORDER BY (p.stock_actual <= p.stock_minimo) DESC, minimo_sugerido DESC, p.nombre";

        List<Map<String, Object>> lista = new ArrayList<>();
        try (Connection conn = conexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, l);
            ps.setDouble(2, s);
            try (ResultSet rs = ps.executeQuery()) {
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
            }
        } catch (SQLException e) {
            System.err.println("Error al calcular reposicion sugerida: " + e.getMessage());
        }
        return lista;
    }

    // RF-06 Reporte 1: Ventas por periodo
    public List<Map<String, Object>> reporteVentasPorPeriodo(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT DATE(d.fecha_documento) AS fecha, COUNT(d.id_documento) AS num_ventas, " +
                "SUM(d.subtotal - d.descuento) AS base_gravable, SUM(d.iva) AS total_iva, SUM(d.total) AS total_vendido " +
                "FROM documento d JOIN tipo_documento td ON td.id_tipo_documento = d.id_tipo_documento " +
                "WHERE td.descripcion = 'Venta' AND d.estado = 'COMPLETADA' " +
                "  AND d.fecha_documento >= ? AND d.fecha_documento < (?::date + INTERVAL '1 day') " +
                "GROUP BY DATE(d.fecha_documento) ORDER BY fecha DESC";
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Connection conn = conexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> fila = new HashMap<>();
                    fila.put("fecha", rs.getDate("fecha").toLocalDate());
                    fila.put("numVentas", rs.getInt("num_ventas"));
                    fila.put("baseGravable", rs.getDouble("base_gravable"));
                    fila.put("totalIva", rs.getDouble("total_iva"));
                    fila.put("totalVendido", rs.getDouble("total_vendido"));
                    lista.add(fila);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en reporte de ventas por periodo: " + e.getMessage());
        }
        return lista;
    }

    // RF-06 Reporte 2: Inventario actual valorizado
    public List<Map<String, Object>> reporteInventarioValorizado() {
        String sql = "SELECT p.id_producto, p.nombre, c.nombre AS categoria, p.stock_actual, p.stock_minimo, " +
                "p.precio_compra, p.precio_venta, (p.stock_actual * p.precio_compra) AS valor_costo, " +
                "(p.stock_actual * p.precio_venta) AS valor_venta " +
                "FROM producto p JOIN categoria c ON c.id_categoria = p.id_categoria " +
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
            System.err.println("Error en reporte de inventario valorizado: " + e.getMessage());
        }
        return lista;
    }

    // RF-06 Reporte 4: Clientes frecuentes
    public List<Map<String, Object>> reporteClientesFrecuentes(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT p.id_persona, p.documento, CONCAT(p.nombres, ' ', p.apellidos) AS cliente, " +
                "COUNT(d.id_documento) AS compras, COALESCE(SUM(d.total), 0) AS total_comprado " +
                "FROM persona p JOIN documento d ON d.id_persona = p.id_persona " +
                "JOIN tipo_documento td ON td.id_tipo_documento = d.id_tipo_documento " +
                "WHERE p.tipo = 'CLIENTE' AND td.descripcion = 'Venta' AND d.estado = 'COMPLETADA' " +
                "  AND d.fecha_documento >= ? AND d.fecha_documento < (?::date + INTERVAL '1 day') " +
                "GROUP BY p.id_persona, p.documento, p.nombres, p.apellidos " +
                "ORDER BY compras DESC, total_comprado DESC";
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Connection conn = conexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> fila = new HashMap<>();
                    fila.put("idPersona", rs.getInt("id_persona"));
                    fila.put("documento", rs.getString("documento"));
                    fila.put("cliente", rs.getString("cliente"));
                    fila.put("compras", rs.getInt("compras"));
                    fila.put("totalComprado", rs.getDouble("total_comprado"));
                    lista.add(fila);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en reporte de clientes frecuentes: " + e.getMessage());
        }
        return lista;
    }

    // RF-06 Reporte 5: Rotación de productos
    public List<Map<String, Object>> reporteRotacionProductos(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT p.id_producto, p.nombre, c.nombre AS categoria, " +
                "SUM(m.cantidad) AS unidades_vendidas, SUM(m.subtotal_linea) AS ingresos_brutos " +
                "FROM movimiento_inventario m " +
                "JOIN documento d ON d.id_documento = m.id_documento " +
                "JOIN tipo_documento td ON td.id_tipo_documento = d.id_tipo_documento " +
                "JOIN producto p ON p.id_producto = m.id_producto " +
                "JOIN categoria c ON c.id_categoria = p.id_categoria " +
                "WHERE td.descripcion = 'Venta' AND d.estado = 'COMPLETADA' " +
                "  AND d.fecha_documento >= ? AND d.fecha_documento < (?::date + INTERVAL '1 day') " +
                "GROUP BY p.id_producto, p.nombre, c.nombre ORDER BY unidades_vendidas DESC";
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Connection conn = conexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> fila = new HashMap<>();
                    fila.put("idProducto", rs.getInt("id_producto"));
                    fila.put("nombre", rs.getString("nombre"));
                    fila.put("categoria", rs.getString("categoria"));
                    fila.put("unidadesVendidas", rs.getInt("unidades_vendidas"));
                    fila.put("ingresosBrutos", rs.getDouble("ingresos_brutos"));
                    lista.add(fila);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en reporte de rotacion: " + e.getMessage());
        }
        return lista;
    }

    // RF-06 Reporte 6: Rentabilidad por categoría usando costo histórico
    public List<Map<String, Object>> reporteRentabilidadPorCategoria(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT c.id_categoria, c.nombre AS categoria, SUM(m.cantidad) AS unidades_vendidas, " +
                "SUM(m.subtotal_linea) AS ingresos, SUM(m.cantidad * m.costo_unitario) AS costo_historico, " +
                "SUM(m.cantidad * (m.precio_unitario - m.costo_unitario)) AS utilidad_bruta " +
                "FROM movimiento_inventario m " +
                "JOIN documento d ON d.id_documento = m.id_documento " +
                "JOIN tipo_documento td ON td.id_tipo_documento = d.id_tipo_documento " +
                "JOIN producto p ON p.id_producto = m.id_producto " +
                "JOIN categoria c ON c.id_categoria = p.id_categoria " +
                "WHERE td.descripcion = 'Venta' AND d.estado = 'COMPLETADA' " +
                "  AND d.fecha_documento >= ? AND d.fecha_documento < (?::date + INTERVAL '1 day') " +
                "GROUP BY c.id_categoria, c.nombre ORDER BY utilidad_bruta DESC";
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Connection conn = conexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(desde));
            ps.setDate(2, Date.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> fila = new HashMap<>();
                    fila.put("idCategoria", rs.getInt("id_categoria"));
                    fila.put("categoria", rs.getString("categoria"));
                    fila.put("unidadesVendidas", rs.getInt("unidades_vendidas"));
                    fila.put("ingresos", rs.getDouble("ingresos"));
                    fila.put("costoHistorico", rs.getDouble("costo_historico"));
                    fila.put("utilidadBruta", rs.getDouble("utilidad_bruta"));
                    lista.add(fila);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error en reporte de rentabilidad: " + e.getMessage());
        }
        return lista;
    }
}
