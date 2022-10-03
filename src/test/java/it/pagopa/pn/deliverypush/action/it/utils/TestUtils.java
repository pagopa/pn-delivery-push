package it.pagopa.pn.deliverypush.action.it.utils;

import it.pagopa.pn.deliverypush.action.CompletionWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock;
import it.pagopa.pn.deliverypush.action.it.mockbean.SafeStorageClientMock;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationDocumentInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusHistoryElementInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import lombok.Builder;
import lombok.Getter;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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


    public static void checkSendCourtesyAddresses(String iun, Integer recIndex, List<CourtesyDigitalAddressInt> courtesyAddresses, TimelineService timelineService, ExternalChannelMock externalChannelMock) {

        checkSendCourtesyAddressFromTimeline(iun, recIndex, courtesyAddresses, timelineService, externalChannelMock);
        //Viene verificato l'effettivo invio del messaggio di cortesia verso external channel
        Mockito.verify(externalChannelMock, Mockito.times(courtesyAddresses.size())).sendCourtesyNotification(
                    Mockito.any(NotificationInt.class),
                    Mockito.any(NotificationRecipientInt.class),
                    Mockito.any(CourtesyDigitalAddressInt.class),
                    Mockito.any(String.class)
                );
    }

    public static void checkSendCourtesyAddressFromTimeline(String iun, Integer recIndex, List<CourtesyDigitalAddressInt> courtesyAddresses, TimelineService timelineService, ExternalChannelMock externalChannelMock) {
        int index = 0;
        for (CourtesyDigitalAddressInt digitalAddress : courtesyAddresses) {
            String eventId = TimelineEventId.SEND_COURTESY_MESSAGE.buildEventId(
                    EventId.builder()
                            .iun(iun)
                            .recIndex(recIndex)
                            .index(index)
                            .build());
            Optional<SendCourtesyMessageDetailsInt> sendCourtesyMessageDetailsOpt = timelineService.getTimelineElementDetails(iun, eventId, SendCourtesyMessageDetailsInt.class);

            Assertions.assertTrue(sendCourtesyMessageDetailsOpt.isPresent());
            SendCourtesyMessageDetailsInt sendCourtesyMessageDetails = sendCourtesyMessageDetailsOpt.get();
            Assertions.assertEquals(digitalAddress.getAddress(), sendCourtesyMessageDetails.getDigitalAddress().getAddress());
            Assertions.assertEquals(digitalAddress.getType(), sendCourtesyMessageDetails.getDigitalAddress().getType());
            index++;
        }
    }

    public static void checkGetAddress(String iun, Integer recIndex, Boolean isAvailable, DigitalAddressSourceInt source, int sentAttempt, TimelineService timelineService) {
        String correlationId = TimelineEventId.GET_ADDRESS.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .source(source)
                        .index(sentAttempt)
                        .build());

        Optional<GetAddressInfoDetailsInt> getAddressInfoOpt = timelineService.getTimelineElementDetails(iun, correlationId, GetAddressInfoDetailsInt.class);
        Assertions.assertTrue(getAddressInfoOpt.isPresent());
        Assertions.assertEquals(isAvailable, getAddressInfoOpt.get().getIsAvailable());
    }

    public static void checkSendPaperToExtChannel(String iun, Integer recIndex, PhysicalAddressInt physicalAddress, int sendAttempt, TimelineService timelineService) {
        String eventIdFirstSend = TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .index(sendAttempt)
                        .build());

        Optional<SendAnalogDetailsInt> sendPaperDetailsOpt = timelineService.getTimelineElementDetails(iun, eventIdFirstSend,  SendAnalogDetailsInt.class);
        Assertions.assertTrue(sendPaperDetailsOpt.isPresent());
         SendAnalogDetailsInt sendPaperDetails = sendPaperDetailsOpt.get();
        Assertions.assertEquals(physicalAddress, sendPaperDetails.getPhysicalAddress());
    }

    public static void checkNotSendPaperToExtChannel(String iun, Integer recIndex, int sendAttempt, TimelineService timelineService) {
        String eventIdFirstSend = TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .index(sendAttempt)
                        .build());

        Optional< SendAnalogDetailsInt> sendPaperDetailsOpt = timelineService.getTimelineElementDetails(iun, eventIdFirstSend,  SendAnalogDetailsInt.class);
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
                notificationCaptor.capture(), recIndexCaptor.capture(), Mockito.any(), Mockito.any(Instant.class), Mockito.any(PhysicalAddressInt.class), endWorkflowStatusArgumentCaptor.capture()
        );
        Assertions.assertEquals(recIndex, recIndexCaptor.getValue());
        Assertions.assertEquals(iun, notificationCaptor.getValue().getIun());
        Assertions.assertEquals(EndWorkflowStatus.SUCCESS, endWorkflowStatusArgumentCaptor.getValue());
    }

    public static void checkSuccessDigitalWorkflow(String iun, Integer recIndex, TimelineService timelineService,
                                                   CompletionWorkFlowHandler completionWorkflow, LegalDigitalAddressInt address,
                                                   int invocationsNumber, int invocation) {
        //Viene verificato che il workflow abbia avuto successo
        checkSuccessDigitalWorkflowFromTimeline(iun, recIndex, address, timelineService);
        
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

    public static void checkSuccessDigitalWorkflowFromTimeline(String iun, Integer recIndex, LegalDigitalAddressInt address, TimelineService timelineService) {
        Optional<TimelineElementInternal> timelineElementOpt = timelineService.getTimelineElement(
                iun,
                TimelineEventId.DIGITAL_SUCCESS_WORKFLOW.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .build()));
        
        Assertions.assertTrue(timelineElementOpt.isPresent());
        TimelineElementInternal timelineElementInternal = timelineElementOpt.get();
        Assertions.assertEquals(address.getAddress(), ((DigitalSuccessWorkflowDetailsInt) timelineElementInternal.getDetails()).getDigitalAddress().getAddress());
    }

    public static void checkFailDigitalWorkflow(String iun, Integer recIndex, TimelineService timelineService, CompletionWorkFlowHandler completionWorkflow) {
        //Viene verificato che il workflow sia fallito
        Optional<TimelineElementInternal> timelineElementOpt = timelineService.getTimelineElement(
                iun,
                TimelineEventId.DIGITAL_FAILURE_WORKFLOW.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .build()));
                
        Assertions.assertTrue(timelineElementOpt.isPresent());
        TimelineElementInternal timelineElementInternal = timelineElementOpt.get();
        Assertions.assertNotNull(timelineElementInternal.getLegalFactsIds().get(0));
        
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

    public static void checkExternalChannelPecSendFromTimeline(String iun, int recIndex, int sendAttemptMade, LegalDigitalAddressInt digitalAddress,
                                                               DigitalAddressSourceInt addressSource, TimelineService timelineService) {
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
        Assertions.assertEquals( digitalAddress.getAddress(), ((SendDigitalDetailsInt) timelineElement.getDetails()).getDigitalAddress().getAddress() );
    }

    public static void checkIsPresentAcceptanceInTimeline(String iun, int recIndex, int sendAttemptMade, LegalDigitalAddressInt digitalAddress,
                                                               DigitalAddressSourceInt addressSource, TimelineService timelineService) {
        String timelineEventId = TimelineEventId.SEND_DIGITAL_PROGRESS.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .sentAttemptMade(sendAttemptMade)
                        .source(addressSource)
                        .progressIndex(1)
                        .build()
        );

        Optional<TimelineElementInternal> timelineElementInternal = timelineService.getTimelineElement(iun, timelineEventId);

        Assertions.assertTrue(timelineElementInternal.isPresent());
        TimelineElementInternal timelineElement = timelineElementInternal.get();
        Assertions.assertNotNull( timelineElement.getLegalFactsIds().get(0) );
        Assertions.assertNotNull( timelineElement.getTimestamp() );

        SendDigitalProgressDetailsInt details = (SendDigitalProgressDetailsInt) timelineElement.getDetails();
        Assertions.assertEquals( digitalAddress.getAddress(), details.getDigitalAddress().getAddress() );
    }

    public static void checkIsPresentDigitalFeedbackInTimeline(String iun, int recIndex, int sendAttemptMade, LegalDigitalAddressInt digitalAddress,
                                                               DigitalAddressSourceInt addressSource, TimelineService timelineService, ResponseStatusInt status) {
        String timelineEventId = TimelineEventId.SEND_DIGITAL_FEEDBACK.buildEventId(
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
        Assertions.assertNotNull( timelineElement.getLegalFactsIds().get(0) );
        Assertions.assertNotNull( timelineElement.getTimestamp() );

        SendDigitalFeedbackDetailsInt details = (SendDigitalFeedbackDetailsInt) timelineElement.getDetails();
        Assertions.assertEquals( digitalAddress.getAddress(), details.getDigitalAddress().getAddress() );
        Assertions.assertEquals(status, details.getResponseStatus());
    }
    
    public static NotificationStatusInt getNotificationStatus(NotificationInt notification, TimelineService timelineService, StatusUtils statusUtils){
        int numberOfRecipient = notification.getRecipients().size();
        Instant notificationCreatedAt = notification.getSentAt();

        Set<TimelineElementInternal> timelineElements = timelineService.getTimeline(notification.getIun(), true);
        
        List<NotificationStatusHistoryElementInt> statusHistoryElements = statusUtils.getStatusHistory(timelineElements, numberOfRecipient, notificationCreatedAt);

        return statusUtils.getCurrentStatus(statusHistoryElements);
    }

    public static void checkIsNotPresentRefinement(String iun, Integer recIndex, TimelineService timelineService) {
        Assertions.assertFalse(timelineService.getTimelineElement(
                iun,
                TimelineEventId.REFINEMENT.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .build())
        ).isPresent());
    }


    public static void checkIsPresentRefinement(String iun, Integer recIndex, TimelineService timelineService) {
        Optional<TimelineElementInternal> timelineElementOpt = timelineService.getTimelineElement(
                iun,
                TimelineEventId.REFINEMENT.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .build()));

        Assertions.assertTrue(timelineElementOpt.isPresent());
        TimelineElementInternal timelineElement = timelineElementOpt.get();
        RefinementDetailsInt detailsInt = (RefinementDetailsInt) timelineElement.getDetails();
        Assertions.assertNotNull(detailsInt.getNotificationCost());
    }

    public static void checkSendRegisteredLetter(NotificationRecipientInt recipient, String iun, Integer recIndex, ExternalChannelMock externalChannelMock, TimelineService timelineService) {
        ArgumentCaptor<PhysicalAddressInt> pnPhysicalAddressArgumentCaptor = ArgumentCaptor.forClass(PhysicalAddressInt.class);
        ArgumentCaptor<NotificationInt> pnNotificationIntArgumentCaptor = ArgumentCaptor.forClass(NotificationInt.class);

        Mockito.verify(externalChannelMock).sendAnalogNotification(pnNotificationIntArgumentCaptor.capture(), Mockito.any(NotificationRecipientInt.class), pnPhysicalAddressArgumentCaptor.capture(), Mockito.anyString(), Mockito.any(), Mockito.anyString());
        PhysicalAddressInt physicalAddress = pnPhysicalAddressArgumentCaptor.getValue();
        NotificationInt notificationInt = pnNotificationIntArgumentCaptor.getValue();

        Assertions.assertEquals(iun, notificationInt.getIun());
        Assertions.assertEquals(recipient.getPhysicalAddress().getAddress(), physicalAddress.getAddress());

        //Viene verificato l'invio della registered letter da timeline
        String eventId = TimelineEventId.SEND_SIMPLE_REGISTERED_LETTER.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        Optional<SimpleRegisteredLetterDetailsInt> sendSimpleRegisteredLetterOpt = timelineService.getTimelineElementDetails(iun, eventId, SimpleRegisteredLetterDetailsInt.class);
        Assertions.assertTrue(sendSimpleRegisteredLetterOpt.isPresent());

        SimpleRegisteredLetterDetailsInt simpleRegisteredLetterDetails = sendSimpleRegisteredLetterOpt.get();
        Assertions.assertEquals( recipient.getPhysicalAddress().getAddress(), simpleRegisteredLetterDetails.getPhysicalAddress().getAddress() );
        Assertions.assertEquals( recipient.getPhysicalAddress().getForeignState() , simpleRegisteredLetterDetails.getPhysicalAddress().getForeignState());
        Assertions.assertEquals(1, simpleRegisteredLetterDetails.getNumberOfPages());
    }

    public static void firstFileUploadFromNotification(NotificationInt notification, SafeStorageClientMock safeStorageClientMock){
        for(NotificationDocumentInt attachment : notification.getDocuments()) {
            FileCreationWithContentRequest fileCreationWithContentRequest = new FileCreationWithContentRequest();
            fileCreationWithContentRequest.setContentType("application/pdf");
            fileCreationWithContentRequest.setContent(attachment.getDigests().getSha256().getBytes(StandardCharsets.UTF_8));
            safeStorageClientMock.createFile(fileCreationWithContentRequest, attachment.getDigests().getSha256());
        }
    }

    public static void firstFileUploadFromNotificationError(NotificationInt notification, SafeStorageClientMock safeStorageClientMock, byte[] fileSha ){
        for(NotificationDocumentInt attachment : notification.getDocuments()) {
            FileCreationWithContentRequest fileCreationWithContentRequest = new FileCreationWithContentRequest();
            fileCreationWithContentRequest.setContentType("application/pdf");
            fileCreationWithContentRequest.setContent(fileSha);
            safeStorageClientMock.createFile(fileCreationWithContentRequest, attachment.getDigests().getSha256());
        }
    }

    public static void writeAllGeneratedLegalFacts(String iun, String className, TimelineService timelineService, SafeStorageClientMock safeStorageClientMock) {
        String testName = className + "-" + getMethodName(3);

        timelineService.getTimeline(iun, true).forEach(
                elem -> {
                    if (! elem.getLegalFactsIds().isEmpty() ){
                        LegalFactsIdInt legalFactsId = elem.getLegalFactsIds().get(0);
                        if( !LegalFactCategoryInt.PEC_RECEIPT.equals(legalFactsId.getCategory()) && !LegalFactCategoryInt.ANALOG_DELIVERY.equals(legalFactsId.getCategory())){
                            String key = legalFactsId.getKey().replace("safestorage://", "");
                            safeStorageClientMock.writeFile(key, legalFactsId.getCategory(), testName);
                        }
                    }
                }
        );
    }

    public static void checkGeneratedLegalFacts(NotificationInt notification,
                                                NotificationRecipientInt recipient,
                                                Integer recIndex,
                                                int sentPecAttemptNumber,
                                                TestUtils.GeneratedLegalFactsInfo generatedLegalFactsInfo,
                                                EndWorkflowStatus endWorkflowStatus,
                                                LegalFactGenerator legalFactGenerator,
                                                TimelineService timelineService
    ) {
        TestUtils.checkNotificationReceivedLegalFactGeneration(
                notification,
                legalFactGenerator,
                generatedLegalFactsInfo.isNotificationReceivedLegalFactGenerated()
        );

        TestUtils.generateNotificationAAR(
                notification,
                recipient,
                legalFactGenerator,
                generatedLegalFactsInfo.isNotificationAARGenerated()
        );

        TestUtils.checkNotificationViewedLegalFact(
                notification.getIun(),
                recipient,
                legalFactGenerator,
                generatedLegalFactsInfo.isNotificationViewedLegalFactGenerated()
        );

        TestUtils.checkPecDeliveryWorkflowLegalFactsGeneration(
                notification,
                timelineService,
                recipient,
                recIndex,
                sentPecAttemptNumber,
                endWorkflowStatus,
                legalFactGenerator,
                generatedLegalFactsInfo.isPecDeliveryWorkflowLegalFactsGenerated()
        );
    }

    private static int getTimes(boolean itWasGenerated) {
        return itWasGenerated ? 1 : 0;
    }

    private static void checkNotificationReceivedLegalFactGeneration(NotificationInt notification,
                                                                    LegalFactGenerator legalFactGenerator,
                                                                    boolean itWasGenerated){
        int times = getTimes(itWasGenerated);
        
        try {
            Mockito.verify(legalFactGenerator, Mockito.times(times)).generateNotificationReceivedLegalFact(notification);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void checkNotificationViewedLegalFact(String iun,
                                                        NotificationRecipientInt recipient,
                                                        LegalFactGenerator legalFactGenerator,
                                                        boolean itWasGenerated){
        int times = getTimes(itWasGenerated);

        try {
            Mockito.verify(legalFactGenerator, Mockito.times(times)).generateNotificationViewedLegalFact(Mockito.eq(iun), Mockito.eq(recipient), Mockito.any(Instant.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void generateNotificationAAR(NotificationInt notification,
                                               NotificationRecipientInt recipient,
                                               LegalFactGenerator legalFactGenerator,
                                               boolean itWasGenerated){
        int times = getTimes(itWasGenerated);

        try {
            Mockito.verify(legalFactGenerator, Mockito.times(times)).generateNotificationAAR(notification, recipient);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void checkPecDeliveryWorkflowLegalFactsGeneration(NotificationInt notification,
                                                                    TimelineService timelineService,
                                                                    NotificationRecipientInt recipient,
                                                                    Integer recIndex,
                                                                    int sentPecAttemptNumber,
                                                                    EndWorkflowStatus endWorkflowStatus,
                                                                    LegalFactGenerator legalFactGenerator,
                                                                    boolean itWasGenerated
    ) {
        int times = getTimes(itWasGenerated);
        
        String eventId = TimelineEventId.SEND_SIMPLE_REGISTERED_LETTER.buildEventId(
                EventId.builder()
                        .iun(notification.getIun())
                        .recIndex(recIndex)
                        .build()
        );

        Optional<PhysicalAddressInt> optionalPhysicalAddress =
                timelineService.getTimelineElementDetails(notification.getIun(), eventId, SimpleRegisteredLetterDetailsInt.class ).map(
                        SimpleRegisteredLetterDetailsInt::getPhysicalAddress
                );

        ArgumentCaptor<List<SendDigitalFeedbackDetailsInt>> sendDigitalFeedbackCaptor = ArgumentCaptor.forClass(List.class);

        try {
            Mockito.verify(legalFactGenerator, Mockito.times(times)).generatePecDeliveryWorkflowLegalFact(sendDigitalFeedbackCaptor.capture(), Mockito.eq(notification),
                    Mockito.eq(recipient), Mockito.eq(endWorkflowStatus), Mockito.any(Instant.class), Mockito.eq(optionalPhysicalAddress.orElse(null)) );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        if(itWasGenerated){
            List<SendDigitalFeedbackDetailsInt> listSendDigitalFeedbackDetail = sendDigitalFeedbackCaptor.getValue();

            Assertions.assertEquals(sentPecAttemptNumber, listSendDigitalFeedbackDetail.size());
        }
    }
    
    public static String getMethodName(final int depth) {
        final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
        return ste[depth].getMethodName();
    }
    
    @Builder
    @Getter
    public static class GeneratedLegalFactsInfo{
        boolean notificationReceivedLegalFactGenerated;
        boolean notificationAARGenerated;
        boolean notificationViewedLegalFactGenerated;
        boolean pecDeliveryWorkflowLegalFactsGenerated;
    }
}
