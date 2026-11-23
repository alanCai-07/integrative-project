package Proyecto.services;

import Proyecto.dao.DocumentoDAO;
import Proyecto.dao.InventarioDAO;
import Proyecto.dao.ItemCarritoDAO;
import Proyecto.dao.PersonaDAO;
import Proyecto.dao.ProcedimientosDAO;
import Proyecto.Model.ItemCarrito;
import Proyecto.Model.Compra;
import Proyecto.Model.DetalleCompra;
import Proyecto.Model.Venta;
import Proyecto.Model.Cliente;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DocumentoServices {

    private final DocumentoDAO documentoDAO;
    private final InventarioDAO inventarioDAO;
    private final ItemCarritoDAO itemCarritoDAO;
    private final ProcedimientosDAO procedimientosDAO;

    public DocumentoServices() {
        this.documentoDAO = new DocumentoDAO();
        this.inventarioDAO = new InventarioDAO();
        this.itemCarritoDAO = new ItemCarritoDAO();
        this.procedimientosDAO = new ProcedimientosDAO();
    }

    /**
     * Registra una venta a partir del carrito activo del cliente utilizando
     * una única transacción atómica en ProcedimientosDAO (RF-07, RNF-07).
     */
    public int crearDocumentoVenta(int idCliente, int idEmpleado,
                                   double descuento, String observaciones) {
        List<ItemCarrito> items = itemCarritoDAO.obtenerItemsDelCarrito(idCliente);

        if (items == null || items.isEmpty()) {
            System.out.println("Error: Carrito vacio para cliente id=" + idCliente);
            return -1;
        }

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            ItemCarrito item = items.get(i);
            if (i > 0) json.append(",");
            json.append(String.format(java.util.Locale.US,
                    "{\"id\":%d,\"qty\":%d,\"precio\":%.2f}",
                    item.getProducto().getIdProducto(),
                    item.getCantidad(),
                    item.getProducto().getPrecioVenta()));
        }
        json.append("]");

        Map<String, Object> res = procedimientosDAO.registrarVenta(
                idCliente, idEmpleado, 1, json.toString());

        int idDocumento = ((Number) res.getOrDefault("idDocumento", -1)).intValue();
        if (idDocumento != -1) {
            itemCarritoDAO.limpiarCarrito(idCliente);
            System.out.println("Venta registrada exitosamente. ID: " + idDocumento);
        } else {
            System.err.println(res.get("mensaje"));
        }
        return idDocumento;
    }

    public int registrarCompra(int idCliente) {
        return crearDocumentoVenta(idCliente, 0, 0, "Compra en linea");
    }

    public List<Venta> obtenerTodasLasVentas() {
        PersonaDAO personaDAO = new PersonaDAO();
        List<Map<String, Object>> documentos = documentoDAO.obtenerTodosLosDocumentos();
        List<Venta> ventas = new ArrayList<>();

        for (Map<String, Object> doc : documentos) {
            try {
                Object idPersObj = doc.get("idPersona");
                int idPersona = idPersObj != null ? ((Number) idPersObj).intValue() : 0;
                Cliente cliente = idPersona > 0 ? personaDAO.obtenerClientePorId(idPersona) : null;

                if (cliente == null) {
                    cliente = new Cliente();
                    cliente.setId(idPersona);
                    cliente.setNombre(idPersona > 0 ? "Persona" : "Ajuste");
                    cliente.setApellido(idPersona > 0 ? "ID-" + idPersona : "Interno");
                }

                ventas.add(Venta.fromMap(doc, cliente));
            } catch (Exception e) {
                System.err.println("Error al mapear venta: " + e.getMessage());
            }
        }
        return ventas;
    }

    public List<Venta> obtenerVentasPorRango(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null || fechaFin == null) {
            return obtenerTodasLasVentas();
        }
        return obtenerTodasLasVentas().stream()
                .filter(v -> v.getFecha() != null
                        && !v.getFecha().isBefore(fechaInicio)
                        && !v.getFecha().isAfter(fechaFin))
                .collect(Collectors.toList());
    }

    public Map<String, Object> obtenerDocumento(int idDocumento) {
        return documentoDAO.obtenerDocumentoPorId(idDocumento);
    }

    public List<Map<String, Object>> obtenerDocumentosDelCliente(int idCliente) {
        return documentoDAO.obtenerDocumentosPorCliente(idCliente);
    }

    public List<Map<String, Object>> obtenerDocumentosPorTipo(int idTipoDocumento) {
        return documentoDAO.obtenerDocumentosPorTipo(idTipoDocumento);
    }

    public List<Map<String, Object>> obtenerTodosLosDocumentos() {
        return documentoDAO.obtenerTodosLosDocumentos();
    }

    public List<Map<String, Object>> obtenerDetallesDocumento(int idDocumento) {
        return inventarioDAO.obtenerMovimientosPorDocumento(idDocumento);
    }

    public String generarReporteVentasCliente(int idCliente) {
        List<Map<String, Object>> documentos = documentoDAO.obtenerDocumentosPorCliente(idCliente);
        if (documentos.isEmpty()) return "No hay compras registradas para este cliente";
        StringBuilder sb = new StringBuilder();
        sb.append("REPORTE DE COMPRAS DEL CLIENTE\n================================\n\n");
        double total = 0;
        int count = 0;
        for (Map<String, Object> doc : documentos) {
            double t = ((Number) doc.get("total")).doubleValue();
            total += t;
            count++;
            sb.append("Documento: ").append(doc.get("idDocumento")).append("\n")
              .append("  Fecha: ").append(doc.get("fecha")).append("\n")
              .append("  Total: $").append(String.format("%.2f", t)).append("\n\n");
        }
        sb.append("Total compras: ").append(count).append("\n");
        sb.append("Total gastado: $").append(String.format("%.2f", total)).append("\n");
        return sb.toString();
    }

    public String generarReporteVentasTotales() {
        List<Map<String, Object>> documentos = documentoDAO.obtenerTodosLosDocumentos();
        StringBuilder sb = new StringBuilder();
        sb.append("REPORTE DE VENTAS TOTALES\n==========================\n\n");
        double totalVentas = 0, totalDesc = 0;
        int cantidad = 0;
        for (Map<String, Object> doc : documentos) {
            totalVentas += ((Number) doc.get("total")).doubleValue();
            totalDesc += ((Number) doc.get("descuento")).doubleValue();
            cantidad++;
        }
        sb.append("Total documentos: ").append(cantidad).append("\n");
        sb.append("Total ventas: $").append(String.format("%,.0f", totalVentas)).append("\n");
        sb.append("Total descuentos: $").append(String.format("%,.0f", totalDesc)).append("\n");
        if (cantidad > 0) {
            sb.append("Promedio: $").append(String.format("%,.0f", totalVentas / cantidad)).append("\n");
        }
        return sb.toString();
    }

    public String generarFactura(int idDocumento) {
        Map<String, Object> doc = obtenerDocumento(idDocumento);
        if (doc == null) return "Documento no encontrado";
        StringBuilder sb = new StringBuilder();
        sb.append("FACTURA\n=======\n\nNumero: ").append(doc.get("idDocumento"))
          .append("\nFecha:  ").append(doc.get("fecha")).append("\n\n");
        for (Map<String, Object> d : obtenerDetallesDocumento(idDocumento)) {
            sb.append("  Cantidad: ").append(d.get("cantidad"))
              .append("\n  Subtotal: $").append(String.format("%,.0f", d.get("subtotal"))).append("\n\n");
        }
        sb.append("Descuento: $").append(String.format("%,.0f", doc.get("descuento")))
          .append("\nTOTAL:     $").append(String.format("%,.0f", doc.get("total"))).append("\n");
        return sb.toString();
    }

    public List<Compra> obtenerComprasCliente(int idCliente) {
        return obtenerDocumentosDelCliente(idCliente).stream()
                .map(Compra::fromMap).collect(Collectors.toList());
    }

    public List<DetalleCompra> obtenerDetalleCompra(int idCompra) {
        return obtenerDetallesDocumento(idCompra).stream()
                .map(DetalleCompra::fromMap).collect(Collectors.toList());
    }

    public String generarReporteVentas() {
        return generarReporteVentasTotales();
    }
}