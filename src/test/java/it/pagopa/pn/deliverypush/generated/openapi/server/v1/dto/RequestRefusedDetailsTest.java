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
        details.setErrors(Collections.singletonList("one"));
    }

    @Test
    void errors() {
        RequestRefusedDetails tmp = RequestRefusedDetails.builder()
                .errors(Collections.singletonList("one"))
                .build();
        Assertions.assertEquals(tmp, details.errors(Collections.singletonList("one")));
    }

    @Test
    void addErrorsItem() {
        RequestRefusedDetails tmp = new RequestRefusedDetails();
        tmp.addErrorsItem("one");
        Assertions.assertEquals(details, tmp);
    }

    @Test
    void getErrors() {
        Assertions.assertEquals(Collections.singletonList("one"), details.getErrors());
    }

    @Test
    void testEquals() {
        RequestRefusedDetails tmp = RequestRefusedDetails.builder()
                .errors(Collections.singletonList("one"))
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