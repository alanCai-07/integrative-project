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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Operaciones transaccionales de inventario para PostgreSQL/Neon.
 *
 * Mantiene el contrato que consumen los servicios, pero ejecuta las
 * operaciones directamente con JDBC en lugar de usar procedimientos MySQL
 * con parámetros OUT.
 */
public class ProcedimientosDAO {

    private static final Pattern PRODUCTO_JSON = Pattern.compile(
            "\\\"id\\\"\\s*:\\s*(\\d+)\\s*,\\s*\\\"qty\\\"\\s*:\\s*(\\d+)"
                    + "\\s*,\\s*\\\"precio\\\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");

    public Map<String, Object> registrarVenta(int idCliente, int idEmpleado,
            int idMetodoPago, String productosJson) {
        Map<String, Object> resultado = new HashMap<>();

        try (Connection conexion = conexionBD.obtenerConexion()) {
            conexion.setAutoCommit(false);
            try {
                List<LineaProducto> lineas = leerProductos(productosJson);
                validarStock(conexion, lineas);

                int idDocumento = insertarDocumento(conexion, 1, idCliente, idEmpleado,
                        idMetodoPago, null);
                double total = insertarMovimientosYActualizarStock(
                        conexion, idDocumento, idEmpleado, lineas, false);
                actualizarTotales(conexion, idDocumento, total);
                conexion.commit();

                resultado.put("idDocumento", idDocumento);
                resultado.put("mensaje", "Venta registrada exitosamente.");
            } catch (Exception e) {
                rollback(conexion);
                resultado.put("idDocumento", -1);
                resultado.put("mensaje", "Error al registrar venta: " + e.getMessage());
            } finally {
                conexion.setAutoCommit(true);
            }
        } catch (SQLException | IllegalStateException e) {
            resultado.put("idDocumento", -1);
            resultado.put("mensaje", "Error de conexión al registrar venta: " + e.getMessage());
        }
        return resultado;
    }

    public Map<String, Object> registrarCompra(int idProveedor, int idEmpleado,
            String nroFacturaExt, String productosJson) {
        Map<String, Object> resultado = new HashMap<>();

        try (Connection conexion = conexionBD.obtenerConexion()) {
            conexion.setAutoCommit(false);
            try {
                List<LineaProducto> lineas = leerProductos(productosJson);
                int idDocumento = insertarDocumento(conexion, 2, idProveedor, idEmpleado,
                        0, nroFacturaExt);
                double total = insertarMovimientosYActualizarStock(
                        conexion, idDocumento, idEmpleado, lineas, true);
                actualizarTotales(conexion, idDocumento, total);
                conexion.commit();

                resultado.put("idDocumento", idDocumento);
                resultado.put("mensaje", "Compra registrada exitosamente.");
            } catch (Exception e) {
                rollback(conexion);
                resultado.put("idDocumento", -1);
                resultado.put("mensaje", "Error al registrar compra: " + e.getMessage());
            } finally {
                conexion.setAutoCommit(true);
            }
        } catch (SQLException | IllegalStateException e) {
            resultado.put("idDocumento", -1);
            resultado.put("mensaje", "Error de conexión al registrar compra: " + e.getMessage());
        }
        return resultado;
    }

