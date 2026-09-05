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

public class GestionEmpleadosView {

    private final PersonaServices personaServices;
    private final Runnable onGuardado;
    private final String[] datosActuales;

    private TextField txtNombres;
    private TextField txtApellidos;
    private TextField txtEmail;
    private TextField txtTelefono;
    private TextField txtDocumento;
    private TextField txtSalario;
    private ComboBox<String> cbCargo;
    private PasswordField txtContrasena;

    private Label lblMensaje;
    private Stage stage;

    // datosActuales: null = nuevo, non-null = editar
    // Indices del array cuando se edita:
    // [0]=id_persona, [1]=nombres, [2]=apellidos, [3]=email, [4]=cargo, [5]=estado
    public GestionEmpleadosView(Stage owner, String[] datosActuales, Runnable onGuardado) {
        this.personaServices = new PersonaServices();
        this.datosActuales = datosActuales;
        this.onGuardado = onGuardado;
        build(owner);
    }

    private void build(Stage owner) {
        stage = new Stage();
        stage.setTitle(datosActuales == null ? "Nuevo Empleado" : "Editar Empleado");
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

        Label lblTitulo = new Label(datosActuales == null ? "NUEVO EMPLEADO" : "EDITAR EMPLEADO");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        lblTitulo.setTextFill(Color.web("#0A1933"));
        GridPane.setColumnSpan(lblTitulo, 2);
        GridPane.setHalignment(lblTitulo, HPos.CENTER);
        grid.add(lblTitulo, 0, fila++);

        Separator sep = new Separator();
        GridPane.setColumnSpan(sep, 2);
        grid.add(sep, 0, fila++);

        txtNombres = campo("Nombres completos");
        txtApellidos = campo("Apellidos completos");
        txtEmail = campo("correo@techzone.co");
        txtTelefono = campo("Ej: 3001234567");
        txtDocumento = campo("Cedula / DNI");
        txtSalario = campo("Salario mensual (sin puntos)");

        txtContrasena = new PasswordField();
        txtContrasena.setPromptText(datosActuales == null
                ? "Contrasena inicial (min. 6 caracteres)"
                : "Dejar en blanco para no cambiar");
        estilo(txtContrasena);

        cbCargo = new ComboBox<>();
        cbCargo.getItems().addAll("Administrador", "Comprador", "Vendedor", "Cajero", "Bodeguero");
        cbCargo.getSelectionModel().select("Vendedor");
        cbCargo.setMaxWidth(Double.MAX_VALUE);

        Object[][] rows = {
                { "Nombres *", txtNombres },
                { "Apellidos *", txtApellidos },
                { "Email *", txtEmail },
                { "Telefono", txtTelefono },
                { "Documento *", txtDocumento },
                { "Cargo *", cbCargo },
                { "Salario", txtSalario },
                { "Contrasena", txtContrasena },
        };

        for (Object[] r : rows) {
            grid.add(etiqueta((String) r[0]), 0, fila);
            grid.add((Control) r[1], 1, fila++);
        }

        // Pre-llenar al editar
        if (datosActuales != null) {
            txtNombres.setText(safe(datosActuales, 1));
            txtApellidos.setText(safe(datosActuales, 2));
            txtEmail.setText(safe(datosActuales, 3));
            txtEmail.setEditable(false);
            txtEmail.setStyle(txtEmail.getStyle() + "-fx-background-color:#F0F0F0;");
            String cargo = safe(datosActuales, 4);
            if (cbCargo.getItems().contains(cargo))
                cbCargo.getSelectionModel().select(cargo);
            txtSalario.setText(safe(datosActuales, 6));
        }

        lblMensaje = new Label("");
        lblMensaje.setFont(Font.font("Arial", 12));
        lblMensaje.setWrapText(true);
        lblMensaje.setMaxWidth(370);
        GridPane.setColumnSpan(lblMensaje, 2);
        GridPane.setHalignment(lblMensaje, HPos.CENTER);
        grid.add(lblMensaje, 0, fila++);

        Button btnGuardar = boton(datosActuales == null ? "CREAR" : "ACTUALIZAR", "#00C8FF");
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

    private void guardar() {
        limpiar();

        // Validaciones de campos obligatorios
        if (txtNombres.getText().trim().isEmpty()) {
            error("Nombres es obligatorio.", txtNombres);
            return;
        }
        if (txtApellidos.getText().trim().isEmpty()) {
            error("Apellidos es obligatorio.", txtApellidos);
            return;
        }
        if (txtEmail.getText().trim().isEmpty()) {
            error("Email es obligatorio.", txtEmail);
            return;
        }
        if (!txtEmail.getText().contains("@")) {
            error("El email no tiene un formato valido.", txtEmail);
            return;
        }
        if (txtDocumento.getText().trim().isEmpty()) {
            error("Documento es obligatorio.", txtDocumento);
            return;
        }
        if (datosActuales == null && txtContrasena.getText().length() < 6) {
            error("La contrasena debe tener al menos 6 caracteres.", txtContrasena);
            return;
        }

        try {
            if (datosActuales == null) {
                // CREAR nuevo empleado
                double salario = 0;
                if (!txtSalario.getText().trim().isEmpty()) {
                    try {
                        salario = Double.parseDouble(txtSalario.getText().trim());
                    } catch (NumberFormatException ex) {
                        error("El salario debe ser un numero valido.", txtSalario);
                        return;
                    }
                }

                boolean ok = personaServices.crearEmpleado(
                        txtNombres.getText().trim(),
                        txtApellidos.getText().trim(),
                        txtEmail.getText().trim(),
                        txtTelefono.getText().trim(),
                        txtDocumento.getText().trim(),
                        cbCargo.getValue(),
                        txtContrasena.getText(),
                        salario);

                if (ok) {
                    if (onGuardado != null)
                        onGuardado.run();
                    stage.close();
                } else {
                    error("No se pudo crear el empleado. Verifique que el email y documento no esten registrados.",
                            null);
                }

            } else {
                // ACTUALIZAR empleado existente
                int idPersona = Integer.parseInt(safe(datosActuales, 0));
                double salario = 0;
                if (!txtSalario.getText().trim().isEmpty()) {
                    try {
                        salario = Double.parseDouble(txtSalario.getText().trim());
                    } catch (NumberFormatException ex) {
                        error("El salario debe ser un numero valido.", txtSalario);
                        return;
                    }
                }

                boolean ok = personaServices.actualizarEmpleado(
                        idPersona,
                        txtNombres.getText().trim(),
                        txtApellidos.getText().trim(),
                        txtTelefono.getText().trim(),
                        cbCargo.getValue(),
                        salario,
                        true);

                if (ok) {
                    if (onGuardado != null)
                        onGuardado.run();
                    stage.close();
                } else {
                    error("No se pudo actualizar el empleado.", null);
                }
            }

        } catch (Exception ex) {
            error("Error inesperado: " + ex.getMessage(), null);
        }
    }

    private TextField campo(String prompt) {
        TextField tf = new TextField();
        estilo(tf);
        tf.setPromptText(prompt);
        return tf;
    }

    private void estilo(Control c) {
        c.setStyle("-fx-border-color: #C0C0C0; -fx-border-width: 1; -fx-padding: 8; -fx-font-size: 12px;");
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
        b.setPrefSize(140, 38);
        b.setStyle("-fx-background-color:" + color + ";-fx-border-width:0;-fx-cursor:hand;");
        return b;
    }

    private void error(String msg, Control foco) {
        lblMensaje.setTextFill(Color.web("#C83C3C"));
        lblMensaje.setText("  " + msg);
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