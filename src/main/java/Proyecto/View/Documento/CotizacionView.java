package Proyecto.View.Documento;

import Proyecto.Model.Cliente;
import Proyecto.Model.Producto;
import Proyecto.services.DocumentoServices;
import Proyecto.services.PersonaServices;
import Proyecto.services.ProductoServices;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CotizacionView {

    private final ProductoServices   productoServices;
    private final PersonaServices    personaServices;
    private final DocumentoServices  documentoServices;

    private ObservableList<ItemCotizacion> itemsCotizacion;
    private List<Producto>                 productosFiltrados;

    private TextField     txtBuscarCliente;
    private Label         lblClienteSeleccionado;
    private Cliente       clienteSeleccionado;

    // Lista de resultados de la busqueda de clientes
    private ListView<String> listClientes;
    private List<Cliente>    clientesEncontrados;

    private TextField     txtBuscarProducto;
    private ListView<String> listProductos;
    private List<Producto>   productosFiltradosLista;

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
        this.documentoServices   = new DocumentoServices();
        this.itemsCotizacion     = FXCollections.observableArrayList();
        this.productosFiltrados  = new ArrayList<>();
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
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: white;");
        VBox.setVgrow(root, Priority.ALWAYS);

        Label lblTitulo = new Label("Generacion de Cotizacion");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        lblTitulo.setTextFill(Color.web("#0A1933"));

        HBox bodyLayout = new HBox(15);
        VBox.setVgrow(bodyLayout, Priority.ALWAYS);

        // ── Columna izquierda ─────────────────────────────────────────────
        VBox leftCol = new VBox(12);
        leftCol.setPrefWidth(320);
        leftCol.setMaxWidth(340);

        // Seccion cliente
        VBox secCliente = seccion("Cliente");

        txtBuscarCliente = campo("Buscar por nombre o correo...");

        Button btnBuscarCliente = boton("Buscar", "#00C8FF");
        btnBuscarCliente.setOnAction(e -> buscarCliente());

        HBox buscarRow = new HBox(8, txtBuscarCliente, btnBuscarCliente);
        HBox.setHgrow(txtBuscarCliente, Priority.ALWAYS);

        // Lista de resultados de clientes
        listClientes = new ListView<>();
        listClientes.setPrefHeight(100);
        listClientes.setVisible(false);
        listClientes.setManaged(false);
        listClientes.setOnMouseClicked(e -> seleccionarCliente());

        lblClienteSeleccionado = new Label("Sin cliente seleccionado");
        lblClienteSeleccionado.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        lblClienteSeleccionado.setTextFill(Color.GRAY);
        lblClienteSeleccionado.setWrapText(true);

        secCliente.getChildren().addAll(buscarRow, listClientes, lblClienteSeleccionado);

        // Seccion catalogo
        VBox secCatalogo = seccion("Agregar Producto");
        VBox.setVgrow(secCatalogo, Priority.ALWAYS);

        txtBuscarProducto = campo("Buscar producto...");
        txtBuscarProducto.textProperty().addListener((obs, o, nv) -> filtrarProductos(nv));

        listProductos = new ListView<>();
        listProductos.setPrefHeight(200);
        VBox.setVgrow(listProductos, Priority.ALWAYS);

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

        secCatalogo.getChildren().addAll(txtBuscarProducto, listProductos, cantRow, btnAgregar);
        leftCol.getChildren().addAll(secCliente, secCatalogo);

        // ── Columna derecha ───────────────────────────────────────────────
        VBox rightCol = new VBox(12);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        VBox secCotizacion = seccion("Items de la Cotizacion");
        VBox.setVgrow(secCotizacion, Priority.ALWAYS);

        tablaCotizacion = new TableView<>(itemsCotizacion);
        tablaCotizacion.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(tablaCotizacion, Priority.ALWAYS);
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
        lblTotal     = new Label("$0.00");
        lblTotal.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        lblTotal.setTextFill(Color.web("#0A1933"));

        txtDescuento = campo("0");
        txtDescuento.setPrefWidth(80);
        txtDescuento.textProperty().addListener((obs, o, nv) -> actualizarTotales());

        gridTotales.add(etiqueta("Subtotal:"),   0, 0); gridTotales.add(lblSubtotal,  1, 0);
        gridTotales.add(etiqueta("Desc. ($):"),  0, 1); gridTotales.add(txtDescuento, 1, 1);
        gridTotales.add(etiqueta("TOTAL:"),      0, 2); gridTotales.add(lblTotal,     1, 2);

        secCotizacion.getChildren().addAll(tablaCotizacion, gridTotales);

        // Observaciones y acciones
        VBox secAcciones = seccion("Observaciones y Acciones");
        txtObservaciones = new TextArea();
        txtObservaciones.setPromptText("Notas adicionales para el cliente...");
        txtObservaciones.setPrefRowCount(3);
        txtObservaciones.setWrapText(true);
        txtObservaciones.setFont(Font.font("Arial", 12));

        HBox btnAcciones = new HBox(10);
        btnAcciones.setAlignment(Pos.CENTER_RIGHT);

        Button btnLimpiar  = boton("Limpiar todo",        "#646464");
        Button btnGenerar  = boton("Generar Cotizacion",  "#0A1933");
        Button btnConfirmar = boton("Confirmar Venta",    "#1A8A2A");

        btnLimpiar.setOnAction(e   -> limpiarCotizacion());
        btnGenerar.setOnAction(e   -> generarCotizacion());
        btnConfirmar.setOnAction(e -> confirmarVenta());

        btnAcciones.getChildren().addAll(btnLimpiar, btnGenerar, btnConfirmar);
        secAcciones.getChildren().addAll(txtObservaciones, btnAcciones);

        rightCol.getChildren().addAll(secCotizacion, secAcciones);
        bodyLayout.getChildren().addAll(leftCol, rightCol);
        root.getChildren().addAll(lblTitulo, bodyLayout);
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
            lblClienteSeleccionado.setTextFill(Color.web("#C83C3C"));
            listClientes.setVisible(false);
            listClientes.setManaged(false);
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

    private void seleccionarCliente() {
        int idx = listClientes.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= clientesEncontrados.size()) return;

        clienteSeleccionado = clientesEncontrados.get(idx);
        lblClienteSeleccionado.setText(
            "Seleccionado: " + clienteSeleccionado.getNombre() +
            " " + clienteSeleccionado.getApellido() +
            "\n" + clienteSeleccionado.getEmail());
        lblClienteSeleccionado.setTextFill(Color.web("#1A8A2A"));

        // Ocultar la lista una vez seleccionado
        listClientes.setVisible(false);
        listClientes.setManaged(false);
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
        } catch (Exception ignored) {}
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
        lblClienteSeleccionado.setTextFill(Color.GRAY);
        listClientes.getItems().clear();
        listClientes.setVisible(false);
        listClientes.setManaged(false);
        actualizarTotales();
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────
    private VBox seccion(String titulo) {
        VBox sec = new VBox(10);
        sec.setPadding(new Insets(15));
        sec.setStyle("-fx-background-color: #FAFAFA; -fx-border-color: #E0E0E0; -fx-border-width: 1;");
        Label lbl = new Label(titulo);
        lbl.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        lbl.setTextFill(Color.web("#0A1933"));
        sec.getChildren().add(lbl);
        return sec;
    }

    private TextField campo(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setFont(Font.font("Arial", 12));
        tf.setStyle("-fx-border-color: #C0C0C0; -fx-border-width: 1; -fx-padding: 7;");
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
