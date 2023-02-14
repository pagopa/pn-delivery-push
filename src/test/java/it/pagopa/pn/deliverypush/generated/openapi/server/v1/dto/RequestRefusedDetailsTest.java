package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class RequestRefusedDetailsTest {

    private RequestRefusedDetails details;

    @BeforeEach
    void setUp() {
        details = new RequestRefusedDetails();
        details.setErrors(Collections.singletonList(
                PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.FILE_NOTFOUND.getValue()
        ));
    }

    @Test
    void errors() {
        RequestRefusedDetails tmp = RequestRefusedDetails.builder()
                .errors(Collections.singletonList(
                        PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.FILE_NOTFOUND.getValue()
                ))
                .build();
        Assertions.assertEquals(tmp, details.errors(Collections.singletonList(
                PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.FILE_NOTFOUND.getValue()
        )));
    }

    @Test
    void addErrorsItem() {
        RequestRefusedDetails tmp = new RequestRefusedDetails();
        tmp.addErrorsItem(
                PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.FILE_NOTFOUND.getValue()
        );
        Assertions.assertEquals(details, tmp);
    }

    @Test
    void getErrors() {
        Assertions.assertEquals(Collections.singletonList(PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.FILE_NOTFOUND.getValue()), details.getErrors());
    }

    @Test
    void testEquals() {
        RequestRefusedDetails tmp = RequestRefusedDetails.builder()
                .errors(Collections.singletonList(PnDeliveryPushExceptionCodes.NotificationRefusedErrorCodeInt.FILE_NOTFOUND.getValue()))
                .build();
        Assertions.assertEquals(Boolean.TRUE, tmp.equals(details));
    }
    
}