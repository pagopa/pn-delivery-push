package it.pagopa.pn.deliverypush.exceptions;


import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_ROOTIDNOTFOUND;

public class PnRootIdNonFoundException extends PnNotFoundException {

    public PnRootIdNonFoundException(String description) {
        super("RootId not found", description, ERROR_CODE_DELIVERYPUSH_ROOTIDNOTFOUND);
    }

}
