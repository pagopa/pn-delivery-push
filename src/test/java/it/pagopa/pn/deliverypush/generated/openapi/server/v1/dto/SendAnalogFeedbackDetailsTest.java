package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class SendAnalogFeedbackDetailsTest {

    private SendAnalogFeedbackDetails details;

    @BeforeEach
    void setUp() {
        details = new SendAnalogFeedbackDetails();
        details.sentAttemptMade(1);
        details.setErrors(Collections.singletonList("error"));
        details.setInvestigation(Boolean.TRUE);
        details.setNewAddress(PhysicalAddress.builder().address("add").build());
        details.setPhysicalAddress(PhysicalAddress.builder().address("add").build());
        details.setRecIndex(1);
        details.setServiceLevel(ServiceLevel.REGISTERED_LETTER_890);
    }

    @Test
    void recIndex() {
        SendAnalogFeedbackDetails expected = buildSendAnalogFeedbackDetails();

        SendAnalogFeedbackDetails actual = details.recIndex(1);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void getRecIndex() {
        Assertions.assertEquals(1, details.getRecIndex());
    }

    @Test
    void physicalAddress() {
        SendAnalogFeedbackDetails expected = buildSendAnalogFeedbackDetails();

        SendAnalogFeedbackDetails actual = details.physicalAddress(PhysicalAddress.builder().address("add").build());

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void getPhysicalAddress() {
        Assertions.assertEquals(PhysicalAddress.builder().address("add").build(), details.getPhysicalAddress());
    }

    @Test
    void serviceLevel() {
        SendAnalogFeedbackDetails expected = buildSendAnalogFeedbackDetails();

        SendAnalogFeedbackDetails actual = details.serviceLevel(ServiceLevel.REGISTERED_LETTER_890);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void getServiceLevel() {
        Assertions.assertEquals(ServiceLevel.REGISTERED_LETTER_890, details.getServiceLevel());
    }

    @Test
    void sentAttemptMade() {
        SendAnalogFeedbackDetails expected = buildSendAnalogFeedbackDetails();

        SendAnalogFeedbackDetails actual = details.sentAttemptMade(1);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void getSentAttemptMade() {
        Assertions.assertEquals(1, details.getSentAttemptMade());
    }

    @Test
    void investigation() {
        SendAnalogFeedbackDetails expected = buildSendAnalogFeedbackDetails();

        SendAnalogFeedbackDetails actual = details.investigation(Boolean.TRUE);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void getInvestigation() {
        Assertions.assertEquals(Boolean.TRUE, details.getInvestigation());
    }

    @Test
    void newAddress() {
        SendAnalogFeedbackDetails expected = buildSendAnalogFeedbackDetails();

        SendAnalogFeedbackDetails actual = details.newAddress(PhysicalAddress.builder().address("add").build());

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void getNewAddress() {
        Assertions.assertEquals(PhysicalAddress.builder().address("add").build(), details.getNewAddress());
    }

    @Test
    void errors() {
        SendAnalogFeedbackDetails expected = buildSendAnalogFeedbackDetails();

        SendAnalogFeedbackDetails actual = details.errors(Collections.singletonList("error"));

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void getErrors() {
        Assertions.assertEquals(Collections.singletonList("error"), details.getErrors());
    }

    @Test
    void testEquals() {
        SendAnalogFeedbackDetails data = buildSendAnalogFeedbackDetails();
        Assertions.assertEquals(Boolean.TRUE, details.equals(data));
    }

    @Test
    void testToString() {
        String expected = "class SendAnalogFeedbackDetails {\n" +
                "    recIndex: 1\n" +
                "    physicalAddress: class PhysicalAddress {\n" +
                "        at: null\n" +
                "        address: add\n" +
                "        addressDetails: null\n" +
                "        zip: null\n" +
                "        municipality: null\n" +
                "        municipalityDetails: null\n" +
                "        province: null\n" +
                "        foreignState: null\n" +
                "    }\n" +
                "    serviceLevel: REGISTERED_LETTER_890\n" +
                "    sentAttemptMade: 1\n" +
                "    investigation: true\n" +
                "    newAddress: class PhysicalAddress {\n" +
                "        at: null\n" +
                "        address: add\n" +
                "        addressDetails: null\n" +
                "        zip: null\n" +
                "        municipality: null\n" +
                "        municipalityDetails: null\n" +
                "        province: null\n" +
                "        foreignState: null\n" +
                "    }\n" +
                "    errors: [error]\n" +
                "}";

        Assertions.assertEquals(expected, details.toString());
    }

    private SendAnalogFeedbackDetails buildSendAnalogFeedbackDetails() {
        return SendAnalogFeedbackDetails.builder()
                .recIndex(1)
                .errors(Collections.singletonList("error"))
                .investigation(Boolean.TRUE)
                .newAddress(PhysicalAddress.builder().address("add").build())
                .physicalAddress(PhysicalAddress.builder().address("add").build())
                .serviceLevel(ServiceLevel.REGISTERED_LETTER_890)
                .sentAttemptMade(1)
                .build();
    }
}