package Proyecto.services;

import Proyecto.Model.Cliente;
import Proyecto.dao.PersonaDAO;
import java.util.List;

public class PersonaServices {

    private final PersonaDAO personaDAO;

    public PersonaServices() {
        this.personaDAO = new PersonaDAO();
    }

    public List<Cliente> buscarClientes(String query) {
        if (query == null || query.trim().isEmpty()) {
            return personaDAO.obtenerTodosLosClientes();
        }
        return personaDAO.buscarClientes(query.trim());
    }

    public boolean registrarCliente(String nombre, String apellido, String email,
            String telefono, String documento,
            String password, String direccion) {

        if (email == null || personaDAO.emailExiste(email.trim())) {
            System.out.println("Error: El email ya esta registrado");
            return false;
        }
        if (!validarDatos(nombre, apellido, email, password)) {
            return false;
        }

        Cliente cliente = new Cliente();
        cliente.setNombre(nombre.trim());
        cliente.setApellido(apellido.trim());
        cliente.setEmail(email.toLowerCase().trim());
        cliente.setTelefono(telefono);
        cliente.setDocumento(documento);
        cliente.setPasswordHash(password);
        cliente.setDireccion(direccion);

        return personaDAO.crearCliente(cliente);
    }

    public boolean registrarClienteParaCotizacion(String nombre, String apellido, String email,
            String documento, String telefono, String direccion) {
        if (nombre == null || nombre.trim().isEmpty()
                || apellido == null || apellido.trim().isEmpty()
                || documento == null || documento.trim().isEmpty()) {
            System.out.println("Error: nombre, apellido y documento son requeridos");
            return false;
        }

        String emailNormalizado = email == null ? "" : email.trim().toLowerCase();
        if (!emailNormalizado.isEmpty() && personaDAO.emailExiste(emailNormalizado)) {
            System.out.println("Error: el email ya esta registrado");
            return false;
        }

        Cliente cliente = new Cliente();
        cliente.setNombre(nombre.trim());
        cliente.setApellido(apellido.trim());
        cliente.setDocumento(documento.trim());
        cliente.setEmail(emailNormalizado.isEmpty() ? null : emailNormalizado);
        cliente.setTelefono(telefono == null ? "" : telefono.trim());
        cliente.setDireccion(direccion == null ? "" : direccion.trim());
        return personaDAO.crearCliente(cliente);
    }

    public boolean crearEmpleado(String nombre, String apellido, String email,
            String telefono, String documento,
            String cargo, String password, double salario) {
        if (nombre == null || nombre.trim().isEmpty()
                || apellido == null || apellido.trim().isEmpty()
                || email == null || !email.contains("@")
                || documento == null || documento.trim().isEmpty()
                || password == null || password.length() < 6) {
            return false;
        }
        if (personaDAO.emailExiste(email.toLowerCase().trim())) {
            System.out.println("Error: El email ya esta registrado");
            return false;
        }

        int idCargo = resolverIdCargo(cargo);
        if (idCargo == -1) {
            System.out.println("Error: cargo no reconocido: " + cargo);
            return false;
        }

        return personaDAO.crearEmpleado(
                nombre, apellido,
                email.toLowerCase().trim(),
                telefono, documento,
                idCargo, password);
    }

    public boolean actualizarEmpleado(int idPersona, String nombre, String apellido,
            String telefono, String cargo,
            double salario, boolean activo) {
        int idCargo = resolverIdCargo(cargo);
        if (idCargo == -1) {
            System.out.println("Error: cargo no reconocido: " + cargo);
            return false;
        }
        return personaDAO.actualizarEmpleado(idPersona, nombre, apellido, telefono, idCargo, activo);
    }

    public boolean crearProveedor(String empresa, String nit, String nombreContacto,
            String apellidosContacto, String email, String telefono, String direccion) {
        if (empresa == null || empresa.trim().isEmpty()
                || nombreContacto == null || nombreContacto.trim().isEmpty()
                || nit == null || nit.trim().isEmpty()) {
            return false;
        }
        return personaDAO.crearProveedor(empresa.trim(), nit.trim(), nombreContacto.trim(),
                apellidosContacto == null ? "" : apellidosContacto.trim(),
                email == null ? null : email.trim().toLowerCase(),
                telefono, direccion);
    }

