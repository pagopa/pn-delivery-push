package it.pagopa.pn.deliverypush.dto.timeline.details;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
class ScheduleDigitalWorkflowDetailsIntTest {
    private ScheduleDigitalWorkflowDetailsInt detailsInt;
    private Instant instant = Instant.parse("2021-09-16T15:24:00.00Z");
    @BeforeEach
    void setUp() {
        detailsInt = new ScheduleDigitalWorkflowDetailsInt();
        detailsInt.setDigitalAddress(LegalDigitalAddressInt.builder().address("address").build());
        detailsInt.setDigitalAddressSource(DigitalAddressSourceInt.SPECIAL);
        detailsInt.setLastAttemptDate(instant);
        detailsInt.setRecIndex(1);
        detailsInt.setSentAttemptMade(2);
        detailsInt.setSchedulingDate(Instant.EPOCH.plusMillis(10));
    }
    @Test
    void toLog() {
        String expected = "recIndex=1 sentAttemptMade=2";
        Assertions.assertEquals(expected, detailsInt.toLog());
    }
    @Test
    void testEquals() {
        ScheduleDigitalWorkflowDetailsInt expected = buildScheduleDigitalWorkflowDetailsInt();
        Assertions.assertEquals(Boolean.TRUE, expected.equals(detailsInt));
    }
    @Test
    void getRecIndex() {
        Assertions.assertEquals(1, detailsInt.getRecIndex());
    }
    @Test
    void getDigitalAddress() {
        Assertions.assertEquals(LegalDigitalAddressInt.builder().address("address").build(), detailsInt.getDigitalAddress());
    }
    @Test
    void getDigitalAddressSource() {
        Assertions.assertEquals(DigitalAddressSourceInt.SPECIAL, detailsInt.getDigitalAddressSource());
    }
    @Test
    void getSentAttemptMade() {
        Assertions.assertEquals(2, detailsInt.getSentAttemptMade());
    }
    @Test
    void getLastAttemptDate() {
        Assertions.assertEquals(instant, detailsInt.getLastAttemptDate());
    }
    @Test
    void testToString() {
        String expected = "ScheduleDigitalWorkflowDetailsInt(recIndex=1, digitalAddress=LegalDigitalAddressInt(type=null), digitalAddressSource=SPECIAL, sentAttemptMade=2, lastAttemptDate=2021-09-16T15:24:00Z, schedulingDate=1970-01-01T00:00:00.010Z)";
        Assertions.assertEquals(expected, detailsInt.toString());
    }
    private ScheduleDigitalWorkflowDetailsInt buildScheduleDigitalWorkflowDetailsInt() {
        return ScheduleDigitalWorkflowDetailsInt.builder().digitalAddress(LegalDigitalAddressInt.builder().address("address").build())
                .digitalAddressSource(DigitalAddressSourceInt.SPECIAL).recIndex(1).sentAttemptMade(2).lastAttemptDate(instant)
                .schedulingDate(Instant.EPOCH.plusMillis(10))
                .build();
    }
}