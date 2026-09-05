package Proyecto.Model;

public class Inventario {
    // Atributos
    private int idInventario;
    private Producto producto;
    private int stockActual;
    private int stockMinimo;
    private int stockMaximo;

    // Constructor sin parámetros
    public Inventario() {
    }

    // Constructor con parámetros
    public Inventario(int idInventario, Producto producto, int stockActual, int stockMinimo, int stockMaximo) {
        this.idInventario = idInventario;
        this.producto = producto;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.stockMaximo = stockMaximo;
    }

    // Getters y Setters
    public int getIdInventario() {
        return idInventario;
    }

    public void setIdInventario(int idInventario) {
        if (idInventario < 0) {
            throw new IllegalArgumentException("El ID no puede ser negativo");
        }
        this.idInventario = idInventario;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        if (producto == null) {
            throw new IllegalArgumentException("El producto no puede ser nulo");
        }
        this.producto = producto;
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

    public int getStockMaximo() {
        return stockMaximo;
    }

    public void setStockMaximo(int stockMaximo) {
        if (stockMaximo < 0) {
            throw new IllegalArgumentException("El stock máximo no puede ser negativo");
        }
        this.stockMaximo = stockMaximo;
    }

    // Métodos
    // Verifica si el inventario está por debajo del nivel mínimo

    public boolean requiereReorden() {
        return stockActual <= stockMinimo;
    }

    // Verifica si el inventario ha alcanzado el nivel máximo

    public boolean estaEnMaximo() {
        return stockActual >= stockMaximo;
    }

    @Override
    public String toString() {
        return String.format("ID: %d, Producto: %s, Stock Actual: %d, Mínimo: %d, Máximo: %d",
                idInventario,
                producto != null ? producto.getNombre() : "N/A",
                stockActual,
                stockMinimo,
                stockMaximo);
    }
}
