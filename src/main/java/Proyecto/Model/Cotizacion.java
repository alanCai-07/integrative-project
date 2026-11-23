package Proyecto.Model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una cotización comercial emitida a un cliente sin afectar el inventario.
 *
 * @author Allan Matthew Caicedo Montaño
 * @author German Astudillo Astudillo
 */
public class Cotizacion {

    private int idCotizacion;
    private int idCliente;
    private String nombreCliente;
    private Integer idEmpleado;
    private LocalDateTime fecha;
    private LocalDate vigenciaHasta;
    private double subtotal;
    private double iva;
    private double total;
    private String estado; // VIGENTE, ACEPTADA, VENCIDA, RECHAZADA
    private List<DetalleCotizacion> detalles = new ArrayList<>();

    public Cotizacion() {
        this.fecha = LocalDateTime.now();
        this.vigenciaHasta = LocalDate.now().plusDays(15);
        this.estado = "VIGENTE";
    }

    public int getIdCotizacion() { return idCotizacion; }
    public void setIdCotizacion(int idCotizacion) { this.idCotizacion = idCotizacion; }

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public Integer getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(Integer idEmpleado) { this.idEmpleado = idEmpleado; }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public LocalDate getVigenciaHasta() { return vigenciaHasta; }
    public void setVigenciaHasta(LocalDate vigenciaHasta) { this.vigenciaHasta = vigenciaHasta; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getIva() { return iva; }
    public void setIva(double iva) { this.iva = iva; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public List<DetalleCotizacion> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleCotizacion> detalles) { this.detalles = detalles; }
}