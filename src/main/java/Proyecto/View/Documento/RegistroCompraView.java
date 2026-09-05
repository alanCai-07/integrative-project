package Proyecto.View.Documento;

import Proyecto.Model.Cliente;
import Proyecto.services.DocumentoServices;
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

/**
 * Vista del historial de compras de un cliente en JavaFX.
 * Muestra cada compra con su detalle y permite exportar el reporte.
 */
public class RegistroCompraView {

    private final Cliente cliente;
    private final DocumentoServices documentoServices;

    private TableView<FilaCompra> tablaCompras;
    private TableView<FilaDetalleCompra> tablaDetalle;
    private ObservableList<FilaCompra> compras;
    private ObservableList<FilaDetalleCompra> detalles;
    private Label lblResumen;
    private VBox root;

    public RegistroCompraView(Cliente cliente) {
        this.cliente = cliente;
        this.documentoServices = new DocumentoServices();
        this.compras = FXCollections.observableArrayList();
        this.detalles = FXCollections.observableArrayList();
        build();
        cargarCompras();
    }

    public Node getRoot() {
        return root;
    }

    // ── Construcción ─────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void build() {
        root = new VBox(15);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: white;");
        VBox.setVgrow(root, Priority.ALWAYS);

        // Encabezado
        Label lblTitulo = new Label("  Historial de Compras");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        lblTitulo.setTextFill(Color.web("#0A1933"));

        // ── Tabla principal de compras ────────────────────────────────────
        tablaCompras = new TableView<>(compras);
        //  Correcto para JavaFX 21
        tablaCompras.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tablaCompras.setPrefHeight(220);

        TableColumn<FilaCompra, Integer> colId = new TableColumn<>("N° Compra");
        colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colId.setMaxWidth(100);

