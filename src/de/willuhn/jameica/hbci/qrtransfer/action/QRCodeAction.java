package de.willuhn.jameica.hbci.qrtransfer.action;

import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.hbci.qrtransfer.gui.QRCodeView;
import de.willuhn.jameica.hbci.qrtransfer.model.SepaData;
import de.willuhn.jameica.hbci.qrtransfer.parser.EmvParser;
import de.willuhn.jameica.hbci.qrtransfer.parser.EpcParser;
import de.willuhn.jameica.hbci.qrtransfer.parser.ParserException;
import de.willuhn.jameica.hbci.qrtransfer.parser.QrCodeParser;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.I18N;
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

public class QRCodeAction implements Action {

    private static I18N i18n;

    private static synchronized I18N getI18n() {
        if (i18n == null) {
            i18n = Application.getPluginLoader()
                .getPlugin("de.willuhn.jameica.hbci.qrtransfer.QRTransferPlugin")
                .getResources()
                .getI18N();
        }
        return i18n;
    }

    private final QrCodeParser[] parsers = {
        new EpcParser(),
        new EmvParser()
    };

    @Override
    public void handleAction(Object context) throws ApplicationException {
        final I18N i = getI18n();
        try {
            BufferedImage image = getImageFromClipboard();
            if (image == null) {
                throw new ApplicationException(
                    i.tr("error.no.image.clipboard")
                );
            }

            String qrText = QrCodeSelector.decodeAndSelect(image, null);
            if (qrText == null || qrText.isEmpty()) {
                throw new ApplicationException(
                    i.tr("error.no.qrcode.image")
                );
            }

            SepaData sepaData = parseQrText(qrText);
            GUI.startView(QRCodeView.class, sepaData);

        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new ApplicationException(
                i.tr("error.reading.qrcode", e.getMessage()), e
            );
        }
    }

    private BufferedImage getImageFromClipboard() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable content = clipboard.getContents(null);
            if (content == null) return null;

            if (content.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                return (BufferedImage) content.getTransferData(DataFlavor.imageFlavor);
            }

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
            getI18n().tr("error.no.parser")
        );
    }
}
