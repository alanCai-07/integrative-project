package Proyecto.services;


import Proyecto.Model.Categoria;
import Proyecto.Model.Producto;
import Proyecto.dao.ProductoDAO;
import Proyecto.dao.CategoriaDAO;
import java.util.ArrayList;
import java.util.List;

public class ProductoServices {

    private ProductoDAO productoDAO;
    private CategoriaDAO categoriaDAO;

    public ProductoServices() {
        this.productoDAO = new ProductoDAO();
        this.categoriaDAO = new CategoriaDAO();
    }

    // Crear producto con validaciones
    public boolean crearProducto(int idCategoria, String nombre, String descripcion,
                                 double precioCompra, double precioVenta, int stockInicial) {

        // Validar categoría
        Categoria categoria = categoriaDAO.obtenerCategoriaPorId(idCategoria);
        if (categoria == null) {
            System.out.println("Error: Categoría no encontrada");
            return false;
        }

        // Validar datos del producto
        if (!validarProducto(nombre, precioCompra, precioVenta, stockInicial)) {
            return false;
        }

        // Crear producto
        Producto producto = new Producto();
        producto.setNombre(nombre.trim());
        producto.setCategoria(categoria);
        producto.setDescripcion(descripcion);
        producto.setPrecioCompra(precioCompra);
        producto.setPrecioVenta(precioVenta);
        producto.setCantidad(stockInicial);

        return productoDAO.crearProducto(producto);
    }

    // Obtener producto por ID
    public Producto obtenerProducto(int idProducto) {
        return productoDAO.obtenerProductoporId(idProducto);
    }

    // Obtener todos los productos
    public List<Producto> obtenerTodosLosProductos() {
        return productoDAO.obtenerTodosProductos();
    }

    // Obtener productos por categoría
    public List<Producto> obtenerProductosPorCategoria(int idCategoria) {
        Categoria categoria = categoriaDAO.obtenerCategoriaPorId(idCategoria);
        if (categoria == null) {
            System.out.println("Error: Categoría no encontrada");
            return new ArrayList<>();
        }
        return productoDAO.obtenerProductosPorCategoria(idCategoria);
    }

    // Buscar productos por nombre
    public List<Producto> buscarProductos(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return obtenerTodosLosProductos();
        }
        return productoDAO.buscarProductosPorNombre(nombre.trim());
    }

    // Actualizar producto
    public boolean actualizarProducto(int idProducto, String nombre, String descripcion,
                                      double precioCompra, double precioVenta, int cantidad) {

        Producto producto = productoDAO.obtenerProductoporId(idProducto);
        if (producto == null) {
            System.out.println("Error: Producto no encontrado");
            return false;
        }

        // Validar datos
        if (!validarProducto(nombre, precioCompra, precioVenta, cantidad)) {
            return false;
        }

        producto.setNombre(nombre.trim());
        producto.setDescripcion(descripcion);
        producto.setPrecioCompra(precioCompra);
        producto.setPrecioVenta(precioVenta);
        producto.setCantidad(cantidad);

        return productoDAO.actualizarProducto(producto);
    }

    // Eliminar producto
    public boolean eliminarProducto(int idProducto) {
        Producto producto = productoDAO.obtenerProductoporId(idProducto);
        if (producto == null) {
            System.out.println("Error: Producto no encontrado");
            return false;
        }
        return productoDAO.eliminarProducto(idProducto);
    }

    // Obtener productos con bajo stock
    public List<Producto> obtenerProductosConBajoStock() {
        List<Producto> todos = obtenerTodosLosProductos();
        List<Producto> bajoStock = new ArrayList<>();

        for (Producto p : todos) {
            if (p.getCantidad() <= 10) {
                bajoStock.add(p);
            }
        }

        return bajoStock;
    }

    // Calcular ganancia por producto
    public double calcularGanancia(int idProducto) {
        Producto producto = productoDAO.obtenerProductoporId(idProducto);
        if (producto == null) {
            return 0;
        }
        return (producto.getPrecioVenta() - producto.getPrecioCompra()) * producto.getCantidad();
    }

    // Validar datos del producto
    private boolean validarProducto(String nombre, double precioCompra,
                                    double precioVenta, int cantidad) {

        if (nombre == null || nombre.trim().isEmpty()) {
            System.out.println("Error: El nombre del producto es requerido");
            return false;
        }

        if (precioCompra <= 0) {
            System.out.println("Error: El precio de compra debe ser mayor a 0");
            return false;
        }

        if (precioVenta <= 0) {
            System.out.println("Error: El precio de venta debe ser mayor a 0");
            return false;
        }

        if (precioVenta < precioCompra) {
            System.out.println("Error: El precio de venta no puede ser menor al precio de compra");
            return false;
        }

        if (cantidad < 0) {
            System.out.println("Error: La cantidad no puede ser negativa");
            return false;
        }

        return true;
    }
}
