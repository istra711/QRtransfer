package de.willuhn.jameica.hbci.qrtransfer.gui;

import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.jameica.hbci.qrtransfer.action.QRCodeAction;
import de.willuhn.jameica.hbci.qrtransfer.action.QRFileAction;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.I18N;

public class WelcomeView extends AbstractView {

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

    @Override
    public void bind() throws Exception {
        final I18N i = getI18n();
        GUI.getView().setTitle(i.tr("qrcode.transfer"));

        SimpleContainer container = new SimpleContainer(getParent());
        container.addHeadline(i.tr("create.sepa.qrcode.transfer"));

        new Button(i.tr("read.qrcode.clipboard"), new Action() {
            @Override
            public void handleAction(Object context) throws ApplicationException {
                try {
                    new QRCodeAction().handleAction(null);
                } catch (Exception e) {
                    throw new ApplicationException(i.tr("error.param", e.getMessage()), e);
                }
            }
        }, null, true).paint(container.getComposite());

        new Button(i.tr("load.qrcode.file"), new Action() {
            @Override
            public void handleAction(Object context) throws ApplicationException {
                try {
                    new QRFileAction().handleAction(null);
                } catch (Exception e) {
                    throw new ApplicationException(i.tr("error.param", e.getMessage()), e);
                }
            }
        }, null).paint(container.getComposite());
    }
}
