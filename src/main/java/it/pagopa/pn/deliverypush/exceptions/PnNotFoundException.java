package it.pagopa.pn.deliverypush.exceptions;

import lombok.Getter;

@Getter
public class PnNotFoundException extends RuntimeException {
    private final String title;
    
    public PnNotFoundException(String title, String message, Throwable cause) {
        super(message, cause);
        this.title = title;
    }
    
    public PnNotFoundException(String title, String message) {
        super(message);
        this.title = title;
    }
}
