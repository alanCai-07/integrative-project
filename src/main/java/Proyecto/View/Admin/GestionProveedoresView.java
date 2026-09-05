package Proyecto.View.Admin;

import Proyecto.services.PersonaServices;
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

/**
 * Formulario modal para crear o editar un Proveedor — TechZone (Admin).
 *
 * Uso:
 * 
 * <pre>
 * new GestionProveedoresView(ownerStage, null, () -> refrescarTabla());
 * new GestionProveedoresView(ownerStage, rowData, () -> refrescarTabla());
 * </pre>
 */
public class GestionProveedoresView {

    private final PersonaServices personaServices;
    private final Runnable onGuardado;
    private final String[] datosActuales;

    private TextField txtEmpresa;
    private TextField txtNit;
    private TextField txtContacto;
    private TextField txtApellidos;
    private TextField txtEmail;
    private TextField txtTelefono;
    private TextField txtDireccion;

    private Label lblMensaje;
    private Stage stage;

    // ── Constructor ───────────────────────────────────────────────────────────
    public GestionProveedoresView(Stage owner, String[] datosActuales, Runnable onGuardado) {
        this.personaServices = new PersonaServices();
        this.datosActuales = datosActuales;
        this.onGuardado = onGuardado;
        build(owner);
    }

    // ── Construcción ─────────────────────────────────────────────────────────
    private void build(Stage owner) {
        stage = new Stage();
        stage.setTitle(datosActuales == null ? "Nuevo Proveedor" : "Editar Proveedor");
        stage.setResizable(false);
        stage.initModality(Modality.WINDOW_MODAL);
        if (owner != null)
            stage.initOwner(owner);

        GridPane grid = new GridPane();
        grid.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(25));

        ColumnConstraints col0 = new ColumnConstraints(150);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col0, col1);

        int fila = 0;

        Label lblTitulo = new Label(datosActuales == null ? "NUEVO PROVEEDOR" : "EDITAR PROVEEDOR");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        lblTitulo.setTextFill(Color.web("#0A1933"));
        GridPane.setColumnSpan(lblTitulo, 2);
        GridPane.setHalignment(lblTitulo, HPos.CENTER);
        grid.add(lblTitulo, 0, fila++);

        Separator sep = new Separator();
        GridPane.setColumnSpan(sep, 2);
        grid.add(sep, 0, fila++);

        txtEmpresa = campo("Razón social / Nombre de empresa");
        txtNit = campo("NIT de la empresa");
        txtContacto = campo("Nombre del contacto");
        txtApellidos = campo("Apellidos del contacto");
        txtEmail = campo("correo@empresa.com");
        txtTelefono = campo("Teléfono de contacto");
        txtDireccion = campo("Dirección o ciudad");

        Object[][] rows = {
                { "Empresa *", txtEmpresa },
                { "NIT *", txtNit },
                { "Contacto *", txtContacto },
                { "Apellidos", txtApellidos },
                { "Email *", txtEmail },
                { "Teléfono", txtTelefono },
                { "Dirección", txtDireccion },
        };

        for (Object[] r : rows) {
            grid.add(etiqueta((String) r[0]), 0, fila);
            grid.add((Control) r[1], 1, fila++);
        }

        // Pre-llenar al editar
        if (datosActuales != null) {
            txtEmpresa.setText(safe(datosActuales, 1));
            txtNit.setText(safe(datosActuales, 2));
            txtContacto.setText(safe(datosActuales, 3));
            txtEmail.setText(safe(datosActuales, 4));
            txtTelefono.setText(safe(datosActuales, 5));
        }

        // Mensaje
        lblMensaje = new Label("");
        lblMensaje.setFont(Font.font("Arial", 12));
        lblMensaje.setWrapText(true);
        lblMensaje.setMaxWidth(350);
        GridPane.setColumnSpan(lblMensaje, 2);
        GridPane.setHalignment(lblMensaje, HPos.CENTER);
        grid.add(lblMensaje, 0, fila++);

        // Botones
        Button btnGuardar = boton(datosActuales == null ? "REGISTRAR" : "ACTUALIZAR", "#00C8FF");
        Button btnCancelar = boton("CANCELAR", "#646464");
        btnGuardar.setOnAction(e -> guardar());
        btnCancelar.setOnAction(e -> stage.close());

        HBox btnBox = new HBox(15, btnGuardar, btnCancelar);
        btnBox.setAlignment(Pos.CENTER);
        GridPane.setColumnSpan(btnBox, 2);
        grid.add(btnBox, 0, fila);

        Scene scene = new Scene(grid, 480, 600);
        stage.setScene(scene);
        stage.showAndWait();
    }

    // ── Guardar ───────────────────────────────────────────────────────────────
    private void guardar() {
        limpiar();
        if (txtEmpresa.getText().trim().isEmpty()) {
            error("Empresa es obligatorio.", txtEmpresa);
            return;
        }
        if (txtNit.getText().trim().isEmpty()) {
            error("NIT es obligatorio.", txtNit);
            return;
        }
        if (txtContacto.getText().trim().isEmpty()) {
            error("Contacto es obligatorio.", txtContacto);
            return;
        }
        if (txtEmail.getText().trim().isEmpty()) {
            error("Email es obligatorio.", txtEmail);
            return;
        }

        try {
            // TODO: conectar con ProveedorServices.crear/actualizar(...)
            // personaServices.crearProveedor(
            // txtEmpresa.getText().trim(), txtNit.getText().trim(),
            // txtContacto.getText().trim(), txtApellidos.getText().trim(),
            // txtEmail.getText().trim(), txtTelefono.getText().trim(),
            // txtDireccion.getText().trim());

            new Alert(Alert.AlertType.INFORMATION,
                    "Proveedor " + (datosActuales == null ? "registrado" : "actualizado") + " correctamente.\n" +
                            "(Conecta el servicio de proveedores para persistencia en BD)",
                    ButtonType.OK).showAndWait();

            if (onGuardado != null)
                onGuardado.run();
            stage.close();
        } catch (Exception ex) {
            error("Error al guardar: " + ex.getMessage(), null);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private TextField campo(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-border-color: #C0C0C0; -fx-border-width: 1; -fx-padding: 8; -fx-font-size: 12px;");
        return tf;
    }

    private Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        return l;
    }

    private Button boton(String texto, String color) {
        Button b = new Button(texto);
        b.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        b.setTextFill(Color.WHITE);
        b.setPrefSize(130, 38);
        b.setStyle("-fx-background-color:" + color + ";-fx-border-width:0;-fx-cursor:hand;");
        return b;
    }

    private void error(String msg, Control foco) {
        lblMensaje.setTextFill(Color.web("#C83C3C"));
        lblMensaje.setText("⚠ " + msg);
        if (foco != null)
            foco.requestFocus();
    }

    private void limpiar() {
        lblMensaje.setText("");
    }

    private String safe(String[] arr, int i) {
        return (arr != null && arr.length > i && arr[i] != null) ? arr[i] : "";
    }
}