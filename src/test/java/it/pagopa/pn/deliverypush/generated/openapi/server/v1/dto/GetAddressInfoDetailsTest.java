package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

class GetAddressInfoDetailsTest {

    private GetAddressInfoDetails details;

    @BeforeEach
    void setUp() {
        Instant instant = Instant.parse("2021-09-16T15:24:00.00Z");
        details = new GetAddressInfoDetails();
        details.setRecIndex(1);
        details.setDigitalAddressSource(DigitalAddressSource.GENERAL);
        details.setIsAvailable(Boolean.TRUE);
        details.setAttemptDate(instant);
    }

    @Test
    void getRecIndex() {
        Assertions.assertEquals(1, details.getRecIndex());
    }

    @Test
    void digitalAddressSource() {

        GetAddressInfoDetails data = details.digitalAddressSource(DigitalAddressSource.GENERAL);
        Assertions.assertEquals(data, details.digitalAddressSource(DigitalAddressSource.GENERAL));
    }

    @Test
    void getDigitalAddressSource() {
        Assertions.assertEquals(DigitalAddressSource.GENERAL, details.getDigitalAddressSource());
    }

    @Test
    void isAvailable() {
        GetAddressInfoDetails data = details.isAvailable(Boolean.TRUE);
        Assertions.assertEquals(data, details);
    }

    @Test
    void getIsAvailable() {
        Assertions.assertEquals(Boolean.TRUE, details.getIsAvailable());
    }

    @Test
    void getAttemptDate() {
        Instant instant = Instant.parse("2021-09-16T15:24:00.00Z");
        Assertions.assertEquals(instant, details.getAttemptDate());
    }

    @Test
    void testEquals() {
        Instant instant = Instant.parse("2021-09-16T15:24:00.00Z");
        GetAddressInfoDetails data = GetAddressInfoDetails.builder()
                .recIndex(1)
                .attemptDate(instant)
                .digitalAddressSource(DigitalAddressSource.GENERAL)
                .isAvailable(Boolean.TRUE)
                .build();

        Assertions.assertEquals(Boolean.TRUE, details.equals(data));
    }
}