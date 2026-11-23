package Proyecto.dao;

import Proyecto.View.Documento.CotizacionView.ItemCotizacion;
import Proyecto.util.conexionBD;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CotizacionDAO {

    private static final double TASA_IVA = 0.19;

    public int crearCotizacion(int idCliente, int idEmpleado, LocalDate vigenciaHasta,
                               List<ItemCotizacion> items) {
        if (idCliente <= 0 || idEmpleado <= 0 || items == null || items.isEmpty()) {
            return -1;
        }

        double subtotal = 0.0;
        for (ItemCotizacion item : items) {
            subtotal += item.getPrecioUnit() * item.getCantidad();
        }
        subtotal = redondear(subtotal);
        double iva = redondear(subtotal * TASA_IVA);
        double total = redondear(subtotal + iva);

        String sqlCot = "INSERT INTO cotizacion (id_cliente, id_empleado, vigencia_hasta, subtotal, iva, total, estado) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'VIGENTE') RETURNING id_cotizacion";
        String sqlDet = "INSERT INTO detalle_cotizacion (id_cotizacion, id_producto, cantidad, precio_unitario) " +
                "VALUES (?, ?, ?, ?)";

        Connection conexion = null;
        try {
            conexion = conexionBD.obtenerConexion();
            conexion.setAutoCommit(false);

            int idCotizacion;
            try (PreparedStatement ps = conexion.prepareStatement(sqlCot)) {
                ps.setInt(1, idCliente);
                ps.setInt(2, idEmpleado);
                ps.setDate(3, Date.valueOf(vigenciaHasta != null ? vigenciaHasta : LocalDate.now().plusDays(15)));
                ps.setDouble(4, subtotal);
                ps.setDouble(5, iva);
                ps.setDouble(6, total);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conexion.rollback();
                        return -1;
                    }
                    idCotizacion = rs.getInt(1);
                }
            }

            try (PreparedStatement psDet = conexion.prepareStatement(sqlDet)) {
                for (ItemCotizacion item : items) {
                    psDet.setInt(1, idCotizacion);
                    psDet.setInt(2, item.getIdProducto());
                    psDet.setInt(3, item.getCantidad());
                    psDet.setDouble(4, item.getPrecioUnit());
                    psDet.addBatch();
                }
                psDet.executeBatch();
            }

            conexion.commit();
            return idCotizacion;
        } catch (SQLException e) {
            System.err.println("Error al crear cotizacion: " + e.getMessage());
            try {
                if (conexion != null) {
                    conexion.rollback();
                }
            } catch (SQLException ignored) {
            }
        } finally {
            if (conexion != null) {
                try {
                    conexion.setAutoCommit(true);
                    conexion.close();
                } catch (SQLException ignored) {
                }
            }
        }
        return -1;
    }

    public List<Map<String, Object>> listarCotizaciones() {
        String sql = "SELECT c.id_cotizacion, c.fecha, c.vigencia_hasta, c.subtotal, c.iva, c.total, c.estado, " +
                "c.id_documento_venta, p.id_persona AS id_cliente, p.documento, " +
                "CONCAT(p.nombres, ' ', p.apellidos) AS cliente, " +
                "CONCAT(emp.nombres, ' ', emp.apellidos) AS vendedor " +
                "FROM cotizacion c " +
                "JOIN persona p ON p.id_persona = c.id_cliente " +
                "JOIN persona emp ON emp.id_persona = c.id_empleado " +
                "ORDER BY c.fecha DESC";
        List<Map<String, Object>> lista = new ArrayList<>();
        try (Connection conexion = conexionBD.obtenerConexion();
             Statement stmt = conexion.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("idCotizacion", rs.getInt("id_cotizacion"));
                row.put("fecha", rs.getTimestamp("fecha"));
                row.put("vigenciaHasta", rs.getDate("vigencia_hasta").toLocalDate());
                row.put("subtotal", rs.getDouble("subtotal"));
                row.put("iva", rs.getDouble("iva"));
                row.put("total", rs.getDouble("total"));
                row.put("estado", rs.getString("estado"));
                row.put("idDocumentoVenta", rs.getObject("id_documento_venta"));
                row.put("idCliente", rs.getInt("id_cliente"));
                row.put("documento", rs.getString("documento"));
                row.put("cliente", rs.getString("cliente"));
                row.put("vendedor", rs.getString("vendedor"));
                lista.add(row);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar cotizaciones: " + e.getMessage());
        }
        return lista;
    }

    public List<Map<String, Object>> obtenerDetalleCotizacion(int idCotizacion) {
        String sql = "SELECT dc.id_detalle_cotizacion, dc.id_producto, pr.nombre, " +
                "dc.cantidad, dc.precio_unitario, dc.subtotal_linea " +
                "FROM detalle_cotizacion dc " +
                "JOIN producto pr ON pr.id_producto = dc.id_producto " +
                "WHERE dc.id_cotizacion = ? ORDER BY dc.id_detalle_cotizacion";
        List<Map<String, Object>> detalles = new ArrayList<>();
        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idCotizacion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> d = new HashMap<>();
                    d.put("idDetalle", rs.getInt("id_detalle_cotizacion"));
                    d.put("idProducto", rs.getInt("id_producto"));
                    d.put("nombre", rs.getString("nombre"));
                    d.put("cantidad", rs.getInt("cantidad"));
                    d.put("precioUnitario", rs.getDouble("precio_unitario"));
                    d.put("subtotalLinea", rs.getDouble("subtotal_linea"));
                    detalles.add(d);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener detalle de cotizacion: " + e.getMessage());
        }
        return detalles;
    }

    public boolean marcarCotizacionConvertida(int idCotizacion, int idDocumentoVenta) {
        String sql = "UPDATE cotizacion SET estado = 'CONVERTIDA', id_documento_venta = ? " +
                "WHERE id_cotizacion = ? AND estado = 'VIGENTE'";
        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idDocumentoVenta);
            ps.setInt(2, idCotizacion);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al convertir cotizacion: " + e.getMessage());
        }
        return false;
    }

    private double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}