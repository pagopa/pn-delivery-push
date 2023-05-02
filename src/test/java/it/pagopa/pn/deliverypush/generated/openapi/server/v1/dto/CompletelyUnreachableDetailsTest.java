package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

class CompletelyUnreachableDetailsTest {

    private CompletelyUnreachableDetails details;

    @BeforeEach
    void setUp() {

        Instant time = Instant.ofEpochSecond(1234567890);

        details = new CompletelyUnreachableDetails();
        details.setRecIndex(1);
        details.legalFactGenerationDate(time);
    }

    @Test
    void getRecIndex() {
        Assertions.assertEquals(1, details.getRecIndex());
    }

    @Test
    void testEquals() {
        Instant time = Instant.ofEpochSecond(1234567890);

        CompletelyUnreachableDetails data = CompletelyUnreachableDetails.builder()
                .recIndex(1)
                .legalFactGenerationDate(time)
                .build();

        Assertions.assertEquals(Boolean.TRUE, details.equals(data));
    }

    @Test
    void testToString() {
        String data = "class CompletelyUnreachableDetails {\n" +
                "    recIndex: 1\n" +
                "    legalFactGenerationDate: 2009-02-13T23:31:30Z\n" +
                "}";
        Assertions.assertEquals(data, details.toString());
    }
}