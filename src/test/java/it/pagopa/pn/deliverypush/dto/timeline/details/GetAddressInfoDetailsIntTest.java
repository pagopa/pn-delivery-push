package it.pagopa.pn.deliverypush.dto.timeline.details;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
class GetAddressInfoDetailsIntTest {
    private GetAddressInfoDetailsInt detailsInt;
    private Instant instant = Instant.parse("2021-09-16T15:24:00.00Z");
    @BeforeEach
    void setUp() {
        detailsInt = new GetAddressInfoDetailsInt();
        detailsInt.setAttemptDate(instant);
        detailsInt.setDigitalAddressSource(DigitalAddressSourceInt.GENERAL);
        detailsInt.setIsAvailable(Boolean.TRUE);
        detailsInt.setRecIndex(1);
    }
    @Test
    void toLog() {
        String expected = "recIndex=1 digitalAddressSource=GENERAL isAvailable=true";
        Assertions.assertEquals(expected, detailsInt.toLog());
    }
    @Test
    void testEquals() {
        GetAddressInfoDetailsInt expected = buildGetAddressInfoDetailsInt();
        Assertions.assertEquals(Boolean.TRUE, detailsInt.equals(expected));
    }
    @Test
    void getRecIndex() {
        Assertions.assertEquals(1, detailsInt.getRecIndex());
    }
    @Test
    void getDigitalAddressSource() {
        Assertions.assertEquals(DigitalAddressSourceInt.GENERAL, detailsInt.getDigitalAddressSource());
    }
    @Test
    void getIsAvailable() {
        Assertions.assertEquals(Boolean.TRUE, detailsInt.getIsAvailable());
    }
    @Test
    void getAttemptDate() {
        Assertions.assertEquals(instant, detailsInt.getAttemptDate());
    }
    @Test
    void testToString() {
        String expected = "GetAddressInfoDetailsInt(recIndex=1, digitalAddressSource=GENERAL, isAvailable=true, attemptDate=2021-09-16T15:24:00Z)";
        Assertions.assertEquals(expected, detailsInt.toString());
    }
    private GetAddressInfoDetailsInt buildGetAddressInfoDetailsInt() {
        return GetAddressInfoDetailsInt.builder().recIndex(1).attemptDate(instant).digitalAddressSource(DigitalAddressSourceInt.GENERAL).isAvailable(Boolean.TRUE).build();
    }
}