    public Map<String, Object> ajustarInventario(int idTipoDoc, int idEmpleado,
            int idProducto, int cantidad, String observacion) {
        Map<String, Object> resultado = new HashMap<>();

        try (Connection conexion = conexionBD.obtenerConexion()) {
            conexion.setAutoCommit(false);
            try {
                int efecto = obtenerEfecto(conexion, idTipoDoc);
                int stock = obtenerStockBloqueado(conexion, idProducto);
                if (efecto < 0 && stock < cantidad) {
                    throw new IllegalArgumentException("Stock insuficiente para registrar la salida.");
                }

                int idDocumento = insertarDocumento(conexion, idTipoDoc, idEmpleado, idEmpleado,
                        0, null, observacion);
                String sqlMovimiento = "INSERT INTO movimiento_inventario "
                        + "(id_documento, id_producto, id_empleado, cantidad, precio_unitario) "
                        + "VALUES (?, ?, ?, ?, 0)";
                try (PreparedStatement ps = conexion.prepareStatement(sqlMovimiento);
                        PreparedStatement psStock = conexion.prepareStatement(
                                "UPDATE producto SET stock_actual = stock_actual + ? WHERE id_producto = ?")) {
                    ps.setInt(1, idDocumento);
                    ps.setInt(2, idProducto);
                    setNullableInt(ps, 3, idEmpleado);
                    ps.setInt(4, cantidad);
                    ps.executeUpdate();

                    psStock.setInt(1, efecto * cantidad);
                    psStock.setInt(2, idProducto);
                    psStock.executeUpdate();
                }
                conexion.commit();
                resultado.put("idDocumento", idDocumento);
                resultado.put("mensaje", "Ajuste registrado correctamente.");
            } catch (Exception e) {
                rollback(conexion);
                resultado.put("idDocumento", -1);
                resultado.put("mensaje", "Error en ajuste de inventario: " + e.getMessage());
            } finally {
                conexion.setAutoCommit(true);
            }
        } catch (SQLException | IllegalStateException e) {
            resultado.put("idDocumento", -1);
            resultado.put("mensaje", "Error de conexión en ajuste de inventario: " + e.getMessage());
        }
        return resultado;
    }

