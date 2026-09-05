package Proyecto.View.Producto;

import Proyecto.Model.Cliente;
import Proyecto.Model.Producto;
import Proyecto.services.CarritoServices;
import Proyecto.services.ProductoServices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

public class ProductoView {

    private final ProductoServices productoServices;
    private final CarritoServices  carritoServices;
    private final Cliente          clienteActual;

    private TableView<FilaProducto>      tablaProductos;
    private ObservableList<FilaProducto> datos;
    private TextField                    txtBuscar;
    private VBox                         root;

    // Constructor sin cliente (compatibilidad)
    public ProductoView() {
        this(null);
    }

    // Constructor con cliente de sesión activa
    public ProductoView(Cliente cliente) {
        this.clienteActual    = cliente;
        this.productoServices = new ProductoServices();
        this.carritoServices  = new CarritoServices();
        this.datos            = FXCollections.observableArrayList();
        initComponents();
        cargarProductos();
    }

    public Node getRoot() { return root; }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        root = new VBox(15);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: white;");
        VBox.setVgrow(root, Priority.ALWAYS);

        HBox topPanel = new HBox(10);
        topPanel.setAlignment(Pos.CENTER_LEFT);

        Label lblTitulo = new Label("Catálogo de Productos");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        lblTitulo.setTextFill(Color.web("#0A1933"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        txtBuscar = new TextField();
        txtBuscar.setPromptText("Buscar producto...");
        txtBuscar.setPrefWidth(220);
        txtBuscar.setStyle("-fx-border-color: #C8C8C8; -fx-border-width: 1; -fx-padding: 8;");

        Button btnBuscar = crearBoton("Buscar", "#00C8FF");
        btnBuscar.setOnAction(e -> buscarProductos());

        topPanel.getChildren().addAll(lblTitulo, spacer, txtBuscar, btnBuscar);

        tablaProductos = new TableView<>();
        tablaProductos.setItems(datos);
        tablaProductos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(tablaProductos, Priority.ALWAYS);

        TableColumn<FilaProducto, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setMaxWidth(60);

        TableColumn<FilaProducto, String> colNombre = new TableColumn<>("Producto");
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));

