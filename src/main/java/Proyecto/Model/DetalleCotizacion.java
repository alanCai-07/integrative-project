package Proyecto.Model;

/**
 * Representa cada producto incluido en una cotización comercial.
 *
 * @author Allan Matthew Caicedo Montaño
 * @author German Astudillo Astudillo
 */
public class DetalleCotizacion {

    private int idDetalleCotizacion;
    private int idCotizacion;
    private int idProducto;
    private String nombreProducto;
    private int cantidad;
    private double precioUnitario;

    public double getSubtotal() {
        return cantidad * precioUnitario;
    }

    public int getIdDetalleCotizacion() { return idDetalleCotizacion; }
    public void setIdDetalleCotizacion(int idDetalleCotizacion) { this.idDetalleCotizacion = idDetalleCotizacion; }

    public int getIdCotizacion() { return idCotizacion; }
    public void setIdCotizacion(int idCotizacion) { this.idCotizacion = idCotizacion; }

    public int getIdProducto() { return idProducto; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }

    public String getNombreProducto() { return nombreProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }

    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }

    public double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(double precioUnitario) { this.precioUnitario = precioUnitario; }
}