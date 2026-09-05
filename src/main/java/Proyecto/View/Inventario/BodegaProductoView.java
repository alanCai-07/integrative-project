package Proyecto.View.Inventario;

import Proyecto.Model.Categoria;
import Proyecto.Model.Producto;
import Proyecto.services.CategoriaServices;
import Proyecto.services.ProductoServices;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

public class BodegaProductoView {

    private static final String COLOR_AZUL = "#0A1933";
    private static final String COLOR_CYAN = "#00C8FF";
    private static final String COLOR_VERDE = "#1A8A2A";
    private static final String COLOR_ROJO = "#C83C3C";
    private static final String COLOR_NARANJA = "#FF9800";

    private final ProductoServices productoServices;
    private final CategoriaServices categoriaServices;

    private final ObservableList<FilaProducto> productos = FXCollections.observableArrayList();

    private TableView<FilaProducto> tablaProductos;
    private TextField txtBuscar;
    private VBox root;

    public BodegaProductoView() {
        this.productoServices = new ProductoServices();
        this.categoriaServices = new CategoriaServices();
        construir();
        cargarProductos();
    }

    public Node getRoot() {
        return root;
    }

    // =========================================================================
    // CONSTRUCCION DE LA INTERFAZ
    // =========================================================================

    @SuppressWarnings("unchecked")
    private void construir() {
        root = new VBox(15);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: white;");
        VBox.setVgrow(root, Priority.ALWAYS);

        // -- Titulo -----------------------------------------------------------
        Label lblTitulo = new Label("Gestion de Productos - Bodega");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        lblTitulo.setTextFill(Color.web(COLOR_AZUL));

        // -- Barra de busqueda y acciones -------------------------------------
        txtBuscar = new TextField();
        txtBuscar.setPromptText("Buscar por ID o nombre del producto...");
        txtBuscar.setPrefWidth(300);
        txtBuscar.setStyle("-fx-border-color: #C0C0C0; -fx-border-width:1; -fx-padding:7;");

        Button btnBuscar = boton("Buscar", COLOR_CYAN);
        Button btnActualizar = boton("Actualizar", COLOR_AZUL);
        Button btnNuevo = boton("Nuevo producto", COLOR_VERDE);
        Button btnInhabilitar = boton("Inhabilitar", COLOR_ROJO);
        Button btnHabilitar = boton("Habilitar", COLOR_NARANJA);

        btnBuscar.setOnAction(e -> buscarProductos());
        btnActualizar.setOnAction(e -> cargarProductos());
        btnNuevo.setOnAction(e -> abrirFormularioNuevo());
        btnInhabilitar.setOnAction(e -> cambiarEstado(false));
        btnHabilitar.setOnAction(e -> cambiarEstado(true));

        // Permitir busqueda con Enter
        txtBuscar.setOnAction(e -> buscarProductos());

        HBox barraBusqueda = new HBox(10,
                txtBuscar, btnBuscar,
                separadorVertical(),
                btnActualizar,
                separadorVertical(),
                btnNuevo, btnInhabilitar, btnHabilitar);
        barraBusqueda.setAlignment(Pos.CENTER_LEFT);

        // -- Tabla de productos -----------------------------------------------
        tablaProductos = new TableView<>(productos);
        tablaProductos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(tablaProductos, Priority.ALWAYS);
        tablaProductos.setPlaceholder(new Label("No se encontraron productos."));

        TableColumn<FilaProducto, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colId.setMaxWidth(60);

        TableColumn<FilaProducto, String> colNombre = new TableColumn<>("Nombre");
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));

        TableColumn<FilaProducto, String> colCategoria = new TableColumn<>("Categoria");
        colCategoria.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategoria()));
        colCategoria.setMaxWidth(130);

        TableColumn<FilaProducto, Double> colPrecioC = new TableColumn<>("P. Compra");
        colPrecioC.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getPrecioCompra()).asObject());
        colPrecioC.setCellFactory(c -> precioCell());
        colPrecioC.setMaxWidth(110);

        TableColumn<FilaProducto, Double> colPrecioV = new TableColumn<>("P. Venta");
        colPrecioV.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getPrecioVenta()).asObject());
        colPrecioV.setCellFactory(c -> precioCell());
        colPrecioV.setMaxWidth(110);

        TableColumn<FilaProducto, Integer> colStock = new TableColumn<>("Stock");
        colStock.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getStock()).asObject());
        colStock.setMaxWidth(70);
        colStock.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(String.valueOf(v));
                setStyle(v <= 0 ? "-fx-text-fill:" + COLOR_ROJO + ";-fx-font-weight:bold;" : "");
            }
        });

        TableColumn<FilaProducto, Integer> colStockMin = new TableColumn<>("Stock Min.");
        colStockMin.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getStockMinimo()).asObject());
        colStockMin.setMaxWidth(90);

        TableColumn<FilaProducto, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstado()));
        colEstado.setMaxWidth(90);
        colEstado.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(v);
                setStyle("Activo".equals(v)
                        ? "-fx-text-fill:" + COLOR_VERDE + ";-fx-font-weight:bold;"
                        : "-fx-text-fill:" + COLOR_ROJO + ";-fx-font-weight:bold;");
            }
        });

        tablaProductos.getColumns().addAll(
                colId, colNombre, colCategoria,
                colPrecioC, colPrecioV, colStock, colStockMin, colEstado);

        tablaProductos.setRowFactory(tv -> {
            TableRow<FilaProducto> row = new TableRow<>();
            row.setStyle("-fx-cell-size: 38px;");
            return row;
        });

        // Nota informativa
        Label lblNota = new Label(
                "Selecciona un producto en la tabla y usa los botones Inhabilitar / Habilitar para cambiar su estado.");
        lblNota.setFont(Font.font("Arial", 12));
        lblNota.setTextFill(Color.GRAY);
        lblNota.setWrapText(true);

        root.getChildren().addAll(lblTitulo, barraBusqueda, new Separator(), tablaProductos, lblNota);
    }

    // =========================================================================
    // CARGA Y BUSQUEDA
    // =========================================================================

    private void cargarProductos() {
        productos.clear();
        try {
            // Obtener todos los productos (activos e inactivos)
            List<Producto> lista = productoServices.obtenerTodosLosProductos();
            for (Producto p : lista) {
                productos.add(mapear(p));
            }
        } catch (Exception e) {
            mostrarError("Error al cargar productos: " + e.getMessage());
        }
    }

    private void buscarProductos() {
        String query = txtBuscar.getText().trim();
        productos.clear();

        if (query.isEmpty()) {
            cargarProductos();
            return;
        }

        try {
            // Busqueda por ID numerico
            boolean busquedarPorId = false;
            try {
                int id = Integer.parseInt(query);
                Producto p = productoServices.obtenerProducto(id);
                if (p != null) {
                    productos.add(mapear(p));
                    busquedarPorId = true;
                }
            } catch (NumberFormatException ignored) {
            }

            // Busqueda por nombre si no se encontro por ID
            if (!busquedarPorId) {
                List<Producto> lista = productoServices.buscarProductos(query);
                for (Producto p : lista) {
                    productos.add(mapear(p));
                }
            }

            if (productos.isEmpty()) {
                mostrarInfo("No se encontraron productos con: " + query);
            }

        } catch (Exception e) {
            mostrarError("Error en la busqueda: " + e.getMessage());
        }
    }

    // =========================================================================
    // ACCIONES
    // =========================================================================

    private void cambiarEstado(boolean activar) {
        FilaProducto seleccionado = tablaProductos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarInfo("Selecciona un producto de la tabla.");
            return;
        }

        String accion = activar ? "habilitar" : "inhabilitar";
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "Confirmar " + accion + " el producto:\n\"" + seleccionado.getNombre() + "\"",
                ButtonType.YES, ButtonType.NO);
        conf.setTitle("Confirmar cambio de estado");
        conf.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                try {
                    boolean ok;
                    if (activar) {
                        // Habilitar: actualizar campo activo = true directamente via DAO
                        // ProductoServices.eliminarProducto hace soft-delete (activo=false)
                        // No hay metodo de activacion en el servicio, lo agregamos aqui
                        // usando el DAO de forma directa a traves del servicio de actualizacion.
                        // Solucion: actualizar precio (mismo valor) fuerza el UPDATE y
                        // luego habilitamos via una llamada especifica.
                        ok = habilitarProducto(seleccionado.getId());
                    } else {
                        ok = productoServices.eliminarProducto(seleccionado.getId());
                    }

                    if (ok) {
                        mostrarInfo("Producto " + (activar ? "habilitado" : "inhabilitado") + " correctamente.");
                        cargarProductos();
                    } else {
                        mostrarError("No se pudo " + accion + " el producto.");
                    }
                } catch (Exception e) {
                    mostrarError("Error: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Habilita un producto (activo = true) accediendo directamente al DAO.
     * ProductoServices no expone este metodo porque el diseño original
     * solo contemplaba soft-delete. Lo implementamos aqui sin romper la
     * arquitectura existente.
     */
    private boolean habilitarProducto(int idProducto) {
        try {
            Proyecto.util.conexionBD conexion = null;
            String sql = "UPDATE producto SET activo = 1 WHERE id_producto = ?";
            try (java.sql.Connection conn = Proyecto.util.conexionBD.obtenerConexion();
                    java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idProducto);
                return ps.executeUpdate() > 0;
            }
        } catch (java.sql.SQLException e) {
            System.err.println("BodegaProductoView.habilitarProducto: " + e.getMessage());
            return false;
        }
    }

    private void abrirFormularioNuevo() {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Nuevo Producto");
        dlg.setResizable(false);

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        grid.setPadding(new Insets(25));

        ColumnConstraints col0 = new ColumnConstraints(150);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col0, col1);

        int fila = 0;

        // Titulo
        Label lblTitulo = new Label("NUEVO PRODUCTO - BODEGA");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        lblTitulo.setTextFill(Color.web(COLOR_AZUL));
        GridPane.setColumnSpan(lblTitulo, 2);
        GridPane.setHalignment(lblTitulo, HPos.CENTER);
        grid.add(lblTitulo, 0, fila++);

        Separator sep = new Separator();
        GridPane.setColumnSpan(sep, 2);
        grid.add(sep, 0, fila++);

        // Campos
        TextField txtNombre = campo("Nombre del producto");
        TextField txtDescripcion = campo("Descripcion breve");
        TextField txtPrecioCompra = campo("Precio de compra (sin puntos)");
        TextField txtPrecioVenta = campo("Precio de venta (sin puntos)");
        TextField txtStock = campo("Stock inicial");
        TextField txtStockMinimo = campo("Stock minimo para alertas");

        ComboBox<Categoria> cbCategoria = new ComboBox<>();
        cbCategoria.setMaxWidth(Double.MAX_VALUE);
        try {
            cbCategoria.getItems().addAll(categoriaServices.obtenerTodasLasCategorias());
            if (!cbCategoria.getItems().isEmpty())
                cbCategoria.getSelectionModel().selectFirst();
        } catch (Exception e) {
            cbCategoria.getItems().add(new Categoria(1, "General", ""));
        }

        Object[][] filas = {
                { "Nombre *", txtNombre },
                { "Descripcion", txtDescripcion },
                { "Categoria *", cbCategoria },
                { "Precio compra *", txtPrecioCompra },
                { "Precio venta *", txtPrecioVenta },
                { "Stock inicial", txtStock },
                { "Stock minimo", txtStockMinimo },
        };

        for (Object[] f : filas) {
            Label lbl = new Label((String) f[0]);
            lbl.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            grid.add(lbl, 0, fila);
            grid.add((Control) f[1], 1, fila++);
        }

        Label lblMsg = new Label("");
        lblMsg.setFont(Font.font("Arial", 12));
        lblMsg.setWrapText(true);
        GridPane.setColumnSpan(lblMsg, 2);
        GridPane.setHalignment(lblMsg, HPos.CENTER);
        grid.add(lblMsg, 0, fila);

        ButtonType btnGuardar = new ButtonType("Guardar producto", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);
        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().setPrefWidth(480);

        // Deshabilitar boton OK por defecto para hacer validacion manual
        javafx.scene.control.Button okBtn = (javafx.scene.control.Button) dlg.getDialogPane().lookupButton(btnGuardar);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                String nombre = txtNombre.getText().trim();
                if (nombre.isEmpty())
                    throw new IllegalArgumentException("El nombre es obligatorio.");

                Categoria cat = cbCategoria.getSelectionModel().getSelectedItem();
                if (cat == null)
                    throw new IllegalArgumentException("Selecciona una categoria.");

                double precioC = Double.parseDouble(txtPrecioCompra.getText().trim());
                double precioV = Double.parseDouble(txtPrecioVenta.getText().trim());
                if (precioC <= 0 || precioV <= 0)
                    throw new IllegalArgumentException("Los precios deben ser mayores a 0.");
                if (precioV < precioC)
                    throw new IllegalArgumentException("El precio de venta no puede ser menor al de compra.");

                int stock = txtStock.getText().trim().isEmpty()
                        ? 0
                        : Integer.parseInt(txtStock.getText().trim());
                if (stock < 0)
                    throw new IllegalArgumentException("El stock no puede ser negativo.");

                boolean ok = productoServices.crearProducto(
                        cat.getId(),
                        nombre,
                        txtDescripcion.getText().trim(),
                        precioC,
                        precioV,
                        stock);

                // Actualizar stock minimo si se especifico
                if (ok && !txtStockMinimo.getText().trim().isEmpty()) {
                    try {
                        int stockMin = Integer.parseInt(txtStockMinimo.getText().trim());
                        actualizarStockMinimo(nombre, stockMin);
                    } catch (Exception ignored) {
                    }
                }

                if (ok) {
                    mostrarInfo("Producto creado exitosamente.");
                    cargarProductos();
                } else {
                    lblMsg.setTextFill(Color.web(COLOR_ROJO));
                    lblMsg.setText("No se pudo crear el producto. Verifica los datos.");
                    event.consume(); // No cerrar el dialogo
                }

            } catch (NumberFormatException e) {
                lblMsg.setTextFill(Color.web(COLOR_ROJO));
                lblMsg.setText("Precios y stock deben ser numeros validos.");
                event.consume();
            } catch (IllegalArgumentException e) {
                lblMsg.setTextFill(Color.web(COLOR_ROJO));
                lblMsg.setText(e.getMessage());
                event.consume();
            }
        });

        dlg.setResultConverter(bt -> null);
        dlg.showAndWait();
    }

    private void actualizarStockMinimo(String nombreProducto, int stockMin) {
        try {
            String sql = "UPDATE producto SET stock_minimo = ? WHERE nombre = ? ORDER BY id_producto DESC LIMIT 1";
            try (java.sql.Connection conn = Proyecto.util.conexionBD.obtenerConexion();
                    java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, stockMin);
                ps.setString(2, nombreProducto);
                ps.executeUpdate();
            }
        } catch (java.sql.SQLException e) {
            System.err.println("BodegaProductoView.actualizarStockMinimo: " + e.getMessage());
        }
    }

    // =========================================================================
    // HELPERS UI
    // =========================================================================

    private FilaProducto mapear(Producto p) {
        String cat = (p.getCategoria() != null && p.getCategoria().getNombre() != null)
                ? p.getCategoria().getNombre()
                : "-";
        return new FilaProducto(
                p.getIdProducto(),
                p.getNombre(),
                cat,
                p.getPrecioCompra(),
                p.getPrecioVenta(),
                p.getCantidad(),
                p.getStockMinimo(),
                Boolean.TRUE.equals(p.getActivo()) ? "Activo" : "Inactivo");
    }

    private Button boton(String texto, String color) {
        Button b = new Button(texto);
        b.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        b.setTextFill(Color.WHITE);
        b.setStyle("-fx-background-color:" + color +
                ";-fx-border-width:0;-fx-cursor:hand;-fx-padding:7 12 7 12;");
        return b;
    }

    private TextField campo(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-border-color:#C0C0C0;-fx-border-width:1;-fx-padding:7;");
        return tf;
    }

    private Region separadorVertical() {
        Region r = new Region();
        r.setPrefWidth(1);
        r.setPrefHeight(28);
        r.setStyle("-fx-background-color: #CCCCCC;");
        HBox.setMargin(r, new Insets(0, 4, 0, 4));
        return r;
    }

    private TableCell<FilaProducto, Double> precioCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("$%,.0f", v));
            }
        };
    }

    private void mostrarInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    private void mostrarError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    // =========================================================================
    // MODELO DE FILA
    // =========================================================================

    public static class FilaProducto {
        private final int id;
        private final String nombre;
        private final String categoria;
        private final double precioCompra;
        private final double precioVenta;
        private final int stock;
        private final int stockMinimo;
        private final String estado;

        public FilaProducto(int id, String nombre, String categoria,
                double precioCompra, double precioVenta,
                int stock, int stockMinimo, String estado) {
            this.id = id;
            this.nombre = nombre;
            this.categoria = categoria;
            this.precioCompra = precioCompra;
            this.precioVenta = precioVenta;
            this.stock = stock;
            this.stockMinimo = stockMinimo;
            this.estado = estado;
        }

        public int getId() {
            return id;
        }

        public String getNombre() {
            return nombre;
        }

        public String getCategoria() {
            return categoria;
        }

        public double getPrecioCompra() {
            return precioCompra;
        }

        public double getPrecioVenta() {
            return precioVenta;
        }

        public int getStock() {
            return stock;
        }

        public int getStockMinimo() {
            return stockMinimo;
        }

        public String getEstado() {
            return estado;
        }
    }
}