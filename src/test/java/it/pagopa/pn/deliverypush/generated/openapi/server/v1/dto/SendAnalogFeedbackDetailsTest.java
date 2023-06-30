package it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SendAnalogFeedbackDetailsTest {

    private SendAnalogFeedbackDetails details;

    @BeforeEach
    void setUp() {
        details = new SendAnalogFeedbackDetails();
        details.sentAttemptMade(1);
        details.setDeliveryFailureCause("error");
        details.setNewAddress(PhysicalAddress.builder().address("add").build());
        details.setPhysicalAddress(PhysicalAddress.builder().address("add").build());
        details.setRecIndex(1);
        details.setResponseStatus(ResponseStatus.KO);
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

        SendAnalogFeedbackDetails actual = details.deliveryFailureCause("error");

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void getErrors() {
        Assertions.assertEquals("error", details.getDeliveryFailureCause());
    }


    @Test
    void responseStatus() {
        SendAnalogFeedbackDetails expected = buildSendAnalogFeedbackDetails();

        SendAnalogFeedbackDetails actual = details.responseStatus(ResponseStatus.KO);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void getResponseStatus() {
        Assertions.assertEquals(ResponseStatus.KO, details.getResponseStatus());
    }


    @Test
    void testEquals() {
        SendAnalogFeedbackDetails data = buildSendAnalogFeedbackDetails();
        Assertions.assertEquals(Boolean.TRUE, details.equals(data));
    }
    
    private SendAnalogFeedbackDetails buildSendAnalogFeedbackDetails() {
        return SendAnalogFeedbackDetails.builder()
                .recIndex(1)
                .deliveryFailureCause("error")
                .newAddress(PhysicalAddress.builder().address("add").build())
                .physicalAddress(PhysicalAddress.builder().address("add").build())
                .serviceLevel(ServiceLevel.REGISTERED_LETTER_890)
                .responseStatus(ResponseStatus.KO)
                .sentAttemptMade(1)
                .build();
    }
}