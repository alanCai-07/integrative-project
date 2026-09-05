package Proyecto.dao;

import Proyecto.Model.Categoria;
import Proyecto.util.conexionBD;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoriaDAO {

    // Crear categoría
    public boolean crearCategoria(Categoria categoria) {
        String sql = "INSERT INTO categoria (nombre, descripcion, activo) VALUES (?, ?, ?)";

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setString(1, categoria.getNombre());
            pstmt.setString(2, categoria.getDescripcion());
            pstmt.setBoolean(3, true);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al crear categoría: " + e.getMessage());
        }
        return false;
    }

    // Obtener categoría por ID
    public Categoria obtenerCategoriaPorId(int idCategoria) {
        String sql = "SELECT * FROM categoria WHERE id_categoria = ?";

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, idCategoria);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapearCategoria(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener categoría: " + e.getMessage());
        }
        return null;
    }

    // Obtener todas las categorías
    public List<Categoria> obtenerTodasLasCategorias() {
        String sql = "SELECT * FROM categoria WHERE activo = true";
        List<Categoria> categorias = new ArrayList<>();

        try (Connection conexion = conexionBD.obtenerConexion();
             Statement stmt = conexion.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                categorias.add(mapearCategoria(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener categorías: " + e.getMessage());
        }
        return categorias;
    }

    // Actualizar categoría
    public boolean actualizarCategoria(Categoria categoria) {
        String sql = "UPDATE categoria SET nombre = ?, descripcion = ? WHERE id_categoria = ?";

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setString(1, categoria.getNombre());
            pstmt.setString(2, categoria.getDescripcion());
            pstmt.setInt(3, categoria.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar categoría: " + e.getMessage());
        }
        return false;
    }

    // Eliminar categoría
    public boolean eliminarCategoria(int idCategoria) {
        String sql = "UPDATE categoria SET activo = false WHERE id_categoria = ?";

        try (Connection conexion = conexionBD.obtenerConexion();
             PreparedStatement pstmt = conexion.prepareStatement(sql)) {

            pstmt.setInt(1, idCategoria);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar categoría: " + e.getMessage());
        }
        return false;
    }

    private Categoria mapearCategoria(ResultSet rs) throws SQLException {
        return new Categoria(
            rs.getInt("id_categoria"),
            rs.getString("nombre"),
            rs.getString("descripcion")
        );
    }
}
