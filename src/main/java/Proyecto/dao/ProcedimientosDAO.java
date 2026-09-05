package Proyecto.dao;

import Proyecto.util.conexionBD;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ProcedimientosDAO
 *
 * Clase de Acceso a Datos (DAO) que centraliza las llamadas a los
 * procedimientos almacenados del sistema TechZone mediante la clase
 * {@link java.sql.CallableStatement} de la librería java.sql.
 *
 * Cada método corresponde a un procedimiento almacenado en MySQL:
 *
 * - registrarVenta() → llama a sp_registrar_venta
 * - registrarCompra() → llama a sp_registrar_compra
 * - ajustarInventario() → llama a sp_ajuste_inventario
 * - consultarStock() → llama a sp_consultar_stock
 * - reporteMovimientos() → llama a sp_reporte_movimientos
 * - historialCliente() → llama a sp_historial_cliente
 *
 * Patrón de uso de CallableStatement:
 * - Para SP sin OUT: conn.prepareCall("{call nombre_sp(?,?,?)}")
 * - Para SP con OUT: se registra el tipo del parámetro OUT con
 * cs.registerOutParameter(posición, Types.TIPO) antes de ejecutar.
 *
 * @author Juanes / Alan Caicedo
 * @version 1.0
 */
public class ProcedimientosDAO {

    // =========================================================================
    // MÉTODO 1: registrarVenta
    // =========================================================================

    /**
     * Llama al procedimiento almacenado {@code sp_registrar_venta}.
     *
     * Registra una factura de venta completa: crea el documento, descuenta
     * el stock de cada producto y registra cada movimiento de inventario,
     * todo dentro de una transacción atómica manejada en MySQL.
     *
     * Sintaxis del CALL:
     * {call sp_registrar_venta(?, ?, ?, ?, ?, ?)}
     * Parámetros: IN idCliente, IN idEmpleado, IN idMetodoPago,
     * IN productosJson, OUT idDocumento, OUT mensaje
     *
     * @param idCliente     ID de la persona tipo CLIENTE
     * @param idEmpleado    ID del empleado que atiende la venta
     * @param idMetodoPago  ID del método de pago
     * @param productosJson JSON de productos, ej:
     *                      [{"id":1,"qty":2,"precio":2800000}]
     * @return Map con las claves:
     *         "idDocumento" (int) → ID generado, o −1 si falló
     *         "mensaje" (String) → resultado de la operación
     */
    public Map<String, Object> registrarVenta(int idCliente,
            int idEmpleado,
            int idMetodoPago,
            String productosJson) {

        // Mapa que devolveremos con los parámetros OUT del procedimiento
        Map<String, Object> resultado = new HashMap<>();

        // Sintaxis estándar para llamar un SP con CallableStatement
        // Los ? representan cada parámetro (IN y OUT) en orden
        String sql = "{call sp_registrar_venta(?, ?, ?, ?, ?, ?)}";

        try (Connection conn = conexionBD.obtenerConexion();
                CallableStatement cs = conn.prepareCall(sql)) {

            // --- Parámetros de ENTRADA (IN) ---
            cs.setInt(1, idCliente); // p_id_cliente
            cs.setInt(2, idEmpleado); // p_id_empleado
            cs.setInt(3, idMetodoPago); // p_id_metodo_pago
            cs.setString(4, productosJson);// p_productos_json (arreglo JSON)

            // --- Parámetros de SALIDA (OUT) ---
            // Se deben registrar ANTES de ejecutar el procedimiento
            cs.registerOutParameter(5, Types.INTEGER); // p_id_documento (OUT)
            cs.registerOutParameter(6, Types.VARCHAR); // p_mensaje (OUT)

            // Ejecutar el procedimiento almacenado
            cs.execute();

            // Recuperar los valores de los parámetros OUT
            resultado.put("idDocumento", cs.getInt(5));
            resultado.put("mensaje", cs.getString(6));

        } catch (Exception e) {
            resultado.put("idDocumento", -1);
            resultado.put("mensaje", "Error en DAO al registrar venta: " + e.getMessage());
            e.printStackTrace();
        }

        return resultado;
    }

    // =========================================================================
    // MÉTODO 2: registrarCompra
    // =========================================================================

