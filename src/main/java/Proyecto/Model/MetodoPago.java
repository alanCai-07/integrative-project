package Proyecto.Model;

public class MetodoPago {
    private int idMetodoPago;
    private String nombre; // Efectivo, Tarjeta, Transferencia
    private Boolean activo;

    // Constructor sin parámetros
    public MetodoPago() {
        this.activo = true;
    }

    // Constructor con parámetros
    public MetodoPago(int idMetodoPago, String nombre) {
        this.idMetodoPago = idMetodoPago;
        this.nombre = nombre;
        this.activo = true;
    }

    // Getters y Setters
    public int getIdMetodoPago() {
        return idMetodoPago;
    }

    public void setIdMetodoPago(int idMetodoPago) {
        if (idMetodoPago < 0) {
            throw new IllegalArgumentException("El ID no puede ser negativo");
        }
        this.idMetodoPago = idMetodoPago;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del método de pago no puede estar vacío");
        }
        this.nombre = nombre.trim();
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo != null ? activo : true;
    }

    @Override
    public String toString() {
        return String.format("ID: %d, Nombre: %s, Activo: %s", idMetodoPago, nombre, activo);
    }
}