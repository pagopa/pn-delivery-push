package it.pagopa.pn.deliverypush.exceptions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PnReadFileExceptionTest {

    private PnReadFileException pnReadFileException;

    @Test
    void constructorPnReadFileException() {
        pnReadFileException = new PnReadFileException("Title", new Exception("Error"));
        Assertions.assertEquals("Title", pnReadFileException.getProblem().getDetail());
        Assertions.assertEquals("Error", pnReadFileException.getCause().getMessage());
        Assertions.assertEquals(Exception.class, pnReadFileException.getCause().getClass());

    }
}