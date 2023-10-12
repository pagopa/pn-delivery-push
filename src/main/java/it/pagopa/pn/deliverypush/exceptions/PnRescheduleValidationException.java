package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnInternalException;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_RESCHEDULE_VALIDATION;

public class PnRescheduleValidationException extends PnInternalException {
    public PnRescheduleValidationException(String message) {
        super(message, ERROR_CODE_DELIVERYPUSH_RESCHEDULE_VALIDATION);
    }
}
