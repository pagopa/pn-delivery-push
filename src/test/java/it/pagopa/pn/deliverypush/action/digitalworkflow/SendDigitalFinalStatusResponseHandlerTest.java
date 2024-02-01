package it.pagopa.pn.deliverypush.action.digitalworkflow;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.completionworkflow.CompletionWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.details.SendDigitalFinalStatusResponseDetails;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.PhysicalAddressBuilder;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.dto.address.DigitalAddressInfoSentAttempt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalFeedbackDetailsInt;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static it.pagopa.pn.deliverypush.action.it.mockbean.ExternalChannelMock.EXTCHANNEL_SEND_SUCCESS;

@ExtendWith(MockitoExtension.class)
class SendDigitalFinalStatusResponseHandlerTest {
    @Mock
    private NotificationService notificationService;
    @Mock
    private TimelineService timelineService;
    @Mock
    private DigitalWorkFlowHandler digitalWorkFlowHandler;
    @Mock
    private CompletionWorkFlowHandler completionWorkFlowHandler;
    @InjectMocks
    private SendDigitalFinalStatusResponseHandler sendDigitalFinalStatusResponseHandler;
    
    @Test
    void feedbackNotPresentTest() {
        //GIVEN
        String iun = "tesIun";
        String sendDigitalFeedbackTimelineId = "sendDigitalFeedbackTimelineId";
        SendDigitalFinalStatusResponseDetails details = SendDigitalFinalStatusResponseDetails.builder()
                .lastAttemptAddressInfo(
                        DigitalAddressInfoSentAttempt.builder()
                                .relatedFeedbackTimelineId(sendDigitalFeedbackTimelineId)
                                .build()
                )
                .build();

        Mockito.when( timelineService.getTimelineElementDetails(Mockito.eq(iun), Mockito.eq(sendDigitalFeedbackTimelineId), Mockito.any())).thenReturn(Optional.empty());
        
        //WHEN
        Assertions.assertThrows(PnInternalException.class, () -> sendDigitalFinalStatusResponseHandler.handleSendDigitalFinalStatusResponse(iun, details));
    }

    @Test
    void responseStatusNotHandled() {
        //GIVEN
        String iun = "tesIun";
        String sendDigitalFeedbackTimelineId = "sendDigitalFeedbackTimelineId";
        SendDigitalFinalStatusResponseDetails details = SendDigitalFinalStatusResponseDetails.builder()
                .lastAttemptAddressInfo(
                        DigitalAddressInfoSentAttempt.builder()
                                .relatedFeedbackTimelineId(sendDigitalFeedbackTimelineId)
                                .build()
                )
                .isFirstSendRetry(true)
                .build();

        SendDigitalFeedbackDetailsInt sendDigitalFeedbackDetails = SendDigitalFeedbackDetailsInt.builder()
                .responseStatus(ResponseStatusInt.PROGRESS)
                .build();

        Mockito.when( timelineService.getTimelineElementDetails(Mockito.eq(iun), Mockito.eq(sendDigitalFeedbackTimelineId), Mockito.any())).thenReturn(
                Optional.of(sendDigitalFeedbackDetails)
        );

        //WHEN
        Assertions.assertThrows(PnInternalException.class, () -> sendDigitalFinalStatusResponseHandler.handleSendDigitalFinalStatusResponse(iun, details));
    }

    @Test
    void handleSendDigitalFinalStatusResponseTestOKisFirstSendRetry() {
     //GIVEN
        NotificationInt notification = getNotification();
        String iun = notification.getIun();
        String sendDigitalFeedbackTimelineId = "sendDigitalFeedbackTimelineId";
        int recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, notification.getRecipients().get(0).getTaxId());

        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(notification);
        
        SendDigitalFinalStatusResponseDetails details = SendDigitalFinalStatusResponseDetails.builder()
                .lastAttemptAddressInfo(
                        DigitalAddressInfoSentAttempt.builder()
                                .relatedFeedbackTimelineId(sendDigitalFeedbackTimelineId)
                                .build()
                )
                .isFirstSendRetry(true)
                .build();
        
