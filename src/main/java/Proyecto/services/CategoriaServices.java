package Proyecto.services;

import Proyecto.Model.Categoria;
import Proyecto.dao.CategoriaDAO;
import java.util.List;

public class CategoriaServices {

    private CategoriaDAO categoriaDAO;

    public CategoriaServices() {
        this.categoriaDAO = new CategoriaDAO();
    }

    // Crear categoría con validaciones
    public boolean crearCategoria(String nombre, String descripcion) {
        // Validar datos
        if (nombre == null || nombre.trim().isEmpty()) {
            System.out.println("Error: El nombre de la categoría es requerido");
            return false;
        }

        // Crear categoría
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre.trim());
        categoria.setDescripcion(descripcion != null ? descripcion : "");

        boolean creada = categoriaDAO.crearCategoria(categoria);
        if (creada) {
            System.out.println("Categoría creada exitosamente: " + nombre);
        }
        return creada;
    }

    // Obtener categoría por ID
    public Categoria obtenerCategoria(int idCategoria) {
        return categoriaDAO.obtenerCategoriaPorId(idCategoria);
    }

    // Obtener todas las categorías
    public List<Categoria> obtenerTodasLasCategorias() {
        return categoriaDAO.obtenerTodasLasCategorias();
    }

    // Actualizar categoría
    public boolean actualizarCategoria(int idCategoria, String nombre, String descripcion) {
        Categoria categoria = categoriaDAO.obtenerCategoriaPorId(idCategoria);

        if (categoria == null) {
            System.out.println("Error: Categoría no encontrada");
            return false;
        }

        // Validar datos
        if (nombre == null || nombre.trim().isEmpty()) {
            System.out.println("Error: El nombre de la categoría es requerido");
            return false;
        }

        categoria.setNombre(nombre.trim());
        categoria.setDescripcion(descripcion != null ? descripcion : "");

        boolean actualizada = categoriaDAO.actualizarCategoria(categoria);
        if (actualizada) {
            System.out.println("Categoría actualizada: " + nombre);
        }
        return actualizada;
    }

    // Eliminar categoría
    public boolean eliminarCategoria(int idCategoria) {
        Categoria categoria = categoriaDAO.obtenerCategoriaPorId(idCategoria);

        if (categoria == null) {
            System.out.println("Error: Categoría no encontrada");
            return false;
        }

        boolean eliminada = categoriaDAO.eliminarCategoria(idCategoria);
        if (eliminada) {
            System.out.println("Categoría eliminada: " + categoria.getNombre());
        }
        return eliminada;
    }

    // Verificar si una categoría existe
    public boolean categoriaExiste(int idCategoria) {
        return categoriaDAO.obtenerCategoriaPorId(idCategoria) != null;
    }

    // Obtener cantidad de categorías
    public int obtenerCantidadCategorias() {
        return obtenerTodasLasCategorias().size();
    }
}
