package Proyecto.Model;

public class Categoria {
    private int idCategoria;
    private String nombre;
    private String descripcion;
    private Boolean activo;

    // Constructor sin parámetros
    public Categoria() {
        this.activo = true;
    }

    // Constructor con parámetros
    public Categoria(int idCategoria, String nombre, String descripcion) {
        this.idCategoria = idCategoria;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.activo = true;
    }

    // Getters y Setters
    public int getId() {
        return idCategoria;
    }

    public void setId(int idCategoria) {
        this.idCategoria = idCategoria;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo != null ? activo : true;
    }

    @Override
    public String toString() {
        return String.format("ID: %d, Nombre: %s, Activo: %s", idCategoria, nombre, activo);
    }
}
