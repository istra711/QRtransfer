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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class QRFileAction implements Action {

    private final QrCodeParser[] parsers = {
        new EpcParser(),
        new EmvParser()
    };

    @Override
    public void handleAction(Object context) throws ApplicationException {
        try {
            Shell shell = Display.getCurrent().getActiveShell();
            FileDialog dialog = new FileDialog(shell, SWT.OPEN);
            dialog.setText("QR-Code-Bild auswählen");
            dialog.setFilterExtensions(new String[]{"*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"});
            dialog.setFilterNames(new String[]{"Bilddateien (*.png, *.jpg)", "Alle Dateien (*.*)"});

            String path = dialog.open();
            if (path == null) {
                return;
            }

            File file = new File(path);
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                throw new ApplicationException("Datei konnte nicht als Bild gelesen werden: " + file.getName());
            }

            String qrText = decodeQRCode(image);
            if (qrText == null || qrText.isEmpty()) {
                throw new ApplicationException("Kein QR-Code in der Datei erkannt: " + file.getName());
            }

            SepaData sepaData = parseQrText(qrText);
            GUI.startView(QRCodeView.class, sepaData);

        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new ApplicationException("Fehler: " + e.getMessage(), e);
        }
    }

    private String decodeQRCode(BufferedImage image) throws Exception {
        LuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Map<DecodeHintType, Object> hints = new HashMap<>();
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(BarcodeFormat.QR_CODE));
        try {
            Result result = new MultiFormatReader().decode(bitmap, hints);
            return result.getText();
        } catch (NotFoundException e) {
            return null;
        }
    }

    private SepaData parseQrText(String qrText) throws ParserException {
        for (QrCodeParser parser : parsers) {
            if (parser.canParse(qrText)) {
                SepaData data = parser.parse(qrText);
                data.setRawText(qrText);
                data.setFormat(parser instanceof EpcParser ? "EPC (BCD)" : "EMV (TLV)");
                return data;
            }
        }
        throw new ParserException("Kein Parser für QR-Code-Format gefunden.");
    }
}
