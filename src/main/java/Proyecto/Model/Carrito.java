package Proyecto.Model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Carrito {

    // Atributos
    private int idCarrito;
    private Cliente cliente;
    private List<ItemCarrito> items;
    private double total;
    private LocalDateTime creadoEn;

    // Constructor sin parámetros
    public Carrito() {
        this.items = new ArrayList<>();
        this.total = 0.0;
        this.creadoEn = LocalDateTime.now();
    }

    // Constructor con parámetros
    public Carrito(int idCarrito, Cliente cliente) {
        this.idCarrito = idCarrito;
        this.cliente = cliente;
        this.items = new ArrayList<>();
        this.total = 0.0;
        this.creadoEn = LocalDateTime.now();
    }

    // Métodos
    public void agregarProducto(Producto producto, int cantidad) {
        if (cantidad <= 0)
            return;

        for (ItemCarrito item : items) {
            if (item.getProducto().getIdProducto() == producto.getIdProducto()) {
                item.actualizarCantidad(item.getCantidad() + cantidad);
                recalcularTotal();
                return;
            }
        }

        items.add(new ItemCarrito(producto, cantidad));
        recalcularTotal();
    }

    public void eliminarProducto(int idProducto) {
        items.removeIf(item -> item.getProducto().getIdProducto() == idProducto);
        recalcularTotal();
    }

    public boolean estaVacio() {
        return items.isEmpty();
    }

    public void vaciar() {
        items.clear();
        total = 0.0;
    }

    private void recalcularTotal() {
        total = 0.0;
        for (ItemCarrito item : items) {
            total += item.getSubtotal();
        }
    }

    public List<ItemCarrito> getItems() {
        return items;
    }

    public double getTotal() {
        return total;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException("El cliente no puede ser nulo");
        }
        this.cliente = cliente;
    }

    public int getIdCarrito() {
        return idCarrito;
    }

    public void setIdCarrito(int idCarrito) {
        if (idCarrito < 0) {
            throw new IllegalArgumentException("El ID no puede ser negativo");
        }
        this.idCarrito = idCarrito;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        if (creadoEn == null) {
            throw new IllegalArgumentException("La fecha no puede ser nula");
        }
        this.creadoEn = creadoEn;
    }

    @Override
    public String toString() {
        return String.format("ID: %d, Cliente: %s, Total: %.2f, Creado: %s, Items: %d",
                idCarrito, cliente != null ? cliente.getNombre() : "N/A", total, creadoEn, items.size());
    }
}