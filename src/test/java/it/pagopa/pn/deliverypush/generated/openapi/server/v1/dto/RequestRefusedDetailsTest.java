package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class RequestRefusedDetailsTest {

    private RequestRefusedDetails details;

    @BeforeEach
    void setUp() {
        details = new RequestRefusedDetails();
        details.setRefusalReasons(List.of(NotificationRefusedError.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build()));
    }

    @Test
    void errors() {
        RequestRefusedDetails tmp = RequestRefusedDetails.builder()
                .refusalReasons(List.of(NotificationRefusedError.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build()))
                .build();
        Assertions.assertEquals(tmp, details.refusalReasons(List.of(NotificationRefusedError.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build())
        ));
    }

    @Test
    void addErrorsItem() {
        RequestRefusedDetails tmp = new RequestRefusedDetails();
        tmp.addRefusalReasonsItem(
                NotificationRefusedError.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build()
        );
        Assertions.assertEquals(details, tmp);
    }

    @Test
    void getErrors() {
        Assertions.assertEquals(List.of(NotificationRefusedError.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build()), details.getRefusalReasons());
    }

    @Test
    void testEquals() {
        RequestRefusedDetails tmp = RequestRefusedDetails.builder()
                .refusalReasons(List.of(NotificationRefusedError.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build()))
                .build();
        Assertions.assertEquals(Boolean.TRUE, tmp.equals(details));
    }
    
}