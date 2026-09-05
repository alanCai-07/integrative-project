package Proyecto.services;


import Proyecto.Model.ItemCarrito;
import Proyecto.Model.Producto;
import Proyecto.dao.ItemCarritoDAO;
import Proyecto.dao.ProductoDAO;
import java.util.List;

public class ItemCarritoServices {

    private ItemCarritoDAO itemCarritoDAO;
    private ProductoDAO productoDAO;

    public ItemCarritoServices() {
        this.itemCarritoDAO = new ItemCarritoDAO();
        this.productoDAO = new ProductoDAO();
    }

    // Agregar item al carrito
    public boolean agregarItemAlCarrito(int idCarrito, int idProducto, int cantidad) {
        // Validar cantidad
        if (cantidad <= 0) {
            System.out.println("Error: La cantidad debe ser mayor a 0");
            return false;
        }

        // Validar que el producto existe
        Producto producto = productoDAO.obtenerProductoporId(idProducto);
        if (producto == null) {
            System.out.println("Error: Producto no encontrado");
            return false;
        }

        // Validar stock disponible
        if (producto.getCantidad() < cantidad) {
            System.out.println("Error: Stock insuficiente. Disponible: " + producto.getCantidad());
            return false;
        }

        boolean agregado = itemCarritoDAO.agregarItemAlCarrito(idCarrito, idProducto, cantidad);
        if (agregado) {
            System.out.println("Item agregado al carrito: " + producto.getNombre() + " x" + cantidad);
        }
        return agregado;
    }

    // Obtener items del carrito
    public List<ItemCarrito> obtenerItemsDelCarrito(int idCarrito) {
        return itemCarritoDAO.obtenerItemsDelCarrito(idCarrito);
    }

    // Actualizar cantidad del item
    public boolean actualizarCantidadItem(int idItem, int nuevaCantidad) {
        // Validar nueva cantidad
        if (nuevaCantidad <= 0) {
            System.out.println("Error: La cantidad debe ser mayor a 0");
            return false;
        }

        boolean actualizado = itemCarritoDAO.actualizarCantidadItem(idItem, nuevaCantidad);
        if (actualizado) {
            System.out.println("Cantidad actualizada a: " + nuevaCantidad);
        }
        return actualizado;
    }

    // Eliminar item del carrito
    public boolean eliminarItemDelCarrito(int idItem) {
        boolean eliminado = itemCarritoDAO.eliminarItemDelCarrito(idItem);
        if (eliminado) {
            System.out.println("Item eliminado del carrito");
        }
        return eliminado;
    }

    // Limpiar carrito (eliminar todos los items)
    public boolean limpiarCarrito(int idCarrito) {
        boolean limpiado = itemCarritoDAO.limpiarCarrito(idCarrito);
        if (limpiado) {
            System.out.println("Carrito vaciado exitosamente");
        }
        return limpiado;
    }

    // Obtener cantidad total de items en el carrito
    public int obtenerCantidadTotalItems(int idCarrito) {
        return obtenerItemsDelCarrito(idCarrito).size();
    }

    // Obtener total del carrito
    public double obtenerTotalCarrito(int idCarrito) {
        List<ItemCarrito> items = obtenerItemsDelCarrito(idCarrito);
        double total = 0;

        for (ItemCarrito item : items) {
            total += item.getSubtotal();
        }

        return total;
    }

    // Validar si el carrito tiene items
    public boolean carritoTieneItems(int idCarrito) {
        return obtenerCantidadTotalItems(idCarrito) > 0;
    }

    // Generar resumen de items del carrito
    public String generarResumenItemsCarrito(int idCarrito) {
        List<ItemCarrito> items = obtenerItemsDelCarrito(idCarrito);

        if (items.isEmpty()) {
            return "El carrito está vacío";
        }

        StringBuilder resumen = new StringBuilder();
        resumen.append("╔════════════════════════════════════════╗\n");
        resumen.append("║      RESUMEN DE ITEMS DEL CARRITO     ║\n");
        resumen.append("╚════════════════════════════════════════╝\n\n");

        double total = 0;
        int item = 1;

        for (ItemCarrito ic : items) {
            Producto producto = ic.getProducto();
            double subtotal = ic.getSubtotal();
            total += subtotal;

            resumen.append(item).append(". ").append(producto.getNombre()).append("\n")
                    .append("   Cantidad: ").append(ic.getCantidad()).append("\n")
                    .append("   Precio unitario: $").append(String.format("%.2f", producto.getPrecioVenta()))
                    .append("\n")
                    .append("   Subtotal: $").append(String.format("%.2f", subtotal)).append("\n\n");

            item++;
        }

        resumen.append("─────────────────────────────────\n");
        resumen.append("TOTAL: $").append(String.format("%.2f", total)).append("\n");

        return resumen.toString();
    }

    // Incrementar cantidad de un item
    public boolean incrementarCantidadItem(int idItem, int cantidad) {
        if (cantidad <= 0) {
            System.out.println("Error: El incremento debe ser mayor a 0");
            return false;
        }
        // Obtener cantidad actual y sumar el incremento
        // Nota: Esto requeriría obtener el item actual, lo que no es posible con la
        // estructura actual
        System.out.println("Método requiere acceso al DAO para obtener cantidad actual");
        return false;
    }

    // Decrementar cantidad de un item
    public boolean decrementarCantidadItem(int idItem, int cantidad) {
        if (cantidad <= 0) {
            System.out.println("Error: El decremento debe ser mayor a 0");
            return false;
        }
        // Obtener cantidad actual y restar el decremento
        // Nota: Esto requeriría obtener el item actual, lo que no es posible con la
        // estructura actual
        System.out.println("Método requiere acceso al DAO para obtener cantidad actual");
        return false;
    }
}
