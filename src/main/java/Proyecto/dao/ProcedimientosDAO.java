package Proyecto.dao;

import Proyecto.util.conexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ejecuta operaciones transaccionales críticas de inventario y documentos.
 * El ajuste físico de stock se delega al trigger trg_movimiento_stock en PostgreSQL.
 *
 * @author Allan Matthew Caicedo Montaño
 * @author German Astudillo Astudillo
 */
public class ProcedimientosDAO {

    private static final Pattern PRODUCTO_JSON = Pattern.compile(
            "\"id\"\\s*:\\s*(\\d+)\\s*,\\s*\"qty\"\\s*:\\s*(\\d+)"
            + "\\s*,\\s*\"precio\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");

    public Map<String, Object> registrarVenta(int idCliente, int idEmpleado,
                                              int idMetodoPago, String productosJson) {
        Map<String, Object> resultado = new HashMap<>();

        try (Connection conexion = conexionBD.obtenerConexion()) {
            conexion.setAutoCommit(false);
            try {
                if (idCliente <= 0) {
                    throw new IllegalArgumentException("Debe seleccionar un cliente válido.");
                }

                List<LineaProducto> lineas = leerProductos(productosJson);
                lineas.sort(Comparator.comparingInt(LineaProducto::id));

                Map<Integer, Double> costosPorProducto = validarStockYCostos(conexion, lineas);
                double tasaIva = obtenerParametro(conexion, "IVA_PORCENTAJE", 0.19);

                int idDocumento = insertarDocumento(conexion, 1, idCliente, idEmpleado,
                        idMetodoPago, null, null);

                double subtotal = insertarMovimientos(
                        conexion, idDocumento, idEmpleado, lineas, costosPorProducto);
                double iva = Math.round(subtotal * tasaIva * 100.0) / 100.0;
                double total = subtotal + iva;

                actualizarTotales(conexion, idDocumento, subtotal, 0.0, iva, total);
                conexion.commit();

                resultado.put("idDocumento", idDocumento);
                resultado.put("subtotal", subtotal);
                resultado.put("iva", iva);
                resultado.put("total", total);
                resultado.put("mensaje", "Venta registrada exitosamente.");
            } catch (Exception e) {
                rollback(conexion);
                resultado.put("idDocumento", -1);
                resultado.put("mensaje", "No se pudo registrar la venta. La operación fue revertida y el stock no cambió. Detalle: " + e.getMessage());
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
                validarProveedorActivo(conexion, idProveedor);
                List<LineaProducto> lineas = leerProductos(productosJson);
                lineas.sort(Comparator.comparingInt(LineaProducto::id));

                int idDocumento = insertarDocumento(conexion, 2, idProveedor, idEmpleado,
                        0, nroFacturaExt, null);

                Map<Integer, Double> costosCompra = new HashMap<>();
                for (LineaProducto l : lineas) {
                    costosCompra.put(l.id(), l.precio());
                }

                double total = insertarMovimientos(
                        conexion, idDocumento, idEmpleado, lineas, costosCompra);
                actualizarTotales(conexion, idDocumento, total, 0.0, 0.0, total);
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
                if (observacion == null || observacion.trim().isEmpty()) {
                    throw new IllegalArgumentException("La observación es obligatoria para registrar un ajuste o baja (RF-05).");
                }
                if (cantidad <= 0) {
                    throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
                }

                int efecto = obtenerEfecto(conexion, idTipoDoc);
                ProductoInfo info = obtenerProductoBloqueado(conexion, idProducto);
                if (efecto < 0 && info.stock < cantidad) {
                    throw new IllegalArgumentException("Stock insuficiente para registrar la salida. Disponible: " + info.stock);
                }

                int idDocumento = insertarDocumento(conexion, idTipoDoc, 0, idEmpleado,
                        0, null, observacion.trim());

                String sqlMovimiento = "INSERT INTO movimiento_inventario "
                        + "(id_documento, id_producto, id_empleado, cantidad, costo_unitario, precio_unitario) "
                        + "VALUES (?, ?, ?, ?, ?, 0)";
                try (PreparedStatement ps = conexion.prepareStatement(sqlMovimiento)) {
                    ps.setInt(1, idDocumento);
                    ps.setInt(2, idProducto);
                    setNullableInt(ps, 3, idEmpleado);
                    ps.setInt(4, cantidad);
                    ps.setDouble(5, info.precioCompra);
                    ps.executeUpdate();
                }

                actualizarTotales(conexion, idDocumento, 0.0, 0.0, 0.0, 0.0);
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
        String filtro = soloAlertas ? " AND p.stock_actual <= p.stock_minimo" : "";
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
                + "COALESCE(CONCAT(pe.nombres, ' ', pe.apellidos), 'Ajuste interno') AS persona_doc, "
                + "COALESCE(CONCAT(emp.nombres, ' ', emp.apellidos), 'Sin empleado') AS empleado_reg "
                + "FROM movimiento_inventario m "
                + "JOIN documento d ON m.id_documento = d.id_documento "
                + "JOIN tipo_documento td ON d.id_tipo_documento = td.id_tipo_documento "
                + "JOIN producto p ON m.id_producto = p.id_producto "
                + "LEFT JOIN persona pe ON d.id_persona = pe.id_persona "
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
        String sql = "SELECT d.id_documento, d.numero_comprobante, d.fecha_documento, "
                + "td.descripcion AS tipo, d.subtotal, d.iva, d.total, d.estado, "
                + "COALESCE(mp.nombre, 'N/A') AS metodo_pago, "
                + "COUNT(m.id_movimiento) AS cant_productos "
                + "FROM documento d JOIN tipo_documento td ON d.id_tipo_documento = td.id_tipo_documento "
                + "LEFT JOIN metodo_pago mp ON d.id_metodo_pago = mp.id_metodo_pago "
                + "LEFT JOIN movimiento_inventario m ON d.id_documento = m.id_documento "
                + "WHERE d.id_persona = ? "
                + "GROUP BY d.id_documento, d.numero_comprobante, d.fecha_documento, td.descripcion, d.subtotal, d.iva, d.total, d.estado, mp.nombre "
                + "ORDER BY d.fecha_documento DESC";
        List<Map<String, Object>> lista = new ArrayList<>();

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> fila = new HashMap<>();
                    fila.put("id_documento", rs.getInt("id_documento"));
                    fila.put("numero_comprobante", rs.getInt("numero_comprobante"));
                    fila.put("fecha_documento", rs.getTimestamp("fecha_documento"));
                    fila.put("tipo", rs.getString("tipo"));
                    fila.put("subtotal", rs.getBigDecimal("subtotal"));
                    fila.put("iva", rs.getBigDecimal("iva"));
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
                                  int metodoPago, String numeroExterno, String observaciones) throws SQLException {
        int siguienteConsecutivo = obtenerSiguienteConsecutivo(conexion, tipo);

        String sql = "INSERT INTO documento "
                + "(id_tipo_documento, numero_comprobante, id_persona, id_empleado, id_metodo_pago, "
                + "numero_doc_externo, observaciones, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDIENTE')";
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, tipo);
            ps.setInt(2, siguienteConsecutivo);
            setNullableInt(ps, 3, persona);
            setNullableInt(ps, 4, empleado);
            setNullableInt(ps, 5, metodoPago);
            ps.setString(6, numeroExterno);
            ps.setString(7, observaciones);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("PostgreSQL no devolvió el ID del documento.");
    }

    private int obtenerSiguienteConsecutivo(Connection conexion, int idTipoDocumento) throws SQLException {
        String sql = "SELECT COALESCE(MAX(numero_comprobante), 0) + 1 FROM documento WHERE id_tipo_documento = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idTipoDocumento);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 1;
            }
        }
    }

    private double insertarMovimientos(Connection conexion, int idDocumento, int idEmpleado,
                                       List<LineaProducto> lineas, Map<Integer, Double> costos) throws SQLException {
        String sqlMovimiento = "INSERT INTO movimiento_inventario "
                + "(id_documento, id_producto, id_empleado, cantidad, costo_unitario, precio_unitario) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        double subtotal = 0;
        try (PreparedStatement movimiento = conexion.prepareStatement(sqlMovimiento)) {
            for (LineaProducto linea : lineas) {
                movimiento.setInt(1, idDocumento);
                movimiento.setInt(2, linea.id);
                setNullableInt(movimiento, 3, idEmpleado);
                movimiento.setInt(4, linea.cantidad);
                movimiento.setDouble(5, costos.getOrDefault(linea.id, 0.0));
                movimiento.setDouble(6, linea.precio);
                movimiento.executeUpdate();

                subtotal += linea.cantidad * linea.precio;
            }
        }
        return subtotal;
    }

    private void actualizarTotales(Connection conexion, int idDocumento,
                                   double subtotal, double descuento, double iva, double total)
            throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(
                "UPDATE documento SET subtotal = ?, descuento = ?, iva = ?, total = ?, estado = 'COMPLETADA' "
                + "WHERE id_documento = ?")) {
            ps.setDouble(1, subtotal);
            ps.setDouble(2, descuento);
            ps.setDouble(3, iva);
            ps.setDouble(4, total);
            ps.setInt(5, idDocumento);
            ps.executeUpdate();
        }
    }

    private Map<Integer, Double> validarStockYCostos(Connection conexion, List<LineaProducto> lineas) throws SQLException {
        Map<Integer, Double> costos = new HashMap<>();
        for (LineaProducto linea : lineas) {
            ProductoInfo info = obtenerProductoBloqueado(conexion, linea.id);
            if (!info.activo) {
                throw new IllegalArgumentException("El producto id=" + linea.id + " está inactivo.");
            }
            if (info.stock < linea.cantidad) {
                throw new IllegalArgumentException(
                        "Stock insuficiente para el producto id=" + linea.id + ". Disponible: " + info.stock);
            }
            costos.put(linea.id, info.precioCompra);
        }
        return costos;
    }

    private void validarProveedorActivo(Connection conexion, int idProveedor) throws SQLException {
        String sql = "SELECT 1 FROM persona p JOIN proveedor pr ON p.id_persona = pr.id_persona "
                + "WHERE p.id_persona = ? AND p.tipo = 'PROVEEDOR' AND p.activo = TRUE";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, idProveedor);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("El ID seleccionado no corresponde a un proveedor activo.");
                }
            }
        }
    }

    private ProductoInfo obtenerProductoBloqueado(Connection conexion, int idProducto) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement(
                "SELECT stock_actual, precio_compra, activo FROM producto WHERE id_producto = ? FOR UPDATE")) {
            ps.setInt(1, idProducto);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Producto inexistente: " + idProducto);
                }
                return new ProductoInfo(rs.getInt("stock_actual"), rs.getDouble("precio_compra"), rs.getBoolean("activo"));
            }
        }
    }

    private double obtenerParametro(Connection conexion, String clave, double defecto) throws SQLException {
        try (PreparedStatement ps = conexion.prepareStatement("SELECT valor FROM parametro WHERE clave = ?")) {
            ps.setString(1, clave);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : defecto;
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

    private record LineaProducto(int id, int cantidad, double precio) {}
    private record ProductoInfo(int stock, double precioCompra, boolean activo) {}
}