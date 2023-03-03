package it.pagopa.pn.deliverypush.dto.timeline.details;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
class SendCourtesyMessageDetailsIntTest {
    private SendCourtesyMessageDetailsInt detailsInt;
    private Instant instant = Instant.parse("2021-09-16T15:24:00.00Z");
    @BeforeEach
    void setUp() {
        detailsInt = new SendCourtesyMessageDetailsInt();
        detailsInt.setSendDate(instant);
        detailsInt.setDigitalAddress(CourtesyDigitalAddressInt.builder().address("address").build());
        detailsInt.setRecIndex(1);
    }
    @Test
    void toLog() {
        String expected = "recIndex=1 addressType=null digitalAddress='Sensitive information'";
        Assertions.assertEquals(expected, detailsInt.toLog());
    }
    @Test
    void testEquals() {
        SendCourtesyMessageDetailsInt expected = buildSendCourtesyMessageDetailsInt();
        Assertions.assertEquals(Boolean.TRUE, expected.equals(detailsInt));
    }
    @Test
    void getRecIndex() {
        Assertions.assertEquals(1, detailsInt.getRecIndex());
    }
    @Test
    void getDigitalAddress() {
        Assertions.assertEquals(CourtesyDigitalAddressInt.builder().address("address").build(), detailsInt.getDigitalAddress());
    }
    @Test
    void getSendDate() {
        Assertions.assertEquals(instant, detailsInt.getSendDate());
    }
    @Test
    void testToString() {
        String expected = "SendCourtesyMessageDetailsInt(recIndex=1, digitalAddress=CourtesyDigitalAddressInt(type=null), sendDate=2021-09-16T15:24:00Z, ioSendMessageResult=null)";
        Assertions.assertEquals(expected, detailsInt.toString());
    }
    private SendCourtesyMessageDetailsInt buildSendCourtesyMessageDetailsInt() {
        return SendCourtesyMessageDetailsInt.builder().digitalAddress(CourtesyDigitalAddressInt.builder().address("address").build()).sendDate(instant).recIndex(1).build();
    }
}