package Proyecto.View.Producto;

import Proyecto.Model.Categoria;
import Proyecto.Model.Producto;
import Proyecto.services.CategoriaServices;
import Proyecto.services.ProductoServices;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;

public class ProductoFormView {

    private TextField txtNombre;
    private TextArea txtDescripcion;
    private ComboBox<Categoria> cbCategoria;
    private TextField txtPrecioCompra;
    private TextField txtPrecioVenta;
    private TextField txtStock;
    private Button btnGuardar;
    private Button btnCancelar;

    private final ProductoServices productoServices;
    private final CategoriaServices categoriaServices;
    private final boolean editando;
    private final Integer idProducto;
    private boolean guardadoExitoso = false;

    private Stage dialogStage;

    // ── Constructores ────────────────────────────────────────────────────────
    /** Nuevo producto */
    public ProductoFormView(Window owner) {
        this.productoServices = new ProductoServices();
        this.categoriaServices = new CategoriaServices();
        this.editando = false;
        this.idProducto = null;
        build(owner, "Nuevo Producto");
    }

    /** Editar producto existente */
    public ProductoFormView(Window owner, int idProducto) {
        this.productoServices = new ProductoServices();
        this.categoriaServices = new CategoriaServices();
        this.editando = true;
        this.idProducto = idProducto;
        build(owner, "Editar Producto");
        cargarDatosProducto();
    }

    // ── Construcción de la interfaz ──────────────────────────────────────────
    private void build(Window owner, String titulo) {
        dialogStage = new Stage();
        dialogStage.setTitle(titulo);
        dialogStage.setResizable(false);
        dialogStage.initModality(Modality.WINDOW_MODAL);
        dialogStage.initOwner(owner);

        GridPane grid = new GridPane();
        grid.setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(25));

