package Proyecto.Controller;

import Proyecto.Model.Carrito;
import Proyecto.Model.ItemCarrito;
import Proyecto.Model.Producto;
import Proyecto.dao.CarritoDAO;
import Proyecto.dao.ItemCarritoDAO;
import Proyecto.dao.ProductoDAO;
import java.util.List;

public class CarritoController {

    private CarritoDAO carritoDAO;
    private ItemCarritoDAO itemCarritoDAO;
    private ProductoDAO productoDAO;

    public CarritoController() {
        this.carritoDAO = new CarritoDAO();
        this.itemCarritoDAO = new ItemCarritoDAO();
        this.productoDAO = new ProductoDAO();
    }

    // Crear carrito
    public int crearCarrito(int idCliente) {
        return carritoDAO.crearCarrito(idCliente);
    }

    // Obtener carrito activo del cliente
    public Carrito obtenerCarritoDelCliente(int idCliente) {
        return carritoDAO.obtenerCarritoActivoDelCliente(idCliente);
    }

    // Agregar producto al carrito
    public boolean agregarProductoAlCarrito(int idCliente, int idProducto, int cantidad) {
        // Validar producto existe
        Producto producto = productoDAO.obtenerProductoporId(idProducto);
        if (producto == null) {
            System.out.println("Producto no encontrado");
            return false;
        }

        // Validar stock disponible
        if (producto.getCantidad() < cantidad) {
            System.out.println("Stock insuficiente. Disponible: " + producto.getCantidad());
            return false;
        }

        // Obtener o crear carrito del cliente
        Carrito carrito = carritoDAO.obtenerCarritoActivoDelCliente(idCliente);
        if (carrito == null) {
            int idCarrito = crearCarrito(idCliente);
            if (idCarrito == -1) {
                System.out.println("Error al crear carrito");
                return false;
            }
            carrito = carritoDAO.obtenerCarritoPorId(idCarrito);
        }

        // Agregar item al carrito
        return itemCarritoDAO.agregarItemAlCarrito(carrito.getCliente().getId(), idProducto, cantidad);
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
        return itemCarritoDAO.eliminarItemDelCarrito(idItem);
    }

    // Actualizar cantidad del producto en carrito
    public boolean actualizarCantidadDelProducto(int idItem, int cantidad) {
        if (cantidad <= 0) {
            return itemCarritoDAO.eliminarItemDelCarrito(idItem);
        }
        return itemCarritoDAO.actualizarCantidadItem(idItem, cantidad);
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

    // Vaciar carrito
    public boolean vaciarCarrito(int idCliente) {
        Carrito carrito = carritoDAO.obtenerCarritoActivoDelCliente(idCliente);
        if (carrito == null) {
            return false;
        }
        return itemCarritoDAO.limpiarCarrito(carrito.getCliente().getId());
    }

    // Checkout (finalizar compra)
    public boolean realizarCheckout(int idCliente) {
        Carrito carrito = carritoDAO.obtenerCarritoActivoDelCliente(idCliente);

        if (carrito == null || carrito.estaVacio()) {
            System.out.println("El carrito está vacío");
            return false;
        }

        // Aquí iría la lógica para crear el documento de venta
        // y actualizar el inventario
        System.out.println("Compra procesada exitosamente");
        return true;
    }
}