    public List<Map<String, Object>> consultarStock(boolean soloAlertas) {
        String filtro = soloAlertas
                ? " AND p.stock_actual <= p.stock_minimo"
                : "";
        String sql = "SELECT p.id_producto, p.nombre, c.nombre AS categoria, "
                + "p.stock_actual, p.stock_minimo, "
                + "(p.stock_actual - p.stock_minimo) AS diferencia "
                + "FROM producto p JOIN categoria c ON p.id_categoria = c.id_categoria "
                + "WHERE p.activo = TRUE" + filtro
                + " ORDER BY " + (soloAlertas ? "diferencia ASC" : "p.nombre");
        List<Map<String, Object>> lista = new ArrayList<>();

        try (Connection conexion = conexionBD.obtenerConexion();
                Statement statement = conexion.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> fila = new HashMap<>();
                fila.put("id_producto", rs.getInt("id_producto"));
                fila.put("nombre", rs.getString("nombre"));
                fila.put("categoria", rs.getString("categoria"));
                fila.put("stock_actual", rs.getInt("stock_actual"));
                fila.put("stock_minimo", rs.getInt("stock_minimo"));
                fila.put("diferencia", rs.getInt("diferencia"));
                lista.add(fila);
            }
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error al consultar stock: " + e.getMessage());
        }
        return lista;
    }

    public List<Map<String, Object>> reporteMovimientos(Integer idProducto,
            String fechaDesde, String fechaHasta) {
        String sql = "SELECT m.id_movimiento, m.fecha_movimiento, td.descripcion AS tipo_documento, "
                + "td.efecto_en_inventario, d.numero_doc_externo, p.nombre AS producto, "
                + "m.cantidad, m.precio_unitario, m.subtotal_linea, "
                + "CONCAT(pe.nombres, ' ', pe.apellidos) AS persona_doc, "
                + "COALESCE(CONCAT(emp.nombres, ' ', emp.apellidos), 'Sin empleado') AS empleado_reg "
                + "FROM movimiento_inventario m "
                + "JOIN documento d ON m.id_documento = d.id_documento "
                + "JOIN tipo_documento td ON d.id_tipo_documento = td.id_tipo_documento "
                + "JOIN producto p ON m.id_producto = p.id_producto "
                + "JOIN persona pe ON d.id_persona = pe.id_persona "
                + "LEFT JOIN empleado e ON m.id_empleado = e.id_persona "
                + "LEFT JOIN persona emp ON e.id_persona = emp.id_persona "
                + "WHERE (?::integer IS NULL OR m.id_producto = ?) "
                + "AND m.fecha_movimiento::date BETWEEN ?::date AND ?::date "
                + "ORDER BY m.fecha_movimiento DESC";
        List<Map<String, Object>> lista = new ArrayList<>();

        try (Connection conexion = conexionBD.obtenerConexion();
                PreparedStatement ps = conexion.prepareStatement(sql)) {
            if (idProducto == null) {
                ps.setNull(1, Types.INTEGER);
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(1, idProducto);
                ps.setInt(2, idProducto);
            }
            ps.setString(3, fechaDesde);
            ps.setString(4, fechaHasta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> fila = new HashMap<>();
                    fila.put("id_movimiento", rs.getInt("id_movimiento"));
                    fila.put("fecha_movimiento", rs.getTimestamp("fecha_movimiento"));
                    fila.put("tipo_documento", rs.getString("tipo_documento"));
                    fila.put("efecto_en_inventario", rs.getInt("efecto_en_inventario"));
                    fila.put("numero_doc_externo", rs.getString("numero_doc_externo"));
                    fila.put("producto", rs.getString("producto"));
                    fila.put("cantidad", rs.getInt("cantidad"));
                    fila.put("precio_unitario", rs.getBigDecimal("precio_unitario"));
                    fila.put("subtotal_linea", rs.getBigDecimal("subtotal_linea"));
                    fila.put("persona_doc", rs.getString("persona_doc"));
                    fila.put("empleado_reg", rs.getString("empleado_reg"));
                    lista.add(fila);
                }
            }
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error al obtener reporte de movimientos: " + e.getMessage());
        }
        return lista;
    }

    public List<Map<String, Object>> historialCliente(int idCliente) {
        String sql = "SELECT d.id_documento, d.fecha_documento, td.descripcion AS tipo, "
                + "d.total, d.estado, COALESCE(mp.nombre, 'N/A') AS metodo_pago, "
                + "COUNT(m.id_movimiento) AS cant_productos "
                + "FROM documento d JOIN tipo_documento td ON d.id_tipo_documento = td.id_tipo_documento "
                + "LEFT JOIN metodo_pago mp ON d.id_metodo_pago = mp.id_metodo_pago "
                + "LEFT JOIN movimiento_inventario m ON d.id_documento = m.id_documento "
                + "WHERE d.id_persona = ? "
                + "GROUP BY d.id_documento, d.fecha_documento, td.descripcion, d.total, d.estado, mp.nombre "
                + "ORDER BY d.fecha_documento DESC";
        List<Map<String, Object>> lista = new ArrayList<>();

        try (Connection conexion = conexionBD.obtenerConexion();
                PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> fila = new HashMap<>();
                    fila.put("id_documento", rs.getInt("id_documento"));
                    fila.put("fecha_documento", rs.getTimestamp("fecha_documento"));
                    fila.put("tipo", rs.getString("tipo"));
                    fila.put("total", rs.getBigDecimal("total"));
                    fila.put("estado", rs.getString("estado"));
                    fila.put("metodo_pago", rs.getString("metodo_pago"));
                    fila.put("cant_productos", rs.getInt("cant_productos"));
                    lista.add(fila);
                }
            }
        } catch (SQLException | IllegalStateException e) {
            System.err.println("Error al obtener historial del cliente: " + e.getMessage());
        }
        return lista;
    }

    private int insertarDocumento(Connection conexion, int tipo, int persona, int empleado,
            int metodoPago, String numeroExterno) throws SQLException {
        return insertarDocumento(conexion, tipo, persona, empleado, metodoPago,
                numeroExterno, null);
    }

    private int insertarDocumento(Connection conexion, int tipo, int persona, int empleado,
            int metodoPago, String numeroExterno, String observaciones) throws SQLException {
        String sql = "INSERT INTO documento "
                + "(id_tipo_documento, id_persona, id_empleado, id_metodo_pago, "
                + "numero_doc_externo, observaciones, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'PENDIENTE')";
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, tipo);
            ps.setInt(2, persona);
            setNullableInt(ps, 3, empleado);
            setNullableInt(ps, 4, metodoPago);
            ps.setString(5, numeroExterno);
            ps.setString(6, observaciones);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("PostgreSQL no devolvió el ID del documento.");
    }

    private double insertarMovimientosYActualizarStock(Connection conexion, int idDocumento,
            int idEmpleado, List<LineaProducto> lineas, boolean entrada) throws SQLException {
        String sqlMovimiento = "INSERT INTO movimiento_inventario "
                + "(id_documento, id_producto, id_empleado, cantidad, precio_unitario) "
                + "VALUES (?, ?, ?, ?, ?)";
        String sqlStock = "UPDATE producto SET stock_actual = stock_actual + ? WHERE id_producto = ?";
        double total = 0;
        try (PreparedStatement movimiento = conexion.prepareStatement(sqlMovimiento);
                PreparedStatement stock = conexion.prepareStatement(sqlStock)) {
            for (LineaProducto linea : lineas) {
                movimiento.setInt(1, idDocumento);
                movimiento.setInt(2, linea.id);
                setNullableInt(movimiento, 3, idEmpleado);
                movimiento.setInt(4, linea.cantidad);
                movimiento.setDouble(5, linea.precio);
                movimiento.executeUpdate();

                stock.setInt(1, entrada ? linea.cantidad : -linea.cantidad);
                stock.setInt(2, linea.id);
                if (stock.executeUpdate() == 0) {
                    throw new SQLException("Producto inexistente: " + linea.id);
                }
                total += linea.cantidad * linea.precio;
            }
        }
        return total;
    }

    private void actualizarTotales(Connection conexion, int idDocumento, double total)
            throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(
                "UPDATE documento SET subtotal = ?, total = ?, estado = 'COMPLETADA' "
                        + "WHERE id_documento = ?")) {
            ps.setDouble(1, total);
            ps.setDouble(2, total);
            ps.setInt(3, idDocumento);
            ps.executeUpdate();
        }
    }

    private void validarStock(Connection conexion, List<LineaProducto> lineas) throws SQLException {
        for (LineaProducto linea : lineas) {
            if (obtenerStockBloqueado(conexion, linea.id) < linea.cantidad) {
                throw new IllegalArgumentException(
                        "Stock insuficiente para el producto id=" + linea.id);
            }
        }
    }

    private int obtenerStockBloqueado(Connection conexion, int idProducto) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(
                "SELECT stock_actual FROM producto WHERE id_producto = ? FOR UPDATE")) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Producto inexistente: " + idProducto);
                }
                return rs.getInt(1);
            }
        }
    }

    private int obtenerEfecto(Connection conexion, int idTipoDocumento) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(
                "SELECT efecto_en_inventario FROM tipo_documento WHERE id_tipo_documento = ?")) {
            ps.setInt(1, idTipoDocumento);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Tipo de documento inexistente: " + idTipoDocumento);
                }
                return rs.getInt(1);
            }
        }
    }

    private List<LineaProducto> leerProductos(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("La lista de productos está vacía.");
        }
        List<LineaProducto> lineas = new ArrayList<>();
        Matcher matcher = PRODUCTO_JSON.matcher(json);
        while (matcher.find()) {
            int cantidad = Integer.parseInt(matcher.group(2));
            if (cantidad <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
            }
            lineas.add(new LineaProducto(
                    Integer.parseInt(matcher.group(1)),
                    cantidad,
                    Double.parseDouble(matcher.group(3))));
        }
        if (lineas.isEmpty()) {
            throw new IllegalArgumentException("Formato de productos inválido.");
        }
        return lineas;
    }

    private void setNullableInt(PreparedStatement ps, int index, int value) throws SQLException {
        if (value > 0) {
            ps.setInt(index, value);
        } else {
            ps.setNull(index, Types.INTEGER);
        }
    }

    private void rollback(Connection conexion) {
        try {
            conexion.rollback();
        } catch (SQLException e) {
            System.err.println("No se pudo revertir la transacción: " + e.getMessage());
        }
    }

    private record LineaProducto(int id, int cantidad, double precio) {
    }
}
