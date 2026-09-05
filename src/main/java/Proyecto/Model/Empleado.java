package Proyecto.Model;

import java.time.LocalDateTime;

public class Empleado extends Persona {
    // Atributos
    private Cargo cargo;
    private LocalDateTime fechaIngreso;
    private String passwordHash;

    // Constructor sin parámetros
    public Empleado() {
        super();
    }

    // Constructor con parámetros
    public Empleado(int id, String nombre, String apellido, String documento, String telefono,
            String email, String direccion, Cargo cargo, String passwordHash) {
        super(id, nombre, apellido, documento, telefono, email, direccion);
        this.cargo = cargo;
        this.fechaIngreso = LocalDateTime.now();
        this.passwordHash = passwordHash;
    }

    // Getters y Setters
    public Cargo getCargo() {
        return cargo;
    }

    public void setCargo(Cargo cargo) {
        if (cargo == null) {
            throw new IllegalArgumentException("El cargo no puede ser nulo");
        }
        this.cargo = cargo;
    }

    public LocalDateTime getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDateTime fechaIngreso) {
        if (fechaIngreso == null) {
            throw new IllegalArgumentException("La fecha de ingreso no puede ser nula");
        }
        this.fechaIngreso = fechaIngreso;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.trim().isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }
        this.passwordHash = passwordHash.trim();
    }

    @Override
    public String toString() {
        return String.format("ID: %d, Nombre: %s, Apellido: %s, Cargo: %s, Fecha Ingreso: %s, Activo: %s",
                id, nombre, apellido, cargo != null ? cargo.getNombreCargo() : "N/A", fechaIngreso, activo);
    }
}
