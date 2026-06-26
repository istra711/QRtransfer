package de.willuhn.jameica.hbci.qrtransfer.model;

/**
 * Datenmodell für SEPA-Überweisungsdaten aus einem QR-Code.
 */
public class SepaData {

    private String iban;
    private String bic;
    private String empfaengerName;
    private String empfaengerOrt;
    private double betrag;
    private String waehrung;
    private String betreff;
    private String verwendungszweck;
    private String rawText;
    private String format;

    public SepaData() {
        this.waehrung = "EUR";
    }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban != null ? iban.trim().toUpperCase() : null; }

    public String getBic() { return bic; }
    public void setBic(String bic) { this.bic = bic != null ? bic.trim().toUpperCase() : null; }

    public String getEmpfaengerName() { return empfaengerName; }
    public void setEmpfaengerName(String empfaengerName) { this.empfaengerName = empfaengerName; }

    public String getEmpfaengerOrt() { return empfaengerOrt; }
    public void setEmpfaengerOrt(String empfaengerOrt) { this.empfaengerOrt = empfaengerOrt; }

    public double getBetrag() { return betrag; }
    public void setBetrag(double betrag) { this.betrag = betrag; }
    public void setBetrag(String betragStr) {
        if (betragStr != null && !betragStr.isEmpty()) {
            try {
                this.betrag = Double.parseDouble(betragStr.replace(",", "."));
            } catch (NumberFormatException e) {
                this.betrag = 0.0;
            }
        }
    }

    public String getWaehrung() { return waehrung; }
    public void setWaehrung(String waehrung) { this.waehrung = waehrung; }

    public String getBetreff() { return betreff; }
    public void setBetreff(String betreff) { this.betreff = betreff; }

    public String getVerwendungszweck() { return verwendungszweck; }
    public void setVerwendungszweck(String verwendungszweck) { this.verwendungszweck = verwendungszweck; }

    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    /**
     * Gibt den kombinierten Verwendungszweck zurück.
     * Verwendet Verwendungszweck oder Betreff (was verfügbar ist).
     */
    public String getZweckFeld() {
        if (verwendungszweck != null && !verwendungszweck.isEmpty()) {
            return verwendungszweck;
        }
        if (betreff != null && !betreff.isEmpty()) {
            return betreff;
        }
        return null;
    }

    /**
     * Prüft, ob die Mindestdaten für eine Überweisung vorhanden sind.
     */
    public boolean isValid() {
        return iban != null && !iban.isEmpty()
            && empfaengerName != null && !empfaengerName.isEmpty();
    }

    @Override
    public String toString() {
        return "SepaData{" +
            "iban='" + iban + '\'' +
            ", bic='" + bic + '\'' +
            ", empfaengerName='" + empfaengerName + '\'' +
            ", betrag=" + betrag +
            ", waehrung='" + waehrung + '\'' +
            ", verwendungszweck='" + getZweckFeld() + '\'' +
            '}';
    }
}
