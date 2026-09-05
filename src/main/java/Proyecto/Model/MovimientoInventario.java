package Proyecto.Model;

import java.time.LocalDateTime;

public class MovimientoInventario {
    // Atributos
    private int idMovimiento;
    private Documento documento;
    private Producto producto;
    private Empleado empleado;
    private int cantidad;
    private double precioUnitario;
    private double subtotalLinea;
    private LocalDateTime fechaMovimiento;

    // Constructor sin parámetros
    public MovimientoInventario() {
        this.fechaMovimiento = LocalDateTime.now();
    }

    // Constructor con parámetros
    public MovimientoInventario(int idMovimiento, Documento documento, Producto producto, Empleado empleado,
            int cantidad, double precioUnitario) {
        this.idMovimiento = idMovimiento;
        this.documento = documento;
        this.producto = producto;
        this.empleado = empleado;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotalLinea = cantidad * precioUnitario;
        this.fechaMovimiento = LocalDateTime.now();
    }

    // Getters y Setters
    public int getIdMovimiento() {
        return idMovimiento;
    }

    public void setIdMovimiento(int idMovimiento) {
        if (idMovimiento < 0) {
            throw new IllegalArgumentException("El ID no puede ser negativo");
        }
        this.idMovimiento = idMovimiento;
    }

    public Documento getDocumento() {
        return documento;
    }

    public void setDocumento(Documento documento) {
        if (documento == null) {
            throw new IllegalArgumentException("El documento no puede ser nulo");
        }
        this.documento = documento;
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

    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        if (empleado == null) {
            throw new IllegalArgumentException("El empleado no puede ser nulo");
        }
        this.empleado = empleado;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        this.cantidad = cantidad;
        recalcularSubtotal();
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(double precioUnitario) {
        if (precioUnitario < 0) {
            throw new IllegalArgumentException("El precio unitario no puede ser negativo");
        }
        this.precioUnitario = precioUnitario;
        recalcularSubtotal();
    }

    public double getSubtotalLinea() {
        return subtotalLinea;
    }

    public LocalDateTime getFechaMovimiento() {
        return fechaMovimiento;
    }

    public void setFechaMovimiento(LocalDateTime fechaMovimiento) {
        if (fechaMovimiento == null) {
            throw new IllegalArgumentException("La fecha no puede ser nula");
        }
        this.fechaMovimiento = fechaMovimiento;
    }

    // Método auxiliar
    private void recalcularSubtotal() {
        this.subtotalLinea = this.cantidad * this.precioUnitario;
    }

    // ── Alias de métodos para compatibilidad ────────────────────────────────
    public int getId() {
        return idMovimiento;
    }

    public String getTipo() {
        // Determinar el tipo basado en la cantidad (positivo = entrada, negativo =
        // salida)
        return cantidad > 0 ? "Entrada" : "Salida";
    }

    public java.time.LocalDate getFecha() {
        return fechaMovimiento.toLocalDate();
    }

    public String getObservacion() {
        return "Movimiento registrado";
    }

    @Override
    public String toString() {
        return String.format("ID: %d, Producto: %s, Cantidad: %d, Precio Unitario: %.2f, Subtotal: %.2f, Fecha: %s",
                idMovimiento,
                producto != null ? producto.getNombre() : "N/A",
                cantidad,
                precioUnitario,
                subtotalLinea,
                fechaMovimiento);
    }
}
