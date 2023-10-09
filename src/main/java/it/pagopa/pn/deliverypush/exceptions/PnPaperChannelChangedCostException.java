package it.pagopa.pn.deliverypush.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_PAPERCHANNELSENDCOSTCHANGED;

public class PnPaperChannelChangedCostException extends PnRuntimeException {

    public PnPaperChannelChangedCostException() {
        this(null);
    }

    public PnPaperChannelChangedCostException(Throwable ex) {
        super("Send cost is different from prepare, need to redo prepare", "Send cost is different from prepare, need to redo prepare", HttpStatus.UNPROCESSABLE_ENTITY.value(), ERROR_CODE_DELIVERYPUSH_PAPERCHANNELSENDCOSTCHANGED, null, null, ex);
    }

}
