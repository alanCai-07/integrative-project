package Proyecto.View.Documento;

import Proyecto.services.PersonaServices;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class ClienteCotizacionView {

    private final PersonaServices personaServices = new PersonaServices();
    private boolean registradoExitoso;
    private Stage stage;
    private TextField txtNombre;
    private TextField txtApellido;
    private TextField txtDocumento;
    private TextField txtEmail;
    private TextField txtTelefono;
    private TextField txtDireccion;
    private Label lblMensaje;

    public ClienteCotizacionView(Window owner) {
        construir(owner);
    }

    public boolean isRegistradoExitoso() {
        return registradoExitoso;
    }

    private void construir(Window owner) {
        stage = new Stage();
        stage.setTitle("TechZone - Cliente para cotización");
        stage.setResizable(false);
        stage.initModality(Modality.WINDOW_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }

        VBox root = new VBox(14);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #f5f7fb;");

        Label titulo = new Label("Registrar cliente para cotización");
        titulo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 21));
        titulo.setTextFill(Color.web("#0A1933"));
        Label ayuda = new Label("No se crea una cuenta ni se solicita contraseña.");
        ayuda.setTextFill(Color.web("#60758A"));

        GridPane formulario = new GridPane();
        formulario.setHgap(12);
        formulario.setVgap(10);
        formulario.setPadding(new Insets(16));
        formulario.setStyle("-fx-background-color: white; -fx-border-color: #DCE6F0; "
                + "-fx-border-radius: 7; -fx-background-radius: 7;");

        txtNombre = campo("Nombres");
        txtApellido = campo("Apellidos");
        txtDocumento = campo("Documento");
        txtEmail = campo("Correo electrónico (opcional)");
        txtTelefono = campo("Teléfono (opcional)");
        txtDireccion = campo("Dirección (opcional)");

        agregarCampo(formulario, "Nombres *", txtNombre, 0);
        agregarCampo(formulario, "Apellidos *", txtApellido, 1);
        agregarCampo(formulario, "Documento *", txtDocumento, 2);
        agregarCampo(formulario, "Correo", txtEmail, 3);
        agregarCampo(formulario, "Teléfono", txtTelefono, 4);
        agregarCampo(formulario, "Dirección", txtDireccion, 5);

        lblMensaje = new Label();
        lblMensaje.setWrapText(true);
        lblMensaje.setTextFill(Color.web("#C83C3C"));

        Button guardar = boton("Guardar cliente", "#1A9B43");
        Button cancelar = boton("Cancelar", "#6B7280");
        guardar.setOnAction(e -> guardar());
        cancelar.setOnAction(e -> stage.close());

        HBox acciones = new HBox(10, guardar, cancelar);
        acciones.setAlignment(Pos.CENTER_RIGHT);
        root.getChildren().addAll(titulo, ayuda, formulario, lblMensaje, acciones);

        stage.setScene(new Scene(root, 540, 470));
        stage.showAndWait();
    }

    private void guardar() {
        if (txtNombre.getText().trim().isEmpty()
                || txtApellido.getText().trim().isEmpty()
                || txtDocumento.getText().trim().isEmpty()) {
            lblMensaje.setText("Completa nombres, apellidos y documento.");
            return;
        }

        boolean guardado = personaServices.registrarClienteParaCotizacion(
                txtNombre.getText(), txtApellido.getText(), txtEmail.getText(),
                txtDocumento.getText(), txtTelefono.getText(), txtDireccion.getText());
        if (guardado) {
            registradoExitoso = true;
            new Alert(Alert.AlertType.INFORMATION,
                    "Cliente guardado. Ya puedes buscarlo en la cotización.",
                    ButtonType.OK).showAndWait();
            stage.close();
        } else {
            lblMensaje.setText("No se pudo guardar. Verifica que el documento o correo no estén repetidos.");
        }
    }

    private void agregarCampo(GridPane grid, String etiqueta, TextField campo, int fila) {
        Label label = new Label(etiqueta);
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        label.setTextFill(Color.web("#334155"));
        grid.add(label, 0, fila);
        grid.add(campo, 1, fila);
    }

    private TextField campo(String prompt) {
        TextField campo = new TextField();
        campo.setPromptText(prompt);
        campo.setPrefWidth(320);
        campo.setStyle("-fx-padding: 8; -fx-border-color: #C7D4E2; "
                + "-fx-border-radius: 4; -fx-background-radius: 4;");
        return campo;
    }

    private Button boton(String texto, String color) {
        Button boton = new Button(texto);
        boton.setTextFill(Color.WHITE);
        boton.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        boton.setStyle("-fx-background-color: " + color
                + "; -fx-background-radius: 5; -fx-padding: 9 15 9 15;");
        return boton;
    }
}
