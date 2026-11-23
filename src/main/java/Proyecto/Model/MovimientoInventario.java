package Proyecto.Model;

import java.time.LocalDateTime;

/**
 * Representa cada línea de movimiento físico de inventario asociado a un documento.
 *
 * @author Allan Matthew Caicedo Montaño
 * @author German Astudillo Astudillo
 */
public class MovimientoInventario {

    private int idMovimiento;
    private int idDocumento;
    private Documento documento;
    private int idProducto;
    private Producto producto;
    private Integer idEmpleado;
    private Empleado empleado;
    private int cantidad;
    private double costoUnitario;
    private double precioUnitario;
    private double subtotalLinea;
    private LocalDateTime fechaMovimiento;

    public int getIdMovimiento() { return idMovimiento; }
    public void setIdMovimiento(int idMovimiento) { this.idMovimiento = idMovimiento; }

    public int getIdDocumento() {
        return documento != null ? documento.getIdDocumento() : idDocumento;
    }
    public void setIdDocumento(int idDocumento) { this.idDocumento = idDocumento; }

    public Documento getDocumento() { return documento; }
    public void setDocumento(Documento documento) {
        this.documento = documento;
        if (documento != null) {
            this.idDocumento = documento.getIdDocumento();
        }
    }

    public int getIdProducto() {
        return producto != null ? producto.getIdProducto() : idProducto;
    }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }

    public Producto getProducto() { return producto; }
    public void setProducto(Producto producto) {
        this.producto = producto;
        if (producto != null) {
            this.idProducto = producto.getIdProducto();
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

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public double getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(double costoUnitario) { this.costoUnitario = costoUnitario; }

    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }

    public double getSubtotalLinea() {
        return subtotalLinea > 0 ? subtotalLinea : cantidad * precioUnitario;
    }
    public void setSubtotalLinea(double subtotalLinea) { this.subtotalLinea = subtotalLinea; }

    public LocalDateTime getFechaMovimiento() { return fechaMovimiento; }
    public void setFechaMovimiento(LocalDateTime fechaMovimiento) { this.fechaMovimiento = fechaMovimiento; }

    /**
     * Devuelve la descripción del tipo de documento asociado al movimiento.
     */
    public String getTipo() {
        if (documento != null && documento.getTipoDocumento() != null) {
            return documento.getTipoDocumento().getDescripcion();
        }
        return "";
    }
}