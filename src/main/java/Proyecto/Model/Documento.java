package Proyecto.Model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa un comprobante comercial o documento de movimiento de inventario.
 *
 * @author Allan Matthew Caicedo Montaño
 * @author German Astudillo Astudillo
 */
public class Documento {

    private int idDocumento;
    private int idTipoDocumento;
    private TipoDocumento tipoDocumento;
    private int numeroComprobante;
    private Integer idPersona;
    private Persona persona;
    private Integer idEmpleado;
    private Empleado empleado;
    private Integer idMetodoPago;
    private MetodoPago metodoPago;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaDocumento;
    private String numeroDocExterno;
    private double subtotal;
    private double descuento;
    private double iva;
    private double total;
    private String estado;
    private String observaciones;
    private List<MovimientoInventario> movimientos = new ArrayList<>();

    public Documento() {
        this.fechaInicio = LocalDateTime.now();
        this.estado = "COMPLETADA";
    }

    /**
     * Regla de negocio: total = subtotal - descuento + iva.
     */
    public void recalcularTotal() {
        this.total = Math.max(0, (this.subtotal - this.descuento) + this.iva);
    }

    public int getIdDocumento() { return idDocumento; }
    public void setIdDocumento(int idDocumento) { this.idDocumento = idDocumento; }

    public int getIdTipoDocumento() {
        return tipoDocumento != null ? tipoDocumento.getIdTipoDocumento() : idTipoDocumento;
    }
    public void setIdTipoDocumento(int idTipoDocumento) { this.idTipoDocumento = idTipoDocumento; }

    public TipoDocumento getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
        if (tipoDocumento != null) {
            this.idTipoDocumento = tipoDocumento.getIdTipoDocumento();
        }
    }

    public int getNumeroComprobante() { return numeroComprobante; }
    public void setNumeroComprobante(int numeroComprobante) { this.numeroComprobante = numeroComprobante; }

    public Integer getIdPersona() {
        return persona != null ? persona.getId() : idPersona;
    }
    public void setIdPersona(Integer idPersona) { this.idPersona = idPersona; }

    public Persona getPersona() { return persona; }
    public void setPersona(Persona persona) {
        this.persona = persona;
        if (persona != null) {
            this.idPersona = persona.getId();
        }
    }

    public Integer getIdEmpleado() {
        return empleado != null ? empleado.getId() : idEmpleado;
    }
    public void setIdEmpleado(Integer idEmpleado) { this.idEmpleado = idEmpleado; }

    public Empleado getEmpleado() { return empleado; }
    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
        if (empleado != null) {
            this.idEmpleado = empleado.getId();
        }
    }

    public Integer getIdMetodoPago() {
        return metodoPago != null ? metodoPago.getIdMetodoPago() : idMetodoPago;
    }
    public void setIdMetodoPago(Integer idMetodoPago) { this.idMetodoPago = idMetodoPago; }

    public MetodoPago getMetodoPago() { return metodoPago; }
    public void setMetodoPago(MetodoPago metodoPago) {
        this.metodoPago = metodoPago;
        if (metodoPago != null) {
            this.idMetodoPago = metodoPago.getIdMetodoPago();
        }
    }

    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDateTime getFechaDocumento() { return fechaDocumento; }
    public void setFechaDocumento(LocalDateTime fechaDocumento) { this.fechaDocumento = fechaDocumento; }

    public String getNumeroDocExterno() { return numeroDocExterno; }
    public void setNumeroDocExterno(String numeroDocExterno) { this.numeroDocExterno = numeroDocExterno; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getDescuento() { return descuento; }
    public void setDescuento(double descuento) { this.descuento = descuento; }

    public double getIva() { return iva; }
    public void setIva(double iva) { this.iva = iva; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public List<MovimientoInventario> getMovimientos() { return movimientos; }
    public void setMovimientos(List<MovimientoInventario> movimientos) { this.movimientos = movimientos; }
}