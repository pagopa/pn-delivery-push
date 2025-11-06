package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_SERVICE_NOT_IMPLEMENTED;


public class PnNotImplementedException extends PnRuntimeException {

    public PnNotImplementedException() {
        this(null);
    }

    public PnNotImplementedException(Throwable ex) {
        super("This service is not implemented yet.", "This service is not implemented yet.", HttpStatus.NOT_IMPLEMENTED.value(), ERROR_CODE_DELIVERYPUSH_SERVICE_NOT_IMPLEMENTED, null, null, ex);
    }

}
