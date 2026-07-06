package de.willuhn.jameica.hbci.qrtransfer.action;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;

import de.willuhn.jameica.hbci.qrtransfer.parser.EmvParser;
import de.willuhn.jameica.hbci.qrtransfer.parser.EpcParser;
import de.willuhn.jameica.hbci.qrtransfer.parser.ParserException;
import de.willuhn.jameica.hbci.qrtransfer.parser.QrCodeParser;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.I18N;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

public class QrCodeSelector {

    private static final Logger logger = Logger.getLogger(QrCodeSelector.class.getName());
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

    private static final QrCodeParser[] PARSERS = {
        new EpcParser(),
        new EmvParser()
    };

    public static List<String> decodeMultiple(BufferedImage image) throws Exception {
        List<String> results = new ArrayList<>();
        Map<DecodeHintType, Object> hints = new HashMap<>();
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(BarcodeFormat.QR_CODE));

        // GenericMultipleBarcodeReader für mehrere QR-Codes im ganzen Bild
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            MultiFormatReader delegate = new MultiFormatReader();
            GenericMultipleBarcodeReader multiReader = new GenericMultipleBarcodeReader(delegate);
            Result[] multiResults = multiReader.decodeMultiple(bitmap, hints);
            if (multiResults != null) {
                for (Result r : multiResults) {
                    if (r != null && r.getText() != null && !r.getText().isEmpty()) {
                        if (!results.contains(r.getText())) {
                            results.add(r.getText());
                        }
                    }
                }
            }
            logger.info("GenericMultipleBarcodeReader found " + results.size() + " QR code(s) in full image");
        } catch (NotFoundException e) {
            logger.info("GenericMultipleBarcodeReader: no QR codes in full image");
        }

        // Fallback: Bild in 4 Quadranten aufteilen
        if (results.isEmpty()) {
            int w = image.getWidth();
            int h = image.getHeight();
            if (w > 100 && h > 100) {
                int halfW = w / 2;
                int halfH = h / 2;

                int[][] crops = {
                    {0, 0, halfW, halfH},
                    {halfW, 0, w, halfH},
                    {0, halfH, halfW, h},
                    {halfW, halfH, w, h}
                };

                for (int[] crop : crops) {
                    try {
                        BufferedImage sub = image.getSubimage(crop[0], crop[1], crop[2] - crop[0], crop[3] - crop[1]);
                        LuminanceSource subSource = new BufferedImageLuminanceSource(sub);
                        BinaryBitmap subBitmap = new BinaryBitmap(new HybridBinarizer(subSource));
                        MultiFormatReader subReader = new MultiFormatReader();
                        Result subResult = subReader.decode(subBitmap, hints);
                        if (subResult != null && subResult.getText() != null && !subResult.getText().isEmpty()) {
                            if (!results.contains(subResult.getText())) {
                                results.add(subResult.getText());
                            }
                        }
                    } catch (NotFoundException e) {
                        // Kein QR-Code in diesem Quadranten
                    }
                }
            }
        }

        // Fallback: Bild in 9 Raster aufteilen (3x3)
        if (results.isEmpty()) {
            int w = image.getWidth();
            int h = image.getHeight();
            if (w > 200 && h > 200) {
                int thirdW = w / 3;
                int thirdH = h / 3;

                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 3; col++) {
                        int x = col * thirdW;
                        int y = row * thirdH;
                        int cw = (col == 2) ? w - x : thirdW;
                        int ch = (row == 2) ? h - y : thirdH;
                        if (cw < 50 || ch < 50) continue;
                        try {
                            BufferedImage sub = image.getSubimage(x, y, cw, ch);
                            LuminanceSource subSource = new BufferedImageLuminanceSource(sub);
                            BinaryBitmap subBitmap = new BinaryBitmap(new HybridBinarizer(subSource));
                            MultiFormatReader subReader = new MultiFormatReader();
                            Result subResult = subReader.decode(subBitmap, hints);
                            if (subResult != null && subResult.getText() != null && !subResult.getText().isEmpty()) {
                                if (!results.contains(subResult.getText())) {
                                    results.add(subResult.getText());
                                }
                            }
                        } catch (NotFoundException e) {
                            // Kein QR-Code in diesem Raster
                        }
                    }
                }
            }
        }

        logger.info("decodeMultiple: found " + results.size() + " unique QR code(s)");
        return results;
    }

    public static List<String> filterValidSepa(List<String> qrTexts) {
        List<String> valid = new ArrayList<>();
        for (String text : qrTexts) {
            if (text == null || text.isEmpty()) continue;
            boolean parsed = false;
            for (QrCodeParser parser : PARSERS) {
                if (parser.canParse(text)) {
                    try {
                        parser.parse(text);
                        valid.add(text);
                        parsed = true;
                        break;
                    } catch (ParserException e) {
                        String preview = text.length() > 60 ? text.substring(0, 60) + "..." : text;
                        logger.info("QR code detected but invalid: " + preview + " -> " + e.getMessage());
                    }
                }
            }
            if (!parsed) {
                String preview = text.length() > 60 ? text.substring(0, 60) + "..." : text;
                logger.info("Filtered out non-SEPA QR code: " + preview);
            }
        }
        return valid;
    }

    public static String selectFromMultiple(List<String> qrTexts, Component parent) {
        if (qrTexts == null || qrTexts.isEmpty()) {
            return null;
        }

        if (qrTexts.size() == 1) {
            return qrTexts.get(0);
        }

        I18N i = getI18n();

        String[] displayItems = new String[qrTexts.size()];
        for (int idx = 0; idx < qrTexts.size(); idx++) {
            String text = qrTexts.get(idx);
            String preview = text.length() > 80 ? text.substring(0, 80) + "..." : text;
            displayItems[idx] = (idx + 1) + ": " + preview;
        }

        String selected = (String) JOptionPane.showInputDialog(
            parent,
            i.tr("qrcode.multiple.found", String.valueOf(qrTexts.size())),
            i.tr("qrcode.select.title"),
            JOptionPane.QUESTION_MESSAGE,
            null,
            displayItems,
            displayItems[0]
        );

        if (selected == null) {
            return null;
        }

        int selectedIndex = Integer.parseInt(selected.split(":")[0].trim()) - 1;
        if (selectedIndex >= 0 && selectedIndex < qrTexts.size()) {
            return qrTexts.get(selectedIndex);
        }

        return null;
    }

    public static String decodeAndSelect(BufferedImage image, Component parent) throws Exception {
        List<String> allQr = decodeMultiple(image);
        if (allQr.isEmpty()) {
            return null;
        }

        List<String> validSepa = filterValidSepa(allQr);

        if (validSepa.isEmpty()) {
            return null;
        }

        if (validSepa.size() == 1) {
            return validSepa.get(0);
        }

        return selectFromMultiple(validSepa, parent);
    }
}
