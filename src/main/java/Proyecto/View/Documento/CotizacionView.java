package Proyecto.View.Documento;

import Proyecto.Model.Cliente;
import Proyecto.Model.Producto;
import Proyecto.services.PersonaServices;
import Proyecto.services.ProductoServices;
import Proyecto.util.ProductoImageHelper;
import Proyecto.util.CotizacionPdfGenerator;
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
import javafx.stage.Window;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CotizacionView {

    private final ProductoServices   productoServices;
    private final PersonaServices    personaServices;
    private ObservableList<ItemCotizacion> itemsCotizacion;

    private TextField     txtBuscarCliente;
    private Label         lblClienteSeleccionado;
    private Cliente       clienteSeleccionado;

    // Lista de resultados de la busqueda de clientes
    private ListView<String> listClientes;
    private List<Cliente>    clientesEncontrados;

    private TextField     txtBuscarProducto;
    private ListView<String> listProductos;
    private List<Producto>   productosFiltradosLista;
    private VBox detalleProducto;
    private Label lblDetalleNombre;
    private Label lblDetalleMeta;
    private Label lblDetalleDescripcion;
    private StackPane detalleImagen;

    private TableView<ItemCotizacion> tablaCotizacion;
    private Label         lblSubtotal;
    private Label         lblDescuento;
    private Label         lblTotal;
    private TextField     txtDescuento;
    private TextArea      txtObservaciones;

    private VBox root;

    public CotizacionView() {
        this.productoServices    = new ProductoServices();
        this.personaServices     = new PersonaServices();
        this.itemsCotizacion     = FXCollections.observableArrayList();
        this.clientesEncontrados = new ArrayList<>();
        this.productosFiltradosLista = new ArrayList<>();
        build();
        cargarProductos();
    }

    public Node getRoot() { return root; }

    // ── Construccion ─────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void build() {
        root = new VBox(15);
        root.setPadding(new Insets(12));
        root.getStyleClass().add("quote-root");
        VBox.setVgrow(root, Priority.ALWAYS);

        Label lblTitulo = new Label("Generación de cotización");
        lblTitulo.getStyleClass().add("quote-title");
        Label lblSubtitulo = new Label("Crea una propuesta para tu cliente de forma rápida y ordenada");
        lblSubtitulo.getStyleClass().add("quote-subtitle");
        VBox encabezado = new VBox(3, lblTitulo, lblSubtitulo);

        HBox bodyLayout = new HBox(15);
        VBox.setVgrow(bodyLayout, Priority.ALWAYS);

        // ── Columna izquierda ─────────────────────────────────────────────
        VBox leftCol = new VBox(8);
        leftCol.setPrefWidth(390);
        leftCol.setMinWidth(360);
        leftCol.setMaxWidth(430);

        // Seccion cliente
        VBox secCliente = seccion("1", "Cliente");

        txtBuscarCliente = campo("Buscar por nombre o correo...");

        Button btnBuscarCliente = boton("Buscar", "#00C8FF");
        btnBuscarCliente.setOnAction(e -> buscarCliente());

        Button btnNuevoCliente = boton("Nuevo cliente", "#6c5ce7");
        btnNuevoCliente.setOnAction(e -> registrarNuevoCliente());

        HBox buscarRow = new HBox(8, txtBuscarCliente, btnBuscarCliente);
        HBox.setHgrow(txtBuscarCliente, Priority.ALWAYS);
        HBox nuevoClienteRow = new HBox(btnNuevoCliente);
        nuevoClienteRow.setAlignment(Pos.CENTER_RIGHT);

        // Lista de resultados de clientes
        listClientes = new ListView<>();
        listClientes.getStyleClass().add("quote-list");
        listClientes.setPrefHeight(100);
        listClientes.setVisible(false);
        listClientes.setManaged(false);
        listClientes.setOnMouseClicked(e -> seleccionarCliente());

        lblClienteSeleccionado = new Label("Sin cliente seleccionado");
        lblClienteSeleccionado.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        lblClienteSeleccionado.setWrapText(true);
        lblClienteSeleccionado.getStyleClass().add("quote-client-empty");

        secCliente.getChildren().addAll(buscarRow, listClientes, lblClienteSeleccionado, nuevoClienteRow);

        // Seccion catalogo
        VBox secCatalogo = seccion("2", "Agregar producto");

        txtBuscarProducto = campo("Buscar producto...");
        txtBuscarProducto.textProperty().addListener((obs, o, nv) -> filtrarProductos(nv));

        listProductos = new ListView<>();
        listProductos.getStyleClass().add("quote-list");
        listProductos.setPrefHeight(170);
        listProductos.setMinHeight(140);
        listProductos.getSelectionModel().selectedIndexProperty().addListener(
                (obs, anterior, actual) -> mostrarDetalleProducto(actual.intValue()));
        listProductos.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String texto, boolean empty) {
                super.updateItem(texto, empty);
                if (empty || texto == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText(texto);
                int index = getIndex();
                setGraphic(index >= 0 && index < productosFiltradosLista.size()
                        ? ProductoImageHelper.crearVista(productosFiltradosLista.get(index).getImagenUrl(), 42, 42)
                        : null);
            }
        });

        Spinner<Integer> spinnerCantidad = new Spinner<>(1, 9999, 1);
        spinnerCantidad.setEditable(true);
        spinnerCantidad.setPrefWidth(100);

        Button btnAgregar = boton("Agregar a cotizacion", "#1A8A2A");
        btnAgregar.setMaxWidth(Double.MAX_VALUE);
        btnAgregar.setOnAction(e -> agregarProducto(spinnerCantidad.getValue()));

        Label lblCantidad = new Label("Cantidad:");
        lblCantidad.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        HBox cantRow = new HBox(8, lblCantidad, spinnerCantidad);
        cantRow.setAlignment(Pos.CENTER_LEFT);

        detalleProducto = crearDetalleProducto();
        detalleProducto.setMaxHeight(78);
        HBox controlesProducto = new HBox(10, cantRow, btnAgregar);
        controlesProducto.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(btnAgregar, Priority.ALWAYS);
        secCatalogo.getChildren().addAll(txtBuscarProducto, listProductos, controlesProducto, detalleProducto);
        leftCol.getChildren().addAll(secCliente, secCatalogo);

        // ── Columna derecha ───────────────────────────────────────────────
        VBox rightCol = new VBox(12);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        VBox secCotizacion = seccion("3", "Revisar cotización");

        tablaCotizacion = new TableView<>(itemsCotizacion);
        tablaCotizacion.getStyleClass().add("quote-items");
        tablaCotizacion.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tablaCotizacion.setPrefHeight(220);
        tablaCotizacion.setMinHeight(175);
        tablaCotizacion.setPlaceholder(new Label("Agrega productos desde el panel izquierdo."));

        TableColumn<ItemCotizacion, String> colProd = new TableColumn<>("Producto");
        colProd.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));

        TableColumn<ItemCotizacion, Double> colPrecio = new TableColumn<>("Precio Unit.");
        colPrecio.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getPrecioUnit()).asObject());
        colPrecio.setCellFactory(c -> precioCell());
        colPrecio.setMaxWidth(110);

        TableColumn<ItemCotizacion, Integer> colCant = new TableColumn<>("Cant.");
        colCant.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCantidad()).asObject());
        colCant.setMaxWidth(70);

        TableColumn<ItemCotizacion, Double> colSub = new TableColumn<>("Subtotal");
        colSub.setCellValueFactory(d -> new SimpleDoubleProperty(
                d.getValue().getPrecioUnit() * d.getValue().getCantidad()).asObject());
        colSub.setCellFactory(c -> precioCell());
        colSub.setMaxWidth(110);

        TableColumn<ItemCotizacion, Void> colElim = new TableColumn<>("");
        colElim.setMaxWidth(50);
        colElim.setCellFactory(c -> new TableCell<>() {
            private final Button btn = new Button("X");
            {
                btn.setStyle("-fx-background-color: #C83C3C; -fx-text-fill: white; " +
                             "-fx-cursor: hand; -fx-border-width: 0;");
                btn.setOnAction(e -> {
                    itemsCotizacion.remove(getTableView().getItems().get(getIndex()));
                    actualizarTotales();
                });
            }
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });

        tablaCotizacion.getColumns().addAll(colProd, colPrecio, colCant, colSub, colElim);

        // Totales
        GridPane gridTotales = new GridPane();
        gridTotales.setHgap(15);
        gridTotales.setVgap(6);
        gridTotales.setPadding(new Insets(10, 0, 0, 0));
        gridTotales.setAlignment(Pos.CENTER_RIGHT);

        lblSubtotal  = totalLabel("$0.00");
        lblDescuento = totalLabel("$0.00");
        lblTotal = new Label("$0.00");
        lblTotal.getStyleClass().add("quote-total");

        txtDescuento = campo("0");
        txtDescuento.setPrefWidth(80);
        txtDescuento.textProperty().addListener((obs, o, nv) -> actualizarTotales());

        gridTotales.add(etiqueta("Subtotal:"),   0, 0); gridTotales.add(lblSubtotal,  1, 0);
        gridTotales.add(etiqueta("Desc. ($):"),  0, 1); gridTotales.add(txtDescuento, 1, 1);
        gridTotales.add(etiqueta("TOTAL:"),      0, 2); gridTotales.add(lblTotal,     1, 2);

        secCotizacion.getChildren().addAll(tablaCotizacion, gridTotales);

        // Observaciones y acciones
        VBox secAcciones = seccion("4", "Finalizar");
        txtObservaciones = new TextArea();
        txtObservaciones.setPromptText("Notas adicionales para el cliente...");
        txtObservaciones.setPrefRowCount(3);
        txtObservaciones.setWrapText(true);
        txtObservaciones.setFont(Font.font("Arial", 12));

        HBox btnAcciones = new HBox(10);
        btnAcciones.setAlignment(Pos.CENTER_RIGHT);
        btnAcciones.getStyleClass().add("quote-action-bar");

        Button btnLimpiar  = boton("Limpiar todo",        "#646464");
        Button btnGenerar  = boton("Generar cotización",  "#087FBD");
        Button btnConfirmar = boton("Confirmar venta",    "#1A9B43");
        btnGenerar.setStyle("");
        btnConfirmar.setStyle("");
        btnGenerar.getStyleClass().add("quote-primary-action");
        btnConfirmar.getStyleClass().add("quote-success-action");

        btnLimpiar.setOnAction(e   -> limpiarCotizacion());
        btnGenerar.setOnAction(e   -> generarCotizacion());
        btnConfirmar.setOnAction(e -> confirmarVenta());

        btnAcciones.getChildren().addAll(btnLimpiar, btnGenerar, btnConfirmar);
        secAcciones.getChildren().addAll(txtObservaciones, btnAcciones);

        rightCol.getChildren().addAll(secCotizacion, secAcciones);
        bodyLayout.getChildren().addAll(leftCol, rightCol);
        root.getChildren().addAll(encabezado, bodyLayout);
    }

    // ── Busqueda de clientes CORREGIDA ────────────────────────────────────────
    /**
     * Antes usaba autenticarCliente(query, "") que requiere contraseña.
     * Ahora usa personaServices.buscarClientes(query) que hace LIKE por
     * nombre, apellido o email sin necesitar contraseña.
     */
    private void buscarCliente() {
        String query = txtBuscarCliente.getText().trim();
        if (query.isEmpty()) {
            info("Ingresa un nombre o correo para buscar.");
            return;
        }

        List<Cliente> encontrados = personaServices.buscarClientes(query);
        clientesEncontrados.clear();
        listClientes.getItems().clear();

        if (encontrados.isEmpty()) {
            lblClienteSeleccionado.setText("No se encontro ningun cliente con: " + query);
            lblClienteSeleccionado.getStyleClass().remove("quote-client-selected");
            lblClienteSeleccionado.getStyleClass().add("quote-client-empty");
            listClientes.setVisible(false);
            listClientes.setManaged(false);
            preguntarRegistrarCliente(query);
            return;
        }

        for (Cliente c : encontrados) {
            clientesEncontrados.add(c);
            listClientes.getItems().add(
                c.getNombre() + " " + c.getApellido() + " — " + c.getEmail());
        }

        listClientes.setVisible(true);
        listClientes.setManaged(true);

        // Si hay un solo resultado, seleccionarlo directamente
        if (encontrados.size() == 1) {
            listClientes.getSelectionModel().selectFirst();
            seleccionarCliente();
        }
    }

    private void preguntarRegistrarCliente(String consulta) {
        Alert alerta = new Alert(
                Alert.AlertType.CONFIRMATION,
                "No se encontró un cliente con:\n\"" + consulta
                        + "\"\n\n¿Deseas registrarlo ahora?",
                ButtonType.YES,
                ButtonType.NO);
        alerta.setTitle("Cliente no encontrado");
        alerta.setHeaderText("Registrar nuevo cliente");

        alerta.showAndWait().ifPresent(respuesta -> {
            if (respuesta == ButtonType.YES) {
                registrarNuevoCliente();
            }
        });
    }

    private void seleccionarCliente() {
        int idx = listClientes.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= clientesEncontrados.size()) return;

        clienteSeleccionado = clientesEncontrados.get(idx);
        lblClienteSeleccionado.setText(
            "Seleccionado: " + clienteSeleccionado.getNombre() +
            " " + clienteSeleccionado.getApellido() +
            "\n" + clienteSeleccionado.getEmail());
        lblClienteSeleccionado.getStyleClass().remove("quote-client-empty");
        lblClienteSeleccionado.getStyleClass().add("quote-client-selected");

        // Ocultar la lista una vez seleccionado
        listClientes.setVisible(false);
        listClientes.setManaged(false);
    }

    private void registrarNuevoCliente() {
        Window owner = root.getScene() == null ? null : root.getScene().getWindow();
        ClienteCotizacionView registro = new ClienteCotizacionView(owner);
        if (registro.isRegistradoExitoso()) {
            lblClienteSeleccionado.setText(
                    "Cliente registrado. Busca su nombre o correo para seleccionarlo.");
            lblClienteSeleccionado.getStyleClass().remove("quote-client-selected");
            lblClienteSeleccionado.getStyleClass().add("quote-client-empty");
            txtBuscarCliente.requestFocus();
        }
    }

    // ── Carga y filtrado de productos ─────────────────────────────────────────
    private void cargarProductos() {
        productosFiltradosLista.clear();
        listProductos.getItems().clear();
        try {
            for (Producto p : productoServices.obtenerTodosLosProductos()) {
                if (Boolean.TRUE.equals(p.getActivo()) && p.getCantidad() > 0) {
                    productosFiltradosLista.add(p);
                    listProductos.getItems().add(String.format(
                        "[%d] %s — $%.2f  (stock: %d)",
                        p.getIdProducto(), p.getNombre(),
                        p.getPrecioVenta(), p.getCantidad()));
                }
            }
        } catch (Exception e) {
            listProductos.getItems().add("Sin conexion a base de datos");
        }
    }

    private void filtrarProductos(String query) {
        productosFiltradosLista.clear();
        listProductos.getItems().clear();
        try {
            String q = query.toLowerCase();
            for (Producto p : productoServices.obtenerTodosLosProductos()) {
                if (Boolean.TRUE.equals(p.getActivo()) && p.getCantidad() > 0 &&
                        (q.isEmpty() || p.getNombre().toLowerCase().contains(q))) {
                    productosFiltradosLista.add(p);
                    listProductos.getItems().add(String.format(
                        "[%d] %s — $%.2f",
                        p.getIdProducto(), p.getNombre(), p.getPrecioVenta()));
                }
            }
            mostrarDetalleProducto(-1);
        } catch (Exception ignored) {}
    }

    private VBox crearDetalleProducto() {
        detalleImagen = new StackPane();
        detalleImagen.setMinSize(58, 58);
        detalleImagen.setPrefSize(58, 58);
        detalleImagen.setStyle("-fx-background-color: #dceff6; -fx-background-radius: 6px;");

        lblDetalleNombre = new Label("Selecciona un producto");
        lblDetalleNombre.getStyleClass().add("quote-product-name");
        lblDetalleNombre.setWrapText(true);

        lblDetalleMeta = new Label("Verás aquí el precio y el stock disponible");
        lblDetalleMeta.getStyleClass().add("quote-product-meta");
        lblDetalleMeta.setWrapText(true);

        lblDetalleDescripcion = new Label("Selecciona un producto de la lista para consultar sus detalles.");
        lblDetalleDescripcion.getStyleClass().add("quote-product-description");
        lblDetalleDescripcion.setWrapText(true);

        VBox textos = new VBox(5, lblDetalleNombre, lblDetalleMeta, lblDetalleDescripcion);
        HBox contenido = new HBox(12, detalleImagen, textos);
        HBox.setHgrow(textos, Priority.ALWAYS);

        detalleProducto = new VBox(contenido);
        detalleProducto.getStyleClass().add("quote-product-detail");
        return detalleProducto;
    }

    private void mostrarDetalleProducto(int indice) {
        if (indice < 0 || indice >= productosFiltradosLista.size()) {
            if (lblDetalleNombre != null) {
                lblDetalleNombre.setText("Selecciona un producto");
                lblDetalleMeta.setText("Verás aquí el precio y el stock disponible");
                lblDetalleDescripcion.setText("Selecciona un producto de la lista para consultar sus detalles.");
                detalleImagen.getChildren().clear();
            }
            return;
        }

        Producto producto = productosFiltradosLista.get(indice);
        lblDetalleNombre.setText(producto.getNombre());
        lblDetalleMeta.setText(String.format("$%,.0f  |  Stock disponible: %d",
                producto.getPrecioVenta(), producto.getCantidad()));
        String descripcion = producto.getDescripcion();
        lblDetalleDescripcion.setText(descripcion == null || descripcion.isBlank()
                ? "Este producto no tiene descripción registrada."
                : descripcion);

        detalleImagen.getChildren().clear();
        Node imagen = ProductoImageHelper.crearVista(producto.getImagenUrl(), 54, 54);
        if (imagen != null) {
            detalleImagen.getChildren().add(imagen);
        }
    }

    // ── Acciones ─────────────────────────────────────────────────────────────
    private void agregarProducto(int cantidad) {
        int idx = listProductos.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= productosFiltradosLista.size()) {
            info("Selecciona un producto de la lista.");
            return;
        }
        Producto p = productosFiltradosLista.get(idx);
        if (cantidad <= 0) { info("La cantidad debe ser mayor a 0."); return; }
        if (cantidad > p.getCantidad()) {
            info("Stock insuficiente. Disponible: " + p.getCantidad()); return;
        }

        for (ItemCotizacion item : itemsCotizacion) {
            if (item.getIdProducto() == p.getIdProducto()) {
                item.setCantidad(item.getCantidad() + cantidad);
                tablaCotizacion.refresh();
                actualizarTotales();
                return;
            }
        }

        itemsCotizacion.add(new ItemCotizacion(
            p.getIdProducto(), p.getNombre(), p.getPrecioVenta(), cantidad));
        actualizarTotales();
    }

    private void actualizarTotales() {
        double subtotal = itemsCotizacion.stream()
                .mapToDouble(i -> i.getPrecioUnit() * i.getCantidad()).sum();
        double descuento;
        try { descuento = Double.parseDouble(txtDescuento.getText().trim()); }
        catch (NumberFormatException e) { descuento = 0; }
        double total = Math.max(0, subtotal - descuento);

        lblSubtotal.setText(String.format("$%.2f", subtotal));
        lblDescuento.setText(String.format("$%.2f", descuento));
        lblTotal.setText(String.format("$%.2f", total));
    }

    private void generarCotizacion() {
        if (itemsCotizacion.isEmpty()) { info("Agrega al menos un producto."); return; }
        if (clienteSeleccionado == null) {
            info("Selecciona un cliente antes de generar la cotización.");
            return;
        }

        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar cotización en PDF");
        selector.setInitialFileName("cotizacion-" + LocalDate.now() + ".pdf");
        selector.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));
        Window owner = root.getScene() == null ? null : root.getScene().getWindow();
        File destino = selector.showSaveDialog(owner);
        if (destino == null) {
            return;
        }
        if (!destino.getName().toLowerCase().endsWith(".pdf")) {
            destino = new File(destino.getAbsolutePath() + ".pdf");
        }

        double descuento;
        try {
            descuento = Double.parseDouble(txtDescuento.getText().trim());
        } catch (NumberFormatException e) {
            descuento = 0;
        }
        try {
            CotizacionPdfGenerator.generar(destino.toPath(), clienteSeleccionado,
                    itemsCotizacion, descuento, txtObservaciones.getText());
            info("Cotización PDF generada correctamente en:\n" + destino.getAbsolutePath());
        } catch (IOException | RuntimeException e) {
            String detalle = e.getMessage() == null || e.getMessage().isBlank()
                    ? e.getClass().getSimpleName()
                    : e.getMessage();
            new Alert(Alert.AlertType.ERROR,
                    "No se pudo generar el PDF.\n\n" + detalle, ButtonType.OK).showAndWait();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("===========================================\n");
        sb.append("           COTIZACION TECHZONE\n");
        sb.append("===========================================\n");
        sb.append("Fecha: ").append(LocalDate.now()).append("\n");
        if (clienteSeleccionado != null)
            sb.append("Cliente: ").append(clienteSeleccionado.getNombre())
              .append(" ").append(clienteSeleccionado.getApellido()).append("\n");
        sb.append("-------------------------------------------\n");
        sb.append(String.format("%-25s %8s %6s %12s\n", "PRODUCTO", "PRECIO", "CANT", "SUBTOTAL"));
        sb.append("-------------------------------------------\n");

        for (ItemCotizacion item : itemsCotizacion) {
            sb.append(String.format("%-25s %8.2f %6d %12.2f\n",
                truncar(item.getNombre(), 25),
                item.getPrecioUnit(), item.getCantidad(),
                item.getPrecioUnit() * item.getCantidad()));
        }

        sb.append("-------------------------------------------\n");
        sb.append("Subtotal:   ").append(lblSubtotal.getText()).append("\n");
        sb.append("Descuento:  ").append(lblDescuento.getText()).append("\n");
        sb.append("TOTAL:      ").append(lblTotal.getText()).append("\n");
        sb.append("===========================================\n");
        if (!txtObservaciones.getText().trim().isEmpty())
            sb.append("\nObservaciones:\n").append(txtObservaciones.getText());

        TextArea txt = new TextArea(sb.toString());
        txt.setEditable(false);
        txt.setFont(Font.font("Monospaced", 12));
        txt.setPrefSize(550, 420);

        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Cotizacion generada");
        dlg.getDialogPane().setContent(new ScrollPane(txt));
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.showAndWait();
    }

    private void confirmarVenta() {
        if (itemsCotizacion.isEmpty()) { info("Agrega al menos un producto."); return; }
        if (clienteSeleccionado == null) { info("Selecciona un cliente antes de confirmar."); return; }

        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
            "Confirmar la venta por " + lblTotal.getText() +
            " para " + clienteSeleccionado.getNombre() + "?",
            ButtonType.YES, ButtonType.NO);
        conf.setTitle("Confirmar Venta");
        conf.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                info("Venta registrada exitosamente.");
                limpiarCotizacion();
            }
        });
    }

    private void limpiarCotizacion() {
        itemsCotizacion.clear();
        clienteSeleccionado = null;
        txtBuscarCliente.clear();
        txtDescuento.setText("0");
        txtObservaciones.clear();
        lblClienteSeleccionado.setText("Sin cliente seleccionado");
        lblClienteSeleccionado.getStyleClass().remove("quote-client-selected");
        lblClienteSeleccionado.getStyleClass().add("quote-client-empty");
        listClientes.getItems().clear();
        listClientes.setVisible(false);
        listClientes.setManaged(false);
        actualizarTotales();
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────
    private VBox seccion(String paso, String titulo) {
        VBox sec = new VBox(10);
        sec.getStyleClass().add("quote-section");
        HBox encabezado = new HBox(8);
        encabezado.setAlignment(Pos.CENTER_LEFT);
        Label lblPaso = new Label(paso);
        lblPaso.getStyleClass().add("quote-step");
        Label lbl = new Label(titulo);
        lbl.getStyleClass().add("quote-section-title");
        encabezado.getChildren().addAll(lblPaso, lbl);
        sec.getChildren().add(encabezado);
        return sec;
    }

    private TextField campo(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setFont(Font.font("Arial", 12));
        tf.getStyleClass().add("quote-search");
        return tf;
    }

    private Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        l.setTextFill(Color.web("#555555"));
        return l;
    }

    private Label totalLabel(String valor) {
        Label l = new Label(valor);
        l.setFont(Font.font("Arial", 14));
        l.setTextFill(Color.web("#0A1933"));
        return l;
    }

    private Button boton(String texto, String color) {
        Button b = new Button(texto);
        b.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        b.setTextFill(Color.WHITE);
        b.setStyle("-fx-background-color:" + color +
                   ";-fx-border-width:0;-fx-cursor:hand;-fx-padding:8 14 8 14;");
        return b;
    }

    private TableCell<ItemCotizacion, Double> precioCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("$%.2f", v));
            }
        };
    }

    private String truncar(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "..." : s;
    }

    private void info(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    // ── Modelo de item ────────────────────────────────────────────────────────
    public static class ItemCotizacion {
        private final int    idProducto;
        private final String nombre;
        private final double precioUnit;
        private int          cantidad;

        public ItemCotizacion(int idProducto, String nombre, double precioUnit, int cantidad) {
            this.idProducto = idProducto;
            this.nombre     = nombre;
            this.precioUnit = precioUnit;
            this.cantidad   = cantidad;
        }

        public int    getIdProducto() { return idProducto; }
        public String getNombre()     { return nombre; }
        public double getPrecioUnit() { return precioUnit; }
        public int    getCantidad()   { return cantidad; }
        public void   setCantidad(int c) { this.cantidad = c; }
    }
}
