package de.willuhn.jameica.hbci.qrtransfer.parser;

/**
 * Exception für Fehler beim Parsen von QR-Codes.
 */
public class ParserException extends Exception {

    public ParserException(String message) {
        super(message);
    }

    public ParserException(String message, Throwable cause) {
        super(message, cause);
    }
}
