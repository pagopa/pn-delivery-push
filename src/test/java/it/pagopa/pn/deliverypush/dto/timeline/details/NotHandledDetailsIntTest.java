package it.pagopa.pn.deliverypush.dto.timeline.details;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
class NotHandledDetailsIntTest {

    private NotHandledDetailsInt detailsInt;

    @BeforeEach
    void setUp() {
        detailsInt = new NotHandledDetailsInt();
        detailsInt.setReason("yes");
        detailsInt.setReasonCode("001");
        detailsInt.setRecIndex(1);
    }
    @Test
    void toLog() {
        String expected = "recIndex=1 reasonCode=001 reason=yes";
        Assertions.assertEquals(expected, detailsInt.toLog());
    }
    @Test
    void testEquals() {
        NotHandledDetailsInt expected = buildNotHandledDetailsInt();
        Assertions.assertEquals(Boolean.TRUE, expected.equals(detailsInt));
    }
    @Test
    void getRecIndex() {
        Assertions.assertEquals(1, detailsInt.getRecIndex());
    }
    @Test
    void getReasonCode() {
        Assertions.assertEquals("001", detailsInt.getReasonCode());
    }
    @Test
    void getReason() {
        Assertions.assertEquals("yes", detailsInt.getReason());
    }
    @Test
    void testToString() {
        String expected = "NotHandledDetailsInt(recIndex=1, reasonCode=001, reason=yes)";
        Assertions.assertEquals(expected, detailsInt.toString());
    }

    private NotHandledDetailsInt buildNotHandledDetailsInt(){
        return NotHandledDetailsInt.builder()
                .recIndex(1)
                .reason("yes")
                .reasonCode("001")
                .build();
    }
}