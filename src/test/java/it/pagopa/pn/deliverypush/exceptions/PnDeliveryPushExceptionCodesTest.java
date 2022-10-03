package it.pagopa.pn.deliverypush.exceptions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class PnDeliveryPushExceptionCodesTest {

    private PnDeliveryPushExceptionCodes code;

    @Test
    void checkAll() {
        Assertions.assertAll(
                () -> Assertions.assertEquals("PN_DELIVERYPUSH_NOTFOUND", code.ERROR_CODE_DELIVERYPUSH_NOTFOUND),
                () -> Assertions.assertEquals("PN_DELIVERYPUSH_GETFILEERROR", code.ERROR_CODE_DELIVERYPUSH_GETFILEERROR)
        );
    }

}