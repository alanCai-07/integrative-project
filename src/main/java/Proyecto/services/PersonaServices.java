package Proyecto.services;

import Proyecto.Model.Cliente;
import Proyecto.dao.PersonaDAO;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class PersonaServices {

    private PersonaDAO personaDAO;

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
            String telefono, String tipoDocumento,
            String password, String direccion) {

        if (personaDAO.emailExiste(email)) {
            System.out.println("Error: El email ya esta registrado");
            return false;
        }
        if (!validarDatos(nombre, apellido, email, password))
            return false;

        Cliente cliente = new Cliente();
        cliente.setNombre(nombre.trim());
        cliente.setApellido(apellido.trim());
        cliente.setEmail(email.toLowerCase().trim());
        cliente.setTelefono(telefono);
        cliente.setPasswordHash(encriptarPassword(password));
        cliente.setDireccion(direccion);

        return personaDAO.crearCliente(cliente);
    }

    public boolean crearEmpleado(String nombre, String apellido, String email,
            String telefono, String documento,
            String cargo, String password, double salario) {
        if (nombre == null || nombre.trim().isEmpty()) {
            System.out.println("Error: nombre requerido");
            return false;
        }
        if (apellido == null || apellido.trim().isEmpty()) {
            System.out.println("Error: apellido requerido");
            return false;
        }
        if (email == null || !email.contains("@")) {
            System.out.println("Error: email invalido");
            return false;
        }
        if (documento == null || documento.trim().isEmpty()) {
            System.out.println("Error: documento requerido");
            return false;
        }
        if (password == null || password.length() < 6) {
            System.out.println("Error: contrasena minimo 6 caracteres");
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

        String passwordHash = encriptarPassword(password);
        if (passwordHash == null)
            return false;

        return personaDAO.crearEmpleado(
                nombre, apellido,
                email.toLowerCase().trim(),
                telefono, documento,
                idCargo, passwordHash, salario);
    }

    /**
     * Actualiza los datos de un empleado existente.
     */
    public boolean actualizarEmpleado(int idPersona, String nombre, String apellido,
            String telefono, String cargo,
            double salario, boolean activo) {
        int idCargo = resolverIdCargo(cargo);
        if (idCargo == -1) {
            System.out.println("Error: cargo no reconocido: " + cargo);
            return false;
        }
        return personaDAO.actualizarEmpleado(idPersona, nombre, apellido, telefono, idCargo, salario, activo);
    }

    private int resolverIdCargo(String cargo) {
        if (cargo == null)
            return -1;
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
        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            System.out.println("Error: Email y contrasena son requeridos");
            return null;
        }

        String emailLower = email.toLowerCase().trim();
        String hash = encriptarPassword(password);

        Cliente cliente = personaDAO.obtenerClientePorEmail(emailLower);
        if (cliente != null && hash != null && hash.equals(cliente.getPasswordHash())) {
            cliente.setRol("CLIENTE");
            return cliente;
        }

        Cliente empleado = personaDAO.obtenerEmpleadoPorEmail(emailLower);
        if (empleado != null && hash != null && hash.equals(empleado.getPasswordHash())) {
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
        Cliente cliente = personaDAO.obtenerClientePorId(idCliente);
        if (cliente == null) {
            System.out.println("Error: Cliente no encontrado");
            return false;
        }
        if (!verificarPassword(passwordActual, cliente.getPasswordHash())) {
            System.out.println("Error: Contrasena actual incorrecta");
            return false;
        }
        if (passwordNueva == null || passwordNueva.length() < 6) {
            System.out.println("Error: La nueva contrasena debe tener al menos 6 caracteres");
            return false;
        }
        return personaDAO.actualizarPassword(idCliente, encriptarPassword(passwordNueva));
    }

    public boolean desactivarCliente(int idCliente) {
        return personaDAO.eliminarCliente(idCliente);
    }

    private boolean validarDatos(String nombre, String apellido, String email, String password) {
        if (nombre == null || nombre.trim().isEmpty()) {
            System.out.println("Error: nombre requerido");
            return false;
        }
        if (apellido == null || apellido.trim().isEmpty()) {
            System.out.println("Error: apellido requerido");
            return false;
        }
        if (email == null || !email.contains("@")) {
            System.out.println("Error: email invalido");
            return false;
        }
        if (password == null || password.length() < 6) {
            System.out.println("Error: contrasena minimo 6 caracteres");
            return false;
        }
        return true;
    }

    public String encriptarPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error al encriptar: " + e.getMessage());
            return null;
        }
    }

    private boolean verificarPassword(String password, String hashAlmacenado) {
        String hashIngresado = encriptarPassword(password);
        return hashIngresado != null && hashIngresado.equals(hashAlmacenado);
    }
}