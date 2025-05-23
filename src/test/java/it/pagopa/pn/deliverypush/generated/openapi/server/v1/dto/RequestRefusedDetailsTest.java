package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class RequestRefusedDetailsTest {

    private RequestRefusedDetailsV27 details;

    @BeforeEach
    void setUp() {
        details = new RequestRefusedDetailsV27();
        details.setRefusalReasons(List.of(NotificationRefusedErrorV27.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build()));
    }

    @Test
    void errors() {
        RequestRefusedDetailsV27 tmp = RequestRefusedDetailsV27.builder()
                .refusalReasons(List.of(NotificationRefusedErrorV27.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build()))
                .build();
        Assertions.assertEquals(tmp, details.refusalReasons(List.of(NotificationRefusedErrorV27.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build())
        ));
    }

    @Test
    void addErrorsItem() {
        RequestRefusedDetailsV27 tmp = new RequestRefusedDetailsV27();
        tmp.addRefusalReasonsItem(
                NotificationRefusedErrorV27.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build()
        );
        Assertions.assertEquals(details, tmp);
    }

    @Test
    void getErrors() {
        Assertions.assertEquals(List.of(NotificationRefusedErrorV27.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build()), details.getRefusalReasons());
    }

    @Test
    void testEquals() {
        RequestRefusedDetailsV27 tmp = RequestRefusedDetailsV27.builder()
                .refusalReasons(List.of(NotificationRefusedErrorV27.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build()))
                .build();
        Assertions.assertEquals(Boolean.TRUE, tmp.equals(details));
    }
    
}