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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Aktion zum Lesen eines QR-Codes aus der Zwischenablage.
 * Der QR-Code wird dekodiert, die SEPA-Daten geparst
 * und eine Überweisung als Entwurf angelegt.
 */
public class QRCodeAction implements Action {

    private final QrCodeParser[] parsers = {
        new EpcParser(),
        new EmvParser()
    };

    @Override
    public void handleAction(Object context) throws ApplicationException {
        try {
            // 1. Bild aus Zwischenablage lesen
            BufferedImage image = getImageFromClipboard();
            if (image == null) {
                throw new ApplicationException(
                    "Kein Bild in der Zwischenablage gefunden. " +
                    "Bitte einen QR-Code kopieren (z.B. aus einem PDF oder Screenshot)."
                );
            }

            // 2. QR-Code dekodieren
            String qrText = decodeQRCode(image);
            if (qrText == null || qrText.isEmpty()) {
                throw new ApplicationException(
                    "Kein QR-Code im Bild erkannt. " +
                    "Bitte stellen Sie sicher, dass das Bild einen gültigen QR-Code enthält."
                );
            }

            // 3. SEPA-Daten parsen
            SepaData sepaData = parseQrText(qrText);

            // 4. GUI-Ansicht öffnen
            GUI.startView(QRCodeView.class, sepaData);

        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new ApplicationException(
                "Fehler beim Lesen des QR-Codes: " + e.getMessage(), e
            );
        }
    }

    /**
     * Liest ein Bild aus der Systemzwischenablage.
     */
    private BufferedImage getImageFromClipboard() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable content = clipboard.getContents(null);

            if (content == null) {
                return null;
            }

            // Versuche Java-Image
            if (content.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                return (BufferedImage) content.getTransferData(DataFlavor.imageFlavor);
            }

            // Fallback: Versuche Dateiliste (z.B. bei Screenshots)
            if (content.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                @SuppressWarnings("unchecked")
                java.util.List<java.io.File> files =
                    (java.util.List<java.io.File>) content.getTransferData(DataFlavor.javaFileListFlavor);
                if (!files.isEmpty()) {
                    return javax.imageio.ImageIO.read(files.get(0));
                }
            }

            return null;
        } catch (Exception e) {
            return null;
        }
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
            "Kein Parser für QR-Code-Format gefunden. " +
            "Unterstützt werden EPC (BCD) und EMV (TLV)."
        );
    }
}
