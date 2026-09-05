package Proyecto.View.Usuario;

import Proyecto.Model.Cliente;
import Proyecto.services.PersonaServices;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

// Vista del Perfil del Cliente — TechZone

public class PerfilView {

    private final Cliente cliente;
    private final PersonaServices personaServices;

    // Campos de datos personales
    private TextField txtNombres;
    private TextField txtApellidos;
    private TextField txtEmail;
    private TextField txtTelefono;
    private TextField txtDireccion;

    // Campos de cambio de contraseña
    private PasswordField txtPassActual;
    private PasswordField txtPassNueva;
    private PasswordField txtPassConfirmar;

    private Label lblEstado;
    private VBox root;

    // ── Constructor ───────────────────────────────────────────────────────────
    public PerfilView(Cliente cliente) {
        this.cliente = cliente;
        this.personaServices = new PersonaServices();
        build();
        cargarDatos();
    }

    public Node getRoot() {
        return root;
    }

    // ── Construcción de la interfaz ───────────────────────────────────────────
    private void build() {
        root = new VBox(20);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: white;");
        VBox.setVgrow(root, Priority.ALWAYS);

        // ── Encabezado ────────────────────────────────────────────────────
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 10, 0));
        header.setStyle("-fx-border-color: #E0E0E0; -fx-border-width: 0 0 2 0;");

        Label lblTitulo = new Label("👤  Mi Perfil");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        lblTitulo.setTextFill(Color.web("#0A1933"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnGuardar = boton("  Guardar cambios", "#00C8FF");
        btnGuardar.setOnAction(e -> guardarCambios());

        header.getChildren().addAll(lblTitulo, spacer, btnGuardar);

        // ── Mensaje de estado ─────────────────────────────────────────────
        lblEstado = new Label("");
        lblEstado.setFont(Font.font("Arial", 12));
        lblEstado.setManaged(false);
        lblEstado.setVisible(false);

        // ── Sección: Datos personales ─────────────────────────────────────
        VBox seccionDatos = seccion("Datos Personales");

        GridPane gridDatos = grid();

        txtNombres = campo("Nombres");
        txtApellidos = campo("Apellidos");
        txtEmail = campo("Correo electrónico");
        txtTelefono = campo("Teléfono");
        txtDireccion = campo("Dirección");

        // Email no editable (es el identificador)
        txtEmail.setEditable(false);
        txtEmail.setStyle(txtEmail.getStyle() + "-fx-background-color: #F5F5F5;");

        Object[][] camposDatos = {
                { "Nombres:", txtNombres, 0 },
                { "Apellidos:", txtApellidos, 0 },
                { "Correo:", txtEmail, 1 },
                { "Teléfono:", txtTelefono, 1 },
                { "Dirección:", txtDireccion, 2 },
        };

        int fila = 0;
        for (Object[] c : camposDatos) {
            Label lbl = etiqueta((String) c[0]);
            gridDatos.add(lbl, 0, fila);
            gridDatos.add((Control) c[1], 1, fila);
            fila++;
        }

        seccionDatos.getChildren().add(gridDatos);

        // ── Sección: Cambiar contraseña ───────────────────────────────────
        VBox seccionPass = seccion("Cambiar Contraseña");

        GridPane gridPass = grid();

        txtPassActual = passField("Contraseña actual");
        txtPassNueva = passField("Nueva contraseña (mín. 6 caracteres)");
        txtPassConfirmar = passField("Confirmar nueva contraseña");

        gridPass.add(etiqueta("Contraseña actual:"), 0, 0);
        gridPass.add(txtPassActual, 1, 0);
        gridPass.add(etiqueta("Nueva contraseña:"), 0, 1);
        gridPass.add(txtPassNueva, 1, 1);
        gridPass.add(etiqueta("Confirmar:"), 0, 2);
        gridPass.add(txtPassConfirmar, 1, 2);

        Button btnCambiarPass = boton("Actualizar contraseña", "#0A1933");
        btnCambiarPass.setOnAction(e -> cambiarContrasena());
        HBox btnPassBox = new HBox(btnCambiarPass);
        btnPassBox.setPadding(new Insets(5, 0, 0, 0));
        btnPassBox.setAlignment(Pos.CENTER_LEFT);

        seccionPass.getChildren().addAll(gridPass, btnPassBox);

        // ── Info adicional (solo lectura) ─────────────────────────────────
        VBox seccionInfo = seccion("Información de Cuenta");
        GridPane gridInfo = grid();

        String idStr = cliente != null ? String.valueOf(cliente.getId()) : "—";
        String regStr = cliente != null && cliente.getFechaRegistro() != null
                ? cliente.getFechaRegistro().toString()
                : "—";

        gridInfo.add(etiqueta("ID de cliente:"), 0, 0);
        gridInfo.add(infoLabel(idStr), 1, 0);
        gridInfo.add(etiqueta("Fecha de registro:"), 0, 1);
        gridInfo.add(infoLabel(regStr), 1, 1);
        gridInfo.add(etiqueta("Tipo de cuenta:"), 0, 2);
        gridInfo.add(infoLabel("Cliente"), 1, 2);

        seccionInfo.getChildren().add(gridInfo);

        // ── Ensamble final ────────────────────────────────────────────────
        ScrollPane scroll = new ScrollPane(
                new VBox(20, seccionDatos, seccionPass, seccionInfo));
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: white; -fx-background-color: white;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        root.getChildren().addAll(header, lblEstado, scroll);
    }

    // ── Carga de datos ────────────────────────────────────────────────────────
    private void cargarDatos() {
        if (cliente == null)
            return;
        txtNombres.setText(cliente.getNombre() != null ? cliente.getNombre() : "");
        txtApellidos.setText(cliente.getApellido() != null ? cliente.getApellido() : "");
        txtEmail.setText(cliente.getEmail() != null ? cliente.getEmail() : "");
        txtTelefono.setText(cliente.getTelefono() != null ? cliente.getTelefono() : "");
        txtDireccion.setText(cliente.getDireccion() != null ? cliente.getDireccion() : "");
    }

    // ── Guardar cambios personales ────────────────────────────────────────────
    private void guardarCambios() {
        limpiarEstado();

        if (txtNombres.getText().trim().isEmpty() || txtApellidos.getText().trim().isEmpty()) {
            estado(" Nombres y Apellidos son obligatorios.", false);
            return;
        }

        if (cliente == null) {
            estado(" No hay un cliente cargado.", false);
            return;
        }

        new Thread(() -> {
            boolean ok;
            try {
                ok = personaServices.actualizarCliente(
                        cliente.getId(),
                        txtNombres.getText().trim(),
                        txtApellidos.getText().trim(),
                        txtTelefono.getText().trim(),
                        txtDireccion.getText().trim());
                if (ok) {
                    // Actualizar modelo local
                    cliente.setNombre(txtNombres.getText().trim());
                    cliente.setApellido(txtApellidos.getText().trim());
                    cliente.setTelefono(txtTelefono.getText().trim());
                    cliente.setDireccion(txtDireccion.getText().trim());
                }
            } catch (Exception ex) {
                ok = false;
            }
            boolean exito = ok;
            Platform.runLater(() -> {
                if (exito)
                    estado(" Datos actualizados correctamente.", true);
                else
                    estado(" Error al actualizar. Intenta de nuevo.", false);
            });
        }).start();
    }

    // ── Cambiar contraseña ────────────────────────────────────────────────────
    private void cambiarContrasena() {
        limpiarEstado();
        String actual = txtPassActual.getText();
        String nueva = txtPassNueva.getText();
        String confirmar = txtPassConfirmar.getText();

        if (actual.isEmpty() || nueva.isEmpty() || confirmar.isEmpty()) {
            estado(" Completa todos los campos de contraseña.", false);
            return;
        }
        if (nueva.length() < 6) {
            estado(" La nueva contraseña debe tener al menos 6 caracteres.", false);
            return;
        }
        if (!nueva.equals(confirmar)) {
            estado(" La nueva contraseña y la confirmación no coinciden.", false);
            return;
        }
        if (cliente == null) {
            estado(" No hay un cliente cargado.", false);
            return;
        }

        new Thread(() -> {
            boolean ok;
            try {
                ok = personaServices.cambiarPassword(
                        cliente.getId(), actual, nueva);
            } catch (Exception ex) {
                ok = false;
            }
            boolean exito = ok;
            Platform.runLater(() -> {
                if (exito) {
                    estado(" Contraseña cambiada exitosamente.", true);
                    txtPassActual.clear();
                    txtPassNueva.clear();
                    txtPassConfirmar.clear();
                } else {
                    estado(" Contraseña actual incorrecta o error al actualizar.", false);
                }
            });
        }).start();
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────
    private VBox seccion(String titulo) {
        VBox sec = new VBox(12);
        sec.setPadding(new Insets(18));
        sec.setStyle(
                "-fx-background-color: #FAFAFA;" +
                        "-fx-border-color: #E0E0E0;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 4;");

        Label lbl = new Label(titulo);
        lbl.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        lbl.setTextFill(Color.web("#0A1933"));
        sec.getChildren().add(lbl);
        return sec;
    }

    private GridPane grid() {
        GridPane g = new GridPane();
        g.setHgap(15);
        g.setVgap(10);
        ColumnConstraints c0 = new ColumnConstraints(160);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c0, c1);
        return g;
    }

    private TextField campo(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setFont(Font.font("Arial", 13));
        tf.setStyle("-fx-border-color: #C0C0C0; -fx-border-width: 1; -fx-padding: 8;");
        return tf;
    }

    private PasswordField passField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.setFont(Font.font("Arial", 13));
        pf.setStyle("-fx-border-color: #C0C0C0; -fx-border-width: 1; -fx-padding: 8;");
        return pf;
    }

    private Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        l.setTextFill(Color.web("#555555"));
        return l;
    }

    private Label infoLabel(String texto) {
        Label l = new Label(texto);
        l.setFont(Font.font("Arial", 13));
        l.setTextFill(Color.web("#0A1933"));
        return l;
    }

    private Button boton(String texto, String color) {
        Button b = new Button(texto);
        b.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        b.setTextFill(Color.WHITE);
        b.setPrefHeight(38);
        b.setStyle(
                "-fx-background-color: " + color + "; -fx-border-width: 0; -fx-cursor: hand; -fx-padding: 0 18 0 18;");
        return b;
    }

    private void estado(String msg, boolean exito) {
        lblEstado.setText(msg);
        lblEstado.setTextFill(exito ? Color.web("#1A8A2A") : Color.web("#C83C3C"));
        lblEstado.setVisible(true);
        lblEstado.setManaged(true);
    }

    private void limpiarEstado() {
        lblEstado.setText("");
        lblEstado.setVisible(false);
        lblEstado.setManaged(false);
    }
}