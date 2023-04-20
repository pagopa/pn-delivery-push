package it.pagopa.pn.deliverypush.legalfacts;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;

class CustomInstantWriterTest {
    private CustomInstantWriter customInstantWriter;

    @BeforeEach
    public void setup() {
        customInstantWriter = new CustomInstantWriter();
    }

    @ParameterizedTest
    @CsvSource({
            "2021-10-31T00:30:00.000Z, 31/10/2021 02:30 CEST",
            "2021-10-31T01:30:00.000Z, 31/10/2021 02:30 CET",
            "2021-10-30T23:59:00.000Z, 31/10/2021 01:59",
            "2021-10-31T00:00:00.000Z, 31/10/2021 02:00 CEST",
            "2021-10-31T02:00:00.000Z, 31/10/2021 03:00 CET",
            "2021-10-31T02:01:00.000Z, 31/10/2021 03:01",
            "2021-10-11T09:55:00.000Z, 11/10/2021 11:55"
    })
    void testInstantToDateConversion(String isoZuluTimeInstant, String expected) {
        // GIVEN
        Instant instant = Instant.parse(isoZuluTimeInstant);

        // WHEN
        String convertedDate = customInstantWriter.instantToDate(instant);

        // THEN
        Assertions.assertEquals(expected, convertedDate);
    }

    @ParameterizedTest
    @CsvSource({
            "2021-10-31T00:30:00.000Z, 31/10/2021",
            "2021-10-31T01:30:00.000Z, 31/10/2021",
            "2021-10-30T23:59:00.000Z, 31/10/2021",
            "2021-10-31T00:00:00.000Z, 31/10/2021",
            "2021-10-31T02:00:00.000Z, 31/10/2021",
            "2021-10-31T02:01:00.000Z, 31/10/2021",
            "2021-10-11T09:55:00.000Z, 11/10/2021"
    })
    void testInstantToDateWithoutTimeConversion(String isoZuluTimeInstant, String expected) {
        // GIVEN
        Instant instant = Instant.parse(isoZuluTimeInstant);

        // WHEN
        String convertedDate = customInstantWriter.instantToDate(instant, true);

        // THEN
        Assertions.assertEquals(expected, convertedDate);
    }


    @ParameterizedTest
    @CsvSource({
            "2021-10-31T00:30:00.000Z, 02:30 CEST",
            "2021-10-31T01:30:00.000Z, 02:30 CET",
            "2021-10-30T23:59:00.000Z, 01:59",
            "2021-10-31T00:00:00.000Z, 02:00 CEST",
            "2021-10-31T02:00:00.000Z, 03:00 CET",
            "2021-10-31T02:01:00.000Z, 03:01",
            "2021-10-11T09:55:00.000Z, 11:55"
    })
    void testInstantToTimeConversion(String isoZuluTimeInstant, String expected) {
        // GIVEN
        Instant instant = Instant.parse(isoZuluTimeInstant);

        // WHEN
        String convertedDate = customInstantWriter.instantToTime(instant);

        // THEN
        Assertions.assertEquals(expected, convertedDate);
    }


}