    /**
     * Llama al procedimiento almacenado {@code sp_registrar_compra}.
     *
     * Registra una factura de compra a proveedor: crea el documento con el
     * número de factura externo, suma stock a cada producto y genera los
     * movimientos de inventario de entrada.
     *
     * Sintaxis del CALL:
     * {call sp_registrar_compra(?, ?, ?, ?, ?, ?)}
     * Parámetros: IN idProveedor, IN idEmpleado, IN nroFacturaExt,
     * IN productosJson, OUT idDocumento, OUT mensaje
     *
     * @param idProveedor   ID de la persona tipo PROVEEDOR
     * @param idEmpleado    ID del empleado que registra la compra
     * @param nroFacturaExt Número de factura física del proveedor
     * @param productosJson JSON de productos comprados, ej:
     *                      [{"id":1,"qty":10,"precio":2200000}]
     * @return Map con "idDocumento" (int) y "mensaje" (String)
     */
    public Map<String, Object> registrarCompra(int idProveedor,
            int idEmpleado,
            String nroFacturaExt,
            String productosJson) {

        Map<String, Object> resultado = new HashMap<>();
        String sql = "{call sp_registrar_compra(?, ?, ?, ?, ?, ?)}";

        try (Connection conn = conexionBD.obtenerConexion();
                CallableStatement cs = conn.prepareCall(sql)) {

            // --- Parámetros IN ---
            cs.setInt(1, idProveedor); // p_id_proveedor
            cs.setInt(2, idEmpleado); // p_id_empleado
            cs.setString(3, nroFacturaExt); // p_nro_factura_ext
            cs.setString(4, productosJson); // p_productos_json

            // --- Parámetros OUT ---
            cs.registerOutParameter(5, Types.INTEGER); // p_id_documento
            cs.registerOutParameter(6, Types.VARCHAR); // p_mensaje

            cs.execute();

            resultado.put("idDocumento", cs.getInt(5));
            resultado.put("mensaje", cs.getString(6));

        } catch (Exception e) {
            resultado.put("idDocumento", -1);
            resultado.put("mensaje", "Error en DAO al registrar compra: " + e.getMessage());
            e.printStackTrace();
        }

        return resultado;
    }

    // =========================================================================
    // MÉTODO 3: ajustarInventario
    // =========================================================================

    /**
     * Llama al procedimiento almacenado {@code sp_ajuste_inventario}.
     *
     * Registra un ajuste manual de inventario (entrada o salida). El tipo
     * de documento determina el efecto: +1 para entradas, −1 para salidas.
     *
     * Tipos de documento válidos para ajuste:
     * 5 → Ajuste de Inventario (Entrada)
     * 6 → Ajuste de Inventario (Salida)
     * 7 → Baja por mercancía en mal estado (Salida)
     *
     * Sintaxis del CALL:
     * {call sp_ajuste_inventario(?, ?, ?, ?, ?, ?, ?)}
     * Parámetros: IN idTipoDoc, IN idEmpleado, IN idProducto,
     * IN cantidad, IN observacion, OUT idDocumento, OUT mensaje
     *
     * @param idTipoDoc   ID del tipo de documento (5, 6 o 7)
     * @param idEmpleado  ID del empleado que realiza el ajuste
     * @param idProducto  ID del producto a ajustar
     * @param cantidad    Unidades del ajuste (siempre número positivo)
     * @param observacion Motivo del ajuste (texto libre)
     * @return Map con "idDocumento" (int) y "mensaje" (String)
     */
    public Map<String, Object> ajustarInventario(int idTipoDoc,
            int idEmpleado,
            int idProducto,
            int cantidad,
            String observacion) {

        Map<String, Object> resultado = new HashMap<>();
        String sql = "{call sp_ajuste_inventario(?, ?, ?, ?, ?, ?, ?)}";

        try (Connection conn = conexionBD.obtenerConexion();
                CallableStatement cs = conn.prepareCall(sql)) {

            // --- Parámetros IN ---
            cs.setInt(1, idTipoDoc); // p_id_tipo_doc
            cs.setInt(2, idEmpleado); // p_id_empleado
            cs.setInt(3, idProducto); // p_id_producto
            cs.setInt(4, cantidad); // p_cantidad
            cs.setString(5, observacion); // p_observacion

            // --- Parámetros OUT ---
            cs.registerOutParameter(6, Types.INTEGER); // p_id_documento
            cs.registerOutParameter(7, Types.VARCHAR); // p_mensaje

            cs.execute();

            resultado.put("idDocumento", cs.getInt(6));
            resultado.put("mensaje", cs.getString(7));

        } catch (Exception e) {
            resultado.put("idDocumento", -1);
            resultado.put("mensaje", "Error en DAO al ajustar inventario: " + e.getMessage());
            e.printStackTrace();
        }

        return resultado;
    }

