package Proyecto.Controller;

import Proyecto.dao.DocumentoDAO;
import Proyecto.dao.InventarioDAO;
import Proyecto.dao.CarritoDAO;
import Proyecto.dao.ItemCarritoDAO;
import Proyecto.Model.Carrito;
import Proyecto.Model.ItemCarrito;
import java.util.List;
import java.util.Map;

public class DocumentoController {

    private DocumentoDAO documentoDAO;
    private InventarioDAO inventarioDAO;
    private CarritoDAO carritoDAO;
    private ItemCarritoDAO itemCarritoDAO;

    public DocumentoController() {
        this.documentoDAO = new DocumentoDAO();
        this.inventarioDAO = new InventarioDAO();
        this.carritoDAO = new CarritoDAO();
        this.itemCarritoDAO = new ItemCarritoDAO();
    }

    // Crear documento de venta
    public int crearDocumentoVenta(int idCliente, int idEmpleado, double descuento, String observaciones) {
        // Obtener carrito del cliente
        Carrito carrito = carritoDAO.obtenerCarritoActivoDelCliente(idCliente);
        if (carrito == null || carrito.estaVacio()) {
            System.out.println("Carrito vacío");
            return -1;
        }

        // Calcular total
        double total = carrito.getTotal() - descuento;
        if (total < 0) total = 0;

        // Crear documento (tipo 1 = Factura de venta)
        int idDocumento = documentoDAO.crearDocumento(1, idCliente, idEmpleado, descuento, total, observaciones);

        if (idDocumento != -1) {
            // Registrar movimientos de inventario
            List<ItemCarrito> items = itemCarritoDAO.obtenerItemsDelCarrito(carrito.getCliente().getId());
            for (ItemCarrito item : items) {
                inventarioDAO.registrarMovimiento(
                    idDocumento,
                    item.getProducto().getIdProducto(),
                    idEmpleado,
                    item.getCantidad(),
                    item.getSubtotal()
                );
                // Disminuir stock
                inventarioDAO.actualizarStock(item.getProducto().getIdProducto(), -item.getCantidad());
            }

            // Limpiar carrito
            itemCarritoDAO.limpiarCarrito(carrito.getCliente().getId());
        }

        return idDocumento;
    }

    // Obtener documento por ID
    public Map<String, Object> obtenerDocumento(int idDocumento) {
        return documentoDAO.obtenerDocumentoPorId(idDocumento);
    }

    // Obtener documentos del cliente
    public List<Map<String, Object>> obtenerDocumentosDelCliente(int idCliente) {
        return documentoDAO.obtenerDocumentosPorCliente(idCliente);
    }

    // Obtener documentos por tipo
    public List<Map<String, Object>> obtenerDocumentosPorTipo(int idTipoDocumento) {
        return documentoDAO.obtenerDocumentosPorTipo(idTipoDocumento);
    }

    // Obtener todos los documentos
    public List<Map<String, Object>> obtenerTodosLosDocumentos() {
        return documentoDAO.obtenerTodosLosDocumentos();
    }

    // Generar reporte de ventas
    public String generarReporteVentas(int idCliente) {
        List<Map<String, Object>> documentos = documentoDAO.obtenerDocumentosPorCliente(idCliente);

        if (documentos.isEmpty()) {
            return "No hay compras registradas";
        }

        StringBuilder reporte = new StringBuilder();
        reporte.append("REPORTE DE COMPRAS DEL CLIENTE\n");
        reporte.append("================================\n\n");

        double totalGastado = 0;
        for (Map<String, Object> doc : documentos) {
            double total = (double) doc.get("total");
            totalGastado += total;

            reporte.append("Documento: ").append(doc.get("idDocumento")).append("\n")
                   .append("  Fecha: ").append(doc.get("fecha")).append("\n")
                   .append("  Total: $").append(total).append("\n")
                   .append("  Observaciones: ").append(doc.get("observaciones")).append("\n\n");
        }

        reporte.append("TOTAL GASTADO: $").append(totalGastado).append("\n");

        return reporte.toString();
    }

    // Generar reporte de ventas por período
    public String generarReporteVentasPorPeriodo(String fechaInicio, String fechaFin) {
        List<Map<String, Object>> documentos = documentoDAO.obtenerTodosLosDocumentos();

        StringBuilder reporte = new StringBuilder();
        reporte.append("REPORTE DE VENTAS\n");
        reporte.append("Período: ").append(fechaInicio).append(" a ").append(fechaFin).append("\n");
        reporte.append("================================\n\n");

        double totalVentas = 0;
        int cantidadDocumentos = 0;

        for (Map<String, Object> doc : documentos) {
            double total = (double) doc.get("total");
            totalVentas += total;
            cantidadDocumentos++;

            reporte.append("Documento: ").append(doc.get("idDocumento")).append("\n")
                   .append("  Cliente: ").append(doc.get("idPersona")).append("\n")
                   .append("  Total: $").append(total).append("\n\n");
        }

        reporte.append("\nRESUMEN:\n");
        reporte.append("Total de documentos: ").append(cantidadDocumentos).append("\n");
        reporte.append("Total de ventas: $").append(totalVentas).append("\n");

        return reporte.toString();
    }

    // Obtener movimientos de inventario de un documento
    public List<Map<String, Object>> obtenerDetallesDocumento(int idDocumento) {
        return inventarioDAO.obtenerMovimientosPorDocumento(idDocumento);
    }
}
