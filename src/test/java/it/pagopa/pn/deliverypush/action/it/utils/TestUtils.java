package it.pagopa.pn.deliverypush.action.it.utils;

import it.pagopa.pn.commons.utils.DateFormatUtils;
import it.pagopa.pn.deliverypush.action.completionworkflow.CompletionWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.it.mockbean.*;
import it.pagopa.pn.deliverypush.action.utils.EndWorkflowStatus;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.address.CourtesyDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressSourceInt;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.cost.PaymentsInfoForRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.*;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusHistoryElementInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.ext.safestorage.FileCreationWithContentRequest;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactCategoryInt;
import it.pagopa.pn.deliverypush.dto.legalfacts.LegalFactsIdInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.timeline.EventId;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineEventId;
import it.pagopa.pn.deliverypush.dto.timeline.details.*;
import it.pagopa.pn.deliverypush.legalfacts.LegalFactGenerator;
import it.pagopa.pn.deliverypush.logtest.ConsoleAppenderCustom;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.paperchannel.PaperChannelSendRequest;
import it.pagopa.pn.deliverypush.middleware.queue.producer.abstractions.actionspool.ActionType;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.StatusUtils;
import it.pagopa.pn.deliverypush.utils.ThreadPool;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.util.Base64Utils;

