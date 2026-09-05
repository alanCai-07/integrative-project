package Proyecto.Model;

public class Cargo {
    private String nombreCargo;
    private String descripcion;

    // Constructor
    public Cargo() {
    }

    public Cargo(String nombreCargo, String descripcion) {
        this.nombreCargo = nombreCargo;
        this.descripcion = descripcion;
    }

    // Getters y Setters
    public String getNombreCargo() {
        return nombreCargo;
    }

    public void setNombreCargo(String nombreCargo) {
        this.nombreCargo = nombreCargo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

}
