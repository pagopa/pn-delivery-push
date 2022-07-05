package it.pagopa.pn.deliverypush.exceptions;

public class PnNotFoundException extends RuntimeException {
    public PnNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    public PnNotFoundException(String message) {
        super(message);
    }
}
