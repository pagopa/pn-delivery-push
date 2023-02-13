package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationRefusedErrorCodeInt;
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
                NotificationRefusedErrorCodeInt.FILE_NOTFOUND.getValue()
        ));
    }

    @Test
    void errors() {
        RequestRefusedDetails tmp = RequestRefusedDetails.builder()
                .errors(Collections.singletonList(
                        NotificationRefusedErrorCodeInt.FILE_NOTFOUND.getValue()
                ))
                .build();
        Assertions.assertEquals(tmp, details.errors(Collections.singletonList(
                NotificationRefusedErrorCodeInt.FILE_NOTFOUND.getValue()
        )));
    }

    @Test
    void addErrorsItem() {
        RequestRefusedDetails tmp = new RequestRefusedDetails();
        tmp.addErrorsItem(
                NotificationRefusedErrorCodeInt.FILE_NOTFOUND.getValue()
        );
        Assertions.assertEquals(details, tmp);
    }

    @Test
    void getErrors() {
        Assertions.assertEquals(Collections.singletonList(NotificationRefusedErrorCodeInt.FILE_NOTFOUND.getValue()), details.getErrors());
    }

    @Test
    void testEquals() {
        RequestRefusedDetails tmp = RequestRefusedDetails.builder()
                .errors(Collections.singletonList(NotificationRefusedErrorCodeInt.FILE_NOTFOUND.getValue()))
                .build();
        Assertions.assertEquals(Boolean.TRUE, tmp.equals(details));
    }
    
}