    // =========================================================================
    // MÉTODO 4: consultarStock
    // =========================================================================

    /**
     * Llama al procedimiento almacenado {@code sp_consultar_stock}.
     *
     * Obtiene el estado actual del inventario de todos los productos activos.
     * Si {@code soloAlertas = true}, retorna únicamente los productos cuyo
     * stock actual está igual o por debajo del stock mínimo configurado.
     *
     * Sintaxis del CALL:
     * {call sp_consultar_stock(?)}
     * Parámetro: IN soloAlertas (1 = solo alertas, 0 = todos)
     *
     * Columnas del ResultSet retornado:
     * id_producto, nombre, categoria, stock_actual, stock_minimo, diferencia
     *
     * @param soloAlertas true → solo productos con alerta de stock mínimo
     *                    false → todos los productos activos
     * @return Lista de mapas, cada mapa representa una fila con las columnas
     *         del resultado del SP (nombre_columna → valor)
     */
    public List<Map<String, Object>> consultarStock(boolean soloAlertas) {

        List<Map<String, Object>> lista = new ArrayList<>();
        // Se pasa 1 si se quieren solo alertas, 0 para todos
        String sql = "{call sp_consultar_stock(?)}";

        try (Connection conn = conexionBD.obtenerConexion();
                CallableStatement cs = conn.prepareCall(sql)) {

            // Parámetro IN: convertimos boolean a TINYINT (1 o 0)
            cs.setInt(1, soloAlertas ? 1 : 0); // p_solo_alertas

            // Este SP solo retorna un ResultSet (sin OUT), se usa executeQuery()
            ResultSet rs = cs.executeQuery();

            // Recorrer cada fila del resultado y construir un mapa por fila
            while (rs.next()) {
                Map<String, Object> fila = new HashMap<>();
                fila.put("id_producto", rs.getInt("id_producto"));
                fila.put("nombre", rs.getString("nombre"));
                fila.put("categoria", rs.getString("categoria"));
                fila.put("stock_actual", rs.getInt("stock_actual"));
                fila.put("stock_minimo", rs.getInt("stock_minimo"));
                fila.put("diferencia", rs.getInt("diferencia")); // negativo = crítico
                lista.add(fila);
            }

        } catch (Exception e) {
            System.err.println("Error en DAO al consultar stock: " + e.getMessage());
            e.printStackTrace();
        }

        return lista;
    }

    // =========================================================================
    // MÉTODO 5: reporteMovimientos
    // =========================================================================

