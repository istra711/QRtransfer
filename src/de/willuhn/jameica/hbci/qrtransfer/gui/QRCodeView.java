package de.willuhn.jameica.hbci.qrtransfer.gui;

import de.willuhn.jameica.gui.AbstractView;
import de.willuhn.jameica.gui.Action;
import de.willuhn.jameica.gui.GUI;
import de.willuhn.jameica.gui.parts.Button;
import de.willuhn.jameica.gui.util.Headline;
import de.willuhn.jameica.gui.util.SimpleContainer;
import de.willuhn.jameica.gui.util.LabelGroup;
import de.willuhn.jameica.hbci.Settings;
import de.willuhn.jameica.hbci.qrtransfer.model.SepaData;
import de.willuhn.jameica.hbci.rmi.AuslandsUeberweisung;
import de.willuhn.jameica.system.Application;
import de.willuhn.util.ApplicationException;
import de.willuhn.util.I18N;

import java.util.Date;

public class QRCodeView extends AbstractView {

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

    private SepaData sepaData;

    @Override
    public void bind() throws Exception {
        final I18N i = getI18n();
        this.sepaData = (SepaData) getCurrentObject();

        if (sepaData == null) {
            new Headline(getParent(), i.tr("error"));
            return;
        }

        GUI.getView().setTitle(i.tr("sepa.qrtransfer.title"));

        SimpleContainer container = new SimpleContainer(getParent());
        container.addHeadline(i.tr("qrcode.data.read.success"));

        LabelGroup formatGroup = new LabelGroup(container.getComposite(), i.tr("recognized.format"));
        String format = sepaData.getFormat() != null ? sepaData.getFormat() : i.tr("unknown");
        formatGroup.addLabelPair(i.tr("format"), new de.willuhn.jameica.gui.input.TextInput(format));

        LabelGroup ibanGroup = new LabelGroup(container.getComposite(), i.tr("recipient.account"));
        ibanGroup.addLabelPair("IBAN", new de.willuhn.jameica.gui.input.TextInput(
            sepaData.getIban() != null ? sepaData.getIban() : ""));
        ibanGroup.addLabelPair("BIC", new de.willuhn.jameica.gui.input.TextInput(
            sepaData.getBic() != null ? sepaData.getBic() : ""));

        LabelGroup empfaengerGroup = new LabelGroup(container.getComposite(), i.tr("recipient"));
        empfaengerGroup.addLabelPair(i.tr("name"), new de.willuhn.jameica.gui.input.TextInput(
            sepaData.getEmpfaengerName() != null ? sepaData.getEmpfaengerName() : ""));
        empfaengerGroup.addLabelPair(i.tr("city"), new de.willuhn.jameica.gui.input.TextInput(
            sepaData.getEmpfaengerOrt() != null ? sepaData.getEmpfaengerOrt() : ""));

        LabelGroup betragGroup = new LabelGroup(container.getComposite(), i.tr("amount"));
        String betragStr = "";
        if (sepaData.getBetrag() > 0) {
            betragStr = String.format(java.util.Locale.GERMAN, "%.2f", sepaData.getBetrag());
        }
        betragGroup.addLabelPair(i.tr("amount"), new de.willuhn.jameica.gui.input.TextInput(betragStr));
        betragGroup.addLabelPair(i.tr("currency"), new de.willuhn.jameica.gui.input.TextInput(
            sepaData.getWaehrung() != null ? sepaData.getWaehrung() : "EUR"));

        LabelGroup zweckGroup = new LabelGroup(container.getComposite(), i.tr("purpose"));
        String verwendungszweck = sepaData.getVerwendungszweck();
        String betreff = sepaData.getBetreff();

        if (verwendungszweck != null && !verwendungszweck.isEmpty()) {
            zweckGroup.addLabelPair(i.tr("purpose"), new de.willuhn.jameica.gui.input.TextInput(verwendungszweck));
        }
        if (betreff != null && !betreff.isEmpty()) {
            zweckGroup.addLabelPair(i.tr("subject"), new de.willuhn.jameica.gui.input.TextInput(betreff));
        }
        if ((verwendungszweck == null || verwendungszweck.isEmpty()) &&
            (betreff == null || betreff.isEmpty())) {
            zweckGroup.addLabelPair(i.tr("purpose"), new de.willuhn.jameica.gui.input.TextInput(i.tr("none")));
        }

        String rawText = sepaData.getRawText();
        if (rawText != null && !rawText.isEmpty()) {
            LabelGroup rawGroup = new LabelGroup(container.getComposite(), i.tr("qrcode.rawdata"));
            rawGroup.addLabelPair(i.tr("content"), new de.willuhn.jameica.gui.input.TextInput(rawText));
        }

        new Button(i.tr("create.transfer"), new Action() {
            @Override
            public void handleAction(Object context) throws ApplicationException {
                try {
                    createTransfer(sepaData);
                } catch (Exception e) {
                    throw new ApplicationException(i.tr("error.param", e.getMessage()), e);
                }
            }
        }, null, true).paint(container.getComposite());
    }

    private void createTransfer(SepaData data) throws Exception {
        if (data == null || !data.isValid()) {
            throw new ApplicationException(getI18n().tr("sepa.data.incomplete"));
        }

        AuslandsUeberweisung u = (AuslandsUeberweisung) Settings.getDBService()
            .createObject(AuslandsUeberweisung.class, null);

        u.setGegenkontoNummer(data.getIban());
        if (data.getBic() != null && !data.getBic().isEmpty()) {
            u.setGegenkontoBLZ(data.getBic());
        }
        u.setGegenkontoName(data.getEmpfaengerName());

        if (data.getBetrag() > 0) {
            u.setBetrag(data.getBetrag());
        }

        String zweck = data.getZweckFeld();
        if (zweck != null && !zweck.isEmpty()) {
            u.setZweck(zweck);
        }

        u.setTermin(new Date());

        if (u.isNewObject()) {
            u.setInstantPayment(true);
        }

        GUI.startView(de.willuhn.jameica.hbci.gui.views.AuslandsUeberweisungNew.class, u);
    }
}
