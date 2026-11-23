package Proyecto.Model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa el carrito de compras de un cliente registrado.
 *
 * @author Allan Matthew Caicedo Montaño
 * @author German Astudillo Astudillo
 */
public class Carrito {

    private int idCarrito;
    private int idCliente;
    private Cliente cliente;
    private Integer idDocumento;
    private String estado; // ABIERTO, CONVERTIDO, ABANDONADO
    private double total;
    private LocalDateTime creadoEn;
    private List<ItemCarrito> items = new ArrayList<>();

    public Carrito() {
        this.estado = "ABIERTO";
        this.creadoEn = LocalDateTime.now();
    }

    public Carrito(int idCarrito, Cliente cliente, LocalDateTime creadoEn) {
        this.idCarrito = idCarrito;
        this.cliente = cliente;
        if (cliente != null) {
            this.idCliente = cliente.getId();
        }
        this.creadoEn = creadoEn;
        this.estado = "ABIERTO";
    }

    public boolean estaVacio() {
        return items == null || items.isEmpty();
    }

    public void agregarProducto(Producto producto, int cantidad) {
        for (ItemCarrito item : items) {
            if (item.getProducto() != null && item.getProducto().getIdProducto() == producto.getIdProducto()) {
                item.setCantidad(item.getCantidad() + cantidad);
                recalcularTotal();
                return;
            }
        }
        ItemCarrito nuevo = new ItemCarrito();
        nuevo.setProducto(producto);
        nuevo.setCantidad(cantidad);
        items.add(nuevo);
        recalcularTotal();
    }

    public void eliminarProducto(int idProducto) {
        items.removeIf(item -> item.getProducto() != null && item.getProducto().getIdProducto() == idProducto);
        recalcularTotal();
    }

    public void vaciar() {
        items.clear();
        this.total = 0;
    }

    private void recalcularTotal() {
        double suma = 0;
        for (ItemCarrito item : items) {
            suma += item.getSubtotal();
        }
        this.total = suma;
    }

    public int getIdCarrito() { return idCarrito; }
    public void setIdCarrito(int idCarrito) { this.idCarrito = idCarrito; }

    public int getIdCliente() {
        return cliente != null ? cliente.getId() : idCliente;
    }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
        if (cliente != null) {
            this.idCliente = cliente.getId();
        }
    }

    public Integer getIdDocumento() { return idDocumento; }
    public void setIdDocumento(Integer idDocumento) { this.idDocumento = idDocumento; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }

    public List<ItemCarrito> getItems() { return items; }
    public void setItems(List<ItemCarrito> items) {
        this.items = items;
        recalcularTotal();
    }
}