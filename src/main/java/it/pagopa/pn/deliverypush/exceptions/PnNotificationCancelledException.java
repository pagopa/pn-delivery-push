package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTIFICATIONCANCELLED;


public class PnNotificationCancelledException extends PnRuntimeException {

    public PnNotificationCancelledException() {
        this(null);
    }

    public PnNotificationCancelledException(Throwable ex) {
        super("Notification already cancelled", "Notification has already been cancelled", HttpStatus.NOT_FOUND.value(), ERROR_CODE_DELIVERYPUSH_NOTIFICATIONCANCELLED, null, null, ex);
    }

}
