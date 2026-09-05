package Proyecto.services;

import Proyecto.Model.Carrito;
import Proyecto.Model.ItemCarrito;
import Proyecto.Model.Producto;
import Proyecto.dao.CarritoDAO;
import Proyecto.dao.ItemCarritoDAO;
import Proyecto.dao.ProductoDAO;
import java.util.List;

public class CarritoServices {

    private CarritoDAO carritoDAO;
    private ItemCarritoDAO itemCarritoDAO;
    private ProductoDAO productoDAO;

    public CarritoServices() {
        this.carritoDAO = new CarritoDAO();
        this.itemCarritoDAO = new ItemCarritoDAO();
        this.productoDAO = new ProductoDAO();
    }

    // Crear carrito para un cliente
    public int crearCarrito(int idCliente) {
        int idCarrito = carritoDAO.crearCarrito(idCliente);
        if (idCarrito != -1) {
            System.out.println("Carrito creado exitosamente para el cliente ID: " + idCliente);
        }
        return idCarrito;
    }

    // Obtener carrito activo del cliente
    public Carrito obtenerCarritoDelCliente(int idCliente) {
        Carrito carrito = carritoDAO.obtenerCarritoActivoDelCliente(idCliente);
        if (carrito == null) {
            System.out.println("No hay carrito activo para este cliente, creando uno nuevo...");
            int idCarrito = crearCarrito(idCliente);
            if (idCarrito != -1) {
                return carritoDAO.obtenerCarritoPorId(idCarrito);
            }
        }
        return carrito;
    }

    // Agregar producto al carrito
    public boolean agregarProductoAlCarrito(int idCliente, int idProducto, int cantidad) {
        // Validar cantidad
        if (cantidad <= 0) {
            System.out.println("Error: La cantidad debe ser mayor a 0");
            return false;
        }

        // Obtener producto
        Producto producto = productoDAO.obtenerProductoporId(idProducto);
        if (producto == null) {
            System.out.println("Error: Producto no encontrado");
            return false;
        }

        // Validar stock
        if (producto.getCantidad() < cantidad) {
            System.out.println("Error: Stock insuficiente. Disponible: " + producto.getCantidad());
            return false;
        }

        // Obtener o crear carrito
        Carrito carrito = obtenerCarritoDelCliente(idCliente);
        if (carrito == null) {
            System.out.println("Error: No se pudo obtener o crear el carrito");
            return false;
        }

        // Agregar item
        boolean agregado = itemCarritoDAO.agregarItemAlCarrito(
                carrito.getCliente().getId(),
                idProducto,
                cantidad);

        if (agregado) {
            System.out.println("Producto agregado al carrito: " + producto.getNombre() + " x" + cantidad);
        }

        return agregado;
    }

    // Obtener items del carrito
    public List<ItemCarrito> obtenerItemsDelCarrito(int idCliente) {
        Carrito carrito = carritoDAO.obtenerCarritoActivoDelCliente(idCliente);
        if (carrito == null) {
            return List.of();
        }
        return itemCarritoDAO.obtenerItemsDelCarrito(carrito.getCliente().getId());
    }

    // Quitar producto del carrito
    public boolean quitarProductoDelCarrito(int idItem) {
        boolean eliminado = itemCarritoDAO.eliminarItemDelCarrito(idItem);
        if (eliminado) {
            System.out.println("Producto eliminado del carrito");
        }
        return eliminado;
    }

    // Actualizar cantidad de producto en carrito
    public boolean actualizarCantidadDelProducto(int idItem, int nuevaCantidad) {
        if (nuevaCantidad <= 0) {
            System.out.println("Cantidad inválida, eliminando producto...");
            return quitarProductoDelCarrito(idItem);
        }

        boolean actualizado = itemCarritoDAO.actualizarCantidadItem(idItem, nuevaCantidad);
        if (actualizado) {
            System.out.println("Cantidad actualizada a: " + nuevaCantidad);
        }
        return actualizado;
    }

    // Obtener total del carrito
    public double obtenerTotalDelCarrito(int idCliente) {
        List<ItemCarrito> items = obtenerItemsDelCarrito(idCliente);
        double total = 0;

        for (ItemCarrito item : items) {
            total += item.getSubtotal();
        }

        return total;
    }

    // Obtener cantidad de items en el carrito
    public int obtenerCantidadItemsEnCarrito(int idCliente) {
        return obtenerItemsDelCarrito(idCliente).size();
    }

    // Vaciar carrito
    public boolean vaciarCarrito(int idCliente) {
        Carrito carrito = carritoDAO.obtenerCarritoActivoDelCliente(idCliente);
        if (carrito == null) {
            System.out.println("Error: No hay carrito para vaciar");
            return false;
        }

        boolean vaciado = itemCarritoDAO.limpiarCarrito(carrito.getCliente().getId());
        if (vaciado) {
            System.out.println("Carrito vaciado exitosamente");
        }
        return vaciado;
    }

    // Verificar si el carrito está vacío
    public boolean carritoEstaVacio(int idCliente) {
        return obtenerCantidadItemsEnCarrito(idCliente) == 0;
    }

    // Generar resumen del carrito
    public String generarResumenCarrito(int idCliente) {
        List<ItemCarrito> items = obtenerItemsDelCarrito(idCliente);

        if (items.isEmpty()) {
            return "El carrito está vacío";
        }

        StringBuilder resumen = new StringBuilder();
        resumen.append("=== RESUMEN DEL CARRITO ===\n");

        double total = 0;
        for (ItemCarrito item : items) {
            resumen.append(item.getProducto().getNombre())
                    .append(" x").append(item.getCantidad())
                    .append(" - $").append(item.getSubtotal())
                    .append("\n");
            total += item.getSubtotal();
        }

        resumen.append("---------------------------\n");
        resumen.append("TOTAL: $").append(total).append("\n");

        return resumen.toString();
    }

    // ── Alias de métodos para compatibilidad con vistas ────────────────────
    public List<ItemCarrito> obtenerItemsCarrito(int idCliente) {
        return obtenerItemsDelCarrito(idCliente);
    }

    public boolean eliminarItem(int idCliente, int idProducto) {
        // Buscar el item del carrito que corresponde al cliente y producto
        List<ItemCarrito> items = obtenerItemsDelCarrito(idCliente);
        for (ItemCarrito item : items) {
            if (item.getProducto().getIdProducto() == idProducto) {
                return quitarProductoDelCarrito(item.getId());
            }
        }
        return false;
    }
}
