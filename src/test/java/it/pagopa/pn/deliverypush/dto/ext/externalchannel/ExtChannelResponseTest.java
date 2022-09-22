package it.pagopa.pn.deliverypush.dto.ext.externalchannel;

import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

class ExtChannelResponseTest {

    private ExtChannelResponse response;

    @BeforeEach
    public void setup() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        List<String> data = new ArrayList<>();
        data.add("003");

        response = ExtChannelResponse.builder()
                .iun("001")
                .eventId("002")
                .notificationDate(instant)
                .responseStatus(ResponseStatusInt.OK)
                .errorList(data)
                .attachmentKeys(data)
                .analogNewAddressFromInvestigation(PhysicalAddressInt.builder()
                        .at("001").build())
                .build();
    }

    @Test
    void getEventId() {
        Assertions.assertEquals("002", response.getEventId());
    }

    @Test
    void getNotificationDate() {
        Instant instant = Instant.parse("2021-09-16T15:23:00.00Z");
        Assertions.assertEquals(instant, response.getNotificationDate());
    }

    @Test
    void getResponseStatus() {
        Assertions.assertEquals(ResponseStatusInt.OK, response.getResponseStatus());
    }

    @Test
    void getErrorList() {
        List<String> data = new ArrayList<>();
        data.add("003");
        Assertions.assertEquals(data.size(), response.getErrorList().size());
    }

    @Test
    void getAttachmentKeys() {
        List<String> data = new ArrayList<>();
        data.add("003");
        Assertions.assertEquals(data.size(), response.getAttachmentKeys().size());
    }


}