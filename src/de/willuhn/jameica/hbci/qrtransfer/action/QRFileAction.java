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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class QRFileAction implements Action {

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
            Shell shell = GUI.getShell();
            FileDialog dialog = new FileDialog(shell, SWT.OPEN);
            dialog.setText(i.tr("select.qrcode.image"));
            dialog.setFilterExtensions(new String[]{"*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"});
            dialog.setFilterNames(new String[]{i.tr("filter.image.files"), i.tr("filter.all.files")});

            String path = dialog.open();
            if (path == null) return;

            File file = new File(path);
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                throw new ApplicationException(i.tr("error.file.not.image", file.getName()));
            }

            String qrText = QrCodeSelector.decodeAndSelect(image, null);
            if (qrText == null || qrText.isEmpty()) {
                throw new ApplicationException(i.tr("error.no.qrcode.file", file.getName()));
            }

            SepaData sepaData = parseQrText(qrText);
            GUI.startView(QRCodeView.class, sepaData);

        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new ApplicationException(i.tr("error.param", e.getMessage()), e);
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
        throw new ParserException(getI18n().tr("error.no.parser.format"));
    }
}
