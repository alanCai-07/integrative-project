package Proyecto.util;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;
import java.net.URI;

public final class ProductoImageHelper {

    private ProductoImageHelper() {
    }

    public static ImageView crearVista(String imagenUrl, double ancho, double alto) {
        Image image = cargar(imagenUrl);
        if (image == null) {
            return null;
        }

        ImageView vista = new ImageView(image);
        vista.setFitWidth(ancho);
        vista.setFitHeight(alto);
        vista.setPreserveRatio(true);
        return vista;
    }

    private static Image cargar(String imagenUrl) {
        if (imagenUrl == null || imagenUrl.isBlank()) {
            return null;
        }

        try {
            File archivo = new File(imagenUrl);
            if (archivo.isFile()) {
                return new Image(archivo.toURI().toString(), false);
            }

            URI uri = URI.create(imagenUrl);
            if (uri.isAbsolute()) {
                return new Image(imagenUrl, false);
            }

            var recurso = ProductoImageHelper.class.getClassLoader().getResource(imagenUrl);
            return recurso == null ? null : new Image(recurso.toExternalForm(), false);
        } catch (IllegalArgumentException | SecurityException e) {
            return null;
        }
    }
}
