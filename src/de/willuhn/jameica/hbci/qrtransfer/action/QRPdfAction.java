package de.willuhn.jameica.hbci.qrtransfer.action;

import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.hbci.qrtransfer.gui.QRCodeView;
import de.willuhn.jameica.hbci.qrtransfer.model.SepaData;
import de.willuhn.jameica.hbci.qrtransfer.parser.EmvParser;
import de.willuhn.jameica.hbci.qrtransfer.parser.EpcParser;
import de.willuhn.jameica.hbci.qrtransfer.parser.ParserException;
import de.willuhn.jameica.hbci.qrtransfer.parser.QrCodeParser;
import de.willuhn.util.ApplicationException;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Aktion zum Lesen eines QR-Codes aus einer PDF-Datei.
 */
public class QRPdfAction implements Action {

    private final QrCodeParser[] parsers = {
        new EpcParser(),
        new EmvParser()
    };

    @Override
    public void handleAction(Object context) throws ApplicationException {
        try {
            // 1. Datei-Dialog oeffnen
            org.eclipse.swt.widgets.FileDialog dialog =
                new org.eclipse.swt.widgets.FileDialog(GUI.getShell(), org.eclipse.swt.SWT.OPEN);
            dialog.setText("PDF-Datei waehlen");
            dialog.setFilterExtensions(new String[]{"*.pdf"});
            dialog.setFilterNames(new String[]{"PDF-Dateien (*.pdf)"});

            String path = dialog.open();
            if (path == null || path.isEmpty()) {
                return;
            }

            File file = new File(path);
            if (!file.exists()) {
                throw new ApplicationException("Datei nicht gefunden: " + path);
            }

            // 2. PDF laden und QR-Code suchen
            String qrText = extractQRCodeFromPDF(file);
            if (qrText == null || qrText.isEmpty()) {
                throw new ApplicationException(
                    "Kein QR-Code in der PDF-Datei gefunden. " +
                    "Bitte stellen Sie sicher, dass die Datei einen gueltigen SEPA-QR-Code enthaelt."
                );
            }

            // 3. SEPA-Daten parsen
            SepaData sepaData = parseQrText(qrText);

            // 4. GUI-Ansicht oeffnen
            GUI.startView(QRCodeView.class, sepaData);

        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new ApplicationException(
                "Fehler beim Lesen des QR-Codes aus PDF: " + e.getMessage(), e
            );
        }
    }

    /**
     * Laedt eine PDF-Datei und sucht nach QR-Codes auf allen Seiten.
     */
    private String extractQRCodeFromPDF(File file) throws Exception {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFRenderer renderer = new PDFRenderer(document);

            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, 200);
                String qrText = decodeQRCode(image);
                if (qrText != null && !qrText.isEmpty()) {
                    return qrText;
                }
            }
        }
        return null;
    }

    /**
     * Dekodiert einen QR-Code aus einem BufferedImage.
     */
    private String decodeQRCode(BufferedImage image) throws Exception {
        LuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        Map<DecodeHintType, Object> hints = new HashMap<>();
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS,
            EnumSet.of(BarcodeFormat.QR_CODE));

        try {
            Result result = new MultiFormatReader().decode(bitmap, hints);
            return result.getText();
        } catch (NotFoundException e) {
            return null;
        }
    }

    /**
     * Parst den QR-Code-Text mit dem passenden Parser.
     */
    private SepaData parseQrText(String qrText) throws ParserException {
        for (QrCodeParser parser : parsers) {
            if (parser.canParse(qrText)) {
                SepaData data = parser.parse(qrText);
                data.setRawText(qrText);
                data.setFormat(parser instanceof EpcParser ? "EPC (BCD)" : "EMV (TLV)");
                return data;
            }
        }
        throw new ParserException(
            "Kein Parser fuer QR-Code-Format gefunden. " +
            "Unterstuetzt werden EPC (BCD) und EMV (TLV)."
        );
    }
}
