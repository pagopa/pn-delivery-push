package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

class RequestRefusedDetailsTest {

    private RequestRefusedDetails details;

    @BeforeEach
    void setUp() {
        details = new RequestRefusedDetails();
        details.setErrors(List.of(NotificationRefusedError.builder().errorCode(NotificationRefusedError.ErrorCodeEnum.FILE_NOTFOUND).detail("Allegati non trovati").build()));
    }

    @Test
    void errors() {
        RequestRefusedDetails tmp = RequestRefusedDetails.builder()
                .errors(List.of(NotificationRefusedError.builder().errorCode(NotificationRefusedError.ErrorCodeEnum.FILE_NOTFOUND).detail("Allegati non trovati").build()))
                .build();
        Assertions.assertEquals(tmp, details.errors(List.of(NotificationRefusedError.builder().errorCode(NotificationRefusedError.ErrorCodeEnum.FILE_NOTFOUND).detail("Allegati non trovati").build())
        ));
    }

    @Test
    void addErrorsItem() {
        RequestRefusedDetails tmp = new RequestRefusedDetails();
        tmp.addErrorsItem(
                NotificationRefusedError.builder().errorCode(NotificationRefusedError.ErrorCodeEnum.FILE_NOTFOUND).detail("Allegati non trovati").build()
        );
        Assertions.assertEquals(details, tmp);
    }

    @Test
    void getErrors() {
        Assertions.assertEquals(List.of(NotificationRefusedError.builder().errorCode(NotificationRefusedError.ErrorCodeEnum.FILE_NOTFOUND).detail("Allegati non trovati").build()), details.getErrors());
    }

    @Test
    void testEquals() {
        RequestRefusedDetails tmp = RequestRefusedDetails.builder()
                .errors(List.of(NotificationRefusedError.builder().errorCode(NotificationRefusedError.ErrorCodeEnum.FILE_NOTFOUND).detail("Allegati non trovati").build()))
                .build();
        Assertions.assertEquals(Boolean.TRUE, tmp.equals(details));
    }
    
}