        SendDigitalFeedbackDetailsInt sendDigitalFeedbackDetails = SendDigitalFeedbackDetailsInt.builder()
                .responseStatus(ResponseStatusInt.OK)
                .build();

        Mockito.when( timelineService.getTimelineElementDetails(Mockito.eq(iun), Mockito.eq(sendDigitalFeedbackTimelineId), Mockito.any())).thenReturn(
                Optional.of(sendDigitalFeedbackDetails)
        );
        
        //WHEN
        sendDigitalFinalStatusResponseHandler.handleSendDigitalFinalStatusResponse(iun, details);

        //THEN
        Mockito.verify(digitalWorkFlowHandler).checkAndSendNotification(notification, recIndex, details.getLastAttemptAddressInfo());
    }

    @Test
    void handleSendDigitalFinalStatusResponseTestOKisNotFirstSendRetry() {
        //GIVEN
        NotificationInt notification = getNotification();
        String iun = notification.getIun();
        String sendDigitalFeedbackTimelineId = "sendDigitalFeedbackTimelineId";
        int recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, notification.getRecipients().get(0).getTaxId());

        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(notification);

        SendDigitalFinalStatusResponseDetails details = SendDigitalFinalStatusResponseDetails.builder()
                .lastAttemptAddressInfo(
                        DigitalAddressInfoSentAttempt.builder()
                                .relatedFeedbackTimelineId(sendDigitalFeedbackTimelineId)
                                .build()
                )
                .isFirstSendRetry(false)
                .build();

        SendDigitalFeedbackDetailsInt sendDigitalFeedbackDetails = SendDigitalFeedbackDetailsInt.builder()
                .responseStatus(ResponseStatusInt.OK)
                .notificationDate(Instant.now())
                .build();

        Mockito.when( timelineService.getTimelineElementDetails(Mockito.eq(iun), Mockito.eq(sendDigitalFeedbackTimelineId), Mockito.any())).thenReturn(
                Optional.of(sendDigitalFeedbackDetails)
        );

        //WHEN
        sendDigitalFinalStatusResponseHandler.handleSendDigitalFinalStatusResponse(iun, details);

        //THEN
        Mockito.verify(completionWorkFlowHandler).completionSuccessDigitalWorkflow(                
                notification,
                recIndex,
                sendDigitalFeedbackDetails.getNotificationDate(),
                details.getLastAttemptAddressInfo().getDigitalAddress()
        );
    }

    @Test
    void handleSendDigitalFinalStatusResponseTestKOisFirstSendRetry() {
        //GIVEN
        NotificationInt notification = getNotification();
        String iun = notification.getIun();
        String sendDigitalFeedbackTimelineId = "sendDigitalFeedbackTimelineId";
        int recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, notification.getRecipients().get(0).getTaxId());

        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(notification);

        SendDigitalFinalStatusResponseDetails details = SendDigitalFinalStatusResponseDetails.builder()
                .lastAttemptAddressInfo(
                        DigitalAddressInfoSentAttempt.builder()
                                .relatedFeedbackTimelineId(sendDigitalFeedbackTimelineId)
                                .build()
                )
                .isFirstSendRetry(true)
                .build();

        SendDigitalFeedbackDetailsInt sendDigitalFeedbackDetails = SendDigitalFeedbackDetailsInt.builder()
                .responseStatus(ResponseStatusInt.KO)
                .notificationDate(Instant.now())
                .build();

        Mockito.when( timelineService.getTimelineElementDetails(Mockito.eq(iun), Mockito.eq(sendDigitalFeedbackTimelineId), Mockito.any())).thenReturn(
                Optional.of(sendDigitalFeedbackDetails)
        );

        //WHEN
        sendDigitalFinalStatusResponseHandler.handleSendDigitalFinalStatusResponse(iun, details);

        //THEN
        Mockito.verify(digitalWorkFlowHandler).checkAndSendNotification(
                notification,
                recIndex,
                details.getLastAttemptAddressInfo()
        );
    }

    @Test
    void handleSendDigitalFinalStatusResponseTestKOisNotFirstSendRetryAlreadyPresentFeedbackCompletedSuccessWorkflow() {
        //GIVEN
        NotificationInt notification = getNotification();
        String iun = notification.getIun();
        String sendDigitalFeedbackTimelineId = "sendDigitalFeedbackTimelineId";
        int recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, notification.getRecipients().get(0).getTaxId());

        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(notification);

        SendDigitalFinalStatusResponseDetails details = SendDigitalFinalStatusResponseDetails.builder()
                .lastAttemptAddressInfo(
                        DigitalAddressInfoSentAttempt.builder()
                                .relatedFeedbackTimelineId(sendDigitalFeedbackTimelineId)
                                .build()
                )
                .isFirstSendRetry(false)
                .alreadyPresentRelatedFeedbackTimelineId("timelineIdPrecedentFeedback")
                .build();

        SendDigitalFeedbackDetailsInt sendDigitalFeedbackDetails = SendDigitalFeedbackDetailsInt.builder()
                .responseStatus(ResponseStatusInt.KO)
                .notificationDate(Instant.now())
                .build();

        Mockito.when( timelineService.getTimelineElementDetails(Mockito.eq(iun), Mockito.eq(sendDigitalFeedbackTimelineId), Mockito.any())).thenReturn(
                Optional.of(sendDigitalFeedbackDetails)
        );


        Mockito.when( digitalWorkFlowHandler.checkFirstAttemptAndCompleteWorkflow(notification, recIndex, details.getAlreadyPresentRelatedFeedbackTimelineId(), iun)).thenReturn(true);

        //WHEN
        sendDigitalFinalStatusResponseHandler.handleSendDigitalFinalStatusResponse(iun, details);

        //THEN
        Mockito.verify(digitalWorkFlowHandler, Mockito.never()).nextWorkflowStep(
                Mockito.any(), Mockito.anyInt(), Mockito.any()
        );
    }

    @Test
    void handleSendDigitalFinalStatusResponseTestKOisNotFirstSendRetryAlreadyPresentFeedbackNotCompletedSuccessWorkflow() {
        //GIVEN
        NotificationInt notification = getNotification();
        String iun = notification.getIun();
        String sendDigitalFeedbackTimelineId = "sendDigitalFeedbackTimelineId";
        int recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, notification.getRecipients().get(0).getTaxId());

        Mockito.when(notificationService.getNotificationByIun(iun)).thenReturn(notification);

        SendDigitalFinalStatusResponseDetails details = SendDigitalFinalStatusResponseDetails.builder()
                .lastAttemptAddressInfo(
                        DigitalAddressInfoSentAttempt.builder()
                                .relatedFeedbackTimelineId(sendDigitalFeedbackTimelineId)
                                .build()
                )
                .isFirstSendRetry(false)
                .alreadyPresentRelatedFeedbackTimelineId("timelineIdPrecedentFeedback")
                .build();

        SendDigitalFeedbackDetailsInt sendDigitalFeedbackDetails = SendDigitalFeedbackDetailsInt.builder()
                .responseStatus(ResponseStatusInt.KO)
                .notificationDate(Instant.now())
                .build();

        Mockito.when( timelineService.getTimelineElementDetails(Mockito.eq(iun), Mockito.eq(sendDigitalFeedbackTimelineId), Mockito.any())).thenReturn(
                Optional.of(sendDigitalFeedbackDetails)
        );

        Mockito.when( digitalWorkFlowHandler.checkFirstAttemptAndCompleteWorkflow(notification, recIndex, details.getAlreadyPresentRelatedFeedbackTimelineId(), iun)).thenReturn(false);

        //WHEN
        sendDigitalFinalStatusResponseHandler.handleSendDigitalFinalStatusResponse(iun, details);

        //THEN
        ArgumentCaptor<DigitalAddressInfoSentAttempt> lastAttemptMadeCaptor = ArgumentCaptor.forClass(DigitalAddressInfoSentAttempt.class);
        
        Mockito.verify(digitalWorkFlowHandler).nextWorkflowStep(
                Mockito.eq(iun), Mockito.eq(recIndex), lastAttemptMadeCaptor.capture()
        );

        DigitalAddressInfoSentAttempt lastAttemptMadeExpected = DigitalAddressInfoSentAttempt.builder()
                .digitalAddressSource(details.getLastAttemptAddressInfo().getDigitalAddressSource())
                .lastAttemptDate(details.getLastAttemptAddressInfo().getLastAttemptDate())
                .build();

        DigitalAddressInfoSentAttempt lastAttemptMade = lastAttemptMadeCaptor.getValue();
        
        Assertions.assertEquals(lastAttemptMadeExpected, lastAttemptMade);
    }

    @Test
    void handleSendDigitalFinalStatusResponseTestKOisNotFirstSendRetryNotAlreadyPresentFeedback() {
        //GIVEN
        NotificationInt notification = getNotification();
        String iun = notification.getIun();
        String sendDigitalFeedbackTimelineId = "sendDigitalFeedbackTimelineId";
        int recIndex = NotificationUtils.getRecipientIndexFromTaxId(notification, notification.getRecipients().get(0).getTaxId());
        
        SendDigitalFinalStatusResponseDetails details = SendDigitalFinalStatusResponseDetails.builder()
                .lastAttemptAddressInfo(
                        DigitalAddressInfoSentAttempt.builder()
                                .relatedFeedbackTimelineId(sendDigitalFeedbackTimelineId)
                                .build()
                )
                .isFirstSendRetry(false)
                .alreadyPresentRelatedFeedbackTimelineId(null)
                .build();

        SendDigitalFeedbackDetailsInt sendDigitalFeedbackDetails = SendDigitalFeedbackDetailsInt.builder()
                .responseStatus(ResponseStatusInt.KO)
                .notificationDate(Instant.now())
                .build();

        Mockito.when( timelineService.getTimelineElementDetails(Mockito.eq(iun), Mockito.eq(sendDigitalFeedbackTimelineId), Mockito.any())).thenReturn(
                Optional.of(sendDigitalFeedbackDetails)
        );
        
        //WHEN
        sendDigitalFinalStatusResponseHandler.handleSendDigitalFinalStatusResponse(iun, details);

        //THEN
        ArgumentCaptor<DigitalAddressInfoSentAttempt> lastAttemptMadeCaptor = ArgumentCaptor.forClass(DigitalAddressInfoSentAttempt.class);

        Mockito.verify(digitalWorkFlowHandler).nextWorkflowStep(
                Mockito.eq(iun), Mockito.eq(recIndex), lastAttemptMadeCaptor.capture()
        );

        DigitalAddressInfoSentAttempt lastAttemptMadeExpected = DigitalAddressInfoSentAttempt.builder()
                .digitalAddressSource(details.getLastAttemptAddressInfo().getDigitalAddressSource())
                .lastAttemptDate(details.getLastAttemptAddressInfo().getLastAttemptDate())
                .build();

        DigitalAddressInfoSentAttempt lastAttemptMade = lastAttemptMadeCaptor.getValue();

        Assertions.assertEquals(lastAttemptMadeExpected, lastAttemptMade);
    }
    
    private NotificationInt getNotification() {
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder()
                .withTaxId("taxId")
                .withInternalId("ANON_"+"taxId")
                .withPhysicalAddress(
                        PhysicalAddressBuilder.builder()
                                .withAddress(EXTCHANNEL_SEND_SUCCESS + "_Via Nuova")
                                .build()
                )
                .build();

        return NotificationTestBuilder.builder()
                .withIun("Iun")
                .withPaId("paId01")
                .withNotificationRecipient(recipient)
                .build();
    }

}