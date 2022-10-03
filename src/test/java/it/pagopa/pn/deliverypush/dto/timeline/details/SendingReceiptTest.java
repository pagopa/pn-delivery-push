package it.pagopa.pn.deliverypush.dto.timeline.details;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SendingReceiptTest {

    private SendingReceipt sendingReceipt;

    @BeforeEach
    public void setup() {
        sendingReceipt = SendingReceipt.builder()
                .id("001")
                .system("002")
                .build();
    }

    @Test
    void getId() {
        String id = sendingReceipt.getId();
        Assertions.assertEquals("001", id);
    }

    @Test
    void getSystem() {
        String system = sendingReceipt.getSystem();
        Assertions.assertEquals("002", system);
    }

    @Test
    void testToString() {
        String expected = "SendingReceipt(id=001, system=002)";
        String actual = sendingReceipt.toString();
        Assertions.assertEquals(expected, actual);
    }


}