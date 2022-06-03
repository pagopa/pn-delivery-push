package it.pagopa.pn.deliverypush.action2.it.utils;

import it.pagopa.pn.api.dto.events.PnExtChnEmailEvent;
import it.pagopa.pn.api.dto.events.PnExtChnPecEvent;
import it.pagopa.pn.deliverypush.action2.CompletionWorkFlowHandler;
import it.pagopa.pn.deliverypush.action2.it.mockbean.ExternalChannelMock;
import it.pagopa.pn.deliverypush.action2.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.util.StatusUtils;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class TestUtils {

    public static final String EXTERNAL_CHANNEL_ANALOG_FAILURE_ATTEMPT = "EXTERNAL_CHANNEL_ANALOG_FAILURE_ATTEMPT";
    public static final String INVESTIGATION_ADDRESS_PRESENT_FAILURE = "INVESTIGATION_ADDRESS_PRESENT_FAILURE";
    public static final String INVESTIGATION_ADDRESS_PRESENT_POSITIVE = "INVESTIGATION_ADDRESS_PRESENT_POSITIVE";

    public static final String PUBLIC_REGISTRY_FAIL_GET_DIGITAL_ADDRESS = "PUBLIC_REGISTRY_FAIL_GET_DIGITAL_ADDRESS";
    public static final String PUBLIC_REGISTRY_FAIL_GET_ANALOG_ADDRESS = "PUBLIC_REGISTRY_FAIL_GET_ANALOG_ADDRESS";


    public static void checkSendCourtesyAddresses(String iun, Integer recIndex, List<DigitalAddress> courtesyAddresses, TimelineService timelineService, ExternalChannelMock externalChannelMock) {

        checkSendCourtesyAddressFromTimeline(iun, recIndex, courtesyAddresses, timelineService);
        //Viene verificato l'effettivo invio del messaggio di cortesia verso external channel
        Mockito.verify(externalChannelMock, Mockito.times(courtesyAddresses.size())).sendNotification(Mockito.any(PnExtChnEmailEvent.class));
    }

    public static void checkSendCourtesyAddressFromTimeline(String iun, Integer recIndex, List<DigitalAddress> courtesyAddresses, TimelineService timelineService) {
        int index = 0;
        for (DigitalAddress digitalAddress : courtesyAddresses) {
            String eventId = TimelineEventId.SEND_COURTESY_MESSAGE.buildEventId(
                    EventId.builder()
                            .iun(iun)
                            .recIndex(recIndex)
                            .index(index)
                            .build());
            Optional<SendCourtesyMessageDetails> sendCourtesyMessageDetailsOpt = timelineService.getTimelineElementDetails(iun, eventId, SendCourtesyMessageDetails.class);

            Assertions.assertTrue(sendCourtesyMessageDetailsOpt.isPresent());
            SendCourtesyMessageDetails sendCourtesyMessageDetails = sendCourtesyMessageDetailsOpt.get();
            Assertions.assertEquals(digitalAddress, sendCourtesyMessageDetails.getDigitalAddress());
            index++;
        }
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

    public static void checkNotSendPaperToExtChannel(String iun, Integer recIndex, int sendAttempt, TimelineService timelineService) {
        String eventIdFirstSend = TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .index(sendAttempt)
                        .build());

        Optional<SendPaperDetails> sendPaperDetailsOpt = timelineService.getTimelineElementDetails(iun, eventIdFirstSend, SendPaperDetails.class);
        Assertions.assertFalse(sendPaperDetailsOpt.isPresent());
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
                                                   CompletionWorkFlowHandler completionWorkflow, DigitalAddress address,
                                                   int invocationsNumber, int invocation) {
        //Viene verificato che il workflow abbia avuto successo
        checkSuccessDigitalWorkflowFromTimeline(iun, recIndex, address, timelineService);
        
        ArgumentCaptor<EndWorkflowStatus> endWorkflowStatusArgumentCaptor = ArgumentCaptor.forClass(EndWorkflowStatus.class);
        ArgumentCaptor<Integer> recIndexCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<NotificationInt> notificationCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<DigitalAddress> addressCaptor = ArgumentCaptor.forClass(DigitalAddress.class);

        Mockito.verify(completionWorkflow, Mockito.times(invocationsNumber)).completionDigitalWorkflow(
                notificationCaptor.capture(), recIndexCaptor.capture(), Mockito.any(Instant.class), addressCaptor.capture(),
                endWorkflowStatusArgumentCaptor.capture());

        List<EndWorkflowStatus> endWorkflowStatusArgumentCaptorValue = endWorkflowStatusArgumentCaptor.getAllValues();
        List<Integer> recIndexCaptorValue = recIndexCaptor.getAllValues();
        List<NotificationInt> notificationCaptorValue = notificationCaptor.getAllValues();
        List<DigitalAddress> addressCaptorValue = addressCaptor.getAllValues();

        Assertions.assertEquals(recIndex, recIndexCaptorValue.get(invocation));
        Assertions.assertEquals(iun, notificationCaptorValue.get(invocation).getIun());
        Assertions.assertEquals(EndWorkflowStatus.SUCCESS, endWorkflowStatusArgumentCaptorValue.get(invocation));
        Assertions.assertEquals(address, addressCaptorValue.get(invocation));
    }

    public static void checkSuccessDigitalWorkflowFromTimeline(String iun, Integer recIndex, DigitalAddress address, TimelineService timelineService) {
        Optional<TimelineElementInternal> timelineElementOpt = timelineService.getTimelineElement(
                iun,
                TimelineEventId.DIGITAL_SUCCESS_WORKFLOW.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .build()));
        
        Assertions.assertTrue(timelineElementOpt.isPresent());
        TimelineElementInternal timelineElementInternal = timelineElementOpt.get();
        Assertions.assertEquals(address, timelineElementInternal.getDetails().getDigitalAddress());
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

    public static void checkExternalChannelPecSend(String iun, String taxId, List<PnExtChnPecEvent> sendPecEvent, int sendTime, String address) {
        Assertions.assertEquals(iun, sendPecEvent.get(sendTime).getPayload().getIun());
        Assertions.assertEquals(taxId, sendPecEvent.get(sendTime).getPayload().getRecipientTaxId());
        Assertions.assertEquals(address, sendPecEvent.get(sendTime).getPayload().getPecAddress());
    }

    public static void checkExternalChannelPecSendFromTimeline(String iun, int recIndex, int sendAttemptMade, DigitalAddress digitalAddress, 
                                                               DigitalAddressSource addressSource, TimelineService timelineService) {
        String timelineEventId = TimelineEventId.SEND_DIGITAL_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .index(sendAttemptMade)
                        .source(addressSource)
                        .build()
        );

        Optional<TimelineElementInternal> timelineElementInternal = timelineService.getTimelineElement(iun, timelineEventId);
        
        Assertions.assertTrue(timelineElementInternal.isPresent());
        TimelineElementInternal timelineElement = timelineElementInternal.get();
        Assertions.assertEquals( digitalAddress, timelineElement.getDetails().getDigitalAddress() );
    }
    
    public static NotificationStatus getNotificationStatus(NotificationInt notification, TimelineService timelineService, StatusUtils statusUtils){
        int numberOfRecipient = notification.getRecipients().size();
        Instant notificationCreatedAt = notification.getSentAt();

        Set<TimelineElementInternal> timelineElements = timelineService.getTimeline(notification.getIun());
        
        List<NotificationStatusHistoryElement> statusHistoryElements = statusUtils.getStatusHistory(timelineElements, numberOfRecipient, notificationCreatedAt);

        return statusUtils.getCurrentStatus(statusHistoryElements);
    }
}