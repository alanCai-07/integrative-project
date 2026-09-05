package Proyecto.View.Carrito;

import Proyecto.Model.Cliente;
import Proyecto.services.CarritoServices;
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
 * Vista del Carrito de Compras en JavaFX.
 * Muestra los productos añadidos, permite modificar cantidades,
 * eliminar ítems y finalizar la compra.
 */
public class CarritoView {

    private final Cliente cliente;
    private final CarritoServices carritoServices;
    private final DocumentoServices documentoServices;

    private TableView<ItemCarrito> tabla;
    private ObservableList<ItemCarrito> items;
    private Label lblTotal;
    private VBox root;

    public CarritoView(Cliente cliente) {
        this.cliente = cliente;
        this.carritoServices = new CarritoServices();
        this.documentoServices = new DocumentoServices();
        this.items = FXCollections.observableArrayList();
        build();
        cargarCarrito();
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
        Label lblTitulo = new Label(" Mi Carrito de Compras");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        lblTitulo.setTextFill(Color.web("#0A1933"));

        // Tabla
        tabla = new TableView<>(items);
        //  Correcto para JavaFX 21
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(tabla, Priority.ALWAYS);

        TableColumn<ItemCarrito, String> colNombre = new TableColumn<>("Producto");
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));

        TableColumn<ItemCarrito, Double> colPrecio = new TableColumn<>("Precio Unit.");
        colPrecio.setCellValueFactory(d -> new SimpleDoubleProperty(d.getValue().getPrecio()).asObject());
        colPrecio.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("$%.2f", v));
            }
        });

        TableColumn<ItemCarrito, Integer> colCantidad = new TableColumn<>("Cantidad");
        colCantidad.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getCantidad()).asObject());

        TableColumn<ItemCarrito, Double> colSubtotal = new TableColumn<>("Subtotal");
        colSubtotal.setCellValueFactory(
                d -> new SimpleDoubleProperty(d.getValue().getPrecio() * d.getValue().getCantidad()).asObject());
        colSubtotal.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty || v == null ? null : String.format("$%.2f", v));
            }
        });

        TableColumn<ItemCarrito, Void> colAcciones = new TableColumn<>("Acciones");
        colAcciones.setMaxWidth(120);
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnElim = boton(" Eliminar", "#C83C3C");
            {
                btnElim.setOnAction(e -> {
                    ItemCarrito item = getTableView().getItems().get(getIndex());
                    eliminarItem(item);
                });
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btnElim);
            }
        });

        tabla.getColumns().addAll(colNombre, colPrecio, colCantidad, colSubtotal, colAcciones);
        tabla.setRowFactory(tv -> {
            TableRow<ItemCarrito> row = new TableRow<>();
            row.setStyle("-fx-cell-size: 40px;");
            return row;
        });

        // Footer: total + botones
        HBox footer = new HBox(20);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(10, 0, 0, 0));

        lblTotal = new Label("Total: $0.00");
        lblTotal.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        lblTotal.setTextFill(Color.web("#0A1933"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnVaciar = boton(" Vaciar", "#646464");
        Button btnComprar = boton(" Finalizar Compra", "#00C8FF");

        btnVaciar.setOnAction(e -> vaciarCarrito());
        btnComprar.setOnAction(e -> finalizarCompra());

        footer.getChildren().addAll(spacer, lblTotal, btnVaciar, btnComprar);

        root.getChildren().addAll(lblTitulo, tabla, footer);
    }

    // ── Datos ────────────────────────────────────────────────────────────────
    private void cargarCarrito() {
        items.clear();
        if (cliente == null || carritoServices == null)
            return;

        // carritoServices.obtenerItemsCarrito(cliente.getId()) debe retornar
        // una lista con los productos y cantidades del cliente.
        // Adaptar según la firma real del servicio.
        try {
            var itemsDB = carritoServices.obtenerItemsCarrito(cliente.getId());
            for (var i : itemsDB) {
                items.add(new ItemCarrito(
                        i.getProducto().getIdProducto(),
                        i.getProducto().getNombre(),
                        i.getProducto().getPrecioVenta(),
                        i.getCantidad()));
            }
        } catch (Exception ex) {
            // Si el servicio aún no está implementado, mostramos tabla vacía
        }
        actualizarTotal();
    }

    private void eliminarItem(ItemCarrito item) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar \"" + item.getNombre() + "\" del carrito?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmar eliminación");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                try {
                    carritoServices.eliminarItem(cliente.getId(), item.getIdProducto());
                } catch (Exception ignored) {
                }
                items.remove(item);
                actualizarTotal();
            }
        });
    }

    private void vaciarCarrito() {
        if (items.isEmpty())
            return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Vaciar todo el carrito?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Vaciar carrito");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                try {
                    carritoServices.vaciarCarrito(cliente.getId());
                } catch (Exception ignored) {
                }
                items.clear();
                actualizarTotal();
            }
        });
    }

    private void finalizarCompra() {
        if (items.isEmpty()) {
            new Alert(Alert.AlertType.WARNING,
                    "El carrito está vacío.", ButtonType.OK).showAndWait();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Confirmar la compra por " + lblTotal.getText() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmar Compra");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                try {
                    int idDocumento = documentoServices.registrarCompra(cliente.getId());
                    if (idDocumento > 0) {
                        new Alert(Alert.AlertType.INFORMATION,
                                "¡Compra realizada exitosamente!", ButtonType.OK).showAndWait();
                        items.clear();
                        actualizarTotal();
                    } else {
                        new Alert(Alert.AlertType.ERROR,
                                "Error al procesar la compra.", ButtonType.OK).showAndWait();
                    }
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.INFORMATION,
                            "Funcionalidad de compra en desarrollo.", ButtonType.OK).showAndWait();
                }
            }
        });
    }

    private void actualizarTotal() {
        double total = items.stream()
                .mapToDouble(i -> i.getPrecio() * i.getCantidad())
                .sum();
        lblTotal.setText(String.format("Total: $%.2f", total));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private Button boton(String texto, String color) {
        Button b = new Button(texto);
        b.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        b.setTextFill(Color.WHITE);
        b.setStyle("-fx-background-color: " + color + "; -fx-border-width:0; -fx-cursor:hand; -fx-padding: 8 16 8 16;");
        return b;
    }

    // ── Modelo de fila ───────────────────────────────────────────────────────
    public static class ItemCarrito {
        private final int idProducto;
        private final String nombre;
        private final double precio;
        private final int cantidad;

        public ItemCarrito(int idProducto, String nombre, double precio, int cantidad) {
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
    }
}