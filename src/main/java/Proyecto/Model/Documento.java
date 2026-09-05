package Proyecto.Model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Documento {
    // Atributos
    private int idDocumento;
    private TipoDocumento tipoDocumento;
    private Persona persona; // Cliente o Proveedor
    private Empleado empleado;
    private LocalDateTime fechaDocumento;
    private String numeroDocExterno;
    private int cantidad;
    private double subtotal;
    private double descuento;
    private double total;
    private String observaciones;
    private List<ItemCarrito> detalles;

    // Constructor sin parámetros
    public Documento() {
        this.detalles = new ArrayList<>();
        this.fechaDocumento = LocalDateTime.now();
        this.cantidad = 0;
        this.subtotal = 0.0;
        this.descuento = 0.0;
        this.total = 0.0;
    }

    // Constructor con parámetros
    public Documento(int idDocumento, TipoDocumento tipoDocumento, Persona persona, Empleado empleado,
            LocalDateTime fechaDocumento, String numeroDocExterno) {
        this.idDocumento = idDocumento;
        this.tipoDocumento = tipoDocumento;
        this.persona = persona;
        this.empleado = empleado;
        this.fechaDocumento = fechaDocumento;
        this.numeroDocExterno = numeroDocExterno;
        this.cantidad = 0;
        this.subtotal = 0.0;
        this.descuento = 0.0;
        this.total = 0.0;
        this.detalles = new ArrayList<>();
    }

    // Getters y Setters
    public int getIdDocumento() {
        return idDocumento;
    }

    public void setIdDocumento(int idDocumento) {
        if (idDocumento < 0) {
            throw new IllegalArgumentException("El ID no puede ser negativo");
        }
        this.idDocumento = idDocumento;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        if (tipoDocumento == null) {
            throw new IllegalArgumentException("El tipo de documento no puede ser nulo");
        }
        this.tipoDocumento = tipoDocumento;
    }

    public Persona getPersona() {
        return persona;
    }

    public void setPersona(Persona persona) {
        if (persona == null) {
            throw new IllegalArgumentException("La persona no puede ser nula");
        }
        this.persona = persona;
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

    public LocalDateTime getFechaDocumento() {
        return fechaDocumento;
    }

    public void setFechaDocumento(LocalDateTime fechaDocumento) {
        if (fechaDocumento == null) {
            throw new IllegalArgumentException("La fecha no puede ser nula");
        }
        this.fechaDocumento = fechaDocumento;
    }

    public String getNumeroDocExterno() {
        return numeroDocExterno;
    }

    public void setNumeroDocExterno(String numeroDocExterno) {
        this.numeroDocExterno = numeroDocExterno;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        if (cantidad < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa");
        }
        this.cantidad = cantidad;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        if (subtotal < 0) {
            throw new IllegalArgumentException("El subtotal no puede ser negativo");
        }
        this.subtotal = subtotal;
    }

    public double getDescuento() {
        return descuento;
    }

    public void setDescuento(double descuento) {
        if (descuento < 0) {
            throw new IllegalArgumentException("El descuento no puede ser negativo");
        }
        this.descuento = descuento;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        if (total < 0) {
            throw new IllegalArgumentException("El total no puede ser negativo");
        }
        this.total = total;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public List<ItemCarrito> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<ItemCarrito> detalles) {
        this.detalles = detalles != null ? detalles : new ArrayList<>();
    }

    // Métodos
    // Agrega un item al documento

    public void agregarDetalle(ItemCarrito item) {
        if (item == null) {
            throw new IllegalArgumentException("El ítem no puede ser nulo");
        }
        this.detalles.add(item);
        recalcularTotal();
    }

    // Elimina un ítem del documento

    public void eliminarDetalle(int idProducto) {
        this.detalles.removeIf(item -> item.getProducto().getIdProducto() == idProducto);
        recalcularTotal();
    }

    // Recalcula el total del documento basado en los detalles

    private void recalcularTotal() {
        this.total = 0.0;
        for (ItemCarrito item : detalles) {
            this.total += item.getSubtotal();
        }
    }

    // Verifica si el documento está vacío

    public boolean estaVacio() {
        return detalles.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("ID: %d, Tipo Doc: %s, Persona: %s, Fecha: %s, Total: %.2f",
                idDocumento,
                tipoDocumento != null ? tipoDocumento.getDescripcion() : "N/A",
                persona != null ? persona.getNombre() : "N/A",
                fechaDocumento,
                total);
    }
}