    /**
     * Llama al procedimiento almacenado {@code sp_reporte_movimientos}.
     *
     * Genera un reporte detallado de los movimientos de inventario filtrando
     * por producto (opcional) y rango de fechas. Hace JOIN de 6 tablas en MySQL,
     * por lo que retorna información descriptiva completa de cada movimiento.
     *
     * Sintaxis del CALL:
     * {call sp_reporte_movimientos(?, ?, ?)}
     * Parámetros: IN idProducto (NULL = todos), IN fechaDesde, IN fechaHasta
     *
     * Columnas del ResultSet:
     * id_movimiento, fecha_movimiento, tipo_documento, efecto_en_inventario,
     * numero_doc_externo, producto, cantidad, precio_unitario,
     * subtotal_linea, persona_doc, empleado_reg
     *
     * @param idProducto ID del producto a filtrar, o null para todos
     * @param fechaDesde Fecha inicio del rango, formato "YYYY-MM-DD"
     * @param fechaHasta Fecha fin del rango, formato "YYYY-MM-DD"
     * @return Lista de mapas, cada mapa es un movimiento con sus columnas
     */
    public List<Map<String, Object>> reporteMovimientos(Integer idProducto,
            String fechaDesde,
            String fechaHasta) {

        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "{call sp_reporte_movimientos(?, ?, ?)}";

        try (Connection conn = conexionBD.obtenerConexion();
                CallableStatement cs = conn.prepareCall(sql)) {

            // Parámetro IN opcional: si es null, MySQL omite el filtro por producto
            if (idProducto == null) {
                cs.setNull(1, Types.INTEGER); // p_id_producto = NULL
            } else {
                cs.setInt(1, idProducto);
            }
            cs.setString(2, fechaDesde); // p_fecha_desde (DATE como String)
            cs.setString(3, fechaHasta); // p_fecha_hasta (DATE como String)

            ResultSet rs = cs.executeQuery();

            while (rs.next()) {
                Map<String, Object> fila = new HashMap<>();
                fila.put("id_movimiento", rs.getInt("id_movimiento"));
                fila.put("fecha_movimiento", rs.getTimestamp("fecha_movimiento"));
                fila.put("tipo_documento", rs.getString("tipo_documento"));
                fila.put("efecto_en_inventario", rs.getInt("efecto_en_inventario")); // +1 o -1
                fila.put("numero_doc_externo", rs.getString("numero_doc_externo"));
                fila.put("producto", rs.getString("producto"));
                fila.put("cantidad", rs.getInt("cantidad"));
                fila.put("precio_unitario", rs.getBigDecimal("precio_unitario"));
                fila.put("subtotal_linea", rs.getBigDecimal("subtotal_linea"));
                fila.put("persona_doc", rs.getString("persona_doc"));
                fila.put("empleado_reg", rs.getString("empleado_reg"));
                lista.add(fila);
            }

        } catch (Exception e) {
            System.err.println("Error en DAO al obtener reporte de movimientos: " + e.getMessage());
            e.printStackTrace();
        }

        return lista;
    }

    // =========================================================================
    // MÉTODO 6: historialCliente
    // =========================================================================

    /**
     * Llama al procedimiento almacenado {@code sp_historial_cliente}.
     *
     * Retorna el historial de documentos (ventas, devoluciones) asociados a
     * un cliente, ordenado del más reciente al más antiguo. Cada registro
     * incluye el resumen del documento y la cantidad de productos involucrados.
     *
     * Sintaxis del CALL:
     * {call sp_historial_cliente(?)}
     * Parámetro: IN idCliente
     *
     * Columnas del ResultSet:
     * id_documento, fecha_documento, tipo, total, metodo_pago, cant_productos
     *
     * @param idCliente ID de la persona tipo CLIENTE
     * @return Lista de mapas, cada mapa es un documento del historial
     */
    public List<Map<String, Object>> historialCliente(int idCliente) {

        List<Map<String, Object>> lista = new ArrayList<>();
        String sql = "{call sp_historial_cliente(?)}";

        try (Connection conn = conexionBD.obtenerConexion();
                CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, idCliente); // p_id_cliente

            ResultSet rs = cs.executeQuery();

            while (rs.next()) {
                Map<String, Object> fila = new HashMap<>();
                fila.put("id_documento", rs.getInt("id_documento"));
                fila.put("fecha_documento", rs.getTimestamp("fecha_documento"));
                fila.put("tipo", rs.getString("tipo"));
                fila.put("total", rs.getBigDecimal("total"));
                fila.put("metodo_pago", rs.getString("metodo_pago"));
                fila.put("cant_productos", rs.getInt("cant_productos"));
                lista.add(fila);
            }

        } catch (Exception e) {
            System.err.println("Error en DAO al obtener historial del cliente: " + e.getMessage());
            e.printStackTrace();
        }

