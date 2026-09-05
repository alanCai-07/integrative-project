package Proyecto.Model;

import java.util.Map;

/**
 * Modelo de datos para representar el detalle de una compra (item de compra)
 */
public class DetalleCompra {
    private String producto;
    private int cantidad;
    private double precioUnitario;

    public DetalleCompra(String producto, int cantidad, double precioUnitario) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    // Getters
    public String getProducto() {
        return producto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    // Setters
    public void setProducto(String producto) {
        this.producto = producto;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    /**
     * Crea un DetalleCompra desde un Map devuelto por la DAO
     */
    public static DetalleCompra fromMap(Map<String, Object> data) {
        String producto = (String) data.get("producto");
        int cantidad = ((Number) data.get("cantidad")).intValue();
        double precioUnitario = ((Number) data.get("precioUnitario")).doubleValue();

        return new DetalleCompra(producto, cantidad, precioUnitario);
    }

    @Override
    public String toString() {
        return "DetalleCompra{" +
                "producto='" + producto + '\'' +
                ", cantidad=" + cantidad +
                ", precioUnitario=" + precioUnitario +
                '}';
    }
}
