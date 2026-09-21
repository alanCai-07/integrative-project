package Proyecto.util;

import Proyecto.Model.Cliente;
import Proyecto.View.Documento.CotizacionView.ItemCotizacion;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;

public final class CotizacionPdfGenerator {

    private static final float MARGEN = 50;
    private static final float INTERLINEADO = 16;
    private static final float ANCHO = 495;
    private static final PDColor AZUL_TECHZONE = color(10, 25, 51);
    private static final PDColor AZUL_VIVO = color(0, 160, 210);
    private static final PDColor AZUL_CLARO = color(232, 247, 252);
    private static final PDColor GRIS_FILA = color(247, 250, 252);
    private static final PDColor GRIS_TEXTO = color(74, 93, 112);
    private static final PDColor BLANCO = color(255, 255, 255);

    private CotizacionPdfGenerator() {
    }

    public static void generar(Path destino, Cliente cliente, List<ItemCotizacion> items,
            double descuento, String observaciones) throws IOException {
        if (destino == null || destino.getParent() == null) {
            throw new IOException("La ruta de destino del PDF no es válida.");
        }
        Files.createDirectories(destino.getParent());

        try (PDDocument documento = new PDDocument()) {
            PDPage pagina = new PDPage(PDRectangle.LETTER);
            documento.addPage(pagina);

            try (PDPageContentStream contenido = new PDPageContentStream(documento, pagina)) {
                float y = 740;
                rectangulo(contenido, 0, 700, PDRectangle.LETTER.getWidth(), 92, AZUL_TECHZONE);
                texto(contenido, "TECHZONE", MARGEN, y, 22, PDType1Font.HELVETICA_BOLD, BLANCO);
                texto(contenido, "COTIZACION", 420, y, 16, PDType1Font.HELVETICA_BOLD, AZUL_VIVO);
                y -= 30;
                linea(contenido, y, AZUL_VIVO);
                y -= 25;

                rectangulo(contenido, MARGEN - 10, y - 63, ANCHO + 20, 78, AZUL_CLARO);
                texto(contenido, "Fecha: " + LocalDate.now(), MARGEN, y, 10, PDType1Font.HELVETICA, GRIS_TEXTO);
                y -= INTERLINEADO;
                texto(contenido, "Cliente: " + cliente.getNombre() + " " + cliente.getApellido(),
                        MARGEN, y, 10, PDType1Font.HELVETICA, AZUL_TECHZONE);
                y -= INTERLINEADO;
                texto(contenido, "Documento: " + valor(cliente.getDocumento()), MARGEN, y, 10,
                        PDType1Font.HELVETICA, GRIS_TEXTO);
                y -= INTERLINEADO;
                texto(contenido, "Correo: " + valor(cliente.getEmail()), MARGEN, y, 10,
                        PDType1Font.HELVETICA, GRIS_TEXTO);
                y -= 25;

                linea(contenido, y, AZUL_VIVO);
                y -= 20;
                rectangulo(contenido, MARGEN - 8, y - 8, ANCHO + 16, 25, AZUL_VIVO);
                texto(contenido, "PRODUCTO", MARGEN, y, 10, PDType1Font.HELVETICA_BOLD, BLANCO);
                texto(contenido, "CANT.", 340, y, 10, PDType1Font.HELVETICA_BOLD, BLANCO);
                texto(contenido, "PRECIO", 400, y, 10, PDType1Font.HELVETICA_BOLD, BLANCO);
                texto(contenido, "SUBTOTAL", 480, y, 10, PDType1Font.HELVETICA_BOLD, BLANCO);
                y -= 8;
                y -= 20;

                double subtotal = 0;
                boolean filaAlterna = false;
                for (ItemCotizacion item : items) {
                    double totalItem = item.getPrecioUnit() * item.getCantidad();
                    subtotal += totalItem;
                    if (filaAlterna) {
                        rectangulo(contenido, MARGEN - 8, y - 7, ANCHO + 16, 20, GRIS_FILA);
                    }
                    texto(contenido, limitar(item.getNombre(), 38), MARGEN, y, 9,
                            PDType1Font.HELVETICA, AZUL_TECHZONE);
                    texto(contenido, String.valueOf(item.getCantidad()), 345, y, 9,
                            PDType1Font.HELVETICA, GRIS_TEXTO);
                    texto(contenido, dinero(item.getPrecioUnit()), 400, y, 9,
                            PDType1Font.HELVETICA, GRIS_TEXTO);
                    texto(contenido, dinero(totalItem), 480, y, 9, PDType1Font.HELVETICA, AZUL_TECHZONE);
                    y -= INTERLINEADO;
                    filaAlterna = !filaAlterna;
                }

                y -= 8;
                linea(contenido, y, AZUL_VIVO);
                y -= 24;
                textoDerecha(contenido, "Subtotal: " + dinero(subtotal), y, GRIS_TEXTO);
                y -= 22;
                textoDerecha(contenido, "Descuento: " + dinero(descuento), y, GRIS_TEXTO);
                y -= 24;
                rectangulo(contenido, 380, y - 10, 165, 30, AZUL_TECHZONE);
                textoDerecha(contenido, "TOTAL: " + dinero(Math.max(0, subtotal - descuento)), y,
                        BLANCO);

                if (observaciones != null && !observaciones.isBlank()) {
                    y -= 35;
                    texto(contenido, "Observaciones:", MARGEN, y, 10, PDType1Font.HELVETICA_BOLD,
                            AZUL_VIVO);
                    y -= INTERLINEADO;
                    for (String linea : observaciones.split("\\R")) {
                        texto(contenido, limitar(linea, 95), MARGEN, y, 9,
                                PDType1Font.HELVETICA, GRIS_TEXTO);
                        y -= INTERLINEADO;
                    }
                }

                texto(contenido, "Gracias por confiar en TechZone.", MARGEN, 55, 9,
                        PDType1Font.HELVETICA_OBLIQUE, AZUL_VIVO);
            }
            documento.save(destino.toFile());
        }
    }

