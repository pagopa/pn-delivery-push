package it.pagopa.pn.deliverypush.action2.it.utils;

import it.pagopa.pn.api.dto.events.PnExtChnEmailEvent;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressSource;
import it.pagopa.pn.api.dto.notification.address.PhysicalAddress;
import it.pagopa.pn.api.dto.notification.timeline.*;
import it.pagopa.pn.deliverypush.action2.it.mockbean.ExternalChannelMock;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

public class TestUtils {

    public static final String EXTERNAL_CHANNEL_ANALOG_FAILURE_ATTEMPT = "EXTERNAL_CHANNEL_ANALOG_FAILURE_ATTEMPT";
    public static final String INVESTIGATION_ADDRESS_PRESENT_FAILURE = "INVESTIGATION_ADDRESS_PRESENT_FAILURE";
    public static final String INVESTIGATION_ADDRESS_PRESENT_POSITIVE = "INVESTIGATION_ADDRESS_PRESENT_POSITIVE";

    public static final String PUBLIC_REGISTRY_FAIL_GET_DIGITAL_ADDRESS = "PUBLIC_REGISTRY_FAIL_GET_DIGITAL_ADDRESS";
    public static final String PUBLIC_REGISTRY_FAIL_GET_ANALOG_ADDRESS = "PUBLIC_REGISTRY_FAIL_GET_ANALOG_ADDRESS";


    public static void checkSendCourtesyAddresses(String iun, String taxId, List<DigitalAddress> courtesyAddresses, TimelineService timelineService, ExternalChannelMock externalChannelMock) {

        int index = 0;
        for (DigitalAddress digitalAddress : courtesyAddresses) {
            String eventId = TimelineEventId.SEND_COURTESY_MESSAGE.buildEventId(
                    EventId.builder()
                            .iun(iun)
                            .recipientId(taxId)
                            .index(index)
                            .build());
            Optional<SendCourtesyMessageDetails> sendCourtesyMessageDetailsOpt = timelineService.getTimelineElement(iun, eventId, SendCourtesyMessageDetails.class);

            Assertions.assertTrue(sendCourtesyMessageDetailsOpt.isPresent());
            SendCourtesyMessageDetails sendCourtesyMessageDetails = sendCourtesyMessageDetailsOpt.get();
            Assertions.assertEquals(digitalAddress, sendCourtesyMessageDetails.getAddress());
            index++;
        }
        //Viene verificato l'effettivo invio del messaggio di cortesia verso external channel
        Mockito.verify(externalChannelMock, Mockito.times(courtesyAddresses.size())).sendNotification(Mockito.any(PnExtChnEmailEvent.class));
    }

    public static void checkGetAddress(String iun, String taxId, Boolean isAvailable, DigitalAddressSource source, int sentAttempt, TimelineService timelineService) {
        String correlationId = TimelineEventId.GET_ADDRESS.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recipientId(taxId)
                        .source(source)
                        .index(sentAttempt)
                        .build());

        Optional<GetAddressInfo> getAddressInfoOpt = timelineService.getTimelineElement(iun, correlationId, GetAddressInfo.class);
        Assertions.assertTrue(getAddressInfoOpt.isPresent());
        Assertions.assertEquals(isAvailable, getAddressInfoOpt.get().isAvailable());
    }

    public static void checkSendPaperToExtChannel(String iun, String taxId, PhysicalAddress physicalAddress, int sendAttempt, TimelineService timelineService) {
        String eventIdFirstSend = TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recipientId(taxId)
                        .index(sendAttempt)
                        .build());
        Optional<SendPaperDetails> sendPaperDetailsOpt = timelineService.getTimelineElement(iun, eventIdFirstSend, SendPaperDetails.class);
        Assertions.assertTrue(sendPaperDetailsOpt.isPresent());
        SendPaperDetails firstSendPaperDetails = sendPaperDetailsOpt.get();
        Assertions.assertEquals(physicalAddress, firstSendPaperDetails.getAddress());
    }
}