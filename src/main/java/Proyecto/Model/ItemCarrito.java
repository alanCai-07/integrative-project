package Proyecto.Model;

public class ItemCarrito {
    private int idItem;
    private Carrito carrito;
    private Producto producto;
    private int cantidad;
    private double precioUnitario;
    private double subtotal;

    // Constructor sin parámetros
    public ItemCarrito() {
    }

    // Constructor con parámetros
    public ItemCarrito(int idItem, Carrito carrito, Producto producto, int cantidad) {
        this.idItem = idItem;
        this.carrito = carrito;
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = producto.getPrecioVenta();
        this.subtotal = precioUnitario * cantidad;
    }

    // Constructor auxiliar (para compatibilidad)
    public ItemCarrito(Producto producto, int cantidad) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = producto.getPrecioVenta();
        this.subtotal = precioUnitario * cantidad;
    }

    public void actualizarCantidad(int cantidad) {
        this.cantidad = cantidad;
        this.subtotal = this.precioUnitario * cantidad;
    }

    public Producto getProducto() {
        return producto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public int getIdItem() {
        return idItem;
    }

    public int getId() {
        return idItem;
    }

    public void setIdItem(int idItem) {
        if (idItem < 0) {
            throw new IllegalArgumentException("El ID no puede ser negativo");
        }
        this.idItem = idItem;
    }

    public Carrito getCarrito() {
        return carrito;
    }

    public void setCarrito(Carrito carrito) {
        this.carrito = carrito;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    @Override
    public String toString() {
        return String.format("ID: %d, Producto: %s, Cantidad: %d, Subtotal: %.2f",
                idItem, producto != null ? producto.getNombre() : "N/A", cantidad, subtotal);
    }
}