    private static void texto(PDPageContentStream contenido, String valor, float x, float y,
            float tamano, PDType1Font fuente, PDColor color) throws IOException {
        contenido.beginText();
        contenido.setNonStrokingColor(color);
        contenido.setFont(fuente, tamano);
        contenido.newLineAtOffset(x, y);
        contenido.showText(sanitizar(valor));
        contenido.endText();
    }

    private static void textoDerecha(PDPageContentStream contenido, String valor, float y, PDColor color)
            throws IOException {
        float ancho = PDType1Font.HELVETICA.getStringWidth(sanitizar(valor)) / 1000 * 10;
        texto(contenido, valor, 545 - ancho, y, 10, PDType1Font.HELVETICA, color);
    }

    private static void linea(PDPageContentStream contenido, float y, PDColor color) throws IOException {
        contenido.setStrokingColor(color);
        contenido.moveTo(MARGEN, y);
        contenido.lineTo(545, y);
        contenido.stroke();
    }

    private static void rectangulo(PDPageContentStream contenido, float x, float y, float ancho,
            float alto, PDColor color) throws IOException {
        contenido.setNonStrokingColor(color);
        contenido.addRect(x, y, ancho, alto);
        contenido.fill();
    }

    private static PDColor color(int rojo, int verde, int azul) {
        return new PDColor(new float[] { rojo / 255f, verde / 255f, azul / 255f }, PDDeviceRGB.INSTANCE);
    }

    private static String dinero(double valor) {
        return String.format("$%,.2f", valor);
    }

    private static String valor(String valor) {
        return valor == null || valor.isBlank() ? "No registrado" : valor;
    }

    private static String limitar(String valor, int maximo) {
        String texto = valor == null ? "" : valor;
        return texto.length() > maximo ? texto.substring(0, maximo - 3) + "..." : texto;
    }

    private static String sanitizar(String valor) {
        String texto = valor.replace("á", "a").replace("é", "e").replace("í", "i")
                .replace("ó", "o").replace("ú", "u").replace("ñ", "n")
                .replace("Á", "A").replace("É", "E").replace("Í", "I")
                .replace("Ó", "O").replace("Ú", "U").replace("Ñ", "N");
        StringBuilder compatible = new StringBuilder(texto.length());
        for (char caracter : texto.toCharArray()) {
            compatible.append(caracter >= 32 && caracter <= 126 ? caracter : '?');
        }
        return compatible.toString();
    }
}
