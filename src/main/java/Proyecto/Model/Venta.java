package Proyecto.Model;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class Venta {

    private int              id;
    private Cliente          cliente;
    private LocalDate        fecha;
    private List<DetalleCompra> detalles;
    private double           total;
    private String           estado;

    public Venta(int id, Cliente cliente, LocalDate fecha,
                 List<DetalleCompra> detalles, double total, String estado) {
        this.id       = id;
        this.cliente  = cliente;
        this.fecha    = fecha;
        this.detalles = detalles;
        this.total    = total;
        this.estado   = estado;
    }

    // Getters
    public int                 getId()       { return id; }
    public Cliente             getCliente()  { return cliente; }
    public LocalDate           getFecha()    { return fecha; }
    public List<DetalleCompra> getDetalles() { return detalles; }
    public double              getTotal()    { return total; }
    public String              getEstado()   { return estado; }

    // Setters
    public void setId(int id)                          { this.id = id; }
    public void setCliente(Cliente cliente)            { this.cliente = cliente; }
    public void setFecha(LocalDate fecha)              { this.fecha = fecha; }
    public void setDetalles(List<DetalleCompra> d)    { this.detalles = d; }
    public void setTotal(double total)                 { this.total = total; }
    public void setEstado(String estado)               { this.estado = estado; }

    /**
     * CORRECCION:
     * DocumentoDAO.mapearDocumento() guarda la fecha como Timestamp (rs.getTimestamp()),
     * no como String. El metodo original hacia LocalDate.parse((String) data.get("fecha"))
     * lo que lanzaba ClassCastException y dejaba la lista de ventas vacia.
     *
     * Ahora se detecta el tipo real del objeto y se convierte correctamente:
     *   - Timestamp → toLocalDateTime().toLocalDate()
     *   - String    → LocalDate.parse() como fallback
     *   - null      → LocalDate.now() como valor por defecto seguro
     */
    public static Venta fromMap(Map<String, Object> data, Cliente cliente) {
        int id = ((Number) data.get("idDocumento")).intValue();

        // Conversion segura de fecha independientemente del tipo que venga del DAO
        LocalDate fecha;
        Object fechaObj = data.get("fecha");
        if (fechaObj instanceof Timestamp) {
            fecha = ((Timestamp) fechaObj).toLocalDateTime().toLocalDate();
        } else if (fechaObj instanceof String) {
            try {
                fecha = LocalDate.parse((String) fechaObj);
            } catch (Exception e) {
                fecha = LocalDate.now();
            }
        } else {
            fecha = LocalDate.now();
        }

        double total  = ((Number) data.get("total")).doubleValue();
        String estado = data.get("estado") != null ? (String) data.get("estado") : "Completada";

        return new Venta(id, cliente, fecha, List.of(), total, estado);
    }

    @Override
    public String toString() {
        return "Venta{id=" + id +
               ", cliente=" + (cliente != null ? cliente.getNombre() : "N/A") +
               ", fecha=" + fecha +
               ", total=" + total +
               ", estado='" + estado + "'}";
    }
}
