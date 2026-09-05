package Proyecto.View;

import javax.swing.*;
import java.awt.*;
import Proyecto.Controller.UsuarioController;

public class ViewGUI extends JFrame {

    private JTextField tfCorreo;
    private JPasswordField pfContrasena;
    private JButton btnLogin;
    private JButton btnRegistro;
    private JButton btnSalir;

    private JTextField tfRegNombre, tfRegApellido, tfRegCorreo;
    private JPasswordField pfRegPass;
    private JButton btnCrearRegistro, btnCancelarRegistro;

    private JPanel panelArea;

    private UsuarioController controller;

    public ViewGUI() {
        initComponents();
    }

    private void initComponents() {
        this.controller = new UsuarioController();

        setTitle("TechZone - Sistema");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        Color fondo = new Color(10, 25, 47);
        getContentPane().setBackground(fondo);

        // LEFT
        JPanel left = new JPanel(new GridBagLayout());
        left.setBackground(fondo);
        left.setPreferredSize(new Dimension(420, 0));

        JLabel lblLogo = new JLabel();
        ImageIcon icon = loadLogoIcon("resources/logo.jpg", 360, 360);

        if (icon != null) {
            lblLogo.setIcon(icon);
        } else {
            lblLogo.setText("<Logo>");
            lblLogo.setForeground(Color.WHITE);
        }

        left.add(lblLogo);

        // RIGHT
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(fondo);

        JTabbedPane tabs = new JTabbedPane();

        tabs.addTab("Login", crearPanelLogin(fondo));
        tabs.addTab("Registro", crearPanelRegistro(fondo));

        right.add(tabs, BorderLayout.NORTH);

        // PANEL ÁREA
        panelArea = new JPanel(new BorderLayout());
        panelArea.setBackground(fondo);

        JLabel lblWelcome = new JLabel("Bienvenido a TechZone");
        lblWelcome.setForeground(Color.WHITE);
        lblWelcome.setHorizontalAlignment(SwingConstants.CENTER);

        panelArea.add(lblWelcome, BorderLayout.CENTER);

        right.add(panelArea, BorderLayout.CENTER);

        add(left, BorderLayout.WEST);
        add(right, BorderLayout.CENTER);
    }

    // 🔹 LOGIN PANEL
    private JPanel crearPanelLogin(Color fondo) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(fondo);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel titulo = new JLabel("Iniciar Sesión");
        titulo.setForeground(Color.WHITE);
        p.add(titulo, gbc);

        gbc.gridwidth = 1;
        gbc.gridy++;

        p.add(new JLabel("Correo:"), gbc);
        gbc.gridx = 1;
        tfCorreo = new JTextField(20);
        p.add(tfCorreo, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        p.add(new JLabel("Contraseña:"), gbc);
        gbc.gridx = 1;
        pfContrasena = new JPasswordField(20);
        p.add(pfContrasena, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        btnLogin = new JButton("Login");
        p.add(btnLogin, gbc);

        gbc.gridx = 1;
        btnRegistro = new JButton("Registro");
        btnRegistro.addActionListener(e -> {
            // Acción al hacer clic en el botón de registro

            System.out.println("Botón de registro clickeado");
            controller.registrarCliente(getRegNombre(), getRegApellido(), getRegCorreo(), "", "DNI", getRegPassword(),
                    "");
        });
        p.add(btnRegistro, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        btnSalir = new JButton("Salir");
        p.add(btnSalir, gbc);

        return p;
    }

    // 🔹 REGISTRO PANEL
    private JPanel crearPanelRegistro(Color fondo) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(fondo);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 8, 8, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridy = 0;
        g.gridwidth = 2;
        p.add(new JLabel("Registro"), g);

        g.gridwidth = 1;
        g.gridy++;

        p.add(new JLabel("Nombre:"), g);
        g.gridx = 1;
        tfRegNombre = new JTextField(18);
        p.add(tfRegNombre, g);

        g.gridy++;
        g.gridx = 0;
        p.add(new JLabel("Apellido:"), g);
        g.gridx = 1;
        tfRegApellido = new JTextField(18);
        p.add(tfRegApellido, g);

        g.gridy++;
        g.gridx = 0;
        p.add(new JLabel("Correo:"), g);
        g.gridx = 1;
        tfRegCorreo = new JTextField(18);
        p.add(tfRegCorreo, g);

        g.gridy++;
        g.gridx = 0;
        p.add(new JLabel("Contraseña:"), g);
        g.gridx = 1;
        pfRegPass = new JPasswordField(18);
        p.add(pfRegPass, g);

        g.gridy++;
        g.gridx = 0;
        btnCrearRegistro = new JButton("Crear");
        p.add(btnCrearRegistro, g);

        g.gridx = 1;
        btnCancelarRegistro = new JButton("Cancelar");
        p.add(btnCancelarRegistro, g);

        return p;
    }

    // 🔹 MÉTODOS PARA CONTROLLER

    public String getCorreo() {
        return tfCorreo.getText();
    }

    public String getPassword() {
        return new String(pfContrasena.getPassword());
    }

    public String getRegNombre() {
        return tfRegNombre.getText();
    }

    public String getRegApellido() {
        return tfRegApellido.getText();
    }

    public String getRegCorreo() {
        return tfRegCorreo.getText();
    }

    public String getRegPassword() {
        return new String(pfRegPass.getPassword());
    }

    public JButton getBtnLogin() {
        return btnLogin;
    }

    public JButton getBtnRegistro() {
        return btnRegistro;
    }

    public JButton getBtnSalir() {
        return btnSalir;
    }

    public JButton getBtnCrearRegistro() {
        return btnCrearRegistro;
    }

    public JButton getBtnCancelarRegistro() {
        return btnCancelarRegistro;
    }

    // 🔹 UTILIDADES

    public void mostrarMensaje(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }

    public void limpiarLogin() {
        tfCorreo.setText("");
        pfContrasena.setText("");
    }

    public void limpiarRegistro() {
        tfRegNombre.setText("");
        tfRegApellido.setText("");
        tfRegCorreo.setText("");
        pfRegPass.setText("");
    }

    // 🔹 LOGO
    private ImageIcon loadLogoIcon(String path, int w, int h) {
        try {
            java.io.File f = new java.io.File(path);
            if (f.exists()) {
                ImageIcon original = new ImageIcon(f.getAbsolutePath());
                Image scaled = original.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        } catch (Exception e) {
            System.err.println("Error cargando logo");
        }
        return null;
    }
}