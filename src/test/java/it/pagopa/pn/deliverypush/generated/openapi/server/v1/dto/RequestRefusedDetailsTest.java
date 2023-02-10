package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

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
                NotificationRefusedError.builder()
                        .detail("dettaglio")
                        .errorCode(NotificationRefusedErrorCode.FILE_NOTFOUND)
                        .build()
        ));
    }

    @Test
    void errors() {
        RequestRefusedDetails tmp = RequestRefusedDetails.builder()
                .errors(Collections.singletonList(NotificationRefusedError.builder()
                        .detail("dettaglio")
                        .errorCode(NotificationRefusedErrorCode.FILE_NOTFOUND)
                        .build()))
                .build();
        Assertions.assertEquals(tmp, details.errors(Collections.singletonList(NotificationRefusedError.builder()
                .detail("dettaglio")
                .errorCode(NotificationRefusedErrorCode.FILE_NOTFOUND)
                .build())));
    }

    @Test
    void addErrorsItem() {
        RequestRefusedDetails tmp = new RequestRefusedDetails();
        tmp.addErrorsItem(
                NotificationRefusedError.builder()
                .detail("dettaglio")
                .errorCode(NotificationRefusedErrorCode.FILE_NOTFOUND)
                .build());
        Assertions.assertEquals(details, tmp);
    }

    @Test
    void getErrors() {
        Assertions.assertEquals(Collections.singletonList(NotificationRefusedError.builder()
                .detail("dettaglio")
                .errorCode(NotificationRefusedErrorCode.FILE_NOTFOUND)
                .build()), details.getErrors());
    }

    @Test
    void testEquals() {
        RequestRefusedDetails tmp = RequestRefusedDetails.builder()
                .errors(Collections.singletonList(NotificationRefusedError.builder()
                        .detail("dettaglio")
                        .errorCode(NotificationRefusedErrorCode.FILE_NOTFOUND)
                        .build()))
                .build();
        Assertions.assertEquals(Boolean.TRUE, tmp.equals(details));
    }

    @Test
    void testToString() {
        String tmp = "class RequestRefusedDetails {\n" +
                "    errors: [one]\n" +
                "}";
        Assertions.assertEquals(tmp, details.toString());
    }
}