        TableColumn<FilaCompra, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFecha()));

        TableColumn<FilaCompra, Integer> colItems = new TableColumn<>("Ítems");
        colItems.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCantidadItems()).asObject());
        colItems.setMaxWidth(80);

        TableColumn<FilaCompra, Double> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getTotal()).asObject());
        colTotal.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("$%.2f", v));
            }
        });

        TableColumn<FilaCompra, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstado()));

        tablaCompras.getColumns().addAll(colId, colFecha, colItems, colTotal, colEstado);
        tablaCompras.setRowFactory(tv -> {
            TableRow<FilaCompra> row = new TableRow<>();
            row.setStyle("-fx-cell-size: 38px;");
            return row;
        });

        // Al seleccionar una compra, mostrar su detalle
        tablaCompras.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> {
                    if (sel != null)
                        cargarDetalle(sel.getId());
                });

        // ── Detalle de la compra seleccionada ─────────────────────────────
        Label lblDetalle = new Label("Detalle de la compra seleccionada:");
        lblDetalle.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        lblDetalle.setTextFill(Color.web("#323232"));

        tablaDetalle = new TableView<>(detalles);
        // Correcto para JavaFX 21
        tablaDetalle.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tablaDetalle.setPrefHeight(160);
        VBox.setVgrow(tablaDetalle, Priority.ALWAYS);

        TableColumn<FilaDetalleCompra, String> colProd = new TableColumn<>("Producto");
        colProd.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getProducto()));

        TableColumn<FilaDetalleCompra, Integer> colCant = new TableColumn<>("Cantidad");
        colCant.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCantidad()).asObject());
        colCant.setMaxWidth(90);

        TableColumn<FilaDetalleCompra, Double> colPU = new TableColumn<>("Precio Unit.");
        colPU.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getPrecioUnit()).asObject());
        colPU.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("$%.2f", v));
            }
        });

        TableColumn<FilaDetalleCompra, Double> colSub = new TableColumn<>("Subtotal");
        colSub.setCellValueFactory(
                d -> new SimpleDoubleProperty(d.getValue().getPrecioUnit() * d.getValue().getCantidad()).asObject());
        colSub.setCellFactory(colPU.getCellFactory());

        tablaDetalle.getColumns().addAll(colProd, colCant, colPU, colSub);

        // Resumen y botón exportar
        lblResumen = new Label("Seleccione una compra para ver el detalle.");
        lblResumen.setFont(Font.font("Arial", 13));
        lblResumen.setTextFill(Color.GRAY);

        Button btnExportar = boton(" Exportar Reporte", "#00C8FF");
        btnExportar.setOnAction(e -> exportarReporte());

        HBox footerRow = new HBox(20, lblResumen, new Region() {
            {
                HBox.setHgrow(this, Priority.ALWAYS);
            }
        }, btnExportar);
        footerRow.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(lblTitulo, tablaCompras, lblDetalle, tablaDetalle, footerRow);
    }

    // ── Datos ────────────────────────────────────────────────────────────────
    private void cargarCompras() {
        compras.clear();
        if (cliente == null)
            return;
        try {
            var lista = documentoServices.obtenerComprasCliente(cliente.getId());
            for (var c : lista) {
                compras.add(new FilaCompra(
                        c.getId(),
                        c.getFecha().toString(),
                        c.getDetalles().size(),
                        c.getTotal(),
                        c.getEstado()));
            }
        } catch (Exception ex) {
            // servicio no implementado aún — tabla vacía
        }
    }

    private void cargarDetalle(int idCompra) {
        detalles.clear();
        try {
            var lista = documentoServices.obtenerDetalleCompra(idCompra);
            double total = 0;
            for (var d : lista) {
                detalles.add(new FilaDetalleCompra(
                        d.getProducto(),
                        d.getCantidad(),
                        d.getPrecioUnitario()));
                total += d.getCantidad() * d.getPrecioUnitario();
            }
            lblResumen.setText(
                    String.format("Compra #%d  |  %d ítem(s)  |  Total: $%.2f", idCompra, lista.size(), total));
            lblResumen.setTextFill(Color.web("#0A1933"));
        } catch (Exception ex) {
            lblResumen.setText("No se pudo cargar el detalle.");
        }
    }

    private void exportarReporte() {
        if (cliente == null)
            return;
        try {
            String reporte = documentoServices.generarReporteVentasCliente(cliente.getId());
            TextArea txt = new TextArea(reporte);
            txt.setEditable(false);
            txt.setFont(Font.font("Monospaced", 12));
            txt.setPrefSize(560, 380);

            Dialog<Void> dlg = new Dialog<>();
            dlg.setTitle("Reporte de Compras");
            dlg.getDialogPane().setContent(new ScrollPane(txt));
            dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dlg.showAndWait();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Error al generar el reporte.", ButtonType.OK).showAndWait();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private Button boton(String texto, String color) {
        Button b = new Button(texto);
        b.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        b.setTextFill(Color.WHITE);
        b.setStyle("-fx-background-color:" + color + ";-fx-border-width:0;-fx-cursor:hand;-fx-padding:8 16 8 16;");
        return b;
    }

    // ── Modelos de fila ──────────────────────────────────────────────────────
    public static class FilaCompra {
        private final int id;
        private final String fecha;
        private final int cantidadItems;
        private final double total;
        private final String estado;

        public FilaCompra(int id, String fecha, int cantidadItems, double total, String estado) {
            this.id = id;
            this.fecha = fecha;
            this.cantidadItems = cantidadItems;
            this.total = total;
            this.estado = estado;
        }

        public int getId() {
            return id;
        }

        public String getFecha() {
            return fecha;
        }

        public int getCantidadItems() {
            return cantidadItems;
        }

        public double getTotal() {
            return total;
        }

        public String getEstado() {
            return estado;
        }
    }

    public static class FilaDetalleCompra {
        private final String producto;
        private final int cantidad;
        private final double precioUnit;

        public FilaDetalleCompra(String producto, int cantidad, double precioUnit) {
            this.producto = producto;
            this.cantidad = cantidad;
            this.precioUnit = precioUnit;
        }

        public String getProducto() {
            return producto;
        }

        public int getCantidad() {
            return cantidad;
        }

        public double getPrecioUnit() {
            return precioUnit;
        }
    }
}