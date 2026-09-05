package Proyecto.View.Usuario;

import Proyecto.Model.Cliente;
import Proyecto.services.PersonaServices;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

public class LoginView extends Application {

    private TextField txtEmail;
    private PasswordField txtPassword;
    private Button btnLogin;
    private Button btnRegistro;
    private Button btnSalir;
    private Label lblMensaje;

    private final PersonaServices personaServices;
    private Stage stage;

    public LoginView() {
        this.personaServices = new PersonaServices();
    }

    public LoginView(Stage stage) {
        this.personaServices = new PersonaServices();
        this.stage = stage;
        initComponents(stage);
    }

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        initComponents(primaryStage);
    }

    private void initComponents(Stage stage) {
        stage.setTitle("TechZone - Iniciar Sesión");
        stage.setResizable(false);

        // ── Panel izquierdo (logo) ─────────────────────────────────────────
        VBox leftPanel = new VBox(20);
        leftPanel.setAlignment(Pos.CENTER);
        leftPanel.setPrefWidth(420);
        leftPanel.setStyle("-fx-background-color: #0A1933;");

        ImageView imgLogo = cargarLogo();
        Label lblNombre = new Label("TECHZONE");
        lblNombre.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        lblNombre.setTextFill(Color.web("#00C8FF"));

        Label lblSub = new Label("GADGETS & HOBBIES");
        lblSub.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        lblSub.setTextFill(Color.web("#00C8FF"));

        if (imgLogo != null) {
            leftPanel.getChildren().addAll(imgLogo, lblNombre, lblSub);
        } else {
            leftPanel.getChildren().addAll(lblNombre, lblSub);
        }

        // ── Panel derecho (formulario) ─────────────────────────────────────
        GridPane rightPanel = new GridPane();
        rightPanel.setAlignment(Pos.CENTER);
        rightPanel.setHgap(10);
        rightPanel.setVgap(15);
        rightPanel.setPadding(new Insets(50));
        rightPanel.setStyle("-fx-background-color: #0F1E37;");

        Label lblTitulo = new Label("INICIAR SESIÓN");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        lblTitulo.setTextFill(Color.WHITE);
        lblTitulo.setTextAlignment(TextAlignment.CENTER);
        GridPane.setColumnSpan(lblTitulo, 2);
        GridPane.setHalignment(lblTitulo, javafx.geometry.HPos.CENTER);
        rightPanel.add(lblTitulo, 0, 0);

        Label lblEmail = new Label("Correo Electrónico:");
        lblEmail.setTextFill(Color.WHITE);
        lblEmail.setFont(Font.font("Arial", 14));
        rightPanel.add(lblEmail, 0, 1);

        txtEmail = new TextField();
        txtEmail.setPromptText("usuario@correo.com");
        txtEmail.setPrefWidth(250);
        txtEmail.setFont(Font.font("Arial", 14));
        txtEmail.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #00C8FF;" +
                "-fx-border-width: 1.5;" +
                "-fx-padding: 8;");
        rightPanel.add(txtEmail, 1, 1);

        Label lblPassword = new Label("Contraseña:");
        lblPassword.setTextFill(Color.WHITE);
        lblPassword.setFont(Font.font("Arial", 14));
        rightPanel.add(lblPassword, 0, 2);

        txtPassword = new PasswordField();
        txtPassword.setPromptText("••••••••");
        txtPassword.setPrefWidth(250);
        txtPassword.setFont(Font.font("Arial", 14));
        txtPassword.setStyle(
                "-fx-background-color: white;" +
                "-fx-border-color: #00C8FF;" +
                "-fx-border-width: 1.5;" +
                "-fx-padding: 8;");
        rightPanel.add(txtPassword, 1, 2);

        lblMensaje = new Label("");
        lblMensaje.setFont(Font.font("Arial", 12));
        lblMensaje.setTextFill(Color.web("#FF4444"));
        lblMensaje.setWrapText(true);
        lblMensaje.setMaxWidth(350);
        GridPane.setColumnSpan(lblMensaje, 2);
        GridPane.setHalignment(lblMensaje, javafx.geometry.HPos.CENTER);
        rightPanel.add(lblMensaje, 0, 3);

        btnLogin    = crearBoton("INGRESAR",     "#00C8FF");
        btnRegistro = crearBoton("REGISTRARSE",  "#1A8A2A"); // verde para distinguirlo
        btnSalir    = crearBoton("SALIR",         "#C83C3C");

        HBox buttonBox = new HBox(12, btnLogin, btnRegistro, btnSalir);
        buttonBox.setAlignment(Pos.CENTER);
        GridPane.setColumnSpan(buttonBox, 2);
        rightPanel.add(buttonBox, 0, 4);

        // ── Acciones ───────────────────────────────────────────────────────
        btnLogin.setOnAction(e -> autenticar());
        txtEmail.setOnAction(e -> autenticar());
        txtPassword.setOnAction(e -> autenticar());

        // CORRECCIÓN: se instancia RegistroView que ya existe en el proyecto
        btnRegistro.setOnAction(e -> abrirRegistro());

        btnSalir.setOnAction(e -> {
            stage.close();
            Platform.exit();
        });

        HBox mainPanel = new HBox(leftPanel, rightPanel);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        Scene scene = new Scene(mainPanel, 900, 600);
        stage.setScene(scene);
        stage.show();
    }

    // ── Autenticación ──────────────────────────────────────────────────────────
    private void autenticar() {
        String email    = txtEmail.getText().trim();
        String password = txtPassword.getText();

        if (email.isEmpty() || password.isEmpty()) {
            mostrarMensajeError("Por favor ingresa tu correo y contraseña.");
            return;
        }
        if (!email.contains("@")) {
            mostrarMensajeError("El correo electrónico no es válido.");
            return;
        }

        btnLogin.setDisable(true);
        btnLogin.setText("Verificando...");
        limpiarMensaje();

        new Thread(() -> {
            Cliente cliente = personaServices.autenticarCliente(email, password);

            Platform.runLater(() -> {
                btnLogin.setDisable(false);
                btnLogin.setText("INGRESAR");

                if (cliente != null) {
                    mostrarMensajeExito("Bienvenido, " + cliente.getNombre() + "!");
                    abrirMenuPrincipal(cliente);
                } else {
                    mostrarMensajeError("Correo o contraseña incorrectos.");
                    txtPassword.clear();
                    txtPassword.requestFocus();
                }
            });
        }).start();
    }

    // ── CORRECCIÓN PRINCIPAL: abrirRegistro ahora usa RegistroView real ────────
    private void abrirRegistro() {
        // RegistroView ya existe en el proyecto en Proyecto.View.Usuario.
        // Se abre como ventana modal sobre el login.
        RegistroView registroView = new RegistroView(stage);
        // Si el registro fue exitoso mostramos un mensaje de éxito en el login
        if (registroView.isRegistradoExitoso()) {
            mostrarMensajeExito("¡Cuenta creada! Ya puedes iniciar sesión.");
        }
    }

    // ── Helpers UI ─────────────────────────────────────────────────────────────
    private ImageView cargarLogo() {
        try {
            java.io.InputStream stream = getClass().getResourceAsStream("/logo.jpg");
            if (stream == null) stream = getClass().getResourceAsStream("logo.jpg");
            if (stream != null) {
                Image img = new Image(stream);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(200);
                iv.setFitHeight(200);
                iv.setPreserveRatio(true);
                Circle clip = new Circle(100, 100, 100);
                iv.setClip(clip);
                return iv;
            }
        } catch (Exception ex) {
            System.err.println("[LoginView] Logo no encontrado: " + ex.getMessage());
        }
        return null;
    }

    private Button crearBoton(String texto, String colorHex) {
        Button btn = new Button(texto);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        btn.setTextFill(Color.WHITE);
        btn.setPrefSize(130, 40);
        btn.setStyle("-fx-background-color: " + colorHex + "; -fx-border-width: 0; -fx-cursor: hand;");

        String hoverColor = colorHex.equals("#00C8FF") ? "#009DBF"
                          : colorHex.equals("#C83C3C")  ? "#A03030"
                          : colorHex.equals("#1A8A2A")  ? "#126B20"
                          : "#222232";
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: " + hoverColor + "; -fx-border-width: 0; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: " + colorHex + "; -fx-border-width: 0; -fx-cursor: hand;"));
        return btn;
    }

    private void mostrarMensajeError(String msg) {
        lblMensaje.setTextFill(Color.web("#FF4444"));
        lblMensaje.setText("⚠ " + msg);
    }

    private void mostrarMensajeExito(String msg) {
        lblMensaje.setTextFill(Color.web("#00C84B"));
        lblMensaje.setText("✓ " + msg);
    }

    private void limpiarMensaje() {
        lblMensaje.setText("");
    }

    public void abrirMenuPrincipal(Cliente cliente) {
        Stage menuStage = new Stage();
        new MenuPrincipalView(cliente, menuStage);
        stage.close();
    }

    public String getEmail()       { return txtEmail.getText().trim(); }
    public String getPassword()    { return txtPassword.getText(); }
    public Button getBtnLogin()    { return btnLogin; }
    public Button getBtnRegistro() { return btnRegistro; }
    public Button getBtnSalir()    { return btnSalir; }

    public void mostrarMensaje(String msg) { mostrarMensajeExito(msg); }
    public void mostrarError(String msg)   { mostrarMensajeError(msg); }
    public void limpiarCampos()            { txtEmail.clear(); txtPassword.clear(); }

    public static void main(String[] args) { launch(args); }
}
