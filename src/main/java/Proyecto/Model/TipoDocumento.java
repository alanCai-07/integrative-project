package Proyecto.Model;

public class TipoDocumento {
    // Atributos
    private int idTipoDocumento;
    private String descripcion;
    private Boolean efectosEnInventario;

    // Constructor sin parámetros
    public TipoDocumento() {
        this.efectosEnInventario = false;
    }

    // Constructor con parámetros
    public TipoDocumento(int idTipoDocumento, String descripcion, Boolean efectosEnInventario) {
        this.idTipoDocumento = idTipoDocumento;
        this.descripcion = descripcion;
        this.efectosEnInventario = efectosEnInventario != null ? efectosEnInventario : false;
    }

    // Getters y Setters
    public int getIdTipoDocumento() {
        return idTipoDocumento;
    }

    public void setIdTipoDocumento(int idTipoDocumento) {
        if (idTipoDocumento < 0) {
            throw new IllegalArgumentException("El ID no puede ser negativo");
        }
        this.idTipoDocumento = idTipoDocumento;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción no puede estar vacía");
        }
        this.descripcion = descripcion.trim();
    }

    public Boolean getEfectosEnInventario() {
        return efectosEnInventario;
    }

    public void setEfectosEnInventario(Boolean efectosEnInventario) {
        this.efectosEnInventario = efectosEnInventario != null ? efectosEnInventario : false;
    }

    @Override
    public String toString() {
        return String.format("ID: %d, Descripción: %s, Efectos en Inventario: %s",
                idTipoDocumento, descripcion, efectosEnInventario);
    }
}
