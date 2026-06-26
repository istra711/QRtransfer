package de.willuhn.jameica.hbci.qrtransfer.gui;

import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.util.ApplicationException;

/**
 * Aktion für den Navigationsbaum - öffnet die WelcomeView.
 */
public class WelcomeAction implements Action {

    @Override
    public void handleAction(Object context) throws ApplicationException {
        GUI.startView(WelcomeView.class, null);
    }
}
