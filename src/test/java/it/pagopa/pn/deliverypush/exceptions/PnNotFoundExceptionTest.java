package it.pagopa.pn.deliverypush.exceptions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PnNotFoundExceptionTest {

    private PnNotFoundException pnNotFoundException;

    @Test
    void constructorPnNotFoundException1() {
        pnNotFoundException = new PnNotFoundException("Title", "Message");
        Assertions.assertEquals("Title", pnNotFoundException.getTitle());
    }

    @Test
    void constructorPnNotFoundException2() {
        pnNotFoundException = new PnNotFoundException("Title", "Message", Mockito.any());
        Assertions.assertEquals("Title", pnNotFoundException.getTitle());
    }
}