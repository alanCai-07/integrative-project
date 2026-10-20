package Proyecto.View.Usuario;

import Proyecto.services.PersonaServices;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Vista de Registro de Nuevo Cliente — TechZone
 *
 * Abre como ventana modal. Cuando el registro es exitoso,
 * {@link #isRegistradoExitoso()} devuelve {@code true}.
 *
 * Uso desde LoginView:
 * 
 * <pre>
 *   RegistroView reg = new RegistroView(loginStage);
 *   if (reg.isRegistradoExitoso()) { ... }
 * </pre>
 */
public class RegistroView {

    // ── Campos del formulario ─────────────────────────────────────────────────
    private TextField txtNombres;
    private TextField txtApellidos;
    private TextField txtEmail;
    private TextField txtTelefono;
    private TextField txtDocumento;
    private TextField txtDireccion;
    private PasswordField txtContrasena;
    private PasswordField txtConfirmar;

    private Label lblMensaje;
    private Button btnRegistrar;
    private Button btnCancelar;

    private final PersonaServices personaServices;
    private Stage dialogStage;
    private boolean registradoExitoso = false;

    // ── Constructor ───────────────────────────────────────────────────────────
    public RegistroView(Window owner) {
        this.personaServices = new PersonaServices();
        build(owner);
    }

    // ── Construcción de la interfaz ───────────────────────────────────────────
    private void build(Window owner) {
        dialogStage = new Stage();
        dialogStage.setTitle("TechZone — Crear cuenta");
        dialogStage.setResizable(false);
        if (owner != null) {
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(owner);
        }

        // ── Panel izquierdo decorativo ────────────────────────────────────
        VBox leftPanel = new VBox(12);
        leftPanel.setAlignment(Pos.CENTER);
        leftPanel.setPrefWidth(240);
        leftPanel.setPadding(new Insets(40, 25, 40, 25));
        leftPanel.setStyle("-fx-background-color: #0A1933;");

        Label lblBrand = new Label("TECHZONE");
        lblBrand.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        lblBrand.setTextFill(Color.web("#00C8FF"));

        Label lblSub = new Label("GADGETS & HOBBIES");
        lblSub.setFont(Font.font("Arial", 13));
        lblSub.setTextFill(Color.web("#00C8FF"));

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #00C8FF40; -fx-pref-height: 1;");

        Label lblInfo = new Label("Crea tu cuenta y accede\na nuestro catálogo\npersonalizado.");
        lblInfo.setTextFill(Color.web("#AABBCC"));
        lblInfo.setFont(Font.font("Arial", 13));
        lblInfo.setWrapText(true);
        lblInfo.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        leftPanel.getChildren().addAll(lblBrand, lblSub, sep, lblInfo);

        // ── Formulario derecho ────────────────────────────────────────────
        GridPane grid = new GridPane();
        grid.setBackground(new Background(new BackgroundFill(Color.web("#0F1E37"), null, null)));
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(30, 35, 30, 35));

        ColumnConstraints col0 = new ColumnConstraints(130);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col0, col1);

        int fila = 0;

        // Título
        Label lblTitulo = new Label("CREAR CUENTA");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        lblTitulo.setTextFill(Color.WHITE);
        GridPane.setColumnSpan(lblTitulo, 2);
        GridPane.setHalignment(lblTitulo, HPos.CENTER);
        grid.add(lblTitulo, 0, fila++);

        // Separador
        Separator s2 = new Separator();
        s2.setStyle("-fx-background-color: #00C8FF40;");
        GridPane.setColumnSpan(s2, 2);
        grid.add(s2, 0, fila++);

        // Campos
        txtNombres = campo("Ej: Juan Esteban");
        txtApellidos = campo("Ej: Gaviria Martínez");
        txtEmail = campo("usuario@correo.com");
        txtTelefono = campo("Ej: 3001234567");
        txtDocumento = campo("Número de cédula / DNI");
        txtDireccion = campo("Calle / Barrio / Ciudad");
        txtContrasena = new PasswordField();
        txtConfirmar = new PasswordField();
        estilizarCampo(txtContrasena, "Mínimo 6 caracteres");
        estilizarCampo(txtConfirmar, "Repite tu contraseña");

        Object[][] rows = {
                { "Nombres *", txtNombres },
                { "Apellidos *", txtApellidos },
                { "Correo *", txtEmail },
                { "Teléfono", txtTelefono },
                { "Documento *", txtDocumento },
                { "Dirección", txtDireccion },
                { "Contraseña *", txtContrasena },
                { "Confirmar *", txtConfirmar },
        };

        for (Object[] row : rows) {
            Label lbl = etiqueta((String) row[0]);
            grid.add(lbl, 0, fila);
            grid.add((Control) row[1], 1, fila++);
        }

        // Mensaje de error / éxito
        lblMensaje = new Label("");
        lblMensaje.setFont(Font.font("Arial", 12));
        lblMensaje.setTextFill(Color.web("#FF4444"));
        lblMensaje.setWrapText(true);
        lblMensaje.setMaxWidth(300);
        GridPane.setColumnSpan(lblMensaje, 2);
        GridPane.setHalignment(lblMensaje, HPos.CENTER);
        grid.add(lblMensaje, 0, fila++);

        // Botones
        btnRegistrar = boton("CREAR CUENTA", "#00C8FF");
        btnCancelar = boton("CANCELAR", "#C83C3C");
        btnRegistrar.setOnAction(e -> procesarRegistro());
        btnCancelar.setOnAction(e -> dialogStage.close());

        HBox btnBox = new HBox(15, btnRegistrar, btnCancelar);
        btnBox.setAlignment(Pos.CENTER);
        GridPane.setColumnSpan(btnBox, 2);
        grid.add(btnBox, 0, fila);

        // ── Layout principal ──────────────────────────────────────────────
        HBox root = new HBox(leftPanel, grid);
        HBox.setHgrow(grid, Priority.ALWAYS);

        Scene scene = new Scene(root, 820, 620);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    // ── Lógica de registro ────────────────────────────────────────────────────
    private void procesarRegistro() {
        limpiarMensaje();

        // Validaciones básicas
        if (txtNombres.getText().trim().isEmpty()) {
            error("El campo Nombres es obligatorio.", txtNombres);
            return;
        }
        if (txtApellidos.getText().trim().isEmpty()) {
            error("El campo Apellidos es obligatorio.", txtApellidos);
            return;
        }
        if (txtEmail.getText().trim().isEmpty() || !txtEmail.getText().contains("@")) {
            error("Ingresa un correo electrónico válido.", txtEmail);
            return;
        }
        if (txtDocumento.getText().trim().isEmpty()) {
            error("El número de documento es obligatorio.", txtDocumento);
            return;
        }
        if (txtContrasena.getText().length() < 6) {
            error("La contraseña debe tener al menos 6 caracteres.", txtContrasena);
            return;
        }
        if (!txtContrasena.getText().equals(txtConfirmar.getText())) {
            error("Las contraseñas no coinciden.", txtConfirmar);
            return;
        }

        // Deshabilitar botón mientras se procesa
        btnRegistrar.setDisable(true);
        btnRegistrar.setText("Registrando...");

        String nombres = txtNombres.getText().trim();
        String apellidos = txtApellidos.getText().trim();
        String email = txtEmail.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String documento = txtDocumento.getText().trim();
        String direccion = txtDireccion.getText().trim();
        String password = txtContrasena.getText();

        new Thread(() -> {
            boolean ok;
            try {
                ok = personaServices.registrarCliente(
                        nombres, apellidos, email, telefono,
                        documento, password, direccion);
            } catch (Exception ex) {
                ok = false;
            }
            boolean exito = ok;
            Platform.runLater(() -> {
                btnRegistrar.setDisable(false);
                btnRegistrar.setText("CREAR CUENTA");
                if (exito) {
                    registradoExitoso = true;
                    exito("¡Cuenta creada exitosamente! Ya puedes iniciar sesión.");
                    // Cerrar automáticamente tras 1.5 segundos
                    new Thread(() -> {
                        try {
                            Thread.sleep(1500);
                        } catch (InterruptedException ignored) {
                        }
                        Platform.runLater(dialogStage::close);
                    }).start();
                } else {
                    error("No se pudo crear la cuenta. Verifica que el correo y documento no estén registrados.", null);
                }
            });
        }).start();
    }

    // ── Helpers visuales ─────────────────────────────────────────────────────
    private TextField campo(String prompt) {
        TextField tf = new TextField();
        estilizarCampo(tf, prompt);
        return tf;
    }

    private void estilizarCampo(Control c, String prompt) {
        if (c instanceof TextField tf)
            tf.setPromptText(prompt);
        else if (c instanceof PasswordField pf)
            pf.setPromptText(prompt);
        c.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #00C8FF;" +
                        "-fx-border-width: 1.5;" +
                        "-fx-padding: 8;" +
                        "-fx-font-size: 13px;");
    }

    private Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        l.setTextFill(Color.web("#AABBCC"));
        return l;
    }

    private Button boton(String texto, String color) {
        Button b = new Button(texto);
        b.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        b.setTextFill(Color.WHITE);
        b.setPrefSize(145, 40);
        b.setStyle("-fx-background-color: " + color + "; -fx-border-width: 0; -fx-cursor: hand;");
        return b;
    }

    private void error(String msg, Control foco) {
        lblMensaje.setTextFill(Color.web("#FF4444"));
        lblMensaje.setText(" " + msg);
        if (foco != null)
            foco.requestFocus();
    }

    private void exito(String msg) {
        lblMensaje.setTextFill(Color.web("#00C84B"));
        lblMensaje.setText(" " + msg);
    }

    private void limpiarMensaje() {
        lblMensaje.setText("");
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public boolean isRegistradoExitoso() {
        return registradoExitoso;
    }
}