        TableColumn<FilaProducto, String> colCategoria = new TableColumn<>("Categoría");
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));

        TableColumn<FilaProducto, String> colPrecio = new TableColumn<>("Precio");
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioDisplay"));
        colPrecio.setMaxWidth(130);

        TableColumn<FilaProducto, Integer> colStock = new TableColumn<>("Stock");
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colStock.setMaxWidth(80);

        TableColumn<FilaProducto, Void> colAccion = new TableColumn<>("Acción");
        colAccion.setMaxWidth(130);
        colAccion.setCellFactory(col -> new TableCell<>() {
            private final Button btn = crearBoton(" Comprar", "#00C8FF");
            {
                btn.setOnAction(e -> {
                    FilaProducto fila = getTableView().getItems().get(getIndex());
                    procesarCompra(fila);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        tablaProductos.getStylesheets().add(
                "data:text/css," +
                ".table-view .column-header-background { -fx-background-color: #0A1933; }" +
                ".table-view .column-header .label { -fx-text-fill: white; -fx-font-weight: bold; }");

        tablaProductos.getColumns().addAll(colId, colNombre, colCategoria, colPrecio, colStock, colAccion);
        tablaProductos.setRowFactory(tv -> {
            TableRow<FilaProducto> row = new TableRow<>();
            row.setStyle("-fx-cell-size: 40px;");
            return row;
        });

        HBox bottomPanel = new HBox();
        bottomPanel.setAlignment(Pos.CENTER);
        Button btnActualizar = crearBoton("Actualizar", "#646464");
        btnActualizar.setOnAction(e -> cargarProductos());
        bottomPanel.getChildren().add(btnActualizar);

        root.getChildren().addAll(topPanel, tablaProductos, bottomPanel);
    }

    private void cargarProductos() {
        datos.clear();
        for (Producto p : productoServices.obtenerTodosLosProductos()) {
            if (Boolean.TRUE.equals(p.getActivo())) {
                datos.add(new FilaProducto(
                        p.getIdProducto(), p.getNombre(),
                        p.getCategoria() != null ? p.getCategoria().getNombre() : "Sin categoría",
                        p.getPrecioVenta(), p.getCantidad()));
            }
        }
    }

    private void buscarProductos() {
        String q = txtBuscar.getText().trim();
        datos.clear();
        List<Producto> lista = q.isEmpty()
                ? productoServices.obtenerTodosLosProductos()
                : productoServices.buscarProductos(q);
        for (Producto p : lista) {
            if (Boolean.TRUE.equals(p.getActivo())) {
                datos.add(new FilaProducto(
                        p.getIdProducto(), p.getNombre(),
                        p.getCategoria() != null ? p.getCategoria().getNombre() : "Sin categoría",
                        p.getPrecioVenta(), p.getCantidad()));
            }
        }
    }

    // ── PROCESARCOMPRA: ahora hace la llamada real al carrito ──────────────────
    private void procesarCompra(FilaProducto fila) {
        if (fila.getStock() <= 0) {
            alert(Alert.AlertType.WARNING, "Sin stock", "No hay stock disponible.");
            return;
        }

        if (clienteActual == null) {
            alert(Alert.AlertType.WARNING, "Sesión requerida",
                    "Debes iniciar sesión para agregar productos al carrito.");
            return;
        }

        TextInputDialog dlg = new TextInputDialog("1");
        dlg.setTitle("Agregar al Carrito");
        dlg.setHeaderText("Producto: " + fila.getNombre()
                + "\nPrecio:   " + fila.getPrecioDisplay()
                + "\nStock:    " + fila.getStock());
        dlg.setContentText("Cantidad:");

        dlg.showAndWait().ifPresent(cantStr -> {
            int cantidad;
            try {
                cantidad = Integer.parseInt(cantStr.trim());
            } catch (NumberFormatException ex) {
                alert(Alert.AlertType.ERROR, "Error", "Ingresa un número entero válido.");
                return;
            }

            if (cantidad <= 0) {
                alert(Alert.AlertType.ERROR, "Error", "La cantidad debe ser mayor a 0.");
                return;
            }
            if (cantidad > fila.getStock()) {
                alert(Alert.AlertType.ERROR, "Stock insuficiente",
                        "Solo hay " + fila.getStock() + " unidades disponibles.");
                return;
            }

            // Llamada real a CarritoServices
            boolean ok = carritoServices.agregarProductoAlCarrito(
                    clienteActual.getId(), fila.getId(), cantidad);

            if (ok) {
                double subtotal = cantidad * fila.getPrecioDouble();
                alert(Alert.AlertType.INFORMATION, "Agregado",
                        String.format("✓ %s x%d agregado al carrito.%nSubtotal: $%,.2f",
                                fila.getNombre(), cantidad, subtotal));
                cargarProductos(); // refresca stock
            } else {
                alert(Alert.AlertType.ERROR, "Error",
                        "No se pudo agregar al carrito.\nVerifica la conexión a la base de datos.");
            }
        });
    }

    private void alert(Alert.AlertType tipo, String titulo, String msg) {
        Alert a = new Alert(tipo, msg, ButtonType.OK);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private Button crearBoton(String texto, String color) {
        Button btn = new Button(texto);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        btn.setTextFill(Color.WHITE);
        btn.setStyle("-fx-background-color: " + color
                + "; -fx-border-width: 0; -fx-cursor: hand; -fx-padding: 7 14 7 14;");
        return btn;
    }

    // ── Modelo de fila ─────────────────────────────────────────────────────────
    public static class FilaProducto {
        private final int    id;
        private final String nombre;
        private final String categoria;
        private final double precioDouble;
        private final String precioDisplay;
        private final int    stock;

        public FilaProducto(int id, String nombre, String categoria, double precio, int stock) {
            this.id            = id;
            this.nombre        = nombre;
            this.categoria     = categoria;
            this.precioDouble  = precio;
            this.precioDisplay = String.format("$%,.2f", precio);
            this.stock         = stock;
        }

        public int    getId()            { return id; }
        public String getNombre()        { return nombre; }
        public String getCategoria()     { return categoria; }
        public double getPrecioDouble()  { return precioDouble; }
        public String getPrecioDisplay() { return precioDisplay; }
        public String getPrecio()        { return precioDisplay; }
        public int    getStock()         { return stock; }
    }
}
