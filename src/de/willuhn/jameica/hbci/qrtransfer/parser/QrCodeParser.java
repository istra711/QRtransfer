package de.willuhn.jameica.hbci.qrtransfer.parser;

import de.willuhn.jameica.hbci.qrtransfer.model.SepaData;

/**
 * Interface für SEPA-QR-Code-Parser.
 */
public interface QrCodeParser {

    /**
     * Prüft, ob der gegebene Text von diesem Parser verarbeitet werden kann.
     * @param text der geparste QR-Code-Text
     * @return true, wenn das Format erkannt wurde
     */
    boolean canParse(String text);

    /**
     * Parst den QR-Code-Text und gibt die SEPA-Daten zurück.
     * @param text der geparste QR-Code-Text
     * @return die geparsten SEPA-Daten
     * @throws ParserException wenn das Parsen fehlschlägt
     */
    SepaData parse(String text) throws ParserException;
}
