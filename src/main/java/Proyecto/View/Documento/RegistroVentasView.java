package Proyecto.View.Documento;

import Proyecto.services.DocumentoServices;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Vista del registro general de ventas (vista administrativa) en JavaFX.
 * Muestra todas las ventas, estadísticas y permite filtrar por rango de fechas.
 */
public class RegistroVentasView {

    private final DocumentoServices documentoServices;

    private TableView<FilaVenta> tablaVentas;
    private ObservableList<FilaVenta> ventas;
    private DatePicker dpDesde;
    private DatePicker dpHasta;
    private Label lblTotalVentas;
    private Label lblTotalMonto;
    private VBox root;

    public RegistroVentasView() {
        this.documentoServices = new DocumentoServices();
        this.ventas = FXCollections.observableArrayList();
        build();
        cargarVentas();
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
        Label lblTitulo = new Label("  Registro de Ventas");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        lblTitulo.setTextFill(Color.web("#0A1933"));

        // ── Filtros ───────────────────────────────────────────────────────
        HBox filtros = new HBox(12);
        filtros.setAlignment(Pos.CENTER_LEFT);
        filtros.setPadding(new Insets(0, 0, 5, 0));

        dpDesde = new DatePicker();
        dpDesde.setPromptText("Desde");
        dpHasta = new DatePicker();
        dpHasta.setPromptText("Hasta");

        Button btnFiltrar = boton(" Filtrar", "#00C8FF");
        Button btnLimpiar = boton(" Limpiar", "#646464");
        Button btnExportar = boton(" Exportar Reporte", "#0A1933");

        btnFiltrar.setOnAction(e -> filtrarVentas());
        btnLimpiar.setOnAction(e -> {
            dpDesde.setValue(null);
            dpHasta.setValue(null);
            cargarVentas();
        });
        btnExportar.setOnAction(e -> exportarReporte());

        filtros.getChildren().addAll(
                new Label("Desde:"), dpDesde,
                new Label("Hasta:"), dpHasta,
                btnFiltrar, btnLimpiar,
                new Region() {
                    {
                        HBox.setHgrow(this, Priority.ALWAYS);
                    }
                },
                btnExportar);
        for (Node n : filtros.getChildren())
            if (n instanceof Label l) {
                l.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            }

        // ── Tarjetas de resumen ───────────────────────────────────────────
        HBox statsRow = new HBox(15);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        lblTotalVentas = new Label("0");
        lblTotalMonto = new Label("$0.00");

        statsRow.getChildren().addAll(
                tarjeta("Total de Ventas", lblTotalVentas, "#00C8FF"),
                tarjeta("Monto Total", lblTotalMonto, "#0A1933"));

        // ── Tabla de ventas ───────────────────────────────────────────────
        tablaVentas = new TableView<>(ventas);
        //  Correcto para JavaFX 21
        tablaVentas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(tablaVentas, Priority.ALWAYS);

        TableColumn<FilaVenta, Integer> colId = new TableColumn<>("N° Venta");
        colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getId()).asObject());
        colId.setMaxWidth(90);

        TableColumn<FilaVenta, String> colCliente = new TableColumn<>("Cliente");
        colCliente.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCliente()));

        TableColumn<FilaVenta, String> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFecha()));

        TableColumn<FilaVenta, Integer> colItems = new TableColumn<>("Ítems");
        colItems.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCantidadItems()).asObject());
        colItems.setMaxWidth(80);

        TableColumn<FilaVenta, Double> colTotal = new TableColumn<>("Total");
        colTotal.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getTotal()).asObject());
        colTotal.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("$%.2f", v));
            }
        });

        TableColumn<FilaVenta, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEstado()));
        colEstado.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(v);
                    setStyle(v.equalsIgnoreCase("Completada")
                            ? "-fx-text-fill: #1A8A2A; -fx-font-weight: bold;"
                            : "-fx-text-fill: #C83C3C; -fx-font-weight: bold;");
                }
            }
        });

        tablaVentas.getColumns().addAll(colId, colCliente, colFecha, colItems, colTotal, colEstado);
        tablaVentas.setRowFactory(tv -> {
            TableRow<FilaVenta> row = new TableRow<>();
            row.setStyle("-fx-cell-size: 38px;");
            return row;
        });

        // ── Mini gráfico de barras ────────────────────────────────────────
        VBox chartBox = new VBox(6);
        chartBox.setPadding(new Insets(10));
        chartBox.setStyle("-fx-border-color: #DCDCDC; -fx-border-width: 1;");
        Label lblChart = new Label("Ventas por mes (últimos 5 meses)");
        lblChart.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        chartBox.getChildren().addAll(lblChart, crearGrafico());

        root.getChildren().addAll(lblTitulo, filtros, statsRow, tablaVentas, chartBox);
    }

    private VBox tarjeta(String titulo, Label lblValor, String colorAccent) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(200);
        card.setPadding(new Insets(12));
        card.setStyle(
                "-fx-background-color: #F5F5FA; -fx-border-color: " + colorAccent + "; -fx-border-width: 0 0 3 0;");

        Label lblT = new Label(titulo);
        lblT.setTextFill(Color.GRAY);
        lblT.setFont(Font.font("Arial", 12));
        lblValor.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        lblValor.setTextFill(Color.web("#0A1933"));
        card.getChildren().addAll(lblT, lblValor);
        return card;
    }

    private Canvas crearGrafico() {
        Canvas canvas = new Canvas(600, 120);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        String[] meses = { "Dic", "Ene", "Feb", "Mar", "Abr" };
        int[] valores = { 30, 55, 40, 70, 60 };

        double ml = 35, mb = 25, aw = 600 - ml - 15, ah = 120 - mb - 10;
        double bw = aw / meses.length - 8;

        gc.setStroke(Color.LIGHTGRAY);
        gc.strokeLine(ml, 5, ml, 120 - mb);
        gc.strokeLine(ml, 120 - mb, 595, 120 - mb);

        for (int i = 0; i < meses.length; i++) {
            double bh = valores[i] * ah / 100.0;
            double x = ml + i * (bw + 8);
            double y = 120 - mb - bh;

            gc.setFill(Color.web("#00C8FF"));
            gc.fillRoundRect(x, y, bw, bh, 4, 4);

            gc.setFill(Color.web("#0A1933"));
            gc.setFont(Font.font("Arial", 11));
            gc.fillText(meses[i], x + bw / 4, 120 - 6);
            gc.fillText(valores[i] + "%", x + 2, y - 3);
        }
        return canvas;
    }

    // ── Datos ────────────────────────────────────────────────────────────────
    private void cargarVentas() {
        ventas.clear();
        try {
            var lista = documentoServices.obtenerTodasLasVentas();
            double monto = 0;
            for (var v : lista) {
                ventas.add(new FilaVenta(
                        v.getId(),
                        v.getCliente().getNombre() + " " + v.getCliente().getApellido(),
                        v.getFecha().toString(),
                        v.getDetalles().size(),
                        v.getTotal(),
                        v.getEstado()));
                monto += v.getTotal();
            }
            lblTotalVentas.setText(String.valueOf(lista.size()));
            lblTotalMonto.setText(String.format("$%.2f", monto));
        } catch (Exception ex) {
            lblTotalVentas.setText("0");
            lblTotalMonto.setText("$0.00");
        }
    }

    private void filtrarVentas() {
        if (dpDesde.getValue() == null || dpHasta.getValue() == null) {
            new Alert(Alert.AlertType.WARNING,
                    "Seleccione ambas fechas para filtrar.", ButtonType.OK).showAndWait();
            return;
        }
        ventas.clear();
        try {
            var lista = documentoServices.obtenerVentasPorRango(
                    dpDesde.getValue(), dpHasta.getValue());
            double monto = 0;
            for (var v : lista) {
                ventas.add(new FilaVenta(
                        v.getId(),
                        v.getCliente().getNombre() + " " + v.getCliente().getApellido(),
                        v.getFecha().toString(),
                        v.getDetalles().size(),
                        v.getTotal(),
                        v.getEstado()));
                monto += v.getTotal();
            }
            lblTotalVentas.setText(String.valueOf(lista.size()));
            lblTotalMonto.setText(String.format("$%.2f", monto));
        } catch (Exception ex) {
            new Alert(Alert.AlertType.INFORMATION,
                    "Filtro no disponible aún.", ButtonType.OK).showAndWait();
        }
    }

    private void exportarReporte() {
        try {
            String reporte = documentoServices.generarReporteVentas();
            TextArea txt = new TextArea(reporte);
            txt.setEditable(false);
            txt.setFont(Font.font("Monospaced", 12));
            txt.setPrefSize(600, 420);
            Dialog<Void> dlg = new Dialog<>();
            dlg.setTitle("Reporte de Ventas");
            dlg.getDialogPane().setContent(new ScrollPane(txt));
            dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dlg.showAndWait();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Error al generar el reporte.", ButtonType.OK).showAndWait();
        }
    }

    // ── Helper botón ─────────────────────────────────────────────────────────
    private Button boton(String texto, String color) {
        Button b = new Button(texto);
        b.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        b.setTextFill(Color.WHITE);
        b.setStyle("-fx-background-color:" + color + ";-fx-border-width:0;-fx-cursor:hand;-fx-padding:7 14 7 14;");
        return b;
    }

    // ── Modelo de fila ───────────────────────────────────────────────────────
    public static class FilaVenta {
        private final int id;
        private final String cliente;
        private final String fecha;
        private final int cantidadItems;
        private final double total;
        private final String estado;

        public FilaVenta(int id, String cliente, String fecha, int cantidadItems, double total, String estado) {
            this.id = id;
            this.cliente = cliente;
            this.fecha = fecha;
            this.cantidadItems = cantidadItems;
            this.total = total;
            this.estado = estado;
        }

        public int getId() {
            return id;
        }

        public String getCliente() {
            return cliente;
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
}