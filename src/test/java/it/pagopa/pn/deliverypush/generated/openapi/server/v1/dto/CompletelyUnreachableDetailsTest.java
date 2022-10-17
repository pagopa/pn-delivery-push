package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompletelyUnreachableDetailsTest {

    private CompletelyUnreachableDetails details;

    @BeforeEach
    void setUp() {
        details = new CompletelyUnreachableDetails();
        details.setRecIndex(1);
    }

    @Test
    void getRecIndex() {
        Assertions.assertEquals(1, details.getRecIndex());
    }

    @Test
    void testEquals() {
        CompletelyUnreachableDetails data = CompletelyUnreachableDetails.builder()
                .recIndex(1)
                .build();

        Assertions.assertEquals(Boolean.TRUE, details.equals(data));
    }

    @Test
    void testToString() {
        String data = "class CompletelyUnreachableDetails {\n" +
                "    recIndex: 1\n" +
                "}";
        Assertions.assertEquals(data, details.toString());
    }
}