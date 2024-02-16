package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class RequestRefusedDetailsTest {

    private RequestRefusedDetailsV23 details;

    @BeforeEach
    void setUp() {
        details = new RequestRefusedDetailsV23();
        details.setRefusalReasons(List.of(NotificationRefusedErrorV23.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build()));
    }

    @Test
    void errors() {
        RequestRefusedDetailsV23 tmp = RequestRefusedDetailsV23.builder()
                .refusalReasons(List.of(NotificationRefusedErrorV23.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build()))
                .build();
        Assertions.assertEquals(tmp, details.refusalReasons(List.of(NotificationRefusedErrorV23.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build())
        ));
    }

    @Test
    void addErrorsItem() {
        RequestRefusedDetailsV23 tmp = new RequestRefusedDetailsV23();
        tmp.addRefusalReasonsItem(
                NotificationRefusedErrorV23.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build()
        );
        Assertions.assertEquals(details, tmp);
    }

    @Test
    void getErrors() {
        Assertions.assertEquals(List.of(NotificationRefusedErrorV23.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build()), details.getRefusalReasons());
    }

    @Test
    void testEquals() {
        RequestRefusedDetailsV23 tmp = RequestRefusedDetailsV23.builder()
                .refusalReasons(List.of(NotificationRefusedErrorV23.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build()))
                .build();
        Assertions.assertEquals(Boolean.TRUE, tmp.equals(details));
    }
    
}