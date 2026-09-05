package Proyecto.View.Inventario;

import Proyecto.Model.MovimientoInventario;
import Proyecto.services.InventarioServices;
import Proyecto.services.ProductoServices;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class MovimientosView {

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String COLOR_ENTRADA = "#1A8A2A";
    private static final String COLOR_SALIDA = "#C8820A";
    private static final String COLOR_AZUL = "#0A1933";
    private static final String COLOR_CYAN = "#00C8FF";
    private static final String COLOR_ROJO = "#C83C3C";
    private static final String COLOR_VERDE = "#1A8A2A";

    private final InventarioServices inventarioServices;
    private final ProductoServices productoServices;

    private final ObservableList<FilaMovimiento> movimientos = FXCollections.observableArrayList();
    private final ObservableList<FilaAlerta> alertas = FXCollections.observableArrayList();

    private TableView<FilaMovimiento> tablaMovimientos;
    private TableView<FilaAlerta> tablaAlertas;

    private ComboBox<String> cbTipoFiltro;
    private TextField txtBuscarProducto;

    private Label lblTotalEntradas;
    private Label lblTotalSalidas;
    private Label lblTotalAlertas;

    private VBox root;
    private VBox panelAlertas;

    public MovimientosView() {
        this.inventarioServices = new InventarioServices();
        this.productoServices = new ProductoServices();
        construir();
        cargarDatos();
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
        Label lblTitulo = new Label("Control de Inventario");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        lblTitulo.setTextFill(Color.web(COLOR_AZUL));

        // -- Tarjetas de resumen ----------------------------------------------
        lblTotalEntradas = valorTarjeta("0");
        lblTotalSalidas = valorTarjeta("0");
        lblTotalAlertas = valorTarjeta("0");

        HBox statsRow = new HBox(15,
                tarjeta("Unidades ingresadas", lblTotalEntradas, COLOR_ENTRADA),
                tarjeta("Unidades retiradas", lblTotalSalidas, COLOR_SALIDA),
                tarjeta("Alertas de stock", lblTotalAlertas, COLOR_ROJO));
        statsRow.setAlignment(Pos.CENTER_LEFT);

        // -- Barra de filtros y acciones --------------------------------------
        cbTipoFiltro = new ComboBox<>(FXCollections.observableArrayList(
                "Todos", "Entrada", "Salida", "Ajuste"));
        cbTipoFiltro.getSelectionModel().selectFirst();
        cbTipoFiltro.setStyle("-fx-font-size: 12px;");

        txtBuscarProducto = new TextField();
        txtBuscarProducto.setPromptText("Buscar por ID o nombre de producto...");
        txtBuscarProducto.setPrefWidth(250);
        txtBuscarProducto.setStyle("-fx-border-color: #C0C0C0; -fx-border-width:1; -fx-padding:6;");

        Button btnFiltrar = boton("Filtrar", COLOR_CYAN);
        Button btnActualizar = boton("Actualizar inventario", COLOR_AZUL);
        Button btnAlertas = boton("Ver alertas de stock", COLOR_ROJO);
        Button btnAjuste = boton("Registrar ajuste", "#795548");
        Button btnCompra = boton("Registrar compra", COLOR_ENTRADA);
        Button btnReporte = boton("Reporte de compras", "#9C27B0");

        btnFiltrar.setOnAction(e -> aplicarFiltro());
        btnActualizar.setOnAction(e -> cargarDatos());
        btnAlertas.setOnAction(e -> toggleAlertas());
        btnAjuste.setOnAction(e -> abrirDialogoAjuste());
        btnCompra.setOnAction(e -> abrirDialogoCompra());
        btnReporte.setOnAction(e -> mostrarReporteCompras());

        HBox filtroPrincipal = new HBox(10,
                new Label("Tipo:") {
                    {
                        setFont(Font.font("Arial", FontWeight.BOLD, 12));
                    }
                },
                cbTipoFiltro,
                txtBuscarProducto,
                btnFiltrar,
                separadorVertical(),
                btnActualizar,
                btnAlertas);
        filtroPrincipal.setAlignment(Pos.CENTER_LEFT);

        HBox accionesFila = new HBox(10, btnAjuste, btnCompra, btnReporte);
        accionesFila.setAlignment(Pos.CENTER_LEFT);

        VBox barraControles = new VBox(8, filtroPrincipal, accionesFila);

        // -- Tabla principal de movimientos -----------------------------------
        tablaMovimientos = new TableView<>(movimientos);
        tablaMovimientos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(tablaMovimientos, Priority.ALWAYS);
        tablaMovimientos.setPlaceholder(new Label("No hay movimientos registrados."));

        TableColumn<FilaMovimiento, Integer> colId = new TableColumn<>("ID Mov.");
        colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getIdMovimiento()).asObject());
        colId.setMaxWidth(80);

        TableColumn<FilaMovimiento, String> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTipoDocumento()));
        colTipo.setMaxWidth(180);
        colTipo.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(v);
                boolean esEntrada = v.toLowerCase().contains("compra") ||
                        v.toLowerCase().contains("devolucion del cliente") ||
                        v.toLowerCase().contains("entrada");
                setStyle("-fx-text-fill:" + (esEntrada ? COLOR_ENTRADA : COLOR_SALIDA) +
                        ";-fx-font-weight:bold;");
            }
        });

        TableColumn<FilaMovimiento, String> colProducto = new TableColumn<>("Producto");
        colProducto.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProducto()));

        TableColumn<FilaMovimiento, Integer> colCantidad = new TableColumn<>("Cantidad");
        colCantidad.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCantidad()).asObject());
        colCantidad.setMaxWidth(80);
        colCantidad.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(String.valueOf(Math.abs(v)));
                setStyle("-fx-text-fill:" + (v >= 0 ? COLOR_ENTRADA : COLOR_SALIDA) + ";-fx-font-weight:bold;");
            }
        });

        TableColumn<FilaMovimiento, Double> colPrecio = new TableColumn<>("Precio Unit.");
        colPrecio.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getPrecioUnitario()).asObject());
        colPrecio.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null || v == 0 ? null : String.format("$%,.0f", v));
            }
        });
        colPrecio.setMaxWidth(120);

        TableColumn<FilaMovimiento, Double> colSubtotal = new TableColumn<>("Subtotal");
        colSubtotal.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getSubtotal()).asObject());
        colSubtotal.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null || v == 0 ? null : String.format("$%,.0f", v));
            }
        });
        colSubtotal.setMaxWidth(120);

        TableColumn<FilaMovimiento, String> colPersona = new TableColumn<>("Persona");
        colPersona.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPersona()));
        colPersona.setMaxWidth(160);

        TableColumn<FilaMovimiento, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFecha()));
        colFecha.setMaxWidth(140);

        tablaMovimientos.getColumns().addAll(
                colId, colTipo, colProducto, colCantidad,
                colPrecio, colSubtotal, colPersona, colFecha);

        tablaMovimientos.setRowFactory(tv -> {
            TableRow<FilaMovimiento> row = new TableRow<>();
            row.setStyle("-fx-cell-size: 38px;");
            return row;
        });

        // -- Panel de alertas (colapsable) ------------------------------------
        panelAlertas = new VBox(8);
        panelAlertas.setVisible(false);
        panelAlertas.setManaged(false);
        panelAlertas.setPadding(new Insets(10));
        panelAlertas.setStyle("-fx-border-color: " + COLOR_ROJO + "; -fx-border-width: 1;");

        Label lblTituloAlertas = new Label("Productos con stock por debajo del minimo:");
        lblTituloAlertas.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        lblTituloAlertas.setTextFill(Color.web(COLOR_ROJO));

        tablaAlertas = new TableView<>(alertas);
        tablaAlertas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tablaAlertas.setPrefHeight(160);

        TableColumn<FilaAlerta, String> colAlProd = new TableColumn<>("Producto");
        colAlProd.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProducto()));

        TableColumn<FilaAlerta, String> colAlCat = new TableColumn<>("Categoria");
        colAlCat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategoria()));
        colAlCat.setMaxWidth(120);

        TableColumn<FilaAlerta, Integer> colAlStock = new TableColumn<>("Stock actual");
        colAlStock.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getStock()).asObject());
        colAlStock.setMaxWidth(100);

        TableColumn<FilaAlerta, Integer> colAlMin = new TableColumn<>("Stock minimo");
        colAlMin.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getStockMinimo()).asObject());
        colAlMin.setMaxWidth(100);

        TableColumn<FilaAlerta, Integer> colAlDef = new TableColumn<>("Deficit");
        colAlDef.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getDeficit()).asObject());
        colAlDef.setMaxWidth(80);
        colAlDef.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Integer v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(String.valueOf(v));
                setStyle(v > 0 ? "-fx-text-fill:#C83C3C;-fx-font-weight:bold;"
                        : "-fx-text-fill:#C8820A;-fx-font-weight:bold;");
            }
        });

        tablaAlertas.getColumns().addAll(colAlProd, colAlCat, colAlStock, colAlMin, colAlDef);
        panelAlertas.getChildren().addAll(lblTituloAlertas, tablaAlertas);

        // -- Ensamble ---------------------------------------------------------
        root.getChildren().addAll(
                lblTitulo,
                statsRow,
                barraControles,
                new Separator(),
                tablaMovimientos,
                panelAlertas);
    }

    // =========================================================================
    // CARGA Y FILTRADO DE DATOS
    // =========================================================================

    private void cargarDatos() {
        movimientos.clear();
        alertas.clear();
        int totalEntradas = 0;
        int totalSalidas = 0;

        try {
            List<MovimientoInventario> lista = inventarioServices.obtenerMovimientos();
            for (MovimientoInventario m : lista) {
                String tipoDoc = (m.getDocumento() != null &&
                        m.getDocumento().getTipoDocumento() != null)
                                ? m.getDocumento().getTipoDocumento().getDescripcion()
                                : "Movimiento";

                int cant = m.getCantidad();
                String fechaStr = (m.getFechaMovimiento() != null)
                        ? m.getFechaMovimiento().format(FMT_FECHA)
                        : "-";

                Object subtotalObj = cant * m.getPrecioUnitario();
                double subtotal = (subtotalObj instanceof Number)
                        ? ((Number) subtotalObj).doubleValue()
                        : 0.0;

                movimientos.add(new FilaMovimiento(
                        m.getIdMovimiento(),
                        tipoDoc,
                        m.getProducto() != null ? m.getProducto().getNombre() : "Desconocido",
                        cant,
                        m.getPrecioUnitario(),
                        subtotal,
                        "",
                        fechaStr));

                if (cant > 0)
                    totalEntradas += cant;
                else
                    totalSalidas += Math.abs(cant);
            }
        } catch (Exception ex) {
            System.err.println("MovimientosView.cargarDatos: " + ex.getMessage());
        }

        // Alertas stock bajo
        try {
            List<Map<String, Object>> stockBajo = inventarioServices.obtenerProductosConStockBajo();
            for (Map<String, Object> p : stockBajo) {
                alertas.add(new FilaAlerta(
                        (String) p.getOrDefault("nombre", "-"),
                        (String) p.getOrDefault("categoria", "-"),
                        ((Number) p.getOrDefault("stockActual", 0)).intValue(),
                        ((Number) p.getOrDefault("stockMinimo", 0)).intValue(),
                        ((Number) p.getOrDefault("deficit", 0)).intValue()));
            }
            int numAlertas = alertas.size();
            lblTotalAlertas.setText(String.valueOf(numAlertas));
            lblTotalAlertas.setTextFill(numAlertas > 0 ? Color.web(COLOR_ROJO) : Color.web(COLOR_ENTRADA));
        } catch (Exception ex) {
            lblTotalAlertas.setText("0");
        }

        lblTotalEntradas.setText(String.valueOf(totalEntradas));
        lblTotalSalidas.setText(String.valueOf(totalSalidas));
    }

    private void aplicarFiltro() {
        String tipo = cbTipoFiltro.getSelectionModel().getSelectedItem();
        String buscar = txtBuscarProducto.getText().trim().toLowerCase();

        if (("Todos".equals(tipo) || tipo == null) && buscar.isEmpty()) {
            cargarDatos();
            return;
        }

        List<FilaMovimiento> filtradas = new java.util.ArrayList<>();

        try {
            List<MovimientoInventario> lista = inventarioServices.obtenerMovimientos();
            for (MovimientoInventario m : lista) {
                String tipoDoc = (m.getDocumento() != null &&
                        m.getDocumento().getTipoDocumento() != null)
                                ? m.getDocumento().getTipoDocumento().getDescripcion()
                                : "Movimiento";

                // Filtro por tipo
                if (!"Todos".equals(tipo) && tipo != null) {
                    boolean esEntrada = m.getCantidad() > 0;
                    if ("Entrada".equals(tipo) && !esEntrada)
                        continue;
                    if ("Salida".equals(tipo) && esEntrada)
                        continue;
                    if ("Ajuste".equals(tipo) && !tipoDoc.toLowerCase().contains("ajuste"))
                        continue;
                }

                // Filtro por nombre o ID
                String nombreProd = m.getProducto() != null ? m.getProducto().getNombre().toLowerCase() : "";
                String idStr = String.valueOf(m.getIdMovimiento());
                if (!buscar.isEmpty() && !nombreProd.contains(buscar) && !idStr.contains(buscar)) {
                    continue;
                }

                String fechaStr = (m.getFechaMovimiento() != null)
                        ? m.getFechaMovimiento().format(FMT_FECHA)
                        : "-";
                double subtotal = Math.abs(m.getCantidad()) * m.getPrecioUnitario();

                filtradas.add(new FilaMovimiento(
                        m.getIdMovimiento(), tipoDoc,
                        m.getProducto() != null ? m.getProducto().getNombre() : "-",
                        m.getCantidad(), m.getPrecioUnitario(), subtotal, "", fechaStr));
            }
        } catch (Exception ex) {
            System.err.println("MovimientosView.aplicarFiltro: " + ex.getMessage());
        }

        movimientos.setAll(filtradas);
    }

    private void toggleAlertas() {
        boolean visible = panelAlertas.isVisible();
        panelAlertas.setVisible(!visible);
        panelAlertas.setManaged(!visible);
    }

    // =========================================================================
    // DIALOGO: REGISTRAR AJUSTE DE INVENTARIO
    // =========================================================================

    private void abrirDialogoAjuste() {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Registrar Ajuste de Inventario");
        dlg.setResizable(false);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(20));

        ComboBox<String> cbTipoAjuste = new ComboBox<>(FXCollections.observableArrayList(
                "Ajuste de Inventario (Entrada)",
                "Ajuste de Inventario (Salida)",
                "Baja por mercancia en mal estado"));
        cbTipoAjuste.getSelectionModel().selectFirst();
        cbTipoAjuste.setMaxWidth(Double.MAX_VALUE);

        TextField txtIdProducto = campo("ID del Producto");
        TextField txtCantidad = campo("Cantidad (numero positivo)");
        TextField txtIdEmpleado = campo("ID del Empleado responsable");
        TextArea txtObservacion = new TextArea();
        txtObservacion.setPromptText("Motivo del ajuste (obligatorio)");
        txtObservacion.setPrefRowCount(3);
        txtObservacion.setWrapText(true);

        grid.add(etiqueta("Tipo de ajuste:"), 0, 0);
        grid.add(cbTipoAjuste, 1, 0);
        grid.add(etiqueta("ID Producto:"), 0, 1);
        grid.add(txtIdProducto, 1, 1);
        grid.add(etiqueta("Cantidad:"), 0, 2);
        grid.add(txtCantidad, 1, 2);
        grid.add(etiqueta("ID Empleado:"), 0, 3);
        grid.add(txtIdEmpleado, 1, 3);
        grid.add(etiqueta("Observacion:"), 0, 4);
        grid.add(txtObservacion, 1, 4);

        ButtonType btnGuardar = new ButtonType("Registrar", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(btnGuardar, ButtonType.CANCEL);
        dlg.getDialogPane().setContent(grid);

        dlg.setResultConverter(bt -> {
            if (bt == btnGuardar) {
                try {
                    int idProducto = Integer.parseInt(txtIdProducto.getText().trim());
                    int cantidad = Integer.parseInt(txtCantidad.getText().trim());
                    int idEmpleado = Integer.parseInt(txtIdEmpleado.getText().trim());
                    String obs = txtObservacion.getText().trim();

                    if (cantidad <= 0)
                        throw new NumberFormatException("cantidad");
                    if (obs.isEmpty())
                        throw new IllegalArgumentException("observacion");

                    // Mapear tipo de ajuste a id_tipo_documento segun Techzone.sql
                    int idTipoDoc = switch (cbTipoAjuste.getSelectionModel().getSelectedIndex()) {
                        case 0 -> 5; // Ajuste Entrada
                        case 1 -> 6; // Ajuste Salida
                        case 2 -> 7; // Baja mal estado
                        default -> 6;
                    };

                    Map<String, Object> resultado = inventarioServices.ajustarInventario(
                            idTipoDoc, idEmpleado, idProducto, cantidad, obs);

                    int idDoc = ((Number) resultado.getOrDefault("idDocumento", -1)).intValue();
                    String msg = (String) resultado.getOrDefault("mensaje", "Error desconocido");

                    if (idDoc > 0) {
                        mostrarInfo("Ajuste registrado correctamente.\nDocumento generado: #" + idDoc);
                        cargarDatos();
                    } else {
                        mostrarError("No se pudo registrar el ajuste:\n" + msg);
                    }

                } catch (NumberFormatException e) {
                    mostrarError(
                            "Verifica que ID de producto, cantidad e ID de empleado sean numeros enteros validos.");
                } catch (IllegalArgumentException e) {
                    mostrarError("La observacion es obligatoria.");
                } catch (Exception e) {
                    mostrarError("Error inesperado: " + e.getMessage());
                }
            }
            return null;
        });

        dlg.showAndWait();
    }

    // =========================================================================
    // DIALOGO: REGISTRAR COMPRA A PROVEEDOR (formulario estructurado)
    // =========================================================================

    private static class FilaProductoCompra {
        final TextField txtId = new TextField();
        final TextField txtCantidad = new TextField();
        final TextField txtPrecio = new TextField();

        FilaProductoCompra() {
            txtId.setPromptText("ID producto");
            txtCantidad.setPromptText("Cantidad");
            txtPrecio.setPromptText("Precio unitario");
            txtId.setPrefWidth(100);
            txtCantidad.setPrefWidth(90);
            txtPrecio.setPrefWidth(120);
            String estiloBase = "-fx-border-color:#C0C0C0;-fx-border-width:1;-fx-padding:6;";
            txtId.setStyle(estiloBase);
            txtCantidad.setStyle(estiloBase);
            txtPrecio.setStyle(estiloBase);
        }

        /** Retorna true si todos los campos tienen contenido no vacio. */
        boolean tieneContenido() {
            return !txtId.getText().trim().isEmpty()
                    && !txtCantidad.getText().trim().isEmpty()
                    && !txtPrecio.getText().trim().isEmpty();
        }

        /** Valida y parsea. Lanza IllegalArgumentException con mensaje descriptivo. */
        int getId() {
            try {
                int v = Integer.parseInt(txtId.getText().trim());
                if (v <= 0)
                    throw new NumberFormatException();
                return v;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("El ID de producto debe ser un numero entero positivo.");
            }
        }

        int getCantidad() {
            try {
                int v = Integer.parseInt(txtCantidad.getText().trim());
                if (v <= 0)
                    throw new NumberFormatException();
                return v;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("La cantidad debe ser un numero entero positivo.");
            }
        }

        double getPrecio() {
            try {
                // Aceptar tanto punto como coma como separador decimal
                String raw = txtPrecio.getText().trim().replace(",", ".");
                double v = Double.parseDouble(raw);
                if (v <= 0)
                    throw new NumberFormatException();
                return v;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("El precio debe ser un numero mayor a 0.");
            }
        }
    }

    private void abrirDialogoCompra() {
        try {
            Stage ventana = new Stage();
            ventana.setTitle("Registrar Compra a Proveedor");
            ventana.setResizable(true);

            // -- Encabezado -------------------------------------------------------
            Label lblTitulo = new Label("Registrar Compra a Proveedor");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        lblTitulo.setTextFill(Color.web(COLOR_AZUL));

        // -- Datos de la compra -----------------------------------------------
        GridPane gridCabecera = new GridPane();
        gridCabecera.setHgap(14);
        gridCabecera.setVgap(10);
        gridCabecera.setPadding(new Insets(0, 0, 10, 0));

        ColumnConstraints colLabel = new ColumnConstraints(140);
        ColumnConstraints colField = new ColumnConstraints();
        colField.setHgrow(Priority.ALWAYS);
        gridCabecera.getColumnConstraints().addAll(colLabel, colField);

        TextField txtIdProveedor = campoFormulario("Ej: 10");
        TextField txtIdEmpleado = campoFormulario("Ej: 5");
        TextField txtNroFactura = campoFormulario("Ej: FAC-SAM-2025-001");

        gridCabecera.add(etiquetaForm("ID Proveedor:"), 0, 0);
        gridCabecera.add(txtIdProveedor, 1, 0);
        gridCabecera.add(etiquetaForm("ID Empleado:"), 0, 1);
        gridCabecera.add(txtIdEmpleado, 1, 1);
        gridCabecera.add(etiquetaForm("Nro. Factura:"), 0, 2);
        gridCabecera.add(txtNroFactura, 1, 2);

        // -- Tabla de productos (filas dinamicas) ----------------------------
        Label lblProductos = new Label("Productos de la compra:");
        lblProductos.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        lblProductos.setTextFill(Color.web(COLOR_AZUL));

        // Cabecera de columnas
        HBox cabeceraColumnas = new HBox(8);
        cabeceraColumnas.setPadding(new Insets(4, 0, 2, 0));
        Label hId = cabCol("ID Producto", 100);
        Label hCant = cabCol("Cantidad", 90);
        Label hPrecio = cabCol("Precio unitario", 120);
        Label hAccion = cabCol("", 40);
        cabeceraColumnas.getChildren().addAll(hId, hCant, hPrecio, hAccion);

        // Contenedor de filas de producto
        VBox contenedorFilas = new VBox(6);

        // Fila de resumen total
        Label lblTotal = new Label("Total: $0");
        lblTotal.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        lblTotal.setTextFill(Color.web(COLOR_AZUL));

        // Lista viva de filas
        List<FilaProductoCompra> listaFilas = new java.util.ArrayList<>();

        // Funcion de actualizacion del total (lambda-compatible via Runnable)
        Runnable actualizarTotal = () -> {
            double total = 0;
            for (FilaProductoCompra fila : listaFilas) {
                if (fila.tieneContenido()) {
                    try {
                        total += fila.getCantidad() * fila.getPrecio();
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            lblTotal.setText(String.format("Total estimado: $%,.0f", total));
        };

        // Agregar primera fila por defecto
        agregarFilaProducto(listaFilas, contenedorFilas, actualizarTotal);

        Button btnAgregarFila = new Button("+ Agregar otro producto");
        btnAgregarFila.setFont(Font.font("Arial", 12));
        btnAgregarFila.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + COLOR_CYAN + ";" +
                        "-fx-cursor: hand;" +
                        "-fx-border-color: " + COLOR_CYAN + ";" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 5 10 5 10;");
        btnAgregarFila.setOnAction(e -> agregarFilaProducto(listaFilas, contenedorFilas, actualizarTotal));

        // -- Mensaje de error interno -----------------------------------------
        Label lblMensaje = new Label("");
        lblMensaje.setFont(Font.font("Arial", 12));
        lblMensaje.setWrapText(true);
        lblMensaje.setMaxWidth(500);

        // -- Botones ----------------------------------------------------------
        Button btnRegistrar = new Button("Registrar Compra");
        btnRegistrar.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        btnRegistrar.setTextFill(Color.WHITE);
        btnRegistrar.setStyle(
                "-fx-background-color: " + COLOR_VERDE + ";" +
                        "-fx-border-width: 0;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 9 20 9 20;");

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        btnCancelar.setTextFill(Color.WHITE);
        btnCancelar.setStyle(
                "-fx-background-color: #646464;" +
                        "-fx-border-width: 0;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 9 20 9 20;");

        btnCancelar.setOnAction(e -> ventana.close());

        btnRegistrar.setOnAction(e -> {
            lblMensaje.setText("");

            // Validar cabecera
            String errCabecera = validarCabecera(txtIdProveedor, txtIdEmpleado, txtNroFactura);
            if (errCabecera != null) {
                lblMensaje.setTextFill(Color.web(COLOR_ROJO));
                lblMensaje.setText(errCabecera);
                return;
            }

            // Recopilar filas con contenido
            List<FilaProductoCompra> filasConDatos = new java.util.ArrayList<>();
            for (FilaProductoCompra fila : listaFilas) {
                if (fila.tieneContenido())
                    filasConDatos.add(fila);
            }

            if (filasConDatos.isEmpty()) {
                lblMensaje.setTextFill(Color.web(COLOR_ROJO));
                lblMensaje.setText("Debes agregar al menos un producto.");
                return;
            }

            // Validar y construir JSON
            try {
                int idProveedor = Integer.parseInt(txtIdProveedor.getText().trim());
                int idEmpleado = Integer.parseInt(txtIdEmpleado.getText().trim());
                String nroFact = txtNroFactura.getText().trim();

                StringBuilder json = new StringBuilder("[");
                List<int[]> datosParaTicket = new java.util.ArrayList<>();
                List<double[]> preciosParaTicket = new java.util.ArrayList<>();

                for (int i = 0; i < filasConDatos.size(); i++) {
                    FilaProductoCompra fila = filasConDatos.get(i);
                    int id = fila.getId();
                    int qty = fila.getCantidad();
                    double precio = fila.getPrecio();

                    if (i > 0)
                        json.append(",");
                    json.append(String.format("{\"id\":%d,\"qty\":%d,\"precio\":%.0f}", id, qty, precio));
                    datosParaTicket.add(new int[] { id, qty });
                    preciosParaTicket.add(new double[] { precio });
                }
                json.append("]");

                Map<String, Object> resultado = inventarioServices.registrarCompra(
                        idProveedor, idEmpleado, nroFact, json.toString());

                int idDoc = ((Number) resultado.getOrDefault("idDocumento", -1)).intValue();
                String msg = (String) resultado.getOrDefault("mensaje", "Error desconocido");

                if (idDoc > 0) {
                    ventana.close();
                    generarTicketCompraEstructurado(
                            idDoc, idProveedor, nroFact, filasConDatos, idEmpleado);
                    cargarDatos();
                } else {
                    lblMensaje.setTextFill(Color.web(COLOR_ROJO));
                    lblMensaje.setText("No se pudo registrar la compra: " + msg);
                }

            } catch (IllegalArgumentException ex) {
                lblMensaje.setTextFill(Color.web(COLOR_ROJO));
                lblMensaje.setText(ex.getMessage());
            } catch (Exception ex) {
                lblMensaje.setTextFill(Color.web(COLOR_ROJO));
                lblMensaje.setText("Error inesperado: " + ex.getMessage());
            }
        });

        HBox filaBotones = new HBox(12, btnRegistrar, btnCancelar);
        filaBotones.setAlignment(Pos.CENTER_RIGHT);
        filaBotones.setPadding(new Insets(10, 0, 0, 0));

        // -- Ensamble del layout ----------------------------------------------
        ScrollPane scrollFilas = new ScrollPane(contenedorFilas);
        scrollFilas.setFitToWidth(true);
        scrollFilas.setPrefHeight(220);
        scrollFilas.setStyle("-fx-background: white; -fx-background-color: white;");

        VBox layout = new VBox(14,
                lblTitulo,
                new Separator(),
                gridCabecera,
                new Separator(),
                lblProductos,
                cabeceraColumnas,
                scrollFilas,
                btnAgregarFila,
                lblTotal,
                new Separator(),
                lblMensaje,
                filaBotones);
        layout.setPadding(new Insets(24));
        layout.setPrefWidth(560);

        Scene escena = new Scene(layout);
        ventana.setScene(escena);
        ventana.initModality(Modality.APPLICATION_MODAL);
        ventana.showAndWait();
    } catch (Exception ex) {
        System.err.println("MovimientosView.abrirDialogoCompra: " + ex.getMessage());
        mostrarError("No se pudo abrir el formulario de compra. " + ex.getMessage());
    }
    }

    /**
     * Agrega una nueva fila de campos (ID, Cantidad, Precio) al contenedor visual
     * y a la lista interna. El boton de eliminar solo aparece cuando hay mas de una
     * fila.
     */
    private void agregarFilaProducto(List<FilaProductoCompra> lista,
            VBox contenedor,
            Runnable actualizarTotal) {
        FilaProductoCompra nuevaFila = new FilaProductoCompra();
        lista.add(nuevaFila);

        // Listener para actualizar total en tiempo real
        nuevaFila.txtCantidad.textProperty().addListener((obs, o, n) -> actualizarTotal.run());
        nuevaFila.txtPrecio.textProperty().addListener((obs, o, n) -> actualizarTotal.run());

        Button btnElim = new Button("X");
        btnElim.setStyle(
                "-fx-background-color: " + COLOR_ROJO + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-border-width: 0;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 5 8 5 8;");
        btnElim.setFont(Font.font("Arial", FontWeight.BOLD, 11));

        HBox fila = new HBox(8,
                nuevaFila.txtId,
                nuevaFila.txtCantidad,
                nuevaFila.txtPrecio,
                btnElim);
        fila.setAlignment(Pos.CENTER_LEFT);

        btnElim.setOnAction(e -> {
            if (lista.size() <= 1)
                return; // Siempre mantener al menos una fila
            lista.remove(nuevaFila);
            contenedor.getChildren().remove(fila);
            actualizarTotal.run();
        });

        contenedor.getChildren().add(fila);
        actualizarTotal.run();
    }

    /**
     * Valida los campos de cabecera de la compra. Retorna null si todo es valido.
     */
    private String validarCabecera(TextField txtIdProveedor,
            TextField txtIdEmpleado,
            TextField txtNroFactura) {
        if (txtIdProveedor.getText().trim().isEmpty())
            return "El ID del proveedor es obligatorio.";
        try {
            int id = Integer.parseInt(txtIdProveedor.getText().trim());
            if (id <= 0)
                return "El ID del proveedor debe ser un numero positivo.";
        } catch (NumberFormatException e) {
            return "El ID del proveedor debe ser un numero entero.";
        }

        if (txtIdEmpleado.getText().trim().isEmpty())
            return "El ID del empleado es obligatorio.";
        try {
            int id = Integer.parseInt(txtIdEmpleado.getText().trim());
            if (id <= 0)
                return "El ID del empleado debe ser un numero positivo.";
        } catch (NumberFormatException e) {
            return "El ID del empleado debe ser un numero entero.";
        }

        if (txtNroFactura.getText().trim().isEmpty())
            return "El numero de factura del proveedor es obligatorio.";

        return null;
    }

    private TextField campoFormulario(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle("-fx-border-color:#C0C0C0;-fx-border-width:1;-fx-padding:7;");
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    private Label etiquetaForm(String texto) {
        Label l = new Label(texto);
        l.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        l.setTextFill(Color.web("#333333"));
        return l;
    }

    private Label cabCol(String texto, double ancho) {
        Label l = new Label(texto);
        l.setPrefWidth(ancho);
        l.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        l.setTextFill(Color.web("#555555"));
        l.setStyle("-fx-border-color: transparent transparent #CCCCCC transparent; -fx-border-width: 0 0 1 0;");
        return l;
    }

    // =========================================================================
    // REPORTE DE COMPRAS - FORMATO TICKET (igual al POS de ventas)
    // =========================================================================

    /**
     * Genera y muestra el ticket de compra a partir de la lista estructurada de
     * productos.
     * Mismo formato visual que el ticket del POS de ventas.
     */
    private void generarTicketCompraEstructurado(int idDocumento, int idProveedor,
            String nroFactExt,
            List<FilaProductoCompra> productos,
            int idEmpleado) {
        StringBuilder sb = new StringBuilder();
        sb.append("===========================================\n");
        sb.append("       TECHZONE  -  FACTURA DE COMPRA\n");
        sb.append("===========================================\n");
        sb.append(String.format("  N Documento   : %d%n", idDocumento));
        sb.append(String.format("  Fecha         : %s%n", LocalDateTime.now().format(FMT_FECHA)));
        sb.append(String.format("  Nro Fact. Ext.: %s%n", nroFactExt));
        sb.append(String.format("  ID Proveedor  : %d%n", idProveedor));
        sb.append(String.format("  ID Empleado   : %d%n", idEmpleado));
        sb.append("-------------------------------------------\n");
        sb.append(String.format("  %-6s %-25s %6s %12s%n",
                "ID", "PRODUCTO", "CANT.", "SUBTOTAL"));
        sb.append("-------------------------------------------\n");

        double totalCompra = 0;

        for (FilaProductoCompra fila : productos) {
            try {
                int id = fila.getId();
                int qty = fila.getCantidad();
                double precio = fila.getPrecio();
                double sub = qty * precio;
                totalCompra += sub;

                String nombreProd = "-";
                try {
                    var prod = productoServices.obtenerProducto(id);
                    if (prod != null)
                        nombreProd = prod.getNombre();
                } catch (Exception ignored) {
                }

                sb.append(String.format("  %-6d %-25s %6d %12,.0f%n",
                        id, truncar(nombreProd, 25), qty, sub));
                sb.append(String.format("         Precio unitario compra: $%,.0f%n", precio));
                sb.append(String.format("         Fecha ingreso         : %s%n",
                        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
                sb.append("\n");
            } catch (IllegalArgumentException ignored) {
            }
        }

        sb.append("-------------------------------------------\n");
        sb.append(String.format("  TOTAL COMPRA  : $%,.0f%n", totalCompra));
        sb.append("===========================================\n");
        sb.append("   Ingresado al inventario TechZone\n");

        mostrarTicket("Factura de Compra N " + idDocumento, sb.toString());
    }

    private void mostrarReporteCompras() {
        try {
            List<Map<String, Object>> lista = inventarioServices.reporteMovimientos(
                    null, "2000-01-01", "2099-12-31");

            if (lista == null || lista.isEmpty()) {
                mostrarError("No se encontraron registros de compras para mostrar.");
                return;
            }

            DecimalFormat moneda = new DecimalFormat("$#,##0");
            StringBuilder sb = new StringBuilder();
            sb.append("===========================================\n");
            sb.append("    REPORTE DE COMPRAS A PROVEEDORES\n");
            sb.append("===========================================\n");
            sb.append("  Generado: ").append(LocalDateTime.now().format(FMT_FECHA)).append("\n");
            sb.append("-------------------------------------------\n");
            sb.append("  Mov.     Tipo                 Producto           Cant.     Subtotal\n");
            sb.append("-------------------------------------------\n");

            double totalGeneral = 0;
            int contCompras = 0;

            for (Map<String, Object> fila : lista) {
                String tipo = fila.get("tipo_documento") instanceof String
                        ? ((String) fila.get("tipo_documento")).toLowerCase()
                        : "";
                if (!tipo.contains("compra")) {
                    continue;
                }

                int id = ((Number) fila.getOrDefault("id_movimiento", 0)).intValue();
                String prod = fila.get("producto") instanceof String
                        ? (String) fila.get("producto") : "-";
                int cant = ((Number) fila.getOrDefault("cantidad", 0)).intValue();
                Object subObj = fila.get("subtotal_linea");
                double sub = subObj instanceof Number ? ((Number) subObj).doubleValue() : 0.0;
                Object fechaObj = fila.get("fecha_movimiento");
                String fecha = fechaObj instanceof java.sql.Timestamp
                        ? ((java.sql.Timestamp) fechaObj).toLocalDateTime().format(FMT_FECHA)
                        : "-";
                String persona = fila.getOrDefault("persona_doc", "-").toString();

                sb.append("  ")
                        .append(padRight(String.valueOf(id), 8))
                        .append(padRight(truncar(tipo, 20), 20))
                        .append(padRight(truncar(prod, 18), 18))
                        .append(padLeft(String.valueOf(cant), 6))
                        .append(padLeft(moneda.format(sub), 12))
                        .append("\n");
                sb.append("    Fecha ingreso: ").append(fecha).append("\n");
                sb.append("    Proveedor: ").append(persona).append("\n\n");

                totalGeneral += sub;
                contCompras++;
            }

            if (contCompras == 0) {
                mostrarError("No se encontraron compras en el periodo indicado.");
                return;
            }

            sb.append("-------------------------------------------\n");
            sb.append("  Total compras:        ").append(contCompras).append(" registros\n");
            sb.append("  Total invertido:      ").append(moneda.format(totalGeneral)).append("\n");
            sb.append("===========================================\n");

            mostrarTicket("Reporte de Compras a Proveedores", sb.toString());
        } catch (Exception ex) {
            System.err.println("MovimientosView.mostrarReporteCompras: " + ex.getMessage());
            ex.printStackTrace();
            mostrarError("Error al generar el reporte de compras: " + ex.getMessage());
        }
    }

    private void mostrarTicket(String titulo, String contenido) {
        TextArea txt = new TextArea(contenido);
        txt.setEditable(false);
        txt.setFont(Font.font("Monospaced", 12));
        txt.setPrefSize(520, 450);

        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle(titulo);
        dlg.getDialogPane().setContent(new ScrollPane(txt));
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.setResizable(true);
        dlg.showAndWait();
    }

    // =========================================================================
    // HELPERS UI
    // =========================================================================

    private VBox tarjeta(String titulo, Label lblValor, String colorBorde) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(200);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: #F5F5FA; -fx-border-color:" +
                colorBorde + "; -fx-border-width: 0 0 3 0;");
        Label lblT = new Label(titulo);
        lblT.setTextFill(Color.GRAY);
        lblT.setFont(Font.font("Arial", 11));
        lblT.setWrapText(true);
        lblValor.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        lblValor.setTextFill(Color.web(COLOR_AZUL));
        card.getChildren().addAll(lblT, lblValor);
        return card;
    }

    private Label valorTarjeta(String val) {
        Label l = new Label(val);
        l.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        l.setTextFill(Color.web(COLOR_AZUL));
        return l;
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
        tf.setPrefWidth(240);
        tf.setStyle("-fx-border-color:#B4B4B4;-fx-border-width:1;-fx-padding:7;");
        return tf;
    }

    private Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        return l;
    }

    private Region separadorVertical() {
        Region r = new Region();
        r.setPrefWidth(1);
        r.setPrefHeight(28);
        r.setStyle("-fx-background-color: #CCCCCC;");
        HBox.setMargin(r, new Insets(0, 4, 0, 4));
        return r;
    }

    private String truncar(String s, int max) {
        if (s == null)
            return "-";
        return s.length() > max ? s.substring(0, max - 1) + "." : s;
    }

    private String padRight(String text, int width) {
        if (text == null)
            text = "";
        if (text.length() >= width)
            return text;
        return text + " ".repeat(width - text.length());
    }

    private String padLeft(String text, int width) {
        if (text == null)
            text = "";
        if (text.length() >= width)
            return text;
        return " ".repeat(width - text.length()) + text;
    }

    private void mostrarInfo(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    private void mostrarError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    // =========================================================================
    // MODELOS DE FILA
    // =========================================================================

    public static class FilaMovimiento {
        private final int idMovimiento;
        private final String tipoDocumento;
        private final String producto;
        private final int cantidad;
        private final double precioUnitario;
        private final double subtotal;
        private final String persona;
        private final String fecha;

        public FilaMovimiento(int idMovimiento, String tipoDocumento, String producto,
                int cantidad, double precioUnitario, double subtotal,
                String persona, String fecha) {
            this.idMovimiento = idMovimiento;
            this.tipoDocumento = tipoDocumento;
            this.producto = producto;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario;
            this.subtotal = subtotal;
            this.persona = persona;
            this.fecha = fecha;
        }

        public int getIdMovimiento() {
            return idMovimiento;
        }

        public String getTipoDocumento() {
            return tipoDocumento;
        }

        public String getProducto() {
            return producto;
        }

        public int getCantidad() {
            return cantidad;
        }

        public double getPrecioUnitario() {
            return precioUnitario;
        }

        public double getSubtotal() {
            return subtotal;
        }

        public String getPersona() {
            return persona;
        }

        public String getFecha() {
            return fecha;
        }
    }

    public static class FilaAlerta {
        private final String producto;
        private final String categoria;
        private final int stock;
        private final int stockMinimo;
        private final int deficit;

        public FilaAlerta(String producto, String categoria, int stock, int stockMinimo, int deficit) {
            this.producto = producto;
            this.categoria = categoria;
            this.stock = stock;
            this.stockMinimo = stockMinimo;
            this.deficit = deficit;
        }

        public String getProducto() {
            return producto;
        }

        public String getCategoria() {
            return categoria;
        }

        public int getStock() {
            return stock;
        }

        public int getStockMinimo() {
            return stockMinimo;
        }

        public int getDeficit() {
            return deficit;
        }
    }
}