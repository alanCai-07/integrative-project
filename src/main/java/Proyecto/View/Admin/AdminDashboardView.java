package Proyecto.View.Admin;

import java.util.List;
import java.util.Map;

import Proyecto.Model.Cliente;
import Proyecto.services.CategoriaServices;
import Proyecto.services.DocumentoServices;
import Proyecto.services.InventarioServices;
import Proyecto.services.PersonaServices;
import Proyecto.services.ProductoServices;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class AdminDashboardView {

    private final Cliente adminCliente;
    private final ProductoServices   productoServices;
    private final InventarioServices inventarioServices;
    private final DocumentoServices  documentoServices;
    private final PersonaServices    personaServices;

    private Label lblTotalProductos;
    private Label lblStockBajo;
    private Label lblTotalVentas;
    private Label lblTotalClientes;

    private VBox root;

    public AdminDashboardView(Cliente admin) {
        this.adminCliente      = admin;
        this.productoServices  = new ProductoServices();
        this.inventarioServices= new InventarioServices();
        this.documentoServices = new DocumentoServices();
        this.personaServices   = new PersonaServices();
        build();
        cargarMetricas();
    }

    public Node getRoot() { return root; }

    // =========================================================================
    // CONSTRUCCION PRINCIPAL
    // =========================================================================

    private void build() {
        root = new VBox(15);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: white;");
        VBox.setVgrow(root, Priority.ALWAYS);

        VBox header = new VBox(4);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 10, 0));
        header.setStyle("-fx-border-color: #E0E0E0; -fx-border-width: 0 0 2 0;");

        Label lblTitulo = new Label("Panel de Administracion");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        lblTitulo.setTextFill(Color.web("#0A1933"));

        Label lblSubtitulo = new Label("Bienvenido, " + adminCliente.getNombre() + " " + adminCliente.getApellido());
        lblSubtitulo.setFont(Font.font("Arial", 14));
        lblSubtitulo.setTextFill(Color.web("#4B5876"));

        header.getChildren().addAll(lblTitulo, lblSubtitulo);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        tabs.getTabs().addAll(
                new Tab("Dashboard",     crearTabDashboard()),
                new Tab("Empleados",     crearTabEmpleados()),
                new Tab("Proveedores",   crearTabProveedores()),
                new Tab("Categorias",    crearTabCategorias()),
                new Tab("Reportes",      crearTabReportes()));

        root.getChildren().addAll(header, tabs);
    }

    // =========================================================================
    // TAB 1: DASHBOARD
    // =========================================================================

    @SuppressWarnings("unchecked")
    private Node crearTabDashboard() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(15));

        Button btnRefrescar = boton("Actualizar dashboard", "#0A1933");
        btnRefrescar.setOnAction(e -> cargarMetricas());
        HBox headerDash = new HBox(btnRefrescar);
        headerDash.setAlignment(Pos.CENTER_RIGHT);

        lblTotalProductos = new Label("...");
        lblStockBajo      = new Label("...");
        lblTotalVentas    = new Label("...");
        lblTotalClientes  = new Label("...");

        HBox kpiRow = new HBox(15);
        kpiRow.setAlignment(Pos.CENTER_LEFT);
        kpiRow.getChildren().addAll(
                kpiCard("Productos",      lblTotalProductos, "#00C8FF"),
                kpiCard("Stock Bajo",     lblStockBajo,      "#C83C3C"),
                kpiCard("Ventas",         lblTotalVentas,    "#1A8A2A"),
                kpiCard("Clientes",       lblTotalClientes,  "#0A1933"));

        VBox chartBox = new VBox(8);
        chartBox.setPadding(new Insets(15));
        chartBox.setStyle("-fx-border-color: #DCDCDC; -fx-border-width: 1;");
        Label lblChart = new Label("Ventas mensuales (ultimos 5 meses)");
        lblChart.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        lblChart.setTextFill(Color.web("#0A1933"));
        chartBox.getChildren().addAll(lblChart, crearGraficoBarras());

        VBox tablaBox = new VBox(8);
        tablaBox.setPadding(new Insets(15));
        tablaBox.setStyle("-fx-border-color: #DCDCDC; -fx-border-width: 1;");
        Label lblTabla = new Label("Ultimas ventas");
        lblTabla.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        lblTabla.setTextFill(Color.web("#0A1933"));

        TableView<String[]> tablaRecientes = new TableView<>();
        tablaRecientes.setPrefHeight(180);
        tablaRecientes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        tablaRecientes.getColumns().addAll(
                columnaStr("N Venta", 0),
                columnaStr("Cliente",  1),
                columnaStr("Fecha",    2),
                columnaStr("Total",    3),
                columnaStr("Estado",   4));

        ObservableList<String[]> rows = FXCollections.observableArrayList();
        try {
            var ventas = documentoServices.obtenerTodasLasVentas();
            int max = Math.min(5, ventas.size());
            for (int i = 0; i < max; i++) {
                var v = ventas.get(i);
                rows.add(new String[]{
                        String.valueOf(v.getId()),
                        v.getCliente().getNombre() + " " + v.getCliente().getApellido(),
                        v.getFecha().toString(),
                        String.format("$%.2f", v.getTotal()),
                        v.getEstado()
                });
            }
        } catch (Exception ignored) {}
        tablaRecientes.setItems(rows);
        tablaBox.getChildren().addAll(lblTabla, tablaRecientes);

        panel.getChildren().addAll(headerDash, kpiRow, chartBox, tablaBox);
        ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: white; -fx-background-color: white;");
        return scroll;
    }

    private void cargarMetricas() {
        try {
            int prods = productoServices.obtenerTodosLosProductos().size();
            lblTotalProductos.setText(String.valueOf(prods));
        } catch (Exception e) { lblTotalProductos.setText("-"); }

        try {
            int bajo = inventarioServices.obtenerProductosConStockBajo().size();
            lblStockBajo.setText(String.valueOf(bajo));
            lblStockBajo.setTextFill(bajo > 0 ? Color.web("#C83C3C") : Color.web("#1A8A2A"));
        } catch (Exception e) { lblStockBajo.setText("-"); }

        try {
            var ventas = documentoServices.obtenerTodasLasVentas();
            lblTotalVentas.setText(String.valueOf(ventas.size()));
        } catch (Exception e) { lblTotalVentas.setText("-"); }

        try {
            var clientes = personaServices.obtenerTodosLosClientes();
            lblTotalClientes.setText(String.valueOf(clientes != null ? clientes.size() : 0));
        } catch (Exception e) { lblTotalClientes.setText("-"); }
    }

    private Canvas crearGraficoBarras() {
        Canvas c = new Canvas(700, 140);
        GraphicsContext gc = c.getGraphicsContext2D();
        List<Map<String, Object>> datos = new Proyecto.dao.InventarioDAO().obtenerVentasPorMes(5);

        if (datos.isEmpty()) {
            gc.setFill(Color.GRAY);
            gc.setFont(Font.font("Arial", 12));
            gc.fillText("Sin ventas registradas en los ultimos 5 meses.", 40, 70);
            return c;
        }

        double maxVenta = datos.stream()
                .mapToDouble(d -> ((Number) d.get("totalVentas")).doubleValue())
                .max().orElse(1.0);
        if (maxVenta <= 0) maxVenta = 1.0;

        double ml = 40, mb = 25, aw = 700 - ml - 20, ah = 140 - mb - 20;
        double bw = Math.min(80, aw / datos.size() - 12);
        gc.setStroke(Color.LIGHTGRAY);
        gc.strokeLine(ml, 10, ml, 140 - mb);
        gc.strokeLine(ml, 140 - mb, 690, 140 - mb);

        for (int i = 0; i < datos.size(); i++) {
            Map<String, Object> fila = datos.get(i);
            double val = ((Number) fila.get("totalVentas")).doubleValue();
            String etiqueta = String.valueOf(fila.get("etiqueta"));
            double bh = (val / maxVenta) * ah;
            double x = ml + 15 + i * (bw + 15);
            double y = 140 - mb - bh;

            gc.setFill(Color.web("#00C8FF"));
            gc.fillRoundRect(x, y, bw, bh, 6, 6);
            gc.setFill(Color.web("#0A1933"));
            gc.setFont(Font.font("Arial", 10));
            gc.fillText(etiqueta, x, 140 - 6);
            gc.fillText(String.format("$%,.0f", val), x, Math.max(12, y - 4));
        }
        return c;
    }

    // =========================================================================
    // TAB 2: EMPLEADOS
    // =========================================================================

    @SuppressWarnings("unchecked")
    private Node crearTabEmpleados() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(15));

        ObservableList<String[]> dataEmps = FXCollections.observableArrayList();
        TableView<String[]> tablaEmps = new TableView<>(dataEmps);
        tablaEmps.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(tablaEmps, Priority.ALWAYS);
        tablaEmps.getColumns().addAll(
                columnaStr("ID",       0),
                columnaStr("Nombres",  1),
                columnaStr("Apellidos",2),
                columnaStr("Email",    3),
                columnaStr("Cargo",    4),
                columnaStr("Estado",   5));

        TextField txtBuscar   = campo("Buscar empleado...");
        Button btnBuscar      = boton("Buscar",     "#00C8FF");
        Button btnRefrescar   = boton("Actualizar", "#0A1933");
        Button btnNuevo       = boton("Nuevo",      "#1A8A2A");
        Button btnEditar      = boton("Editar",     "#795548");
        Button btnDesact      = boton("Desactivar", "#C83C3C");

        Region spc = new Region();
        HBox.setHgrow(spc, Priority.ALWAYS);
        HBox acciones = new HBox(10, txtBuscar, btnBuscar, spc, btnRefrescar, btnNuevo, btnEditar, btnDesact);
        acciones.setAlignment(Pos.CENTER_LEFT);

        cargarEmpleados(dataEmps);

        btnBuscar.setOnAction(e -> {
            String q = txtBuscar.getText().trim().toLowerCase();
            if (q.isEmpty()) { cargarEmpleados(dataEmps); return; }
            ObservableList<String[]> filtrado = FXCollections.observableArrayList();
            for (String[] r : dataEmps) {
                if (r[1].toLowerCase().contains(q) ||
                    r[2].toLowerCase().contains(q) ||
                    r[3].toLowerCase().contains(q))
                    filtrado.add(r);
            }
            tablaEmps.setItems(filtrado);
        });

        btnRefrescar.setOnAction(e -> {
            txtBuscar.clear();
            cargarEmpleados(dataEmps);
            tablaEmps.setItems(dataEmps);
        });

        btnNuevo.setOnAction(e -> {
            Stage s = new Stage();
            new GestionEmpleadosView(s, null, () -> cargarEmpleados(dataEmps));
        });

        btnEditar.setOnAction(e -> {
            String[] sel = tablaEmps.getSelectionModel().getSelectedItem();
            if (sel == null) { info("Selecciona un empleado para editar."); return; }
            Stage s = new Stage();
            new GestionEmpleadosView(s, sel, () -> cargarEmpleados(dataEmps));
        });

        btnDesact.setOnAction(e -> {
            String[] sel = tablaEmps.getSelectionModel().getSelectedItem();
            if (sel == null) { info("Selecciona un empleado para desactivar."); return; }
            Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                    "Desactivar al empleado " + sel[1] + " " + sel[2] + "?",
                    ButtonType.YES, ButtonType.NO);
            conf.showAndWait().ifPresent(r -> {
                if (r == ButtonType.YES) {
                    try {
                        boolean ok = personaServices.actualizarEmpleado(
                                Integer.parseInt(sel[0]), sel[1], sel[2],
                                sel.length > 7 ? sel[7] : "", sel[4], false);
                        if (ok) cargarEmpleados(dataEmps);
                        else    error("No se pudo desactivar el empleado.");
                    } catch (Exception ex) { error("Error: " + ex.getMessage()); }
                }
            });
        });

        panel.getChildren().addAll(acciones, tablaEmps);
        return panel;
    }

    private void cargarEmpleados(ObservableList<String[]> data) {
        data.clear();
        String sql = "SELECT p.id_persona, p.nombres, p.apellidos, p.email, " +
                "c.nombre AS cargo, p.activo, p.telefono" +
                "FROM persona p " +
                "JOIN empleado e ON p.id_persona = e.id_persona " +
                "JOIN cargo    c ON e.id_cargo   = c.id_cargo " +
                "ORDER BY p.nombres ASC";
        try (java.sql.Connection conn = Proyecto.util.conexionBD.obtenerConexion();
             java.sql.Statement  stmt = conn.createStatement();
             java.sql.ResultSet  rs   = stmt.executeQuery(sql)) {
            while (rs.next()) {
                data.add(new String[]{
                        String.valueOf(rs.getInt("id_persona")),
                        rs.getString("nombres"),
                        rs.getString("apellidos"),
                        rs.getString("email"),
                        rs.getString("cargo"),
                        rs.getBoolean("activo") ? "Activo" : "Inactivo",
                        rs.getString("telefono") != null ? rs.getString("telefono") : ""
                });
            }
        } catch (java.sql.SQLException e) {
            System.err.println("AdminDashboardView.cargarEmpleados: " + e.getMessage());
        }
    }

    // =========================================================================
    // TAB 3: PROVEEDORES
    // =========================================================================

    @SuppressWarnings("unchecked")
    private Node crearTabProveedores() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(15));

        ObservableList<String[]> dataProv = FXCollections.observableArrayList();
        TableView<String[]> tablaProv = new TableView<>(dataProv);
        tablaProv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(tablaProv, Priority.ALWAYS);
        tablaProv.getColumns().addAll(
                columnaStr("ID",       0),
                columnaStr("Empresa",  1),
                columnaStr("NIT",      2),
                columnaStr("Contacto", 3),
                columnaStr("Email",    4),
                columnaStr("Telefono", 5));

        Button btnRefrescar = boton("Actualizar", "#0A1933");
        Button btnNuevo     = boton("Nuevo",      "#1A8A2A");
        Button btnEditar    = boton("Editar",     "#795548");

        Region spc = new Region();
        HBox.setHgrow(spc, Priority.ALWAYS);
        HBox acciones = new HBox(10, spc, btnRefrescar, btnNuevo, btnEditar);
        acciones.setAlignment(Pos.CENTER_LEFT);

        cargarProveedores(dataProv);

        btnRefrescar.setOnAction(e -> cargarProveedores(dataProv));
        btnNuevo.setOnAction(e -> {
            Stage s = new Stage();
            new GestionProveedoresView(s, null, () -> cargarProveedores(dataProv));
        });
        btnEditar.setOnAction(e -> {
            String[] sel = tablaProv.getSelectionModel().getSelectedItem();
            if (sel == null) { info("Selecciona un proveedor para editar."); return; }
            Stage s = new Stage();
            new GestionProveedoresView(s, sel, () -> cargarProveedores(dataProv));
        });

        panel.getChildren().addAll(acciones, tablaProv);
        return panel;
    }

    private void cargarProveedores(ObservableList<String[]> data) {
        data.clear();
        String sql = "SELECT p.id_persona, pr.nombre_empresa, pr.nit, " +
                "p.nombres, p.email, p.telefono " +
                "FROM persona p " +
                "JOIN proveedor pr ON p.id_persona = pr.id_persona " +
                "WHERE p.activo = TRUE ORDER BY pr.nombre_empresa ASC";
        try (java.sql.Connection conn = Proyecto.util.conexionBD.obtenerConexion();
             java.sql.Statement  stmt = conn.createStatement();
             java.sql.ResultSet  rs   = stmt.executeQuery(sql)) {
            while (rs.next()) {
                data.add(new String[]{
                        String.valueOf(rs.getInt("id_persona")),
                        rs.getString("nombre_empresa"),
                        rs.getString("nit"),
                        rs.getString("nombres"),
                        rs.getString("email"),
                        rs.getString("telefono") != null ? rs.getString("telefono") : ""
                });
            }
        } catch (java.sql.SQLException e) {
            System.err.println("AdminDashboardView.cargarProveedores: " + e.getMessage());
        }
    }

    // =========================================================================
    // TAB 4: CATEGORIAS
    // =========================================================================

    @SuppressWarnings("unchecked")
    private Node crearTabCategorias() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(15));

        ObservableList<String[]> dataCat = FXCollections.observableArrayList();
        TableView<String[]> tablaCat = new TableView<>(dataCat);
        tablaCat.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(tablaCat, Priority.ALWAYS);
        tablaCat.getColumns().addAll(
                columnaStr("ID",          0),
                columnaStr("Nombre",      1),
                columnaStr("Descripcion", 2),
                columnaStr("Estado",      3));

        Button btnRefrescar = boton("Actualizar",        "#0A1933");
        Button btnNueva     = boton("Nueva categoria",   "#1A8A2A");
        Button btnEditar    = boton("Editar",            "#795548");
        Button btnElim      = boton("Eliminar",          "#C83C3C");

        HBox acciones = new HBox(10, btnRefrescar, btnNueva, btnEditar, btnElim);
        acciones.setAlignment(Pos.CENTER_LEFT);

        cargarCategorias(dataCat);

        btnRefrescar.setOnAction(e -> cargarCategorias(dataCat));
        btnNueva.setOnAction(e    -> mostrarFormCategoria(null, dataCat));
        btnEditar.setOnAction(e -> {
            String[] sel = tablaCat.getSelectionModel().getSelectedItem();
            if (sel == null) { info("Selecciona una categoria."); return; }
            mostrarFormCategoria(sel, dataCat);
        });
        btnElim.setOnAction(e -> {
            String[] sel = tablaCat.getSelectionModel().getSelectedItem();
            if (sel == null) { info("Selecciona una categoria."); return; }
            Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                    "Eliminar la categoria \"" + sel[1] + "\"?", ButtonType.YES, ButtonType.NO);
            conf.showAndWait().ifPresent(r -> {
                if (r == ButtonType.YES) {
                    try {
                        new CategoriaServices().eliminarCategoria(Integer.parseInt(sel[0]));
                        cargarCategorias(dataCat);
                    } catch (Exception ex) { error("No se pudo eliminar la categoria."); }
                }
            });
        });

        panel.getChildren().addAll(acciones, tablaCat);
        return panel;
    }

    private void cargarCategorias(ObservableList<String[]> data) {
        data.clear();
        try {
            var cats = new CategoriaServices().obtenerTodasLasCategorias();
            for (var c : cats)
                data.add(new String[]{
                        String.valueOf(c.getId()),
                        c.getNombre(),
                        c.getDescripcion() != null ? c.getDescripcion() : "",
                        "Activa"
                });
        } catch (Exception e) {
            System.err.println("AdminDashboardView.cargarCategorias: " + e.getMessage());
        }
    }

    private void mostrarFormCategoria(String[] cat, ObservableList<String[]> data) {
        Dialog<String[]> dlg = new Dialog<>();
        dlg.setTitle(cat == null ? "Nueva Categoria" : "Editar Categoria");
        GridPane g = new GridPane();
        g.setHgap(12); g.setVgap(12); g.setPadding(new Insets(20));
        TextField txtNombre = campo("Nombre de la categoria");
        TextField txtDesc   = campo("Descripcion");
        if (cat != null) { txtNombre.setText(cat[1]); txtDesc.setText(cat[2]); }
        g.add(etiqueta("Nombre:"),      0, 0); g.add(txtNombre, 1, 0);
        g.add(etiqueta("Descripcion:"), 0, 1); g.add(txtDesc,   1, 1);
        ButtonType btnOk = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(btnOk, ButtonType.CANCEL);
        dlg.getDialogPane().setContent(g);
        dlg.setResultConverter(bt -> bt == btnOk
                ? new String[]{txtNombre.getText(), txtDesc.getText()} : null);
        dlg.showAndWait().ifPresent(r -> {
            if (r[0].trim().isEmpty()) return;
            try {
                CategoriaServices cs = new CategoriaServices();
                if (cat == null) cs.crearCategoria(r[0].trim(), r[1].trim());
                else             cs.actualizarCategoria(Integer.parseInt(cat[0]), r[0].trim(), r[1].trim());
                cargarCategorias(data);
            } catch (Exception ex) { error("No se pudo guardar la categoria."); }
        });
    }

    // =========================================================================
    // TAB 5: REPORTES — CORRECCIÓN PRINCIPAL
    // Los 4 reportes marcados en la imagen ahora están implementados.
    // =========================================================================

    private Node crearTabReportes() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(15));

        Label lblTitulo = new Label("Generacion de Reportes");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        lblTitulo.setTextFill(Color.web("#0A1933"));

        String[][] reportes = {
                {"Reporte de Ventas",         "Resumen de todas las ventas: cantidad, monto total y promedio."},
                {"Inventario Actual",          "Estado del inventario con stock, precios y valor total por producto."},
                {"Alertas de Stock Bajo",      "Productos cuyo stock actual esta igual o por debajo del minimo."},
                {"Clientes Frecuentes",        "Top 10 clientes con mayor cantidad de compras y monto gastado."},
                {"Rotacion de Productos",      "Productos con mayor y menor numero de unidades vendidas."},
                {"Rentabilidad por Categoria", "Margen de ganancia agrupado por categoria de producto."},
        };

        TextArea txtSalida = new TextArea();
        txtSalida.setEditable(false);
        txtSalida.setFont(Font.font("Monospaced", 12));
        txtSalida.setPrefHeight(300);
        VBox.setVgrow(txtSalida, Priority.ALWAYS);

        FlowPane botonesRep = new FlowPane(10, 10);
        for (String[] rep : reportes) {
            Button btn = boton(rep[0], "#0A1933");
            btn.setPrefWidth(270);
            btn.setTooltip(new Tooltip(rep[1]));
            btn.setOnAction(e -> generarReporte(rep[0], txtSalida));
            botonesRep.getChildren().add(btn);
        }

        HBox exportRow = new HBox(10);
        exportRow.setAlignment(Pos.CENTER_RIGHT);
        Button btnExportar = boton("Copiar al portapapeles", "#1A8A2A");
        btnExportar.setOnAction(e -> {
            if (txtSalida.getText().isEmpty()) { info("Genera un reporte primero."); return; }
            javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(txtSalida.getText());
            cb.setContent(cc);
            info("Reporte copiado al portapapeles.");
        });
        exportRow.getChildren().add(btnExportar);

        Label lblResultado = new Label("Resultado:");
        lblResultado.setFont(Font.font("Arial", FontWeight.BOLD, 13));

        panel.getChildren().addAll(lblTitulo, botonesRep, lblResultado, txtSalida, exportRow);
        ScrollPane scroll = new ScrollPane(panel);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: white; -fx-background-color: white;");
        return scroll;
    }

    /**
     * CORRECCION:
     * El switch original solo tenia 2 casos implementados.
     * Los 4 restantes caian al default con el mensaje
     * "Conecta el servicio correspondiente en generarReporte()".
     *
     * Ahora todos los casos estan implementados con metodos privados
     * que usan los servicios ya existentes en el proyecto.
     */
    private void generarReporte(String tipo, TextArea destino) {
        destino.setText("Generando " + tipo + "...\n");

        new Thread(() -> {
            String resultado;
            try {
                resultado = switch (tipo) {
                    case "Reporte de Ventas"          -> documentoServices.generarReporteVentas();
                    case "Inventario Actual"           -> reporteInventarioActual();
                    case "Alertas de Stock Bajo"       -> inventarioServices.generarAlertaInventario();
                    case "Clientes Frecuentes"         -> reporteClientesFrecuentes();
                    case "Rotacion de Productos"       -> reporteRotacionProductos();
                    case "Rentabilidad por Categoria"  -> reporteRentabilidadCategoria();
                    default -> "Reporte no reconocido: " + tipo;
                };
            } catch (Exception ex) {
                resultado = "[Error al generar el reporte]\n" + ex.getMessage();
            }
            final String res = resultado;
            javafx.application.Platform.runLater(() -> destino.setText(res));
        }).start();
    }

    // -------------------------------------------------------------------------
    // REPORTE 1 — Inventario Actual
    // Fuente: productoServices.obtenerTodosLosProductos()
    // Muestra: ID, nombre, categoria, precio compra, precio venta, stock, minimo
    // -------------------------------------------------------------------------
    private String reporteInventarioActual() {
        List<Map<String, Object>> filas = new Proyecto.dao.InventarioDAO().reporteInventarioActual();
        StringBuilder sb = new StringBuilder();
        sb.append("INVENTARIO ACTUAL VALORIZADO\n============================\n\n");
        sb.append(String.format("%-5s %-28s %-14s %10s %10s %8s %8s\n",
                "ID", "PRODUCTO", "CATEGORIA", "P.COMPRA", "P.VENTA", "STOCK", "MINIMO"));
        sb.append("-".repeat(88)).append("\n");

        double valorCosto = 0, valorVenta = 0;
        int bajoMinimo = 0;

        for (Map<String, Object> f : filas) {
            int stock = ((Number) f.get("stockActual")).intValue();
            int min = ((Number) f.get("stockMinimo")).intValue();
            sb.append(String.format("%-5d %-28s %-14s %10.0f %10.0f %8d %8d\n",
                    ((Number) f.get("idProducto")).intValue(),
                    truncar(String.valueOf(f.get("nombre")), 26),
                    truncar(String.valueOf(f.get("categoria")), 12),
                    ((Number) f.get("precioCompra")).doubleValue(),
                    ((Number) f.get("precioVenta")).doubleValue(),
                    stock, min));
            valorCosto += ((Number) f.get("valorCosto")).doubleValue();
            valorVenta += ((Number) f.get("valorVenta")).doubleValue();
            if (stock <= min) bajoMinimo++;
        }

        sb.append("-".repeat(88)).append("\n");
        sb.append(String.format("Total productos activos          : %d\n", filas.size()));
        sb.append(String.format("Productos bajo o en stock minimo : %d\n", bajoMinimo));
        sb.append(String.format("Valor total al costo             : $%,.0f\n", valorCosto));
        sb.append(String.format("Valor total a precio de venta    : $%,.0f\n", valorVenta));
        return sb.toString();
    }
    // -------------------------------------------------------------------------
    // REPORTE 2 — Clientes Frecuentes
    // Fuente: documentoServices.obtenerTodasLasVentas()
    // Muestra: ranking de los 10 clientes con mas compras y monto gastado
    // -------------------------------------------------------------------------
    private String reporteClientesFrecuentes() {
        List<Map<String, Object>> filas = new Proyecto.dao.InventarioDAO()
                .reporteClientesFrecuentes("2000-01-01", "2099-12-31");
        StringBuilder sb = new StringBuilder();
        sb.append("TOP CLIENTES FRECUENTES\n=======================\n\n");
        sb.append(String.format("%-4s %-30s %10s %16s\n", "N.", "CLIENTE", "COMPRAS", "TOTAL GASTADO"));
        sb.append("-".repeat(63)).append("\n");

        int pos = 1;
        for (Map<String, Object> f : filas) {
            sb.append(String.format("%-4d %-30s %10d %16s\n",
                    pos++,
                    truncar(String.valueOf(f.get("cliente")), 28),
                    ((Number) f.get("totalCompras")).intValue(),
                    String.format("$%,.0f", ((Number) f.get("montoTotal")).doubleValue())));
        }
        if (filas.isEmpty()) sb.append("\nNo hay ventas completadas registradas.\n");
        return sb.toString();
    }
    // -------------------------------------------------------------------------
    // REPORTE 3 — Rotacion de Productos
    // Fuente: inventarioServices.reporteMovimientos() — filtra solo salidas
    //         (efecto_en_inventario = -1 corresponde a ventas y ajustes de salida)
    // Muestra: productos ordenados por unidades vendidas de mayor a menor
    // -------------------------------------------------------------------------
    private String reporteRotacionProductos() {
        List<Map<String, Object>> filas = new Proyecto.dao.InventarioDAO()
                .reporteRotacionProductos("2000-01-01", "2099-12-31");
        StringBuilder sb = new StringBuilder();
        sb.append("ROTACION DE PRODUCTOS (VENTAS COMPLETADAS)\n==========================================\n\n");
        sb.append(String.format("%-4s %-32s %14s %16s\n", "N.", "PRODUCTO", "UNID. VENDIDAS", "INGRESOS"));
        sb.append("-".repeat(68)).append("\n");

        int pos = 1, totalUnidades = 0;
        double totalIngresos = 0;
        for (Map<String, Object> f : filas) {
            int unids = ((Number) f.get("unidadesVendidas")).intValue();
            double ing = ((Number) f.get("ingresos")).doubleValue();
            sb.append(String.format("%-4d %-32s %14d %16s\n",
                    pos++, truncar(String.valueOf(f.get("nombre")), 30), unids, String.format("$%,.0f", ing)));
            totalUnidades += unids;
            totalIngresos += ing;
        }
        sb.append("-".repeat(68)).append("\n");
        sb.append(String.format("Total unidades vendidas : %d\n", totalUnidades));
        sb.append(String.format("Total ingresos          : $%,.0f\n", totalIngresos));
        return sb.toString();
    }
    // -------------------------------------------------------------------------
    // REPORTE 4 — Rentabilidad por Categoria
    // Fuente: productoServices.obtenerTodosLosProductos()
    // Calculo: margen % = ((precio_venta - precio_compra) / precio_compra) * 100
    // Agrupa por categoria y muestra el margen promedio y el valor potencial
    // -------------------------------------------------------------------------
    private String reporteRentabilidadCategoria() {
        List<Map<String, Object>> filas = new Proyecto.dao.InventarioDAO()
                .reporteRentabilidadPorCategoria("2000-01-01", "2099-12-31");
        StringBuilder sb = new StringBuilder();
        sb.append("RENTABILIDAD HISTORICA POR CATEGORIA\n====================================\n\n");
        sb.append(String.format("%-20s %15s %15s %15s %10s\n",
                "CATEGORIA", "INGRESOS", "COSTO HIST.", "UTILIDAD", "MARGEN %"));
        sb.append("-".repeat(78)).append("\n");

        double totIng = 0, totCos = 0, totUti = 0;
        for (Map<String, Object> f : filas) {
            double ing = ((Number) f.get("ingresos")).doubleValue();
            double cos = ((Number) f.get("costos")).doubleValue();
            double uti = ((Number) f.get("utilidad")).doubleValue();
            double margen = cos > 0 ? (uti / cos) * 100.0 : 0.0;
            sb.append(String.format("%-20s %15s %15s %15s %9.1f%%\n",
                    truncar(String.valueOf(f.get("categoria")), 18),
                    String.format("$%,.0f", ing),
                    String.format("$%,.0f", cos),
                    String.format("$%,.0f", uti),
                    margen));
            totIng += ing;
            totCos += cos;
            totUti += uti;
        }
        sb.append("-".repeat(78)).append("\n");
        double margenGlobal = totCos > 0 ? (totUti / totCos) * 100.0 : 0.0;
        sb.append(String.format("%-20s %15s %15s %15s %9.1f%%\n",
                "TOTAL GENERAL",
                String.format("$%,.0f", totIng),
                String.format("$%,.0f", totCos),
                String.format("$%,.0f", totUti),
                margenGlobal));
        return sb.toString();
    }

    // =========================================================================
    // HELPERS UI
    // =========================================================================

    private VBox kpiCard(String titulo, Label lblValor, String color) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(185);
        card.setPadding(new Insets(15, 10, 15, 10));
        card.setStyle("-fx-background-color: #F5F5FA; -fx-border-color: " + color + "; -fx-border-width: 0 0 4 0;");
        Label lbl = new Label(titulo);
        lbl.setTextFill(Color.GRAY);
        lbl.setFont(Font.font("Arial", 12));
        lblValor.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        lblValor.setTextFill(Color.web("#0A1933"));
        card.getChildren().addAll(lbl, lblValor);
        return card;
    }

    private TableColumn<String[], String> columnaStr(String titulo, int idx) {
        TableColumn<String[], String> col = new TableColumn<>(titulo);
        col.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().length > idx ? d.getValue()[idx] : ""));
        return col;
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
        l.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        return l;
    }

    private Button boton(String texto, String color) {
        Button b = new Button(texto);
        b.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        b.setTextFill(Color.WHITE);
        b.setStyle("-fx-background-color:" + color + ";-fx-border-width:0;-fx-cursor:hand;-fx-padding:7 14 7 14;");
        return b;
    }

    /**
     * Recorta un String al maximo de caracteres indicado para evitar
     * que los reportes de texto fijo rompan el alineado de columnas.
     */
    private String truncar(String s, int max) {
        if (s == null || s.isEmpty()) return "-";
        return s.length() > max ? s.substring(0, max - 1) + "." : s;
    }

    private void info(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    private void error(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}