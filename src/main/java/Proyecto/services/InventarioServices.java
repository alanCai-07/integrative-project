package Proyecto.services;

import Proyecto.dao.InventarioDAO;
import Proyecto.dao.ProductoDAO;
import Proyecto.dao.ProcedimientosDAO;
import Proyecto.Model.MovimientoInventario;
import Proyecto.Model.Documento;
import Proyecto.Model.Producto;
import Proyecto.Model.Empleado;
import Proyecto.Model.TipoDocumento;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InventarioServices {

    private final InventarioDAO inventarioDAO;
    private final ProductoDAO productoDAO;
    private final ProcedimientosDAO procedimientosDAO;

    public InventarioServices() {
        this.inventarioDAO = new InventarioDAO();
        this.productoDAO = new ProductoDAO();
        this.procedimientosDAO = new ProcedimientosDAO();
    }

    public boolean registrarMovimiento(int idDocumento, int idProducto, int idEmpleado,
            int cantidad, double subtotal) {
        if (cantidad == 0) {
            System.out.println("Error: La cantidad no puede ser cero");
            return false;
        }
        if (productoDAO.obtenerProductoporId(idProducto) == null) {
            System.out.println("Error: Producto no encontrado");
            return false;
        }
        return inventarioDAO.registrarMovimiento(idDocumento, idProducto, idEmpleado, cantidad, subtotal);
    }

    public boolean registrarMovimiento(int idProducto, String tipo, int cantidad, String observaciones) {
        if (cantidad == 0) {
            System.out.println("Error: La cantidad no puede ser cero");
            return false;
        }
        if (productoDAO.obtenerProductoporId(idProducto) == null) {
            System.out.println("Error: Producto no encontrado");
            return false;
        }
        int cambioStock = tipo.equalsIgnoreCase("Entrada") ? cantidad : -cantidad;
        return actualizarStock(idProducto, cambioStock);
    }

    public Map<String, Object> ajustarInventario(int idTipoDoc, int idEmpleado,
            int idProducto, int cantidad,
            String observacion) {
        return procedimientosDAO.ajustarInventario(idTipoDoc, idEmpleado, idProducto, cantidad, observacion);
    }

    public Map<String, Object> registrarCompra(int idProveedor, int idEmpleado,
            String nroFacturaExt, String productosJson) {
        return procedimientosDAO.registrarCompra(idProveedor, idEmpleado, nroFacturaExt, productosJson);
    }

    public List<MovimientoInventario> obtenerMovimientos() {
        // Obtiene todos los movimientos del reporte (todos los productos, rango amplio)
        List<Map<String, Object>> rawList = procedimientosDAO.reporteMovimientos(
                null, "2000-01-01", "2099-12-31");

        List<MovimientoInventario> resultado = new ArrayList<>();

        for (Map<String, Object> fila : rawList) {
            try {
                MovimientoInventario mov = new MovimientoInventario();
                mov.setIdMovimiento(((Number) fila.get("id_movimiento")).intValue());

                // Producto
                Producto prod = new Producto();
                prod.setNombre((String) fila.getOrDefault("producto", "Desconocido"));
                mov.setProducto(prod);

                // Cantidad con signo segun efecto_en_inventario
                int cantidad = ((Number) fila.get("cantidad")).intValue();
                int efecto = ((Number) fila.get("efecto_en_inventario")).intValue();
                mov.setCantidad(efecto * cantidad);

                // Precio unitario
                Object precioObj = fila.get("precio_unitario");
                if (precioObj != null) {
                    mov.setPrecioUnitario(((Number) precioObj).doubleValue());
                }

                // Fecha
                Object fechaObj = fila.get("fecha_movimiento");
                if (fechaObj instanceof java.sql.Timestamp) {
                    mov.setFechaMovimiento(((java.sql.Timestamp) fechaObj).toLocalDateTime());
                }

                // Tipo de documento como observacion
                String tipoDoc = (String) fila.getOrDefault("tipo_documento", "");
                // Guardamos el tipo en el Documento del movimiento para uso en la vista
                Documento doc = new Documento();
                TipoDocumento td = new TipoDocumento();
                td.setDescripcion(tipoDoc);
                doc.setTipoDocumento(td);
                mov.setDocumento(doc);

                resultado.add(mov);

            } catch (Exception e) {
                System.err.println("InventarioServices.obtenerMovimientos: error mapeando fila - " + e.getMessage());
            }
        }

        return resultado;
    }

    // ── Obtener movimientos filtrados por tipo ────────────────────────────────
    public List<Map<String, Object>> obtenerMovimientosPorDocumento(int idDocumento) {
        return inventarioDAO.obtenerMovimientosPorDocumento(idDocumento);
    }

    public List<Map<String, Object>> obtenerMovimientosPorProducto(int idProducto) {
        return inventarioDAO.obtenerMovimientosPorProducto(idProducto);
    }

    // ── Reporte de movimientos con rango de fechas ────────────────────────────
    public List<Map<String, Object>> reporteMovimientos(Integer idProducto,
            String fechaDesde,
            String fechaHasta) {
        return procedimientosDAO.reporteMovimientos(idProducto, fechaDesde, fechaHasta);
    }

    // ── Stock ─────────────────────────────────────────────────────────────────
    public int obtenerStock(int idProducto) {
        return inventarioDAO.obtenerStockActual(idProducto);
    }

    public boolean actualizarStock(int idProducto, int cantidad) {
        if (productoDAO.obtenerProductoporId(idProducto) == null) {
            System.out.println("Error: Producto no encontrado");
            return false;
        }
        if (cantidad < 0) {
            int stockActual = obtenerStock(idProducto);
            if (stockActual + cantidad < 0) {
                System.out.println("Error: Stock insuficiente. Disponible: " + stockActual);
                return false;
            }
        }
        return inventarioDAO.actualizarStock(idProducto, cantidad);
    }

    public boolean validarStockSuficiente(int idProducto, int cantidadRequerida) {
        return obtenerStock(idProducto) >= cantidadRequerida;
    }

    // ── Alertas ───────────────────────────────────────────────────────────────
    public List<Map<String, Object>> obtenerProductosConStockBajo() {
        return inventarioDAO.obtenerProductosConStockBajo();
    }

    public String generarAlertaInventario() {
        List<Map<String, Object>> lista = obtenerProductosConStockBajo();
        if (lista.isEmpty())
            return "No hay alertas de inventario";

        StringBuilder sb = new StringBuilder();
        sb.append("ALERTA DE INVENTARIO - Productos con stock bajo:\n");
        for (Map<String, Object> p : lista) {
            sb.append("- ").append(p.get("nombre"))
                    .append(" | Stock: ").append(p.get("stockActual"))
                    .append(" | Minimo: ").append(p.get("stockMinimo")).append("\n");
        }
        return sb.toString();
    }

    // ── KPIs dashboard ────────────────────────────────────────────────────────
    public Map<String, Object> obtenerKpisDashboard() {
        return inventarioDAO.obtenerKpisDashboard();
    }

    public List<Map<String, Object>> obtenerVentasPorMes(int meses) {
        return inventarioDAO.obtenerVentasPorMes(meses);
    }
}