package it.pagopa.pn.deliverypush.action2.it.utils;

import it.pagopa.pn.deliverypush.action2.CompletionWorkFlowHandler;
import it.pagopa.pn.deliverypush.action2.it.mockbean.ExternalChannelMock;
import it.pagopa.pn.deliverypush.action2.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class TestUtils {

    public static final String EXTERNAL_CHANNEL_ANALOG_FAILURE_ATTEMPT = "EXTERNAL_CHANNEL_ANALOG_FAILURE_ATTEMPT";
    public static final String INVESTIGATION_ADDRESS_PRESENT_FAILURE = "INVESTIGATION_ADDRESS_PRESENT_FAILURE";
    public static final String INVESTIGATION_ADDRESS_PRESENT_POSITIVE = "INVESTIGATION_ADDRESS_PRESENT_POSITIVE";

    public static final String PUBLIC_REGISTRY_FAIL_GET_DIGITAL_ADDRESS = "PUBLIC_REGISTRY_FAIL_GET_DIGITAL_ADDRESS";
    public static final String PUBLIC_REGISTRY_FAIL_GET_ANALOG_ADDRESS = "PUBLIC_REGISTRY_FAIL_GET_ANALOG_ADDRESS";


    public static void checkSendCourtesyAddresses(String iun, Integer recIndex, List<CourtesyDigitalAddressInt> courtesyAddresses, TimelineService timelineService, ExternalChannelMock externalChannelMock) {

        int index = 0;
        for (CourtesyDigitalAddressInt digitalAddress : courtesyAddresses) {
            String eventId = TimelineEventId.SEND_COURTESY_MESSAGE.buildEventId(
                    EventId.builder()
                            .iun(iun)
                            .recIndex(recIndex)
                            .index(index)
                            .build());
            Optional<SendCourtesyMessageDetails> sendCourtesyMessageDetailsOpt = timelineService.getTimelineElementDetails(iun, eventId, SendCourtesyMessageDetails.class);

            Assertions.assertTrue(sendCourtesyMessageDetailsOpt.isPresent());
            SendCourtesyMessageDetails sendCourtesyMessageDetails = sendCourtesyMessageDetailsOpt.get();
            Assertions.assertEquals(digitalAddress.getAddress(), sendCourtesyMessageDetails.getDigitalAddress().getAddress());
            Assertions.assertEquals(digitalAddress.getType().getValue(), sendCourtesyMessageDetails.getDigitalAddress().getType());
            index++;
        }
        //Viene verificato l'effettivo invio del messaggio di cortesia verso external channel
        Mockito.verify(externalChannelMock, Mockito.times(courtesyAddresses.size())).sendCourtesyNotification(Mockito.any(NotificationInt.class), Mockito.any(CourtesyDigitalAddressInt.class), Mockito.anyString());
    }

    public static void checkGetAddress(String iun, Integer recIndex, Boolean isAvailable, DigitalAddressSource source, int sentAttempt, TimelineService timelineService) {
        String correlationId = TimelineEventId.GET_ADDRESS.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .source(source)
                        .index(sentAttempt)
                        .build());

        Optional<GetAddressInfo> getAddressInfoOpt = timelineService.getTimelineElementDetails(iun, correlationId, GetAddressInfo.class);
        Assertions.assertTrue(getAddressInfoOpt.isPresent());
        Assertions.assertEquals(isAvailable, getAddressInfoOpt.get().getIsAvailable());
    }

    public static void checkSendPaperToExtChannel(String iun, Integer recIndex, PhysicalAddress physicalAddress, int sendAttempt, TimelineService timelineService) {
        String eventIdFirstSend = TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .index(sendAttempt)
                        .build());

        Optional<SendPaperDetails> sendPaperDetailsOpt = timelineService.getTimelineElementDetails(iun, eventIdFirstSend, SendPaperDetails.class);
        Assertions.assertTrue(sendPaperDetailsOpt.isPresent());
        SendPaperDetails sendPaperDetails = sendPaperDetailsOpt.get();
        Assertions.assertEquals(physicalAddress, sendPaperDetails.getPhysicalAddress());
    }

    public static void checkSuccessAnalogWorkflow(String iun, Integer recIndex, TimelineService timelineService, CompletionWorkFlowHandler completionWorkflow) {
        //Viene verificato che il workflow abbia avuto successo
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.ANALOG_SUCCESS_WORKFLOW.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .build())).isPresent());

        ArgumentCaptor<EndWorkflowStatus> endWorkflowStatusArgumentCaptor = ArgumentCaptor.forClass(EndWorkflowStatus.class);
        ArgumentCaptor<Integer> recIndexCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<NotificationInt> notificationCaptor = ArgumentCaptor.forClass(NotificationInt.class);

        Mockito.verify(completionWorkflow, Mockito.times(1)).completionAnalogWorkflow(
                notificationCaptor.capture(), recIndexCaptor.capture(), Mockito.any(Instant.class), Mockito.any(PhysicalAddress.class), endWorkflowStatusArgumentCaptor.capture()
        );
        Assertions.assertEquals(recIndex, recIndexCaptor.getValue());
        Assertions.assertEquals(iun, notificationCaptor.getValue().getIun());
        Assertions.assertEquals(EndWorkflowStatus.SUCCESS, endWorkflowStatusArgumentCaptor.getValue());
    }

    public static void checkSuccessDigitalWorkflow(String iun, Integer recIndex, TimelineService timelineService,
                                                   CompletionWorkFlowHandler completionWorkflow, LegalDigitalAddressInt address,
                                                   int invocationsNumber, int invocation) {
        //Viene verificato che il workflow abbia avuto successo
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.DIGITAL_SUCCESS_WORKFLOW.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .build())).isPresent());

        ArgumentCaptor<EndWorkflowStatus> endWorkflowStatusArgumentCaptor = ArgumentCaptor.forClass(EndWorkflowStatus.class);
        ArgumentCaptor<Integer> recIndexCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<NotificationInt> notificationCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> addressCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);

        Mockito.verify(completionWorkflow, Mockito.times(invocationsNumber)).completionDigitalWorkflow(
                notificationCaptor.capture(), recIndexCaptor.capture(), Mockito.any(Instant.class), addressCaptor.capture(),
                endWorkflowStatusArgumentCaptor.capture());

        List<EndWorkflowStatus> endWorkflowStatusArgumentCaptorValue = endWorkflowStatusArgumentCaptor.getAllValues();
        List<Integer> recIndexCaptorValue = recIndexCaptor.getAllValues();
        List<NotificationInt> notificationCaptorValue = notificationCaptor.getAllValues();
        List<LegalDigitalAddressInt> addressCaptorValue = addressCaptor.getAllValues();

        Assertions.assertEquals(recIndex, recIndexCaptorValue.get(invocation));
        Assertions.assertEquals(iun, notificationCaptorValue.get(invocation).getIun());
        Assertions.assertEquals(EndWorkflowStatus.SUCCESS, endWorkflowStatusArgumentCaptorValue.get(invocation));
        Assertions.assertEquals(address, addressCaptorValue.get(invocation));
    }

    public static void checkFailDigitalWorkflow(String iun, Integer recIndex, TimelineService timelineService, CompletionWorkFlowHandler completionWorkflow) {
        //Viene verificato che il workflow sia fallito
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.DIGITAL_FAILURE_WORKFLOW.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .build())).isPresent());

        ArgumentCaptor<EndWorkflowStatus> endWorkflowStatusArgumentCaptor = ArgumentCaptor.forClass(EndWorkflowStatus.class);
        ArgumentCaptor<NotificationInt> notificationCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<Integer> recIndexCaptor = ArgumentCaptor.forClass(Integer.class);

        Mockito.verify(completionWorkflow, Mockito.times(1)).completionDigitalWorkflow(
                notificationCaptor.capture(), recIndexCaptor.capture(), Mockito.any(Instant.class), Mockito.any(), endWorkflowStatusArgumentCaptor.capture()
        );
        Assertions.assertEquals(iun, notificationCaptor.getValue().getIun());
        Assertions.assertEquals(recIndex, recIndexCaptor.getValue());
        Assertions.assertEquals(EndWorkflowStatus.FAILURE, endWorkflowStatusArgumentCaptor.getValue());
    }

    public static void checkRefinement(String iun, Integer recIndex, TimelineService timelineService) {
        Assertions.assertTrue(timelineService.getTimelineElement(
                iun,
                TimelineEventId.REFINEMENT.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .build())).isPresent());
    }

    public static void checkExternalChannelPecSend(String iunExpected, String addressExpected, String iunValue, String addressValue) {
        Assertions.assertEquals(iunExpected, iunValue);
        //Assertions.assertEquals(taxIdExpected, taxIdValue);
        Assertions.assertEquals(addressExpected, addressValue);
    }
}