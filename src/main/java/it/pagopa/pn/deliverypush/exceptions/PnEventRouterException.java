package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnInternalException;

public class PnEventRouterException extends PnInternalException {
    public PnEventRouterException(String message, String errorCode) {
        super(message, errorCode);
    }

    public PnEventRouterException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
