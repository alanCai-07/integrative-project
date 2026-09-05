package Proyecto.Model;

public class Producto {

    // Atributos
    private int idProducto;
    private Categoria categoria;
    private String nombre;
    private String descripcion;
    private double precioCompra;
    private double precioVenta;
    private int stockActual;
    private int stockMinimo;
    private Boolean activo;

    // Constructor sin parámetros
    public Producto() {
        this.activo = true;
    }

    // Constructor con parámetros
    public Producto(int idProducto, Categoria categoria, String nombre, String descripcion,
            double precioCompra, double precioVenta, int stockActual, int stockMinimo) {
        this.idProducto = idProducto;
        this.categoria = categoria;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precioCompra = precioCompra;
        this.precioVenta = precioVenta;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.activo = true;
    }

    // Getters y Setters
    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getPrecioCompra() {
        return precioCompra;
    }

    public void setPrecioCompra(double precioCompra) {
        this.precioCompra = precioCompra;
    }

    public double getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(double precioVenta) {
        this.precioVenta = precioVenta;
    }

    public int getCantidad() {
        return stockActual;
    }

    public void setCantidad(int cantidad) {
        this.stockActual = cantidad;
    }

    public int getStockActual() {
        return stockActual;
    }

    public void setStockActual(int stockActual) {
        if (stockActual < 0) {
            throw new IllegalArgumentException("El stock actual no puede ser negativo");
        }
        this.stockActual = stockActual;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(int stockMinimo) {
        if (stockMinimo < 0) {
            throw new IllegalArgumentException("El stock mínimo no puede ser negativo");
        }
        this.stockMinimo = stockMinimo;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo != null ? activo : true;
    }

    // Verifica si el producto requiere reorden

    public boolean requiereReorden() {
        return stockActual <= stockMinimo;
    }

    @Override
    public String toString() {
        return String.format("ID: %d, Nombre: %s, Categoría: %s, Stock: %d, Precio Venta: %.2f, Activo: %s",
                idProducto, nombre, categoria != null ? categoria.getNombre() : "N/A",
                stockActual, precioVenta, activo);
    }
}
