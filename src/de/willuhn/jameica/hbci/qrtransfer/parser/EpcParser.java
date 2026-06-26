package de.willuhn.jameica.hbci.qrtransfer.parser;

import de.willuhn.jameica.hbci.qrtransfer.model.SepaData;

/**
 * Parser für EPC QR-Codes im BCD-Format.
 *
 * Format:
 * BCD
 * 001 oder 002 (Version)
 * 1 oder 2 (1=ohne Betrag, 2=mit Betrag)
 * SCT
 * [BIC]
 * [Empfänger Name]
 * [IBAN]
 * [EUR][Betrag]           (optional)
 * //Betreff               (optional)
 * [Verwendungszweck]      (optional)
 */
public class EpcParser implements QrCodeParser {

    @Override
    public boolean canParse(String text) {
        if (text == null || text.isEmpty()) return false;
        return text.trim().startsWith("BCD");
    }

    @Override
    public SepaData parse(String text) throws ParserException {
        if (text == null || text.isEmpty()) {
            throw new ParserException("QR-Code-Text ist leer");
        }

        String[] lines = text.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            lines[i] = lines[i].trim();
        }

        if (lines.length < 7) {
            throw new ParserException("EPC QR-Code hat zu wenige Zeilen");
        }

        SepaData data = new SepaData();

        // Zeile 3: Service Code
        if (!"SCT".equals(lines[3].trim())) {
            throw new ParserException("Nicht-SEPA-Überweisung: " + lines[3]);
        }

        // Zeile 4-6: BIC, Name, IBAN
        data.setBic(getOrNull(lines, 4));
        data.setEmpfaengerName(getOrNull(lines, 5));
        data.setIban(getOrNull(lines, 6));

        // Ab Zeile 7: Betrag, Betreff, Verwendungszweck
        int pos = 7;

        // Zeile 7: Betrag erkennen (z.B. "EUR12.34", "12,34", "12.34")
        if (pos < lines.length) {
            String line7 = lines[pos].trim();
            if (looksLikeAmount(line7)) {
                data.setBetrag(parseBetrag(line7));
                pos++;
            }
        }

        // Nächste Zeile: Betreff mit //
        if (pos < lines.length) {
            String next = lines[pos].trim();
            if (next.startsWith("//")) {
                data.setBetreff(next.substring(2));
                pos++;
            }
        }

        // Rest: Verwendungszweck
        StringBuilder zweck = new StringBuilder();
        while (pos < lines.length) {
            String line = lines[pos].trim();
            if (!line.isEmpty()) {
                if (zweck.length() > 0) zweck.append("\n");
                zweck.append(line);
            }
            pos++;
        }
        if (zweck.length() > 0) {
            data.setVerwendungszweck(zweck.toString());
        }

        if (!data.isValid()) {
            throw new ParserException("Unvollständig: IBAN oder Empfänger fehlt");
        }

        return data;
    }

    /**
     * Erkennt ob eine Zeile wie ein Betrag aussieht.
     * Akzeptiert: "EUR12.34", "12,34", "12.34", "CHF100", etc.
     */
    private boolean looksLikeAmount(String line) {
        if (line.isEmpty()) return false;
        // Entferne optionalen Währungscode am Anfang
        String cleaned = line.replaceAll("^[A-Z]{3}", "").trim();
        // Muss eine Zahl sein (mit optionalem Komma/Punkt)
        return cleaned.matches("\\d+[.,]?\\d*");
    }

    /**
     * Parst einen Betrag aus einer Zeile.
     */
    private String parseBetrag(String line) {
        String cleaned = line.replaceAll("^[A-Z]{3}", "").trim();
        // Komma durch Punkt ersetzen
        cleaned = cleaned.replace(",", ".");
        return cleaned;
    }

    private String getOrNull(String[] lines, int index) {
        if (index < lines.length) {
            String v = lines[index].trim();
            return v.isEmpty() ? null : v;
        }
        return null;
    }
}
