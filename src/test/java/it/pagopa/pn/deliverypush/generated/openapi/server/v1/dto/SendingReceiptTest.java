package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SendingReceiptTest {

    private SendingReceipt receipt;

    @BeforeEach
    void setUp() {
        receipt = new SendingReceipt();
        receipt.setId("one");
        receipt.setSystem("two");
    }

    @Test
    void id() {
        SendingReceipt tmp = SendingReceipt.builder()
                .id("one")
                .system("two")
                .build();
        Assertions.assertEquals(tmp, receipt.id("one"));
    }

    @Test
    void getId() {
        Assertions.assertEquals("one", receipt.getId());
    }

    @Test
    void system() {
        SendingReceipt tmp = SendingReceipt.builder()
                .id("one")
                .system("two")
                .build();
        Assertions.assertEquals(tmp, receipt.system("two"));
    }

    @Test
    void getSystem() {
        Assertions.assertEquals("two", receipt.getSystem());
    }

    @Test
    void testEquals() {
        SendingReceipt tmp = SendingReceipt.builder()
                .id("one")
                .system("two")
                .build();
        Assertions.assertEquals(Boolean.TRUE, tmp.equals(receipt));
    }

    @Test
    void testToString() {
        String tmp = "class SendingReceipt {\n" +
                "    id: one\n" +
                "    system: two\n" +
                "}";
        Assertions.assertEquals(tmp, receipt.toString());
    }
}