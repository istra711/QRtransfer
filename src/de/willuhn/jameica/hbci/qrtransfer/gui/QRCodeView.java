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
import de.willuhn.util.ApplicationException;

import java.util.Date;

public class QRCodeView extends AbstractView {

    private SepaData sepaData;

    @Override
    public void bind() throws Exception {
        this.sepaData = (SepaData) getCurrentObject();

        if (sepaData == null) {
            new Headline(getParent(), "Fehler");
            return;
        }

        GUI.getView().setTitle("SEPA-QR-Code Überweisung");

        SimpleContainer container = new SimpleContainer(getParent());
        container.addHeadline("QR-Code-Daten erfolgreich gelesen");

        // Format-Anzeige
        LabelGroup formatGroup = new LabelGroup(container.getComposite(), "Erkanntes Format");
        String format = sepaData.getFormat() != null ? sepaData.getFormat() : "Unbekannt";
        formatGroup.addLabelPair("Format", new de.willuhn.jameica.gui.input.TextInput(format));

        // Empfänger-Konto
        LabelGroup ibanGroup = new LabelGroup(container.getComposite(), "Empfänger-Konto");
        ibanGroup.addLabelPair("IBAN", new de.willuhn.jameica.gui.input.TextInput(
            sepaData.getIban() != null ? sepaData.getIban() : ""));
        ibanGroup.addLabelPair("BIC", new de.willuhn.jameica.gui.input.TextInput(
            sepaData.getBic() != null ? sepaData.getBic() : ""));

        // Empfänger
        LabelGroup empfaengerGroup = new LabelGroup(container.getComposite(), "Empfänger");
        empfaengerGroup.addLabelPair("Name", new de.willuhn.jameica.gui.input.TextInput(
            sepaData.getEmpfaengerName() != null ? sepaData.getEmpfaengerName() : ""));
        empfaengerGroup.addLabelPair("Ort", new de.willuhn.jameica.gui.input.TextInput(
            sepaData.getEmpfaengerOrt() != null ? sepaData.getEmpfaengerOrt() : ""));

        // Betrag
        LabelGroup betragGroup = new LabelGroup(container.getComposite(), "Betrag");
        String betragStr = "";
        if (sepaData.getBetrag() > 0) {
            betragStr = String.format(java.util.Locale.GERMAN, "%.2f", sepaData.getBetrag());
        }
        betragGroup.addLabelPair("Betrag", new de.willuhn.jameica.gui.input.TextInput(betragStr));
        betragGroup.addLabelPair("Währung", new de.willuhn.jameica.gui.input.TextInput(
            sepaData.getWaehrung() != null ? sepaData.getWaehrung() : "EUR"));

        // Verwendungszweck - beide Felder anzeigen
        LabelGroup zweckGroup = new LabelGroup(container.getComposite(), "Verwendungszweck");
        String verwendungszweck = sepaData.getVerwendungszweck();
        String betreff = sepaData.getBetreff();

        if (verwendungszweck != null && !verwendungszweck.isEmpty()) {
            zweckGroup.addLabelPair("Verwendungszweck", new de.willuhn.jameica.gui.input.TextInput(verwendungszweck));
        }
        if (betreff != null && !betreff.isEmpty()) {
            zweckGroup.addLabelPair("Betreff", new de.willuhn.jameica.gui.input.TextInput(betreff));
        }
        if ((verwendungszweck == null || verwendungszweck.isEmpty()) &&
            (betreff == null || betreff.isEmpty())) {
            zweckGroup.addLabelPair("Verwendungszweck", new de.willuhn.jameica.gui.input.TextInput("(keiner)"));
        }

        // QR-Code Rohdaten
        String rawText = sepaData.getRawText();
        if (rawText != null && !rawText.isEmpty()) {
            LabelGroup rawGroup = new LabelGroup(container.getComposite(), "QR-Code Rohdaten");
            rawGroup.addLabelPair("Inhalt", new de.willuhn.jameica.gui.input.TextInput(rawText));
        }

        // Überweisung anlegen Button
        new Button("Überweisung anlegen", new Action() {
            @Override
            public void handleAction(Object context) throws ApplicationException {
                try {
                    createTransfer(sepaData);
                } catch (Exception e) {
                    throw new ApplicationException("Fehler: " + e.getMessage(), e);
                }
            }
        }, null, true).paint(container.getComposite());
    }

    private void createTransfer(SepaData data) throws Exception {
        if (data == null || !data.isValid()) {
            throw new ApplicationException("Die SEPA-Daten sind unvollständig");
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
