package de.willuhn.jameica.hbci.qrtransfer.action;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.hbci.qrtransfer.gui.QRCodeView;
import de.willuhn.jameica.hbci.qrtransfer.model.SepaData;
import de.willuhn.jameica.hbci.qrtransfer.parser.EmvParser;
import de.willuhn.jameica.hbci.qrtransfer.parser.EpcParser;
import de.willuhn.jameica.hbci.qrtransfer.parser.ParserException;
import de.willuhn.jameica.hbci.qrtransfer.parser.QrCodeParser;
import de.willuhn.util.ApplicationException;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Aktion zum Lesen eines QR-Codes ueber die Webcam.
 */
public class QRWebcamAction implements Action {

    private final QrCodeParser[] parsers = {
        new EpcParser(),
        new EmvParser()
    };

    @Override
    public void handleAction(Object context) throws ApplicationException {
        Webcam webcam = Webcam.getDefault();
        if (webcam == null) {
            throw new ApplicationException(
                "Keine Webcam gefunden. Bitte schliessen Sie eine Webcam an."
            );
        }

        webcam.setViewSize(WebcamResolution.VGA.getSize());

        AtomicBoolean found = new AtomicBoolean(false);
        final String[] qrText = {null};

        WebcamPanel panel = new WebcamPanel(webcam);
        panel.setFPSLimit(2);
        panel.setMirrored(true);

        JFrame frame = new JFrame("QR-Code scannen - ESC zum Abbrechen");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.CENTER);

        JLabel status = new JLabel("Halten Sie den QR-Code vor die Webcam...");
        status.setHorizontalAlignment(SwingConstants.CENTER);
        status.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        frame.add(status, BorderLayout.SOUTH);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        Thread scanThread = new Thread(() -> {
            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            hints.put(DecodeHintType.POSSIBLE_FORMATS,
                EnumSet.of(BarcodeFormat.QR_CODE));

            while (frame.isDisplayable() && !found.get()) {
                if (!webcam.isOpen()) {
                    break;
                }

                BufferedImage image = webcam.getImage();
                if (image == null) {
                    continue;
                }

                try {
                    LuminanceSource source = new BufferedImageLuminanceSource(image);
                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                    Result result = new MultiFormatReader().decode(bitmap, hints);

                    if (result != null && result.getText() != null) {
                        qrText[0] = result.getText();
                        found.set(true);

                        SwingUtilities.invokeLater(() -> {
                            status.setText("QR-Code erkannt! Daten werden verarbeitet...");
                            panel.stop();
                            frame.dispose();
                        });
                    }
                } catch (NotFoundException e) {
                    // Kein QR-Code gefunden, weitermachen
                } catch (Exception e) {
                    // Fehler ignorieren, weitermachen
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (found.get() && qrText[0] != null) {
                try {
                    SepaData sepaData = parseQrText(qrText[0]);
                    GUI.getDisplay().asyncExec(() -> {
                        try {
                            GUI.startView(QRCodeView.class, sepaData);
                        } catch (Exception ex) {
                            GUI.getView().setErrorText("Fehler: " + ex.getMessage());
                        }
                    });
                } catch (ParserException e) {
                    GUI.getDisplay().asyncExec(() -> {
                        GUI.getView().setErrorText("Fehler: " + e.getMessage());
                    });
                }
            }
        }, "QR-Scanner");

        scanThread.setDaemon(true);
        scanThread.start();
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
            "Kein Parser fuer QR-Code-Format gefunden. " +
            "Unterstuetzt werden EPC (BCD) und EMV (TLV)."
        );
    }
}
