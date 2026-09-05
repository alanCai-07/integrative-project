package Proyecto.services;

import Proyecto.dao.DocumentoDAO;
import Proyecto.dao.InventarioDAO;
import Proyecto.dao.CarritoDAO;
import Proyecto.dao.ItemCarritoDAO;
import Proyecto.dao.PersonaDAO;
import Proyecto.Model.Carrito;
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

    private DocumentoDAO   documentoDAO;
    private InventarioDAO  inventarioDAO;
    private CarritoDAO     carritoDAO;
    private ItemCarritoDAO itemCarritoDAO;

    public DocumentoServices() {
        this.documentoDAO   = new DocumentoDAO();
        this.inventarioDAO  = new InventarioDAO();
        this.carritoDAO     = new CarritoDAO();
        this.itemCarritoDAO = new ItemCarritoDAO();
    }

    // ── Crear documento de venta ───────────────────────────────────────────────
    public int crearDocumentoVenta(int idCliente, int idEmpleado,
                                   double descuento, String observaciones) {
        List<ItemCarrito> items = itemCarritoDAO.obtenerItemsDelCarrito(idCliente);

        if (items == null || items.isEmpty()) {
            System.out.println("Error: Carrito vacio para cliente id=" + idCliente);
            return -1;
        }

        double subtotalTotal = 0;
        for (ItemCarrito item : items) {
            subtotalTotal += item.getSubtotal();
        }
        double total = subtotalTotal - descuento;
        if (total < 0) total = 0;

        int idDocumento = documentoDAO.crearDocumento(
                1, idCliente, idEmpleado, descuento, total, observaciones);

        if (idDocumento == -1) {
            System.out.println("Error: No se pudo crear el documento de venta");
            return -1;
        }

        for (ItemCarrito item : items) {
            inventarioDAO.registrarMovimiento(
                    idDocumento,
                    item.getProducto().getIdProducto(),
                    idEmpleado,
                    item.getCantidad(),
                    item.getSubtotal());
            inventarioDAO.actualizarStock(
                    item.getProducto().getIdProducto(),
                    -item.getCantidad());
        }

        itemCarritoDAO.limpiarCarrito(idCliente);
        System.out.println("Venta registrada exitosamente. ID: " + idDocumento);
        return idDocumento;
    }

    public int registrarCompra(int idCliente) {
        return crearDocumentoVenta(idCliente, 0, 0, "Compra en linea");
    }

    // ── Obtener todas las ventas CORREGIDO ────────────────────────────────────
    /**
     * CORRECCION PRINCIPAL:
     * El metodo anterior llamaba a personaDAO.obtenerClientePorId() para cada
     * documento. Si el id_persona del documento correspondia a un empleado o
     * proveedor (no a un cliente), retornaba null y Venta.fromMap() fallaba,
     * dejando la lista vacia.
     *
     * Solucion: construir un Cliente sintetico con los datos disponibles en el
     * Map del documento cuando obtenerClientePorId() retorna null, evitando
     * que un null rompa el stream y dejando la tabla en blanco.
     */
    public List<Venta> obtenerTodasLasVentas() {
        PersonaDAO personaDAO = new PersonaDAO();
        List<Map<String, Object>> documentos = documentoDAO.obtenerTodosLosDocumentos();
        List<Venta> ventas = new ArrayList<>();

        for (Map<String, Object> doc : documentos) {
            try {
                int     idPersona = ((Number) doc.get("idPersona")).intValue();
                Cliente cliente   = personaDAO.obtenerClientePorId(idPersona);

                // Si no es un cliente registrado (p.ej. empleado que hizo la venta)
                // se crea un objeto Cliente sintetico para que la fila se muestre igual
                if (cliente == null) {
                    cliente = new Cliente();
                    cliente.setId(idPersona);
                    cliente.setNombre("Persona");
                    cliente.setApellido("ID-" + idPersona);
                }

                ventas.add(Venta.fromMap(doc, cliente));
            } catch (Exception e) {
                System.err.println("Error al mapear venta: " + e.getMessage());
            }
        }

        return ventas;
    }

    // ── Filtrar por rango de fechas CORREGIDO ─────────────────────────────────
    /**
     * CORRECCION: el metodo anterior filtraba sobre obtenerTodasLasVentas()
     * que ya fallaba. Ahora filtra sobre la lista corregida.
     * Ademas se agrego validacion de null en getFecha() para evitar NPE.
     */
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

    // ── Resto de metodos sin cambios ──────────────────────────────────────────

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
        List<Map<String, Object>> documentos =
                documentoDAO.obtenerDocumentosPorCliente(idCliente);
        if (documentos.isEmpty()) return "No hay compras registradas para este cliente";
        StringBuilder sb = new StringBuilder();
        sb.append("REPORTE DE COMPRAS DEL CLIENTE\n================================\n\n");
        double total = 0;
        int count = 0;
        for (Map<String, Object> doc : documentos) {
            double t = (double) doc.get("total");
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
            totalVentas += (double) doc.get("total");
            totalDesc   += (double) doc.get("descuento");
            cantidad++;
        }
        sb.append("Total documentos: ").append(cantidad).append("\n");
        sb.append("Total ventas: $").append(String.format("%.2f", totalVentas)).append("\n");
        sb.append("Total descuentos: $").append(String.format("%.2f", totalDesc)).append("\n");
        if (cantidad > 0)
            sb.append("Promedio: $")
              .append(String.format("%.2f", totalVentas / cantidad)).append("\n");
        return sb.toString();
    }

    public String generarFactura(int idDocumento) {
        Map<String, Object> doc = obtenerDocumento(idDocumento);
        if (doc == null) return "Documento no encontrado";
        StringBuilder sb = new StringBuilder();
        sb.append("FACTURA\n=======\n\nNumero: ").append(doc.get("idDocumento"))
          .append("\nFecha:  ").append(doc.get("fecha")).append("\n\n");
        for (Map<String, Object> d : obtenerDetallesDocumento(idDocumento))
            sb.append("  Cantidad: ").append(d.get("cantidad"))
              .append("\n  Subtotal: $")
              .append(String.format("%.2f", d.get("subtotal"))).append("\n\n");
        sb.append("Descuento: $").append(String.format("%.2f", doc.get("descuento")))
          .append("\nTOTAL:     $").append(String.format("%.2f", doc.get("total"))).append("\n");
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
