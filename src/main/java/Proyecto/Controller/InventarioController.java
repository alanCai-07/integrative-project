package Proyecto.Controller;

import Proyecto.dao.InventarioDAO;
import java.util.List;
import java.util.Map;

public class InventarioController {

    private InventarioDAO inventarioDAO;

    public InventarioController() {
        this.inventarioDAO = new InventarioDAO();
    }

    // Registrar movimiento de inventario
    public boolean registrarMovimiento(int idDocumento, int idProducto, int idEmpleado,
                                      int cantidad, double subtotal) {
        // Validar cantidad
        if (cantidad == 0) {
            System.out.println("La cantidad no puede ser cero");
            return false;
        }

        return inventarioDAO.registrarMovimiento(idDocumento, idProducto, idEmpleado, cantidad, subtotal);
    }

    // Obtener movimientos de un documento
    public List<Map<String, Object>> obtenerMovimientosPorDocumento(int idDocumento) {
        return inventarioDAO.obtenerMovimientosPorDocumento(idDocumento);
    }

    // Obtener movimientos de un producto
    public List<Map<String, Object>> obtenerMovimientosPorProducto(int idProducto) {
        return inventarioDAO.obtenerMovimientosPorProducto(idProducto);
    }

    // Obtener stock actual de un producto
    public int obtenerStock(int idProducto) {
        return inventarioDAO.obtenerStockActual(idProducto);
    }

    // Actualizar stock del producto
    public boolean actualizarStock(int idProducto, int cantidad) {
        // Validar que no quede stock negativo en caso de disminución
        if (cantidad < 0) {
            int stockActual = inventarioDAO.obtenerStockActual(idProducto);
            if (stockActual + cantidad < 0) {
                System.out.println("Stock insuficiente. Disponible: " + stockActual);
                return false;
            }
        }

        return inventarioDAO.actualizarStock(idProducto, cantidad);
    }

    // Obtener productos con stock bajo
    public List<Map<String, Object>> obtenerProductosConStockBajo() {
        return inventarioDAO.obtenerProductosConStockBajo();
    }

    // Obtener alertas de inventario
    public String generarAlertaInventario() {
        List<Map<String, Object>> productosStockBajo = obtenerProductosConStockBajo();

        if (productosStockBajo.isEmpty()) {
            return "No hay alertas de inventario";
        }

        StringBuilder alerta = new StringBuilder();
        alerta.append("ALERTA DE INVENTARIO - Productos con stock bajo:\n");

        for (Map<String, Object> producto : productosStockBajo) {
            alerta.append("- ").append(producto.get("nombre"))
                  .append(" | Stock: ").append(producto.get("stockActual"))
                  .append(" | Mínimo: ").append(producto.get("stockMinimo"))
                  .append("\n");
        }

        return alerta.toString();
    }

    // Generar reporte de inventario
    public String generarReporteInventario() {
        StringBuilder reporte = new StringBuilder();
        reporte.append("REPORTE DE INVENTARIO\n");
        reporte.append("=====================\n\n");

        List<Map<String, Object>> productosStockBajo = obtenerProductosConStockBajo();
        reporte.append("Productos con stock bajo: ").append(productosStockBajo.size()).append("\n\n");

        for (Map<String, Object> producto : productosStockBajo) {
            reporte.append("Producto: ").append(producto.get("nombre")).append("\n")
                   .append("  Stock Actual: ").append(producto.get("stockActual")).append("\n")
                   .append("  Stock Mínimo: ").append(producto.get("stockMinimo")).append("\n\n");
        }

        return reporte.toString();
    }
}
