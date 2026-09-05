package Proyecto.Model;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class Compra {

    private int id;
    private LocalDate fecha;
    private List<DetalleCompra> detalles;
    private double total;
    private String estado;

    public Compra(int id, LocalDate fecha, List<DetalleCompra> detalles, double total, String estado) {
        this.id = id;
        this.fecha = fecha;
        this.detalles = detalles;
        this.total = total;
        this.estado = estado;
    }

    public int getId() {
        return id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public List<DetalleCompra> getDetalles() {
        return detalles;
    }

    public double getTotal() {
        return total;
    }

    public String getEstado() {
        return estado;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public void setDetalles(List<DetalleCompra> detalles) {
        this.detalles = detalles;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public static Compra fromMap(Map<String, Object> data) {
        int id = ((Number) data.get("idDocumento")).intValue();

        LocalDate fecha;
        Object fechaObj = data.get("fecha");
        if (fechaObj instanceof Timestamp) {
            fecha = ((Timestamp) fechaObj).toLocalDateTime().toLocalDate();
        } else if (fechaObj instanceof java.sql.Date) {
            fecha = ((java.sql.Date) fechaObj).toLocalDate();
        } else if (fechaObj instanceof String) {
            try {
                fecha = LocalDate.parse((String) fechaObj);
            } catch (Exception e) {
                fecha = LocalDate.now();
            }
        } else {
            fecha = LocalDate.now();
        }

        double total = data.get("total") != null ? ((Number) data.get("total")).doubleValue() : 0.0;
        String estado = data.get("estado") != null ? (String) data.get("estado") : "COMPLETADA";

        return new Compra(id, fecha, List.of(), total, estado);
    }

    @Override
    public String toString() {
        return "Compra{id=" + id + ", fecha=" + fecha + ", total=" + total + ", estado='" + estado + "'}";
    }
}