    public boolean actualizarProveedor(int idPersona, String empresa, String nit,
            String nombreContacto, String apellidosContacto, String email,
            String telefono, String direccion) {
        if (idPersona <= 0 || empresa == null || empresa.trim().isEmpty()
                || nombreContacto == null || nombreContacto.trim().isEmpty()
                || nit == null || nit.trim().isEmpty()) {
            return false;
        }
        return personaDAO.actualizarProveedor(idPersona, empresa.trim(), nit.trim(),
                nombreContacto.trim(), apellidosContacto == null ? "" : apellidosContacto.trim(),
                email == null ? null : email.trim().toLowerCase(), telefono, direccion, true);
    }

    private int resolverIdCargo(String cargo) {
        if (cargo == null) {
            return -1;
        }
        return switch (cargo.trim().toUpperCase()) {
            case "ADMINISTRADOR" -> 1;
            case "COMPRADOR" -> 2;
            case "VENDEDOR" -> 3;
            case "CAJERO" -> 4;
            case "BODEGUERO" -> 5;
            default -> -1;
        };
    }

    public Cliente autenticarCliente(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isEmpty()) {
            System.out.println("Error: Email y contrasena son requeridos");
            return null;
        }

        String emailLower = email.toLowerCase().trim();

        Cliente cliente = personaDAO.obtenerCredencialesClientePorEmail(emailLower);
        if (cliente != null && PersonaDAO.verificarPassword(password, cliente.getPasswordHash())) {
            cliente.setPasswordHash(null);
            cliente.setRol("CLIENTE");
            return cliente;
        }

        Cliente empleado = personaDAO.obtenerEmpleadoPorEmail(emailLower);
        if (empleado != null && PersonaDAO.verificarPassword(password, empleado.getPasswordHash())) {
            empleado.setPasswordHash(null);
            return empleado;
        }

        System.out.println("Error: Credenciales incorrectas para " + emailLower);
        return null;
    }

    public Cliente obtenerCliente(int idCliente) {
        return personaDAO.obtenerClientePorId(idCliente);
    }

    public List<Cliente> obtenerTodosLosClientes() {
        return personaDAO.obtenerTodosLosClientes();
    }

    public boolean actualizarCliente(int idCliente, String nombre, String apellido,
            String telefono, String direccion) {
        Cliente cliente = personaDAO.obtenerClientePorId(idCliente);
        if (cliente == null) {
            System.out.println("Error: Cliente no encontrado");
            return false;
        }
        cliente.setNombre(nombre.trim());
        cliente.setApellido(apellido.trim());
        cliente.setTelefono(telefono);
        cliente.setDireccion(direccion);
        return personaDAO.actualizarCliente(cliente);
    }

    public boolean cambiarPassword(int idCliente, String passwordActual, String passwordNueva) {
        Cliente perfil = personaDAO.obtenerClientePorId(idCliente);
        if (perfil == null || perfil.getEmail() == null) {
            System.out.println("Error: Cliente no encontrado");
            return false;
        }
        Cliente credenciales = personaDAO.obtenerCredencialesClientePorEmail(perfil.getEmail());
        if (credenciales == null || !PersonaDAO.verificarPassword(passwordActual, credenciales.getPasswordHash())) {
            System.out.println("Error: Contrasena actual incorrecta");
            return false;
        }
        if (passwordNueva == null || passwordNueva.length() < 6) {
            System.out.println("Error: La nueva contrasena debe tener al menos 6 caracteres");
            return false;
        }
        return personaDAO.actualizarPassword(idCliente, passwordNueva);
    }

    public boolean desactivarCliente(int idCliente) {
        return personaDAO.eliminarCliente(idCliente);
    }

    private boolean validarDatos(String nombre, String apellido, String email, String password) {
        return nombre != null && !nombre.trim().isEmpty()
                && apellido != null && !apellido.trim().isEmpty()
                && email != null && email.contains("@")
                && password != null && password.length() >= 6;
    }
}