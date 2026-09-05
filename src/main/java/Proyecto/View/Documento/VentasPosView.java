package Proyecto.View.Documento;

import Proyecto.Model.Cliente;
import Proyecto.Model.Producto;
import Proyecto.dao.DocumentoDAO;
import Proyecto.dao.InventarioDAO;
import Proyecto.dao.PersonaDAO;
import Proyecto.services.PersonaServices;
import Proyecto.services.ProductoServices;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class VentasPosView {

    private final int empleadoId;

    private final ProductoServices productoServices;
    private final DocumentoDAO documentoDAO;
    private final InventarioDAO inventarioDAO;
    private final PersonaDAO personaDAO;
    private final PersonaServices personaServices;

    private ObservableList<ItemVenta> itemsVenta;
    private List<Producto> productosFiltrados;

    // Cliente seleccionado para la venta actual
    private Cliente clienteActual = null;

    // Widgets
    private TextField txtBuscador;
    private ListView<String> listProductos;
    private Spinner<Integer> spinnerCantidad;
    private ComboBox<String> cbMetodoPago;
    private TextField txtBuscarCliente;
    private Label lblClienteInfo;

    private TableView<ItemVenta> tablaTicket;
    private Label lblSubtotal;
    private Label lblIva;
    private Label lblTotal;

    private VBox root;

    private static final String[] METODOS_PAGO = {
            "Efectivo", "Tarjeta Debito", "Tarjeta Credito",
            "Transferencia Bancaria", "Nequi / Daviplata"
    };

    public VentasPosView(int empleadoId) {
        this.empleadoId = empleadoId;
        this.productoServices = new ProductoServices();
        this.documentoDAO = new DocumentoDAO();
        this.inventarioDAO = new InventarioDAO();
        this.personaDAO = new PersonaDAO();
        this.personaServices = new PersonaServices();
        this.itemsVenta = FXCollections.observableArrayList();
        this.productosFiltrados = new ArrayList<>();
        build();
        cargarProductos("");
    }

    public VentasPosView() {
        this(0);
    }

    public Node getRoot() {
        return root;
    }

    @SuppressWarnings("unchecked")
    private void build() {
        root = new VBox(12);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: white;");
        VBox.setVgrow(root, Priority.ALWAYS);

        // Encabezado
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 10, 0));
        header.setStyle("-fx-border-color: #E0E0E0; -fx-border-width: 0 0 2 0;");

        Label lblTitulo = new Label("Punto de Venta");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        lblTitulo.setTextFill(Color.web("#0A1933"));

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        Label lblFecha = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        lblFecha.setFont(Font.font("Arial", 12));
        lblFecha.setTextFill(Color.GRAY);

        header.getChildren().addAll(lblTitulo, hSpacer, lblFecha);

        HBox body = new HBox(12);
        VBox.setVgrow(body, Priority.ALWAYS);

        // ── Columna izquierda ─────────────────────────────────────────────────
        VBox leftCol = new VBox(10);
        leftCol.setPrefWidth(310);
        leftCol.setMaxWidth(330);

        // Seccion cliente - REDISENNADA
        VBox secCliente = seccion("Datos del Cliente");

        Label lblClienteHint = new Label("Buscar cliente por ID, nombre o correo:");
        lblClienteHint.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        txtBuscarCliente = new TextField();
        txtBuscarCliente.setPromptText("ID, nombre o correo del cliente...");
        txtBuscarCliente.setStyle("-fx-border-color: #C0C0C0; -fx-border-width: 1; -fx-padding: 7;");

        Button btnBuscarCliente = boton("Buscar", "#00C8FF");
        Button btnNuevoCliente = boton("Registro rapido", "#1A8A2A");

        btnBuscarCliente.setOnAction(e -> buscarCliente());
        btnNuevoCliente.setOnAction(e -> abrirRegistroRapido());

        HBox fila1 = new HBox(8, txtBuscarCliente, btnBuscarCliente);
        HBox.setHgrow(txtBuscarCliente, Priority.ALWAYS);

        lblClienteInfo = new Label("Sin cliente seleccionado");
        lblClienteInfo.setFont(Font.font("Arial", 12));
        lblClienteInfo.setTextFill(Color.GRAY);
        lblClienteInfo.setWrapText(true);

        Label lblMetodo = new Label("Metodo de Pago:");
        lblMetodo.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        cbMetodoPago = new ComboBox<>();
        cbMetodoPago.getItems().addAll(METODOS_PAGO);
        cbMetodoPago.getSelectionModel().selectFirst();
        cbMetodoPago.setMaxWidth(Double.MAX_VALUE);

        secCliente.getChildren().addAll(
                lblClienteHint, fila1, btnNuevoCliente, lblClienteInfo,
                new Separator(), lblMetodo, cbMetodoPago);

        // Seccion buscador de productos
        VBox secBuscar = seccion("Buscar Producto");
        VBox.setVgrow(secBuscar, Priority.ALWAYS);

        txtBuscador = new TextField();
        txtBuscador.setPromptText("Nombre o ID del producto...");
        txtBuscador.setFont(Font.font("Arial", 13));
        txtBuscador.setStyle("-fx-border-color: #00C8FF; -fx-border-width: 1.5; -fx-padding: 8;");
        txtBuscador.textProperty().addListener((obs, o, nv) -> cargarProductos(nv));

        listProductos = new ListView<>();
        listProductos.setPrefHeight(240);
        VBox.setVgrow(listProductos, Priority.ALWAYS);
        listProductos.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2)
                agregarProducto();
        });

        Label lblCant = new Label("Cantidad:");
        lblCant.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        spinnerCantidad = new Spinner<>(1, 9999, 1);
        spinnerCantidad.setEditable(true);
        spinnerCantidad.setPrefWidth(90);

        Button btnAgregar = boton("Agregar al ticket", "#1A8A2A");
        btnAgregar.setMaxWidth(Double.MAX_VALUE);
        btnAgregar.setOnAction(e -> agregarProducto());

        HBox cantRow = new HBox(8, lblCant, spinnerCantidad);
        cantRow.setAlignment(Pos.CENTER_LEFT);

        secBuscar.getChildren().addAll(txtBuscador, listProductos, cantRow, btnAgregar);
        leftCol.getChildren().addAll(secCliente, secBuscar);

        // ── Columna derecha: ticket ────────────────────────────────────────────
        VBox rightCol = new VBox(10);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        VBox secTicket = seccion("Ticket de Venta");
        VBox.setVgrow(secTicket, Priority.ALWAYS);

        tablaTicket = new TableView<>(itemsVenta);
        tablaTicket.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(tablaTicket, Priority.ALWAYS);
        tablaTicket.setPlaceholder(new Label("Agrega productos desde el panel izquierdo."));

        TableColumn<ItemVenta, String> colProd = new TableColumn<>("Producto");
        colProd.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));

        TableColumn<ItemVenta, Integer> colCant = new TableColumn<>("Cant.");
        colCant.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCantidad()).asObject());
        colCant.setMaxWidth(70);

        TableColumn<ItemVenta, Double> colPU = new TableColumn<>("P. Unit.");
        colPU.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getPrecio()).asObject());
        colPU.setCellFactory(c -> precioCell());
        colPU.setMaxWidth(100);

        TableColumn<ItemVenta, Double> colSub = new TableColumn<>("Subtotal");
        colSub.setCellValueFactory(d -> new SimpleDoubleProperty(
                d.getValue().getPrecio() * d.getValue().getCantidad()).asObject());
        colSub.setCellFactory(c -> precioCell());
        colSub.setMaxWidth(110);

        TableColumn<ItemVenta, Void> colDel = new TableColumn<>("");
        colDel.setMaxWidth(48);
        colDel.setCellFactory(c -> new TableCell<>() {
            private final Button b = new Button("X");
            {
                b.setStyle("-fx-background-color:#C83C3C;-fx-text-fill:white;-fx-cursor:hand;"
                        + "-fx-border-width:0;-fx-padding:3 6 3 6;");
                b.setOnAction(e -> {
                    itemsVenta.remove(getTableView().getItems().get(getIndex()));
                    actualizarTotales();
                });
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : b);
            }
        });

        tablaTicket.getColumns().addAll(colProd, colCant, colPU, colSub, colDel);

        GridPane gridTotales = new GridPane();
        gridTotales.setHgap(20);
        gridTotales.setVgap(5);
        gridTotales.setPadding(new Insets(10, 0, 0, 0));
        gridTotales.setAlignment(Pos.CENTER_RIGHT);

        lblSubtotal = totalLabel("$0");
        lblIva = totalLabel("$0");
        lblTotal = new Label("$0");
        lblTotal.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        lblTotal.setTextFill(Color.web("#0A1933"));

        gridTotales.add(etiqueta("Subtotal:"), 0, 0);
        gridTotales.add(lblSubtotal, 1, 0);
        gridTotales.add(etiqueta("IVA (19%):"), 0, 1);
        gridTotales.add(lblIva, 1, 1);
        gridTotales.add(etiqueta("TOTAL:"), 0, 2);
        gridTotales.add(lblTotal, 1, 2);

        secTicket.getChildren().addAll(tablaTicket, gridTotales);

        HBox btnCobro = new HBox(10);
        btnCobro.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancelar = boton("Cancelar venta", "#646464");
        Button btnCobrar = boton("Cobrar y emitir factura", "#1A8A2A");
        btnCobrar.setPrefHeight(42);
        btnCobrar.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        btnCancelar.setOnAction(e -> cancelarVenta());
        btnCobrar.setOnAction(e -> cobrarYEmitirFactura());

        btnCobro.getChildren().addAll(btnCancelar, btnCobrar);

        rightCol.getChildren().addAll(secTicket, btnCobro);
        body.getChildren().addAll(leftCol, rightCol);
        root.getChildren().addAll(header, body);
    }

    private void buscarCliente() {
        String query = txtBuscarCliente.getText().trim();
        if (query.isEmpty()) {
            info("Ingresa el ID, nombre o correo del cliente.");
            return;
        }

        Cliente encontrado = null;

        // Intentar por ID numerico
        try {
            int id = Integer.parseInt(query);
            encontrado = personaDAO.obtenerClientePorId(id);
        } catch (NumberFormatException ignored) {
        }

        // Si no se encontro por ID, buscar por nombre/correo
        if (encontrado == null) {
            List<Cliente> resultados = personaDAO.buscarClientes(query);
            if (!resultados.isEmpty()) {
                if (resultados.size() == 1) {
                    encontrado = resultados.get(0);
                } else {
                    // Mostrar opciones para seleccionar
                    encontrado = seleccionarDeMultiples(resultados);
                }
            }
        }

        if (encontrado != null) {
            seleccionarCliente(encontrado);
        } else {
            lblClienteInfo.setText(
                    "No se encontro cliente con: \"" + query + "\"\n"
                            + "Usa 'Registro rapido' para crear un nuevo cliente.");
            lblClienteInfo.setTextFill(Color.web("#C83C3C"));
            clienteActual = null;
        }
    }

    private Cliente seleccionarDeMultiples(List<Cliente> lista) {
        ChoiceDialog<String> dlg = new ChoiceDialog<>();
        dlg.setTitle("Seleccionar Cliente");
        dlg.setHeaderText("Se encontraron varios clientes. Selecciona uno:");
        for (Cliente c : lista) {
            dlg.getItems().add(c.getId() + " - " + c.getNombre() + " " + c.getApellido() + " (" + c.getEmail() + ")");
        }
        dlg.setSelectedItem(dlg.getItems().get(0));
        var result = dlg.showAndWait();
        if (result.isPresent()) {
            int idx = dlg.getItems().indexOf(result.get());
            return lista.get(idx);
        }
        return null;
    }

    private void seleccionarCliente(Cliente c) {
        clienteActual = c;
        lblClienteInfo.setText("Cliente: " + c.getNombre() + " " + c.getApellido()
                + "\nCorreo: " + (c.getEmail() != null ? c.getEmail() : "-")
                + "\nID: " + c.getId());
        lblClienteInfo.setTextFill(Color.web("#1A8A2A"));
    }

    private void abrirRegistroRapido() {
        Dialog<Cliente> dlg = new Dialog<>();
        dlg.setTitle("Registro Rapido de Cliente");
        dlg.setResizable(false);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField txtNombre = campoTexto("Nombres *");
        TextField txtApellido = campoTexto("Apellidos *");
        TextField txtEmail = campoTexto("Correo electronico *");
        TextField txtTelefono = campoTexto("Telefono");
        TextField txtDocumento = campoTexto("Numero de documento *");

        Label lblPass = new Label("Contrasena inicial generada automaticamente.");
        lblPass.setFont(Font.font("Arial", 11));
        lblPass.setTextFill(Color.GRAY);
        lblPass.setWrapText(true);

        Label lblError = new Label("");
        lblError.setFont(Font.font("Arial", 12));
        lblError.setTextFill(Color.web("#C83C3C"));
        lblError.setWrapText(true);

        grid.add(etiqueta("Nombres:"), 0, 0);
        grid.add(txtNombre, 1, 0);
        grid.add(etiqueta("Apellidos:"), 0, 1);
        grid.add(txtApellido, 1, 1);
        grid.add(etiqueta("Correo:"), 0, 2);
        grid.add(txtEmail, 1, 2);
        grid.add(etiqueta("Telefono:"), 0, 3);
        grid.add(txtTelefono, 1, 3);
        grid.add(etiqueta("Documento:"), 0, 4);
        grid.add(txtDocumento, 1, 4);
        grid.add(lblPass, 0, 5);
        GridPane.setColumnSpan(lblPass, 2);
        grid.add(lblError, 0, 6);
        GridPane.setColumnSpan(lblError, 2);

        ButtonType btnCrear = new ButtonType("Crear y Seleccionar", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(btnCrear, ButtonType.CANCEL);
        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().setPrefWidth(440);

        // Deshabilitar el boton OK para validacion manual
        Button okBtn = (Button) dlg.getDialogPane().lookupButton(btnCrear);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            lblError.setText("");

            String nombre = txtNombre.getText().trim();
            String apellido = txtApellido.getText().trim();
            String email = txtEmail.getText().trim();
            String telefono = txtTelefono.getText().trim();
            String documento = txtDocumento.getText().trim();

            if (nombre.isEmpty()) {
                lblError.setText("El nombre es obligatorio.");
                event.consume();
                return;
            }
            if (apellido.isEmpty()) {
                lblError.setText("El apellido es obligatorio.");
                event.consume();
                return;
            }
            if (email.isEmpty() || !email.contains("@")) {
                lblError.setText("Ingresa un correo valido.");
                event.consume();
                return;
            }
            if (documento.isEmpty()) {
                lblError.setText("El numero de documento es obligatorio.");
                event.consume();
                return;
            }

            // Contrasena temporal: nombre+documento (el cliente la cambia desde su perfil)
            String passwordTemporal = nombre.toLowerCase().replaceAll("\\s+", "") + documento;

            okBtn.setDisable(true);
            okBtn.setText("Registrando...");

            // Ejecutar en hilo separado para no bloquear UI
            new Thread(() -> {
                boolean ok;
                try {
                    ok = personaServices.registrarCliente(
                            nombre, apellido, email, telefono, documento, passwordTemporal, "");
                } catch (Exception ex) {
                    ok = false;
                }
                boolean exito = ok;
                Platform.runLater(() -> {
                    okBtn.setDisable(false);
                    okBtn.setText("Crear y Seleccionar");
                    if (exito) {
                        // Recuperar el cliente recien creado para obtener su ID
                        Cliente nuevo = personaDAO.obtenerClientePorEmail(email.toLowerCase());
                        if (nuevo != null) {
                            seleccionarCliente(nuevo);
                            dlg.setResult(nuevo);
                            dlg.close();
                        } else {
                            lblError.setText("Cliente creado pero no se pudo recuperar. Busca por correo.");
                        }
                    } else {
                        lblError.setText(
                                "No se pudo crear el cliente. Verifica que el correo y documento no esten registrados.");
                        event.consume();
                    }
                });
            }).start();

            event.consume(); // siempre consumir; el cierre lo maneja el hilo
        });

        dlg.setResultConverter(bt -> null);
        dlg.showAndWait();
    }

    // =========================================================================
    // LOGICA DE PRODUCTOS
    // =========================================================================

    private void cargarProductos(String query) {
        productosFiltrados.clear();
        listProductos.getItems().clear();
        try {
            List<Producto> todos = query.isEmpty()
                    ? productoServices.obtenerTodosLosProductos()
                    : productoServices.buscarProductos(query);

            for (Producto p : todos) {
                if (Boolean.TRUE.equals(p.getActivo()) && p.getCantidad() > 0) {
                    productosFiltrados.add(p);
                    listProductos.getItems().add(String.format(
                            "[%d] %-30s  $%,.0f  (stock: %d)",
                            p.getIdProducto(),
                            truncar(p.getNombre(), 28),
                            p.getPrecioVenta(),
                            p.getCantidad()));
                }
            }
        } catch (Exception e) {
            listProductos.setPlaceholder(new Label("Error al cargar productos: " + e.getMessage()));
        }
    }

    private void agregarProducto() {
        int idx = listProductos.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= productosFiltrados.size()) {
            info("Selecciona un producto de la lista.");
            return;
        }
        Producto p = productosFiltrados.get(idx);
        int cant = spinnerCantidad.getValue();

        int cantEnTicket = itemsVenta.stream()
                .filter(i -> i.getIdProducto() == p.getIdProducto())
                .mapToInt(ItemVenta::getCantidad).sum();

        if (cantEnTicket + cant > p.getCantidad()) {
            info("Stock insuficiente. Disponible: " + p.getCantidad()
                    + ", ya en ticket: " + cantEnTicket);
            return;
        }

        for (ItemVenta item : itemsVenta) {
            if (item.getIdProducto() == p.getIdProducto()) {
                item.setCantidad(item.getCantidad() + cant);
                tablaTicket.refresh();
                actualizarTotales();
                return;
            }
        }

        itemsVenta.add(new ItemVenta(p.getIdProducto(), p.getNombre(), p.getPrecioVenta(), cant));
        actualizarTotales();
    }

    private static final DecimalFormat PESOS_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        PESOS_FORMAT = new DecimalFormat("#,##0", symbols);
        PESOS_FORMAT.setGroupingUsed(true);
        PESOS_FORMAT.setMinimumFractionDigits(0);
        PESOS_FORMAT.setMaximumFractionDigits(0);
    }

    private String formatPesos(double importe) {
        return "$" + PESOS_FORMAT.format(importe);
    }

    private String padRight(String texto, int ancho) {
        if (texto == null) texto = "";
        if (texto.length() >= ancho) return texto;
        return texto + " ".repeat(ancho - texto.length());
    }

    private String padLeft(String texto, int ancho) {
        if (texto == null) texto = "";
        if (texto.length() >= ancho) return texto;
        return " ".repeat(ancho - texto.length()) + texto;
    }

    private void actualizarTotales() {
        double subtotal = itemsVenta.stream()
                .mapToDouble(i -> i.getPrecio() * i.getCantidad()).sum();
        double iva = subtotal * 0.19;
        double total = subtotal + iva;
        lblSubtotal.setText(formatPesos(subtotal));
        lblIva.setText(formatPesos(iva));
        lblTotal.setText(formatPesos(total));
    }

    // =========================================================================
    // COBRO Y EMISION DE FACTURA - CORREGIDO
    // =========================================================================

    private void cobrarYEmitirFactura() {
        if (itemsVenta.isEmpty()) {
            info("Agrega al menos un producto al ticket.");
            return;
        }

        if (clienteActual == null) {
            info("Debes seleccionar o registrar un cliente antes de procesar la venta.\n\n"
                    + "Usa el buscador de clientes o el boton 'Registro rapido'.");
            return;
        }

        String metodo = cbMetodoPago.getValue();
        int idMetodoPago = cbMetodoPago.getSelectionModel().getSelectedIndex() + 1;

        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "Confirmar cobro de " + lblTotal.getText() + " con " + metodo
                        + "\nCliente: " + clienteActual.getNombre() + " " + clienteActual.getApellido() + "?",
                ButtonType.YES, ButtonType.NO);
        conf.setTitle("Confirmar Cobro");

        conf.showAndWait().ifPresent(r -> {
            if (r != ButtonType.YES)
                return;

            try {
                double subtotalSinIva = itemsVenta.stream()
                        .mapToDouble(i -> i.getPrecio() * i.getCantidad()).sum();
                double totalConIva = subtotalSinIva * 1.19;

                int idDocumento = documentoDAO.crearDocumento(
                        1, // Factura de Venta
                        clienteActual.getId(), // id_persona del cliente real
                        empleadoId, // id del cajero autenticado
                        idMetodoPago,
                        0, // descuento
                        totalConIva,
                        "Venta POS - Metodo: " + metodo);

                if (idDocumento == -1) {
                    new Alert(Alert.AlertType.ERROR,
                            "Error al registrar la venta en la base de datos.", ButtonType.OK).showAndWait();
                    return;
                }

                // Registrar movimientos de inventario y descontar stock
                for (ItemVenta item : itemsVenta) {
                    inventarioDAO.registrarMovimientoConPrecio(
                            idDocumento,
                            item.getIdProducto(),
                            empleadoId,
                            item.getCantidad(),
                            item.getPrecio());
                    inventarioDAO.actualizarStock(item.getIdProducto(), -item.getCantidad());
                }

                // Emitir factura y limpiar ticket
                emitirFactura(metodo, idDocumento, clienteActual);
                itemsVenta.clear();
                actualizarTotales();
                clienteActual = null;
                lblClienteInfo.setText("Sin cliente seleccionado");
                lblClienteInfo.setTextFill(Color.GRAY);
                txtBuscarCliente.clear();
                cargarProductos(""); // refrescar stock visible

            } catch (Exception ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR,
                        "Error al registrar la venta: " + ex.getClass().getSimpleName()
                                + " - " + ex.getMessage(), ButtonType.OK).showAndWait();
            }
        });
    }

    private void emitirFactura(String metodoPago, int idDocumento, Cliente cliente) {
        StringBuilder sb = new StringBuilder();
        sb.append("===========================================\n");
        sb.append("          TECHZONE  -  FACTURA\n");
        sb.append("===========================================\n");
        sb.append("  N Documento  : ").append(idDocumento).append("\n");
        sb.append("  Fecha        : ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                .append("\n");
        sb.append("  Metodo pago  : ").append(metodoPago).append("\n");
        sb.append("  Cliente      : ").append(cliente.getNombre()).append(" ")
                .append(cliente.getApellido()).append("\n");
        sb.append("  ID Cliente   : ").append(cliente.getId()).append("\n");
        sb.append("-------------------------------------------\n");
        sb.append("  PRODUCTO                     CANT    SUBTOTAL\n");
        sb.append("-------------------------------------------\n");

        for (ItemVenta item : itemsVenta) {
            String nombre = padRight(truncar(item.getNombre(), 28), 28);
            String cantidad = String.format("%6d", item.getCantidad());
            String subtotal = padLeft(formatPesos(item.getPrecio() * item.getCantidad()), 12);
            sb.append("  ").append(nombre).append(" ")
                    .append(cantidad).append(" ").append(subtotal).append("\n");
        }

        sb.append("-------------------------------------------\n");
        sb.append("  Subtotal  : ").append(lblSubtotal.getText()).append("\n");
        sb.append("  IVA(19%) : ").append(lblIva.getText()).append("\n");
        sb.append("  TOTAL     : ").append(lblTotal.getText()).append("\n");
        sb.append("===========================================\n");
        sb.append("   Gracias por tu compra en TechZone!\n");

        TextArea txt = new TextArea(sb.toString());
        txt.setEditable(false);
        txt.setFont(Font.font("Monospaced", 12));
        txt.setPrefSize(480, 420);

        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Factura N " + idDocumento);
        dlg.getDialogPane().setContent(new ScrollPane(txt));
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.showAndWait();
    }

    private void cancelarVenta() {
        if (itemsVenta.isEmpty())
            return;
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "Cancelar la venta y limpiar el ticket?", ButtonType.YES, ButtonType.NO);
        conf.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                itemsVenta.clear();
                actualizarTotales();
            }
        });
    }

    // =========================================================================
    // HELPERS UI
    // =========================================================================

    private VBox seccion(String titulo) {
        VBox sec = new VBox(10);
        sec.setPadding(new Insets(12));
        sec.setStyle("-fx-background-color: #FAFAFA; -fx-border-color: #E0E0E0; -fx-border-width: 1;");
        Label lbl = new Label(titulo);
        lbl.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        lbl.setTextFill(Color.web("#0A1933"));
        sec.getChildren().add(lbl);
        return sec;
    }

    private TextField campoTexto(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefWidth(200);
        tf.setStyle("-fx-border-color:#C0C0C0;-fx-border-width:1;-fx-padding:7;");
        return tf;
    }

    private Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        l.setTextFill(Color.web("#555555"));
        return l;
    }

    private Label totalLabel(String v) {
        Label l = new Label(v);
        l.setFont(Font.font("Arial", 14));
        l.setTextFill(Color.web("#0A1933"));
        return l;
    }

    private Button boton(String texto, String color) {
        Button b = new Button(texto);
        b.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        b.setTextFill(Color.WHITE);
        b.setStyle("-fx-background-color:" + color + ";-fx-border-width:0;"
                + "-fx-cursor:hand;-fx-padding:8 14 8 14;");
        return b;
    }

    private TableCell<ItemVenta, Double> precioCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : formatPesos(v));
            }
        };
    }

    private String truncar(String s, int max) {
        if (s == null)
            return "-";
        return s.length() > max ? s.substring(0, max - 1) + "..." : s;
    }

    private void info(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    // =========================================================================
    // MODELO DE FILA
    // =========================================================================

    public static class ItemVenta {
        private final int idProducto;
        private final String nombre;
        private final double precio;
        private int cantidad;

        public ItemVenta(int idProducto, String nombre, double precio, int cantidad) {
            this.idProducto = idProducto;
            this.nombre = nombre;
            this.precio = precio;
            this.cantidad = cantidad;
        }

        public int getIdProducto() {
            return idProducto;
        }

        public String getNombre() {
            return nombre;
        }

        public double getPrecio() {
            return precio;
        }

        public int getCantidad() {
            return cantidad;
        }

        public void setCantidad(int c) {
            this.cantidad = c;
        }
    }
}