import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class TestUtils {
    public static final String PN_NOTIFICATION_ATTACHMENT = "PN_NOTIFICATION_ATTACHMENT";
    public static final String TOO_BIG = "TOO_BIG";
    public static final String NOT_A_PDF = "NOT_A_PDF";


    public static void checkSendCourtesyAddresses(String iun, Integer recIndex, List<CourtesyDigitalAddressInt> courtesyAddresses, TimelineService timelineService, ExternalChannelMock externalChannelMock) {

        checkSendCourtesyAddressFromTimeline(iun, recIndex, courtesyAddresses, timelineService);
        //Viene verificato l'effettivo invio del messaggio di cortesia verso external channel
        Mockito.verify(externalChannelMock, Mockito.times(courtesyAddresses.size())).sendCourtesyNotification(
                Mockito.any(NotificationInt.class),
                Mockito.any(NotificationRecipientInt.class),
                Mockito.any(CourtesyDigitalAddressInt.class),
                Mockito.any(String.class),
                Mockito.anyString(),
                Mockito.anyString()
        );
    }

    public static void checkSendCourtesyAddressFromTimeline(String iun, Integer recIndex, List<CourtesyDigitalAddressInt> courtesyAddresses, TimelineService timelineService) {
        for (CourtesyDigitalAddressInt digitalAddress : courtesyAddresses) {
            String eventId = TimelineEventId.SEND_COURTESY_MESSAGE.buildEventId(
                    EventId.builder()
                            .iun(iun)
                            .recIndex(recIndex)
                            .courtesyAddressType(digitalAddress.getType())
                            .build());
            Optional<SendCourtesyMessageDetailsInt> sendCourtesyMessageDetailsOpt = timelineService.getTimelineElementDetails(iun, eventId, SendCourtesyMessageDetailsInt.class);

            Assertions.assertTrue(sendCourtesyMessageDetailsOpt.isPresent());
            SendCourtesyMessageDetailsInt sendCourtesyMessageDetails = sendCourtesyMessageDetailsOpt.get();
            Assertions.assertEquals(digitalAddress.getAddress(), sendCourtesyMessageDetails.getDigitalAddress().getAddress());
            Assertions.assertEquals(digitalAddress.getType(), sendCourtesyMessageDetails.getDigitalAddress().getType());
        }
    }

    public static void checkGetAddress(String iun, Integer recIndex, Boolean isAvailable, DigitalAddressSourceInt source, int sentAttempt, TimelineService timelineService) {
        String correlationId = TimelineEventId.GET_ADDRESS.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .source(source)
                        .sentAttemptMade(sentAttempt)
                        .build());

        Optional<GetAddressInfoDetailsInt> getAddressInfoOpt = timelineService.getTimelineElementDetails(iun, correlationId, GetAddressInfoDetailsInt.class);
        if(isAvailable){
            Assertions.assertTrue(getAddressInfoOpt.isPresent());
            Assertions.assertEquals(true, getAddressInfoOpt.get().getIsAvailable());
        } else {
            Assertions.assertTrue(getAddressInfoOpt.isEmpty() || !getAddressInfoOpt.get().getIsAvailable());
        }
    }

    public static void checkSendPaperToExtChannel(String iun, Integer recIndex, PhysicalAddressInt physicalAddress, int sendAttempt, TimelineService timelineService) {
        String eventIdFirstSend = TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .sentAttemptMade(sendAttempt)
                        .build());

        Optional<SendAnalogDetailsInt> sendPaperDetailsOpt = timelineService.getTimelineElementDetails(iun, eventIdFirstSend, SendAnalogDetailsInt.class);
        Assertions.assertTrue(sendPaperDetailsOpt.isPresent());
        SendAnalogDetailsInt sendPaperDetails = sendPaperDetailsOpt.get();
        Assertions.assertEquals(physicalAddress.getAddress(), sendPaperDetails.getPhysicalAddress().getAddress());
    }

    public static void checkNotSendPaperToExtChannel(String iun, Integer recIndex, int sendAttempt, TimelineService timelineService) {
        String eventIdFirstSend = TimelineEventId.SEND_ANALOG_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .sentAttemptMade(sendAttempt)
                        .build());

        Optional<SendAnalogDetailsInt> sendPaperDetailsOpt = timelineService.getTimelineElementDetails(iun, eventIdFirstSend, SendAnalogDetailsInt.class);
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
                notificationCaptor.capture(), recIndexCaptor.capture(), Mockito.any(Instant.class), Mockito.any(PhysicalAddressInt.class), endWorkflowStatusArgumentCaptor.capture()
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

        ArgumentCaptor<Integer> recIndexCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<NotificationInt> notificationCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<LegalDigitalAddressInt> addressCaptor = ArgumentCaptor.forClass(LegalDigitalAddressInt.class);

        Mockito.verify(completionWorkflow, Mockito.times(invocationsNumber)).completionSuccessDigitalWorkflow(
                notificationCaptor.capture(), recIndexCaptor.capture(), Mockito.any(Instant.class), addressCaptor.capture());

        List<Integer> recIndexCaptorValue = recIndexCaptor.getAllValues();
        List<NotificationInt> notificationCaptorValue = notificationCaptor.getAllValues();
        List<LegalDigitalAddressInt> addressCaptorValue = addressCaptor.getAllValues();

        Assertions.assertEquals(recIndex, recIndexCaptorValue.get(invocation));
        Assertions.assertEquals(iun, notificationCaptorValue.get(invocation).getIun());
        Assertions.assertEquals(address, addressCaptorValue.get(invocation));
    }

    public static boolean checkSuccessDigitalWorkflowFromTimeline(String iun, Integer recIndex, LegalDigitalAddressInt address, TimelineService timelineService) {
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
        return true;
    }

    public static void checkFailDigitalWorkflow(String iun, Integer recIndex, TimelineService timelineService, CompletionWorkFlowHandler completionWorkflow) {
        //Viene verificato che il workflow sia fallito
        checkInTimlineIsFailedDigitalWorkflow(iun, recIndex, timelineService);

        ArgumentCaptor<NotificationInt> notificationCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<Integer> recIndexCaptor = ArgumentCaptor.forClass(Integer.class);

        Mockito.verify(completionWorkflow, Mockito.times(1)).completionFailureDigitalWorkflow(
                notificationCaptor.capture(), recIndexCaptor.capture()
        );
        Assertions.assertEquals(iun, notificationCaptor.getValue().getIun());
        Assertions.assertEquals(recIndex, recIndexCaptor.getValue());
    }

    public static void checkFailDigitalWorkflowMultiRec(String iun, Integer recIndex, int numberOfCompletedWorkflow, TimelineService timelineService, CompletionWorkFlowHandler completionWorkflow) {
        //Viene verificato che il workflow sia fallito
        checkInTimlineIsFailedDigitalWorkflow(iun, recIndex, timelineService);

        ArgumentCaptor<NotificationInt> notificationCaptor = ArgumentCaptor.forClass(NotificationInt.class);
        ArgumentCaptor<Integer> recIndexCaptor = ArgumentCaptor.forClass(Integer.class);

        Mockito.verify(completionWorkflow, Mockito.times(numberOfCompletedWorkflow)).completionFailureDigitalWorkflow(
                notificationCaptor.capture(), recIndexCaptor.capture()
        );
    }

    private static void checkInTimlineIsFailedDigitalWorkflow(String iun, Integer recIndex, TimelineService timelineService) {
        Optional<TimelineElementInternal> failDigitalWorkflowOpt = timelineService.getTimelineElement(
                iun,
                TimelineEventId.DIGITAL_FAILURE_WORKFLOW.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .build()));

        Assertions.assertTrue(failDigitalWorkflowOpt.isPresent());
        TimelineElementInternal failDigitalWorkflow = failDigitalWorkflowOpt.get();
        Assertions.assertNotNull(failDigitalWorkflow.getLegalFactsIds().get(0));
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

        Boolean isFirstRetry = isPossibileCaseToRepeat(addressSource, sendAttemptMade);

        String timelineEventId = TimelineEventId.SEND_DIGITAL_DOMICILE.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .sentAttemptMade(sendAttemptMade)
                        .source(addressSource)
                        .isFirstSendRetry(isFirstRetry)
                        .build()
        );

        Optional<TimelineElementInternal> timelineElementInternal = timelineService.getTimelineElement(iun, timelineEventId);

        Assertions.assertTrue(timelineElementInternal.isPresent());
        TimelineElementInternal timelineElement = timelineElementInternal.get();
        Assertions.assertEquals(digitalAddress.getAddress(), ((SendDigitalDetailsInt) timelineElement.getDetails()).getDigitalAddress().getAddress());
    }

    private static boolean isPossibileCaseToRepeat(DigitalAddressSourceInt digitalAddressSource, int sentAttemptMade) {
        return (DigitalAddressSourceInt.PLATFORM.equals(digitalAddressSource) ||
                DigitalAddressSourceInt.GENERAL.equals(digitalAddressSource))
                &&
                sentAttemptMade == 1;
    }

    public static void checkIsPresentAcceptanceInTimeline(String iun, int recIndex, int sendAttemptMade, LegalDigitalAddressInt digitalAddress,
                                                          DigitalAddressSourceInt addressSource, TimelineService timelineService) {

        Boolean isFirstRetry = isPossibileCaseToRepeat(addressSource, sendAttemptMade);
        checkAcceptance(iun, recIndex, sendAttemptMade, digitalAddress, addressSource, timelineService, isFirstRetry);
    }

    public static void checkAcceptance(String iun, int recIndex, int sendAttemptMade, LegalDigitalAddressInt digitalAddress, DigitalAddressSourceInt addressSource, TimelineService timelineService, Boolean isFirstRetry) {
        String timelineEventId = TimelineEventId.SEND_DIGITAL_PROGRESS.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .isFirstSendRetry(isFirstRetry)
                        .sentAttemptMade(sendAttemptMade)
                        .source(addressSource)
                        .progressIndex(1)
                        .build()
        );

        Optional<TimelineElementInternal> timelineElementInternal = timelineService.getTimelineElement(iun, timelineEventId);

        Assertions.assertTrue(timelineElementInternal.isPresent());
        TimelineElementInternal timelineElement = timelineElementInternal.get();
        Assertions.assertNotNull(timelineElement.getLegalFactsIds().get(0));
        Assertions.assertNotNull(timelineElement.getTimestamp());

        SendDigitalProgressDetailsInt details = (SendDigitalProgressDetailsInt) timelineElement.getDetails();
        Assertions.assertEquals(digitalAddress.getAddress(), details.getDigitalAddress().getAddress());
    }

    public static void checkIsPresentDigitalFeedbackInTimeline(String iun, int recIndex, int sendAttemptMade, LegalDigitalAddressInt digitalAddress,
                                                               DigitalAddressSourceInt addressSource, TimelineService timelineService, ResponseStatusInt status) {

        Boolean isFirstRetry = isPossibileCaseToRepeat(addressSource, sendAttemptMade);

        checkDigitalFeedback(iun, recIndex, sendAttemptMade, digitalAddress, addressSource, timelineService, status, isFirstRetry);
    }

    public static void checkDigitalFeedback(String iun, int recIndex, int sendAttemptMade, LegalDigitalAddressInt digitalAddress, DigitalAddressSourceInt addressSource, TimelineService timelineService, ResponseStatusInt status, Boolean isFirstRetry) {
        String timelineEventId = TimelineEventId.SEND_DIGITAL_FEEDBACK.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .sentAttemptMade(sendAttemptMade)
                        .source(addressSource)
                        .isFirstSendRetry(isFirstRetry)
                        .build()
        );

        Optional<TimelineElementInternal> timelineElementInternal = timelineService.getTimelineElement(iun, timelineEventId);

        Assertions.assertTrue(timelineElementInternal.isPresent());
        TimelineElementInternal timelineElement = timelineElementInternal.get();
        Assertions.assertNotNull(timelineElement.getLegalFactsIds().get(0));
        Assertions.assertNotNull(timelineElement.getTimestamp());

        SendDigitalFeedbackDetailsInt details = (SendDigitalFeedbackDetailsInt) timelineElement.getDetails();
        Assertions.assertEquals(digitalAddress.getAddress(), details.getDigitalAddress().getAddress());
        Assertions.assertEquals(status, details.getResponseStatus());
    }

    public synchronized static NotificationStatusInt getNotificationStatus(NotificationInt notification, TimelineService timelineService, StatusUtils statusUtils) {
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

    public static boolean checkIsPresentViewed(String iun, Integer recIndex, TimelineService timelineService) {
        Optional<TimelineElementInternal> timelineElementOpt = timelineService.getTimelineElement(
                iun,
                TimelineEventId.NOTIFICATION_VIEWED.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .build()));

        Assertions.assertTrue(timelineElementOpt.isPresent());

        return true;
    }

    public static boolean checkIsPresentRefinement(String iun, Integer recIndex, TimelineService timelineService) {
        Optional<TimelineElementInternal> timelineElementOpt = getRefinement(iun, recIndex, timelineService);

        Assertions.assertTrue(timelineElementOpt.isPresent());
        TimelineElementInternal timelineElement = timelineElementOpt.get();
        RefinementDetailsInt detailsInt = (RefinementDetailsInt) timelineElement.getDetails();
        Assertions.assertNotNull(detailsInt.getNotificationCost());

        return true;
    }

    public static Optional<TimelineElementInternal> getRefinement(String iun, Integer recIndex, TimelineService timelineService) {
        return timelineService.getTimelineElement(
                iun,
                TimelineEventId.REFINEMENT.buildEventId(
                        EventId.builder()
                                .iun(iun)
                                .recIndex(recIndex)
                                .build()));
    }

    public static void checkFailureRefinement(String iun,
                                              Integer recIndex,
                                              int refinementNumberOfInvocation,
                                              TimelineService timelineService,
                                              SchedulerService scheduler,
                                              PnDeliveryPushConfigs pnDeliveryPushConfigs) {
        ArgumentCaptor<Instant> instantArgumentCaptor = ArgumentCaptor.forClass(Instant.class);

        Mockito.verify(scheduler, Mockito.times(refinementNumberOfInvocation)).scheduleEvent(Mockito.eq(iun), Mockito.eq(recIndex), instantArgumentCaptor.capture(), Mockito.any(ActionType.class));
        List<Instant> instantArgumentCaptorList = instantArgumentCaptor.getAllValues();
        //Viene ottenuta la data di perfezionamento (Valutare se inserire la data di scheduling come campo del timeline element details)
        Instant refinementDate = instantArgumentCaptorList.get(instantArgumentCaptorList.size() - 1);

        List<TimelineElementInternal> lastSendDigitalElementList = timelineService.getTimeline(iun, false).stream()
                .filter(element -> TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE.equals(element.getCategory()))
                .sorted(Comparator.comparing(TimelineElementInternal::getTimestamp)).collect(Collectors.toList());

        Instant lastSendDigitalDate = lastSendDigitalElementList.get(lastSendDigitalElementList.size() - 1).getTimestamp();
        //Viene ottenuta la data dell'ultimo invio verso externalChannel
        ZonedDateTime notificationDateTime = DateFormatUtils.parseInstantToZonedDateTime(lastSendDigitalDate);

        ZonedDateTime schedulingDate = notificationDateTime.plus(pnDeliveryPushConfigs.getTimeParams().getSchedulingDaysFailureDigitalRefinement());

        //Viene verificato che la data di perfezionamento sia uguale alla data dell'ultimo invio + giorni previsti dal perfezionamento
        Assertions.assertEquals(schedulingDate.toInstant(), refinementDate);
    }


    public static void checkSendRegisteredLetter(NotificationRecipientInt recipient, String iun, Integer recIndex, PaperChannelMock paperChannelMock, TimelineService timelineService) {
        ArgumentCaptor<PaperChannelSendRequest> paperChannelSendRequestArgumentCaptor = ArgumentCaptor.forClass(PaperChannelSendRequest.class);

        Mockito.verify(paperChannelMock).send(paperChannelSendRequestArgumentCaptor.capture());
        PaperChannelSendRequest paperChannelSendRequest = paperChannelSendRequestArgumentCaptor.getValue();

        Assertions.assertEquals(iun, paperChannelSendRequest.getNotificationInt().getIun());
        Assertions.assertEquals(recipient.getPhysicalAddress().getAddress(), paperChannelSendRequest.getReceiverAddress().getAddress());

        //Viene verificato l'invio della registered letter da timeline
        String eventId = TimelineEventId.SEND_SIMPLE_REGISTERED_LETTER.buildEventId(
                EventId.builder()
                        .iun(iun)
                        .recIndex(recIndex)
                        .build());

        Optional<SimpleRegisteredLetterDetailsInt> sendSimpleRegisteredLetterOpt = timelineService.getTimelineElementDetails(iun, eventId, SimpleRegisteredLetterDetailsInt.class);
        Assertions.assertTrue(sendSimpleRegisteredLetterOpt.isPresent());

        SimpleRegisteredLetterDetailsInt simpleRegisteredLetterDetails = sendSimpleRegisteredLetterOpt.get();
        Assertions.assertEquals(recipient.getPhysicalAddress().getAddress(), simpleRegisteredLetterDetails.getPhysicalAddress().getAddress());

    }

    public static void firstFileUploadFromNotification(List<TestUtils.DocumentWithContent> documentWithContentList, SafeStorageClientMock safeStorageClientMock) {
        for (TestUtils.DocumentWithContent documentWithContent : documentWithContentList) {
            FileCreationWithContentRequest fileCreationWithContentRequest = new FileCreationWithContentRequest();
            fileCreationWithContentRequest.setContentType("application/pdf");
            fileCreationWithContentRequest.setDocumentType(PN_NOTIFICATION_ATTACHMENT);
            fileCreationWithContentRequest.setContent(documentWithContent.getContent().getBytes());
            safeStorageClientMock.createFile(fileCreationWithContentRequest, documentWithContent.getDocument().getDigests().getSha256()).block();
        }
    }


    public static void firstFileUploadFromNotificationTooBig(List<TestUtils.DocumentWithContent> documentWithContentList, SafeStorageClientMock safeStorageClientMock) {
        for (TestUtils.DocumentWithContent documentWithContent : documentWithContentList) {
            FileCreationWithContentRequest fileCreationWithContentRequest = new FileCreationWithContentRequest();
            fileCreationWithContentRequest.setContentType("application/pdf" + TOO_BIG);
            fileCreationWithContentRequest.setDocumentType(PN_NOTIFICATION_ATTACHMENT);
            fileCreationWithContentRequest.setContent(documentWithContent.getContent().getBytes());
            safeStorageClientMock.createFile(fileCreationWithContentRequest, documentWithContent.getDocument().getDigests().getSha256());
        }
    }


    public static void firstFileUploadFromNotificationNotAPDF(List<TestUtils.DocumentWithContent> documentWithContentList, SafeStorageClientMock safeStorageClientMock) {
        for (TestUtils.DocumentWithContent documentWithContent : documentWithContentList) {
            FileCreationWithContentRequest fileCreationWithContentRequest = new FileCreationWithContentRequest();
            fileCreationWithContentRequest.setContentType("application/pdf" + NOT_A_PDF);
            fileCreationWithContentRequest.setDocumentType(PN_NOTIFICATION_ATTACHMENT);
            fileCreationWithContentRequest.setContent(documentWithContent.getContent().getBytes());
            safeStorageClientMock.createFile(fileCreationWithContentRequest, documentWithContent.getDocument().getDigests().getSha256());
        }
    }

    public static void firstFileUploadFromNotificationError(NotificationInt notification, SafeStorageClientMock safeStorageClientMock, byte[] fileSha) {
        for (NotificationDocumentInt attachment : notification.getDocuments()) {
            FileCreationWithContentRequest fileCreationWithContentRequest = new FileCreationWithContentRequest();
            fileCreationWithContentRequest.setContentType("application/pdf");
            fileCreationWithContentRequest.setDocumentType(PN_NOTIFICATION_ATTACHMENT);
            fileCreationWithContentRequest.setContent(fileSha);
            safeStorageClientMock.createFile(fileCreationWithContentRequest, attachment.getDigests().getSha256());
        }
    }

    public static List<TestUtils.DocumentWithContent> getDocumentWithContents(String fileDoc, List<NotificationDocumentInt> notificationDocumentList) {
        TestUtils.DocumentWithContent documentWithContent = TestUtils.DocumentWithContent.builder()
                .content(fileDoc)
                .document(notificationDocumentList.get(0))
                .build();
        return Collections.singletonList(documentWithContent);
    }

    public static List<NotificationDocumentInt> getDocumentList(String fileDoc) {
        return List.of(
                NotificationDocumentInt.builder()
                        .ref(NotificationDocumentInt.Ref.builder()
                                .key(Base64Utils.encodeToString(fileDoc.getBytes()))
                                .versionToken("v01_doc00")
                                .build()
                        )
                        .digests(NotificationDocumentInt.Digests.builder()
                                .sha256(Base64Utils.encodeToString(fileDoc.getBytes()))
                                .build()
                        )
                        .build()
        );
    }

    public static List<NotificationPaymentInfoInt> getPaymentWithF24(NotificationDocumentInt paymentDocumentInt) {
        return List.of(
                NotificationPaymentInfoInt.builder()
                        .f24(F24Int.builder()
                                .applyCost(true)
                                .title("payment_f24_1")
                                .metadataAttachment(paymentDocumentInt)
                                .build()
                        )
                        .pagoPA(null)
                        .build()
        );
    }

    public static void writeAllGeneratedLegalFacts(String iun, String className, TimelineService timelineService, SafeStorageClientMock safeStorageClientMock) {
        String testName = className + "-" + getMethodName(3);

        timelineService.getTimeline(iun, true).forEach(
                elem -> {
                    if (!elem.getLegalFactsIds().isEmpty()) {
                        LegalFactsIdInt legalFactsId = elem.getLegalFactsIds().get(0);
                        if (!LegalFactCategoryInt.PEC_RECEIPT.equals(legalFactsId.getCategory()) && !LegalFactCategoryInt.ANALOG_DELIVERY.equals(legalFactsId.getCategory())) {
                            String key = legalFactsId.getKey().replace("safestorage://", "");
                            log.info("[TEST] writing safestoragemock key={} testName={} cat={}", key, testName, legalFactsId.getCategory());
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
                                                TimelineService timelineService,
                                                DelegateInfoInt delegateInfo
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
                delegateInfo,
                generatedLegalFactsInfo.isNotificationViewedLegalFactGenerated()
        );

        TestUtils.checkPecDeliveryWorkflowLegalFactsGeneration(
                notification,
                recipient,
                sentPecAttemptNumber,
                endWorkflowStatus,
                legalFactGenerator,
                generatedLegalFactsInfo.isPecDeliveryWorkflowLegalFactsGenerated()
        );

        TestUtils.checkCompletelyUnreachableLegalFactsGeneration(notification,
                recipient,
                endWorkflowStatus,
                legalFactGenerator,
                generatedLegalFactsInfo.isNotificationCompletelyUnreachableLegalFactGenerated());
    }

    private static int getTimes(boolean itWasGenerated) {
        return itWasGenerated ? 1 : 0;
    }

    private static void checkNotificationReceivedLegalFactGeneration(NotificationInt notification,
                                                                     LegalFactGenerator legalFactGenerator,
                                                                     boolean itWasGenerated) {
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
                                                         DelegateInfoInt delegateInfo,
                                                         boolean itWasGenerated) {
        int times = getTimes(itWasGenerated);

        try {
            Mockito.verify(legalFactGenerator, Mockito.times(times)).generateNotificationViewedLegalFact(Mockito.eq(iun), Mockito.eq(recipient), Mockito.eq(delegateInfo), Mockito.any(Instant.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void generateNotificationAAR(NotificationInt notification,
                                                NotificationRecipientInt recipient,
                                                LegalFactGenerator legalFactGenerator,
                                                boolean itWasGenerated) {
        int times = getTimes(itWasGenerated);
        String quickAccessToken = "test";

        try {
            Mockito.verify(legalFactGenerator, Mockito.times(times)).generateNotificationAAR(notification, recipient, quickAccessToken);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void checkPecDeliveryWorkflowLegalFactsGeneration(NotificationInt notification,
                                                                     NotificationRecipientInt recipient,
                                                                     int sentPecAttemptNumber,
                                                                     EndWorkflowStatus endWorkflowStatus,
                                                                     LegalFactGenerator legalFactGenerator,
                                                                     boolean itWasGenerated
    ) {
        int times = getTimes(itWasGenerated);

        ArgumentCaptor<List<SendDigitalFeedbackDetailsInt>> sendDigitalFeedbackCaptor = ArgumentCaptor.forClass(List.class);

        try {
            Mockito.verify(legalFactGenerator, Mockito.times(times)).generatePecDeliveryWorkflowLegalFact(sendDigitalFeedbackCaptor.capture(), Mockito.eq(notification),
                    Mockito.eq(recipient), Mockito.eq(endWorkflowStatus), Mockito.any(Instant.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (itWasGenerated) {
            List<SendDigitalFeedbackDetailsInt> listSendDigitalFeedbackDetail = sendDigitalFeedbackCaptor.getValue();

            Assertions.assertEquals(sentPecAttemptNumber, listSendDigitalFeedbackDetail.size());
        }
    }


    private static void checkCompletelyUnreachableLegalFactsGeneration(NotificationInt notification,
                                                                       NotificationRecipientInt recipient,
                                                                       EndWorkflowStatus endWorkflowStatus,
                                                                       LegalFactGenerator legalFactGenerator,
                                                                       boolean itWasGenerated
    ) {
        int times = getTimes(itWasGenerated);


        try {
            Mockito.verify(legalFactGenerator, Mockito.times(times)).generateAnalogDeliveryFailureWorkflowLegalFact(Mockito.eq(notification),
                    Mockito.eq(recipient), Mockito.eq(endWorkflowStatus), Mockito.any(Instant.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static NotificationInt getNotification() {
        return NotificationInt.builder()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId("testIdRecipient")
                                .internalId("test")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(LegalDigitalAddressInt.builder()
                                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .build();
    }

    public static NotificationInt getNotificationV2() {
        return NotificationInt.builder()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .documents(List.of(NotificationDocumentInt.builder()
                        .digests(NotificationDocumentInt.Digests.builder()
                                .sha256("sha256").build())
                        .ref(NotificationDocumentInt.Ref.builder().build())
                        .build()))
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId("testIdRecipient")
                                .internalId("test")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(LegalDigitalAddressInt.builder()
                                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .payments(List.of(NotificationPaymentInfoInt.builder()
                                        .pagoPA(PagoPaInt.builder()
                                                .noticeCode("noticeCode")
                                                .creditorTaxId("taxId")
                                                .attachment(NotificationDocumentInt.builder()
                                                        .ref(NotificationDocumentInt.Ref.builder().build())
                                                        .digests(NotificationDocumentInt.Digests.builder()
                                                                .sha256("sha256").build())
                                                        .build())
                                                .build())
                                        .build()))
                                .build()
                ))
                .build();
    }

    public static NotificationInt getNotificationV2WithF24() {
        return NotificationInt.builder()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .documents(List.of(NotificationDocumentInt.builder()
                        .digests(NotificationDocumentInt.Digests.builder()
                                .sha256("sha256").build())
                        .ref(NotificationDocumentInt.Ref.builder().build())
                        .build()))
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId("testIdRecipient")
                                .internalId("test")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(LegalDigitalAddressInt.builder()
                                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .payments(List.of(NotificationPaymentInfoInt.builder()
                                        .pagoPA(PagoPaInt.builder()
                                                .noticeCode("noticeCode")
                                                .creditorTaxId("taxId")
                                                .attachment(NotificationDocumentInt.builder()
                                                        .ref(NotificationDocumentInt.Ref.builder().build())
                                                        .digests(NotificationDocumentInt.Digests.builder()
                                                                .sha256("sha256").build())
                                                        .build())
                                                .build())
                                        .f24(F24Int.builder()
                                                .title("title")
                                                .applyCost(true)
                                                .metadataAttachment(NotificationDocumentInt.builder()
                                                        .ref(NotificationDocumentInt.Ref.builder().build())
                                                        .digests(NotificationDocumentInt.Digests.builder()
                                                                .sha256("sha256").build())
                                                        .build())
                                                .build())
                                        .build()))
                                .build()
                ))
                .build();
    }


    public static NotificationInt getNotificationMultiRecipient() {
        return NotificationInt.builder()
                .iun("IUN_01")
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId(" pa_02")
                        .build()
                )
                .recipients(Arrays.asList(
                        NotificationRecipientInt.builder()
                                .taxId("testIdRecipient")
                                .internalId("test")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(LegalDigitalAddressInt.builder()
                                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build(),
                        NotificationRecipientInt.builder()
                                .taxId("testIdRecipient")
                                .internalId("test")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .digitalDomicile(LegalDigitalAddressInt.builder()
                                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                                        .address("account@dominio.it")
                                        .build())
                                .build()
                ))
                .build();
    }

    public static String getMethodName(final int depth) {
        final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
        return ste[depth].getMethodName();
    }

    public static String getMethodNameAndClassName(final int depth) {
        final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
        return ste[depth].getClassName()+"."+ste[depth].getMethodName();
    }

    public static String getRandomIun(int level) {
        String callerMethod = getMethodName(level);
        return getIun(callerMethod);
    }

    public static String getRandomIun() {
        String callerMethod = getMethodName(3);
        return getIun(callerMethod);
    }
    
    @NotNull
    private static String getIun(String callerMethod) {
        Random rand = new Random();
        int upperbound = 10000;
        int int_random = rand.nextInt(upperbound);
        return "iun-" + callerMethod + "_" + int_random;
    }


    public static void initializeAllMockClient(SafeStorageClientMock safeStorageClientMock,
                                               PnDeliveryClientMock pnDeliveryClientMock,
                                               UserAttributesClientMock userAttributesClientMock,
                                               NationalRegistriesClientMock nationalRegistriesClientMock,
                                               TimelineDaoMock timelineDaoMock,
                                               PaperNotificationFailedDaoMock paperNotificationFailedDaoMock,
                                               PnDataVaultClientMock pnDataVaultClientMock,
                                               PnDataVaultClientReactiveMock pnDataVaultClientReactiveMock,
                                               DocumentCreationRequestDaoMock documentCreationRequestDaoMock,
                                               AddressManagerClientMock addressManagerClientMock,
                                               F24ClientMock f24ClientMock,
                                               ActionPoolMock actionPoolMock
    ) {

        log.info("CLEARING MOCKS");

        ThreadPool.killThreads();

        safeStorageClientMock.clear();
        pnDeliveryClientMock.clear();
        userAttributesClientMock.clear();
        nationalRegistriesClientMock.clear();
        timelineDaoMock.clear();
        paperNotificationFailedDaoMock.clear();
        pnDataVaultClientMock.clear();
        pnDataVaultClientReactiveMock.clear();
        documentCreationRequestDaoMock.clear();
        addressManagerClientMock.clear();
        f24ClientMock.clear();
        actionPoolMock.clear();
        
        ConsoleAppenderCustom.initializeLog();
    }

    public static void verifyPaymentInfo(NotificationInt notification, int recIndex, List<PaymentsInfoForRecipientInt> paymentsInfoForRecipientsCaptured) {
        notification.getRecipients().forEach(rec ->
                rec.getPayments().forEach(payment -> {
                    final PagoPaInt paymentPagoPA = payment.getPagoPA();
                    if(paymentPagoPA != null && paymentPagoPA.getApplyCost()){
                        Optional<PaymentsInfoForRecipientInt> paymentsInfoForRecipient = paymentsInfoForRecipientsCaptured.stream()
                                .filter(x -> x.getCreditorTaxId().equals(paymentPagoPA.getCreditorTaxId()) &&
                                        x.getNoticeCode().equals(paymentPagoPA.getNoticeCode()) &&
                                        x.getRecIndex().equals(recIndex)).findFirst();

                        Assertions.assertTrue(paymentsInfoForRecipient.isPresent());
                    }
                })
        );
    }


    public static NotificationRecipientInt getNotificationRecipientInt() {
        return NotificationRecipientInt.builder()
                .taxId("testIdRecipient")
                .internalId("test")
                .denomination("Nome Cognome/Ragione Sociale")
                .digitalDomicile(LegalDigitalAddressInt.builder()
                        .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                        .address("account@dominio.it")
                        .build())
                .payments(List.of(NotificationPaymentInfoInt.builder()
                        .pagoPA(PagoPaInt.builder()
                                .noticeCode("noticeCode")
                                .creditorTaxId("taxId")
                                .attachment(NotificationDocumentInt.builder()
                                        .ref(NotificationDocumentInt.Ref.builder().build())
                                        .digests(NotificationDocumentInt.Digests.builder()
                                                .sha256("sha256").build())
                                        .build())
                                .build())
                        .f24(F24Int.builder()
                                .title("title")
                                .applyCost(true)
                                .metadataAttachment(NotificationDocumentInt.builder()
                                        .ref(NotificationDocumentInt.Ref.builder().build())
                                        .digests(NotificationDocumentInt.Digests.builder()
                                                .sha256("sha256").build())
                                        .build())
                                .build())
                        .build()))
                .build();
    }

    @Builder
    @Getter
    public static class GeneratedLegalFactsInfo {
        boolean notificationReceivedLegalFactGenerated;
        boolean notificationAARGenerated;
        boolean notificationViewedLegalFactGenerated;
        boolean pecDeliveryWorkflowLegalFactsGenerated;
        boolean notificationCompletelyUnreachableLegalFactGenerated;
    }

    @Builder
    @Getter
    public static class DocumentWithContent {
        String content;
        NotificationDocumentInt document;
    }
}
