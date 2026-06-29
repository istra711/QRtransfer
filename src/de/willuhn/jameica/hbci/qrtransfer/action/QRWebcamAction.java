package de.willuhn.jameica.hbci.qrtransfer.action;

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
import de.willuhn.jameica.system.Application;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.I18N;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class QRWebcamAction implements Action {

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
        final AtomicBoolean found = new AtomicBoolean(false);
        final String[] qrText = {null};

        int deviceIndex = selectDevice(i);
        if (deviceIndex < 0) {
            return;
        }

        Object grabber;
        Object converter;
        try {
            Class<?> grabberClass = Class.forName("org.bytedeco.javacv.OpenCVFrameGrabber");
            Class<?> converterClass = Class.forName("org.bytedeco.javacv.Java2DFrameConverter");
            grabber = grabberClass.getConstructor(int.class).newInstance(deviceIndex);
            grabberClass.getMethod("start").invoke(grabber);
            converter = converterClass.getConstructor().newInstance();
        } catch (Throwable t) {
            Throwable cause = t.getCause() != null ? t.getCause() : t;
            String msg = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getName();
            JOptionPane.showMessageDialog(null,
                i.tr("webcam.cannot.start", msg),
                i.tr("qrcode.scan.title"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFrame frame = new JFrame(i.tr("qrcode.scan.title"));
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JLabel imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        frame.add(imageLabel, BorderLayout.CENTER);

        JLabel status = new JLabel(i.tr("qrcode.scan.hold"));
        status.setHorizontalAlignment(SwingConstants.CENTER);
        status.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        frame.add(status, BorderLayout.SOUTH);

        imageLabel.setPreferredSize(new java.awt.Dimension(200, 150));
        frame.pack();
        frame.setLocationRelativeTo(null);

        frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");
        frame.getRootPane().getActionMap().put("cancel", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                frame.dispose();
            }
        });

        frame.setVisible(true);

        final Object grabberRef = grabber;
        final Object converterRef = converter;

        Thread scanThread = new Thread(() -> {
            Method grabMethod = null;
            Method convertMethod = null;
            Method stopMethod = null;
            Method releaseMethod = null;
            try {
                grabMethod = grabberRef.getClass().getMethod("grab");
                Class<?> frameClass = Class.forName("org.bytedeco.javacv.Frame");
                convertMethod = converterRef.getClass().getMethod("convert", frameClass);
                stopMethod = grabberRef.getClass().getMethod("stop");
                releaseMethod = grabberRef.getClass().getMethod("release");
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(frame,
                        i.tr("webcam.init.failed", e.getMessage()),
                        i.tr("qrcode.scan.title"), JOptionPane.ERROR_MESSAGE);
                    frame.dispose();
                });
                return;
            }

            Method grabRef = grabMethod;
            Method convertRef = convertMethod;
            Method stopRef = stopMethod;
            Method releaseRef = releaseMethod;

            Map<DecodeHintType, Object> hints = new HashMap<>();
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(BarcodeFormat.QR_CODE));

            try {
                while (frame.isDisplayable() && !found.get()) {
                    Object grabbedFrame;
                    try {
                        grabbedFrame = grabRef.invoke(grabberRef);
                    } catch (Exception e) {
                        try { Thread.sleep(50); } catch (InterruptedException ie) { break; }
                        continue;
                    }

                    if (grabbedFrame == null) {
                        try { Thread.sleep(50); } catch (InterruptedException ie) { break; }
                        continue;
                    }

                    BufferedImage image;
                    try {
                        image = (BufferedImage) convertRef.invoke(converterRef, grabbedFrame);
                    } catch (Exception e) {
                        try { Thread.sleep(50); } catch (InterruptedException ie) { break; }
                        continue;
                    }

                    if (image == null) {
                        try { Thread.sleep(50); } catch (InterruptedException ie) { break; }
                        continue;
                    }

                    BufferedImage displayImage = image;
                    SwingUtilities.invokeLater(() -> {
                        imageLabel.setIcon(new ImageIcon(
                            displayImage.getScaledInstance(200, 150, Image.SCALE_FAST)));
                    });

                    try {
                        LuminanceSource source = new BufferedImageLuminanceSource(image);
                        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                        Result result = new MultiFormatReader().decode(bitmap, hints);

                        if (result != null && result.getText() != null) {
                            qrText[0] = result.getText();
                            found.set(true);

                            SwingUtilities.invokeLater(() -> {
                                status.setText(i.tr("qrcode.detected"));
                                frame.dispose();
                            });
                        }
                    } catch (NotFoundException e) {
                        // Kein QR-Code gefunden
                    } catch (Exception e) {
                        // Fehler ignorieren
                    }

                    try { Thread.sleep(100); } catch (InterruptedException e) { break; }
                }
            } finally {
                try { stopRef.invoke(grabberRef); } catch (Exception e) { /* ignore */ }
                try { releaseRef.invoke(grabberRef); } catch (Exception e) { /* ignore */ }
            }

            if (found.get() && qrText[0] != null) {
                try {
                    SepaData sepaData = parseQrText(qrText[0]);
                    GUI.getDisplay().asyncExec(() -> {
                        try {
                            GUI.startView(QRCodeView.class, sepaData);
                        } catch (Exception ex) {
                            GUI.getView().setErrorText(i.tr("error.param", ex.getMessage()));
                        }
                    });
                } catch (ParserException e) {
                    GUI.getDisplay().asyncExec(() -> {
                        GUI.getView().setErrorText(i.tr("error.param", e.getMessage()));
                    });
                }
            }
        }, "QR-Scanner");

        scanThread.setDaemon(true);
        scanThread.start();
    }

    private int selectDevice(I18N i18n) {
        try {
            Class<?> grabberClass = Class.forName("org.bytedeco.javacv.FrameGrabber");
            Method listMethod = grabberClass.getMethod("getDeviceDescriptions");
            String[] devices = (String[]) listMethod.invoke(null);

            if (devices == null || devices.length == 0) {
                JOptionPane.showMessageDialog(null,
                    i18n.tr("webcam.no.devices"),
                    i18n.tr("qrcode.scan.title"), JOptionPane.WARNING_MESSAGE);
                return -1;
            }

            if (devices.length == 1) {
                return 0;
            }

            String selected = (String) JOptionPane.showInputDialog(null,
                i18n.tr("webcam.select.device"),
                i18n.tr("qrcode.scan.title"),
                JOptionPane.QUESTION_MESSAGE,
                null, devices, devices[0]);

            if (selected == null) {
                return -1;
            }

            for (int idx = 0; idx < devices.length; idx++) {
                if (devices[idx].equals(selected)) {
                    return idx;
                }
            }
            return 0;
        } catch (Exception e) {
            return 0;
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