        return lista;
    }

    // =========================================================================
    // MÉTODO MAIN — Ejemplos de uso de cada método del DAO
    // =========================================================================

    /**
     * Método main de demostración.
     * Muestra cómo llamar cada método del DAO con datos de ejemplo.
     * En el proyecto real, estos llamados los hacen las clases de Services.
     */
    public static void main(String[] args) {

        ProcedimientosDAO dao = new ProcedimientosDAO();

        // ---- Ejemplo 1: Registrar una venta ----
        // JSON con 2 productos: id=1 → 2 unidades a $2.800.000 c/u
        // id=3 → 1 unidad a $250.000
        String productosVenta = "[{\"id\":1,\"qty\":2,\"precio\":2800000},"
                + "{\"id\":3,\"qty\":1,\"precio\":250000}]";

        Map<String, Object> resVenta = dao.registrarVenta(
                2, // idCliente (Laura Gómez, id_persona = 2)
                1, // idEmpleado (Carlos Martínez, id_persona = 1)
                1, // idMetodoPago (Efectivo)
                productosVenta // JSON de productos
        );
        System.out.println("=== SP1: Registrar Venta ===");
        System.out.println("Documento generado: " + resVenta.get("idDocumento"));
        System.out.println("Mensaje:            " + resVenta.get("mensaje"));

        // ---- Ejemplo 2: Registrar una compra al proveedor ----
        // JSON: id=1 → 5 unidades a precio de compra $2.200.000
        String productosCompra = "[{\"id\":1,\"qty\":5,\"precio\":2200000}]";

        Map<String, Object> resCompra = dao.registrarCompra(
                3, // idProveedor (Samsung Colombia, id_persona = 3)
                1, // idEmpleado
                "FAC-SAM-2024-099", // nroFacturaExt (número de factura del proveedor)
                productosCompra);
        System.out.println("\n=== SP2: Registrar Compra ===");
        System.out.println("Documento generado: " + resCompra.get("idDocumento"));
        System.out.println("Mensaje:            " + resCompra.get("mensaje"));

        // ---- Ejemplo 3: Ajuste de inventario (salida por baja) ----
        Map<String, Object> resAjuste = dao.ajustarInventario(
                7, // idTipoDoc: Baja por mal estado (salida)
                1, // idEmpleado
                2, // idProducto (Samsung Galaxy A55)
                1, // cantidad: dar de baja 1 unidad
                "Unidad dañada en bodega, no apta para venta" // observacion
        );
        System.out.println("\n=== SP3: Ajuste Inventario ===");
        System.out.println("Documento generado: " + resAjuste.get("idDocumento"));
        System.out.println("Mensaje:            " + resAjuste.get("mensaje"));

        // ---- Ejemplo 4: Consultar stock con alertas ----
        System.out.println("\n=== SP4: Consultar Stock (solo alertas) ===");
        List<Map<String, Object>> stockAlertas = dao.consultarStock(true);
        if (stockAlertas.isEmpty()) {
            System.out.println("No hay productos en alerta de stock mínimo.");
        } else {
            for (Map<String, Object> p : stockAlertas) {
                System.out.printf("  [%d] %-30s | Stock: %d | Mínimo: %d | Diferencia: %d%n",
                        p.get("id_producto"),
                        p.get("nombre"),
                        p.get("stock_actual"),
                        p.get("stock_minimo"),
                        p.get("diferencia"));
            }
        }

        // ---- Ejemplo 5: Reporte de movimientos (todos los productos, enero 2025) ----
        System.out.println("\n=== SP5: Reporte de Movimientos (enero 2025) ===");
        List<Map<String, Object>> movimientos = dao.reporteMovimientos(
                null, // idProducto = null → todos los productos
                "2025-01-01", // fechaDesde
                "2025-01-31" // fechaHasta
        );
        for (Map<String, Object> m : movimientos) {
            System.out.printf("  Mov#%d | %s | %s | Cant: %d | Subtotal: %s%n",
                    m.get("id_movimiento"),
                    m.get("fecha_movimiento"),
                    m.get("tipo_documento"),
                    m.get("cantidad"),
                    m.get("subtotal_linea"));
        }

        // ---- Ejemplo 6: Historial del cliente (Laura Gómez) ----
        System.out.println("\n=== SP6: Historial Cliente (id=2) ===");
        List<Map<String, Object>> historial = dao.historialCliente(2);
        for (Map<String, Object> doc : historial) {
            System.out.printf("  Doc#%d | %s | %s | Total: $%s | Pago: %s | Productos: %d%n",
                    doc.get("id_documento"),
                    doc.get("fecha_documento"),
                    doc.get("tipo"),
                    doc.get("total"),
                    doc.get("metodo_pago"),
                    doc.get("cant_productos"));
        }
    }
}
