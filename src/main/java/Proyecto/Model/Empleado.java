package Proyecto.Model;

import java.time.LocalDate;

/**
 * Representa un empleado interno del sistema TechZone con un cargo asignado.
 *
 * @author Allan Matthew Caicedo Montaño
 * @author German Astudillo Astudillo
 */
public class Empleado extends Persona {

    private Cargo cargo;
    private LocalDate fechaIngreso;
    private String passwordHash;

    public Empleado() {
        super();
    }

    public Cargo getCargo() {
        return cargo;
    }

    public void setCargo(Cargo cargo) {
        this.cargo = cargo;
    }

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}