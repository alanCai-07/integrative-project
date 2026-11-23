package Proyecto.util;

import Proyecto.Model.Cliente;
import Proyecto.View.Documento.CotizacionView.ItemCotizacion;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private static final double TASA_IVA = 0.19;
    private static final float MARGEN = 50;
    private static final float INTERLINEADO = 16;
    private static final float ANCHO = 495;
    private static final PDColor AZUL_TECHZONE = color(11, 25, 44);
    private static final PDColor AZUL_VIVO = color(0, 168, 232);
    private static final PDColor AZUL_CLARO = color(232, 247, 252);
    private static final PDColor GRIS_FILA = color(247, 250, 252);
    private static final PDColor GRIS_TEXTO = color(74, 93, 112);
    private static final PDColor BLANCO = color(255, 255, 255);

    private CotizacionPdfGenerator() {
    }

    public static void generar(Path destino, int idCotizacion, Cliente cliente,
                               List<ItemCotizacion> items, LocalDate vigenciaHasta,
                               double descuento, String observaciones) throws IOException {
        if (destino == null || destino.getParent() == null) {
            throw new IOException("La ruta de destino del PDF no es valida.");
        }
        Files.createDirectories(destino.getParent());

        LocalDate vigencia = vigenciaHasta != null ? vigenciaHasta : LocalDate.now().plusDays(15);

        try (PDDocument documento = new PDDocument()) {
            PDPage pagina = new PDPage(PDRectangle.LETTER);
            documento.addPage(pagina);

            try (PDPageContentStream contenido = new PDPageContentStream(documento, pagina)) {
                float y = 740;
                rectangulo(contenido, 0, 700, PDRectangle.LETTER.getWidth(), 92, AZUL_TECHZONE);
                texto(contenido, "TECHZONE", MARGEN, y, 22, PDType1Font.HELVETICA_BOLD, BLANCO);
                String tituloCot = idCotizacion > 0
                        ? String.format("COTIZACION N. COT-%05d", idCotizacion)
                        : "COTIZACION COMERCIAL";
                texto(contenido, tituloCot, 355, y, 13, PDType1Font.HELVETICA_BOLD, AZUL_VIVO);
                y -= 30;
                linea(contenido, y, AZUL_VIVO);
                y -= 25;

                rectangulo(contenido, MARGEN - 10, y - 63, ANCHO + 20, 78, AZUL_CLARO);
                texto(contenido, "Fecha de emision: " + LocalDate.now() + "   |   Valida hasta: " + vigencia,
                        MARGEN, y, 10, PDType1Font.HELVETICA_BOLD, GRIS_TEXTO);
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
                y -= 28;

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

                double descAplicado = Math.max(0.0, Math.min(descuento, subtotal));
                double baseGravable = subtotal - descAplicado;
                double iva = baseGravable * TASA_IVA;
                double total = baseGravable + iva;

                y -= 8;
                linea(contenido, y, AZUL_VIVO);
                y -= 20;
                textoDerecha(contenido, "Subtotal: " + dinero(subtotal), y, GRIS_TEXTO);
                y -= 18;
                textoDerecha(contenido, "Descuento: " + dinero(descAplicado), y, GRIS_TEXTO);
                y -= 18;
                textoDerecha(contenido, "IVA (19%): " + dinero(iva), y, GRIS_TEXTO);
                y -= 22;
                rectangulo(contenido, 360, y - 10, 185, 28, AZUL_TECHZONE);
                textoDerecha(contenido, "TOTAL COTIZADO: " + dinero(total), y, BLANCO);

                if (observaciones != null && !observaciones.isBlank()) {
                    y -= 32;
                    texto(contenido, "Observaciones:", MARGEN, y, 10, PDType1Font.HELVETICA_BOLD, AZUL_VIVO);
                    y -= INTERLINEADO;
                    for (String lineaObs : observaciones.split("\\R")) {
                        texto(contenido, limitar(lineaObs, 95), MARGEN, y, 9, PDType1Font.HELVETICA, GRIS_TEXTO);
                        y -= INTERLINEADO;
                    }
                }

                texto(contenido, "Cotizacion comercial informativa sin afectacion de inventario ni validez de factura DIAN.",
                        MARGEN, 55, 8.5f, PDType1Font.HELVETICA_OBLIQUE, GRIS_TEXTO);
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
        return new PDColor(new float[]{rojo / 255f, verde / 255f, azul / 255f}, PDDeviceRGB.INSTANCE);
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