        // Columnas
        ColumnConstraints col0 = new ColumnConstraints(160);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col0, col1);

        int fila = 0;

        // Título
        Label lblTitulo = new Label(editando ? "EDITAR PRODUCTO" : "NUEVO PRODUCTO");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        lblTitulo.setTextFill(Color.web("#0A1933"));
        GridPane.setColumnSpan(lblTitulo, 2);
        GridPane.setHalignment(lblTitulo, HPos.CENTER);
        grid.add(lblTitulo, 0, fila++);

        // Separador
        Separator sep = new Separator();
        GridPane.setColumnSpan(sep, 2);
        grid.add(sep, 0, fila++);

        // Nombre
        txtNombre = campoTexto();
        grid.add(etiqueta("Nombre del Producto:"), 0, fila);
        grid.add(txtNombre, 1, fila++);

        // Categoría
        cbCategoria = new ComboBox<>();
        cbCategoria.setMaxWidth(Double.MAX_VALUE);
        cbCategoria.setStyle("-fx-font-size: 12px;");
        cargarCategorias();
        grid.add(etiqueta("Categoría:"), 0, fila);
        grid.add(cbCategoria, 1, fila++);

        // Descripción
        txtDescripcion = new TextArea();
        txtDescripcion.setPrefRowCount(3);
        txtDescripcion.setWrapText(true);
        txtDescripcion.setFont(Font.font("Arial", 12));
        txtDescripcion.setStyle(
                "-fx-border-color: #B4B4B4; -fx-border-width: 1; -fx-padding: 6;");
        grid.add(etiqueta("Descripción:"), 0, fila);
        grid.add(new ScrollPane(txtDescripcion) {
            {
                setPrefHeight(80);
                setFitToWidth(true);
            }
        }, 1, fila++);

        // Precio Compra
        txtPrecioCompra = campoTexto();
        grid.add(etiqueta("Precio de Compra:"), 0, fila);
        grid.add(txtPrecioCompra, 1, fila++);

        // Precio Venta
        txtPrecioVenta = campoTexto();
        grid.add(etiqueta("Precio de Venta:"), 0, fila);
        grid.add(txtPrecioVenta, 1, fila++);

        // Stock
        txtStock = campoTexto();
        grid.add(etiqueta("Stock Inicial:"), 0, fila);
        grid.add(txtStock, 1, fila++);

        // Botones
        btnGuardar = boton(editando ? "ACTUALIZAR" : "GUARDAR", "#00C8FF");
        btnGuardar.setOnAction(e -> guardarProducto());

        btnCancelar = boton("CANCELAR", "#646464");
        btnCancelar.setOnAction(e -> dialogStage.close());

        HBox btnBox = new HBox(20, btnGuardar, btnCancelar);
        btnBox.setAlignment(Pos.CENTER);
        GridPane.setColumnSpan(btnBox, 2);
        grid.add(btnBox, 0, fila);

        Scene scene = new Scene(grid, 500, 580);
        dialogStage.setScene(scene);
        dialogStage.showAndWait();
    }

    // ── Datos ────────────────────────────────────────────────────────────────
    private void cargarCategorias() {
        cbCategoria.getItems().clear();
        List<Categoria> lista = categoriaServices.obtenerTodasLasCategorias();
        if (lista.isEmpty()) {
            cbCategoria.getItems().add(new Categoria(1, "General", "Categoría general"));
        } else {
            cbCategoria.getItems().addAll(lista);
        }
        cbCategoria.getSelectionModel().selectFirst();
    }

    private void cargarDatosProducto() {
        if (idProducto == null)
            return;
        Producto p = productoServices.obtenerProducto(idProducto);
        if (p == null)
            return;

        txtNombre.setText(p.getNombre());
        txtDescripcion.setText(p.getDescripcion());
        txtPrecioCompra.setText(String.valueOf(p.getPrecioCompra()));
        txtPrecioVenta.setText(String.valueOf(p.getPrecioVenta()));
        txtStock.setText(String.valueOf(p.getCantidad()));

        if (p.getCategoria() != null) {
            cbCategoria.getItems().stream()
                    .filter(c -> c.getId() == p.getCategoria().getId())
                    .findFirst()
                    .ifPresent(c -> cbCategoria.getSelectionModel().select(c));
        }
    }

    // ── Guardar ──────────────────────────────────────────────────────────────
    private void guardarProducto() {
        if (!validarCampos())
            return;

        String nombre = txtNombre.getText().trim();
        String descripcion = txtDescripcion.getText().trim();
        Categoria categoria = cbCategoria.getSelectionModel().getSelectedItem();
        double precioCompra = Double.parseDouble(txtPrecioCompra.getText().trim());
        double precioVenta = Double.parseDouble(txtPrecioVenta.getText().trim());
        int stock = Integer.parseInt(txtStock.getText().trim());

        boolean ok = editando
                ? productoServices.actualizarProducto(idProducto, nombre, descripcion, precioCompra, precioVenta, stock)
                : productoServices.crearProducto(categoria.getId(), nombre, descripcion, precioCompra, precioVenta,
                        stock);

        if (ok) {
            guardadoExitoso = true;
            info(editando ? "Producto actualizado exitosamente" : "Producto creado exitosamente");
            dialogStage.close();
        } else {
            error("Error al " + (editando ? "actualizar" : "crear") + " el producto");
        }
    }

    // ── Validación ───────────────────────────────────────────────────────────
    private boolean validarCampos() {
        if (txtNombre.getText().trim().isEmpty()) {
            return err("El nombre del producto es requerido", txtNombre);
        }
        try {
            double pc = Double.parseDouble(txtPrecioCompra.getText().trim());
            if (pc <= 0)
                return err("El precio de compra debe ser mayor a 0", txtPrecioCompra);
        } catch (NumberFormatException e) {
            return err("Ingrese un precio de compra válido", txtPrecioCompra);
        }
        try {
            double pv = Double.parseDouble(txtPrecioVenta.getText().trim());
            if (pv <= 0)
                return err("El precio de venta debe ser mayor a 0", txtPrecioVenta);
            double pc = Double.parseDouble(txtPrecioCompra.getText().trim());
            if (pv < pc)
                return err("El precio de venta no puede ser menor al de compra", txtPrecioVenta);
        } catch (NumberFormatException e) {
            return err("Ingrese un precio de venta válido", txtPrecioVenta);
        }
        try {
            int s = Integer.parseInt(txtStock.getText().trim());
            if (s < 0)
                return err("El stock no puede ser negativo", txtStock);
        } catch (NumberFormatException e) {
            return err("Ingrese un stock válido", txtStock);
        }
        return true;
    }

    private boolean err(String msg, Control foco) {
        error(msg);
        foco.requestFocus();
        return false;
    }

    // ── Helpers visuales ─────────────────────────────────────────────────────
    private Label etiqueta(String texto) {
        Label l = new Label(texto);
        l.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        l.setTextFill(Color.web("#323232"));
        return l;
    }

    private TextField campoTexto() {
        TextField tf = new TextField();
        tf.setFont(Font.font("Arial", 12));
        tf.setStyle(
                "-fx-border-color: #B4B4B4; -fx-border-width: 1; -fx-padding: 8;");
        return tf;
    }

    private Button boton(String texto, String color) {
        Button b = new Button(texto);
        b.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        b.setTextFill(Color.WHITE);
        b.setPrefSize(130, 40);
        b.setStyle("-fx-background-color: " + color + "; -fx-border-width: 0; -fx-cursor: hand;");
        return b;
    }

    private void info(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    private void error(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setTitle("Error de validación");
        a.showAndWait();
    }

    public boolean isGuardadoExitoso() {
        return guardadoExitoso;
    }
}