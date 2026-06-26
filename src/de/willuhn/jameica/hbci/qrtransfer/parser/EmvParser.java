package de.willuhn.jameica.hbci.qrtransfer.parser;

import de.willuhn.jameica.hbci.qrtransfer.model.SepaData;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parser für EMV QR-Codes im TLV-Format (Tag-Length-Value).
 */
public class EmvParser implements QrCodeParser {

    private static final String SEPA_GUI = "sepa";

    @Override
    public boolean canParse(String text) {
        if (text == null || text.isEmpty()) return false;
        return text.startsWith("000201");
    }

    @Override
    public SepaData parse(String text) throws ParserException {
        if (text == null || text.isEmpty()) {
            throw new ParserException("QR-Code-Text ist leer");
        }

        SepaData data = new SepaData();

        try {
            Map<String, java.util.List<TlvEntry>> parsed = parseTlv(text);

            String format = getValue(parsed, "00");
            if (!"01".equals(format)) {
                throw new ParserException("Ungültiges EMV-Format: " + format);
            }

            // Tag 26: Merchant Account Information
            String merchantInfo = getValue(parsed, "26");
            if (merchantInfo == null) {
                throw new ParserException("Keine Merchant Account Information (Tag 26) gefunden");
            }

            Map<String, TlvEntry> subTags = parseSubTlv(merchantInfo);

            TlvEntry gui = subTags.get("00");
            if (gui == null || !SEPA_GUI.equalsIgnoreCase(gui.value)) {
                throw new ParserException("Kein SEPA-QR-Code (GUI: " +
                    (gui != null ? gui.value : "fehlt") + ")");
            }

            // Sub-Tag 01: IBAN
            TlvEntry iban = subTags.get("01");
            if (iban == null) {
                throw new ParserException("IBAN fehlt");
            }
            data.setIban(iban.value);

            // Sub-Tag 02: BIC (optional)
            TlvEntry bic = subTags.get("02");
            if (bic != null) {
                data.setBic(bic.value);
            }

            // Sub-Tag 04: Empfänger Name
            TlvEntry empfaengerName = subTags.get("04");

            // Tag 53: Währung
            String waehrung = getValue(parsed, "53");
            if (waehrung != null) {
                data.setWaehrung(convertCurrency(waehrung));
            }

            // Tag 54: Betrag
            String betrag = getValue(parsed, "54");
            if (betrag != null) {
                data.setBetrag(betrag);
            }

            // Tag 59: Empfänger Name (Fallback)
            if (empfaengerName != null) {
                data.setEmpfaengerName(empfaengerName.value);
            } else {
                String name59 = getValue(parsed, "59");
                if (name59 != null) {
                    data.setEmpfaengerName(name59);
                }
            }

            // Tag 60: Empfänger Ort
            String ort = getValue(parsed, "60");
            if (ort != null) {
                data.setEmpfaengerOrt(ort);
            }

            // Tag 62: Additional Data (Verwendungszweck)
            String additionalData = getValue(parsed, "62");
            if (additionalData != null) {
                Map<String, TlvEntry> additionalSubTags = parseSubTlv(additionalData);
                // Sub-Tag 01: Reference Type
                // Sub-Tag 02: Reference Number (Verwendungszweck)
                TlvEntry refNumber = additionalSubTags.get("02");
                if (refNumber != null) {
                    data.setVerwendungszweck(refNumber.value);
                }
                // Sub-Tag 03: Additional Data (Betreff)
                TlvEntry additionalRef = additionalSubTags.get("03");
                if (additionalRef != null) {
                    data.setBetreff(additionalRef.value);
                }
            }

            if (!data.isValid()) {
                throw new ParserException("Unvollständige SEPA-Daten: IBAN oder Empfänger fehlt");
            }

        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Fehler beim Parsen: " + e.getMessage(), e);
        }

        return data;
    }

    private Map<String, java.util.List<TlvEntry>> parseTlv(String payload) {
        Map<String, java.util.List<TlvEntry>> result = new LinkedHashMap<>();
        int pos = 0;

        while (pos + 4 <= payload.length()) {
            String tag = payload.substring(pos, pos + 2);
            String lengthStr = payload.substring(pos + 2, pos + 4);
            int length;
            try {
                length = Integer.parseInt(lengthStr);
            } catch (NumberFormatException e) {
                break;
            }

            if (pos + 4 + length > payload.length()) {
                break;
            }

            String value = payload.substring(pos + 4, pos + 4 + length);
            pos += 4 + length;

            result.computeIfAbsent(tag, k -> new java.util.ArrayList<>())
                  .add(new TlvEntry(tag, value, length));
        }

        return result;
    }

    private Map<String, TlvEntry> parseSubTlv(String containerValue) {
        Map<String, TlvEntry> result = new LinkedHashMap<>();
        int pos = 0;

        while (pos + 4 <= containerValue.length()) {
            String subTag = containerValue.substring(pos, pos + 2);
            String lengthStr = containerValue.substring(pos + 2, pos + 4);
            int length;
            try {
                length = Integer.parseInt(lengthStr);
            } catch (NumberFormatException e) {
                break;
            }

            if (pos + 4 + length > containerValue.length()) {
                break;
            }

            String value = containerValue.substring(pos + 4, pos + 4 + length);
            pos += 4 + length;

            result.put(subTag, new TlvEntry(subTag, value, length));
        }

        return result;
    }

    private String getValue(Map<String, java.util.List<TlvEntry>> parsed, String tag) {
        java.util.List<TlvEntry> entries = parsed.get(tag);
        if (entries == null || entries.isEmpty()) return null;
        return entries.get(0).value;
    }

    private String convertCurrency(String code) {
        switch (code) {
            case "978": return "EUR";
            case "840": return "USD";
            case "826": return "GBP";
            case "756": return "CHF";
            default: return code;
        }
    }

    static class TlvEntry {
        final String tag;
        final String value;
        final int length;

        TlvEntry(String tag, String value, int length) {
            this.tag = tag;
            this.value = value;
            this.length = length;
        }
    }
}
