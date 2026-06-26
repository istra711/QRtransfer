package de.willuhn.jameica.hbci.qrtransfer.gui;

import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.util.Headline;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.jameica.hbci.qrtransfer.action.QRCodeAction;
import de.willuhn.jameica.hbci.qrtransfer.action.QRFileAction;
import de.willuhn.util.ApplicationException;

/**
 * Willkommens-Ansicht mit Buttons zum Starten der QR-Code-Aktionen.
 */
public class WelcomeView extends AbstractView {

    @Override
    public void bind() throws Exception {
        GUI.getView().setTitle("QR-Code Überweisung");

        SimpleContainer container = new SimpleContainer(getParent());

        container.addHeadline("SEPA-QR-Code Überweisung anlegen");

        new Button("QR-Code aus Zwischenablage lesen", new Action() {
            @Override
            public void handleAction(Object context) throws ApplicationException {
                try {
                    new QRCodeAction().handleAction(null);
                } catch (Exception e) {
                    throw new ApplicationException("Fehler: " + e.getMessage(), e);
                }
            }
        }, null, true).paint(container.getComposite());

        new Button("QR-Code aus Datei laden", new Action() {
            @Override
            public void handleAction(Object context) throws ApplicationException {
                try {
                    new QRFileAction().handleAction(null);
                } catch (Exception e) {
                    throw new ApplicationException("Fehler: " + e.getMessage(), e);
                }
            }
        }, null).paint(container.getComposite());
    }
}
