package Proyecto.Controller;

import Proyecto.Model.Categoria;
import Proyecto.Model.Producto;
import Proyecto.dao.ProductoDAO;
import Proyecto.dao.CategoriaDAO;
import java.util.List;

public class ProductoController {

    private ProductoDAO productoDAO;
    private CategoriaDAO categoriaDAO;

    public ProductoController() {
        this.productoDAO = new ProductoDAO();
        this.categoriaDAO = new CategoriaDAO();
    }

    // Crear producto
    public boolean crearProducto(int idCategoria, String nombre, String descripcion,
                                 double precioCompra, double precioVenta, int cantidad) {
        // Validar datos
        if (nombre == null || nombre.trim().isEmpty() ||
            precioCompra <= 0 || precioVenta <= 0 || cantidad < 0) {
            System.out.println("Datos inválidos");
            return false;
        }

        // Obtener categoría
        Categoria categoria = categoriaDAO.obtenerCategoriaPorId(idCategoria);
        if (categoria == null) {
            System.out.println("Categoría no encontrada");
            return false;
        }

        // Crear producto
        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setCategoria(categoria);
        producto.setDescripcion(descripcion);
        producto.setPrecioCompra(precioCompra);
        producto.setPrecioVenta(precioVenta);
        producto.setCantidad(cantidad);

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
        return productoDAO.obtenerProductosPorCategoria(idCategoria);
    }

    // Buscar productos por nombre
    public List<Producto> buscarProductos(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return obtenerTodosLosProductos();
        }
        return productoDAO.buscarProductosPorNombre(nombre);
    }

    // Actualizar producto
    public boolean actualizarProducto(int idProducto, String nombre, String descripcion,
                                      double precioCompra, double precioVenta, int cantidad) {
        Producto producto = productoDAO.obtenerProductoporId(idProducto);

        if (producto == null) {
            System.out.println("Producto no encontrado");
            return false;
        }

        // Validar datos
        if (nombre == null || nombre.trim().isEmpty() ||
            precioCompra <= 0 || precioVenta <= 0 || cantidad < 0) {
            System.out.println("Datos inválidos");
            return false;
        }

        producto.setNombre(nombre);
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
            System.out.println("Producto no encontrado");
            return false;
        }

        return productoDAO.eliminarProducto(idProducto);
    }

    // Obtener todas las categorías
    public List<Categoria> obtenerCategorias() {
        return categoriaDAO.obtenerTodasLasCategorias();
    }

    // Crear categoría
    public boolean crearCategoria(String nombre, String descripcion) {
        if (nombre == null || nombre.trim().isEmpty()) {
            System.out.println("El nombre de la categoría no puede estar vacío");
            return false;
        }

        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setDescripcion(descripcion);

        return categoriaDAO.crearCategoria(categoria);
    }
}
