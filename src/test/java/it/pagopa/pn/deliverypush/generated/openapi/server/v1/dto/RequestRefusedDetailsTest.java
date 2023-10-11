package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class RequestRefusedDetailsTest {

    private RequestRefusedDetailsV20 details;

    @BeforeEach
    void setUp() {
        details = new RequestRefusedDetailsV20();
        details.setRefusalReasons(List.of(NotificationRefusedErrorV20.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build()));
    }

    @Test
    void errors() {
        RequestRefusedDetailsV20 tmp = RequestRefusedDetailsV20.builder()
                .refusalReasons(List.of(NotificationRefusedErrorV20.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build()))
                .build();
        Assertions.assertEquals(tmp, details.refusalReasons(List.of(NotificationRefusedErrorV20.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build())
        ));
    }

    @Test
    void addErrorsItem() {
        RequestRefusedDetailsV20 tmp = new RequestRefusedDetailsV20();
        tmp.addRefusalReasonsItem(
                NotificationRefusedErrorV20.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build()
        );
        Assertions.assertEquals(details, tmp);
    }

    @Test
    void getErrors() {
        Assertions.assertEquals(List.of(NotificationRefusedErrorV20.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build()), details.getRefusalReasons());
    }

    @Test
    void testEquals() {
        RequestRefusedDetailsV20 tmp = RequestRefusedDetailsV20.builder()
                .refusalReasons(List.of(NotificationRefusedErrorV20.builder().errorCode("FILE_NOTFOUND").detail("Allegati non trovati").build()))
                .build();
        Assertions.assertEquals(Boolean.TRUE, tmp.equals(details));
    }
    
}