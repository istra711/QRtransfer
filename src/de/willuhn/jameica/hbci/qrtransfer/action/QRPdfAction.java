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

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class QRPdfAction implements Action {

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
            org.eclipse.swt.widgets.FileDialog dialog =
                new org.eclipse.swt.widgets.FileDialog(GUI.getShell(), org.eclipse.swt.SWT.OPEN);
            dialog.setText(i.tr("select.pdf.file"));
            dialog.setFilterExtensions(new String[]{"*.pdf"});
            dialog.setFilterNames(new String[]{i.tr("filter.pdf.files")});

            String path = dialog.open();
            if (path == null || path.isEmpty()) return;

            File file = new File(path);
            if (!file.exists()) {
                throw new ApplicationException(i.tr("error.file.not.found", path));
            }

            String qrText = extractQRCodeFromPDF(file);
            if (qrText == null || qrText.isEmpty()) {
                throw new ApplicationException(
                    i.tr("error.no.qrcode.pdf")
                );
            }

            SepaData sepaData = parseQrText(qrText);
            GUI.startView(QRCodeView.class, sepaData);

        } catch (ApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new ApplicationException(
                i.tr("error.reading.qrcode.pdf", e.getMessage()), e
            );
        }
    }

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
        throw new ParserException(
            getI18n().tr("error.no.parser")
        );
    }
}
