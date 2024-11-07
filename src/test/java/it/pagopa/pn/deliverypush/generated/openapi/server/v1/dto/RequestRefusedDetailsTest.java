package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class RequestRefusedDetailsTest {

    private RequestRefusedDetailsV25 details;

    @BeforeEach
    void setUp() {
        details = new RequestRefusedDetailsV25();
        details.setRefusalReasons(List.of(NotificationRefusedErrorV25.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build()));
    }

    @Test
    void errors() {
        RequestRefusedDetailsV25 tmp = RequestRefusedDetailsV25.builder()
                .refusalReasons(List.of(NotificationRefusedErrorV25.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build()))
                .build();
        Assertions.assertEquals(tmp, details.refusalReasons(List.of(NotificationRefusedErrorV25.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build())
        ));
    }

    @Test
    void addErrorsItem() {
        RequestRefusedDetailsV25 tmp = new RequestRefusedDetailsV25();
        tmp.addRefusalReasonsItem(
                NotificationRefusedErrorV25.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build()
        );
        Assertions.assertEquals(details, tmp);
    }

    @Test
    void getErrors() {
        Assertions.assertEquals(List.of(NotificationRefusedErrorV25.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build()), details.getRefusalReasons());
    }

    @Test
    void testEquals() {
        RequestRefusedDetailsV25 tmp = RequestRefusedDetailsV25.builder()
                .refusalReasons(List.of(NotificationRefusedErrorV25.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build()))
                .build();
        Assertions.assertEquals(Boolean.TRUE, tmp.equals(details));
    }
    
}