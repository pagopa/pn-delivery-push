package it.pagopa.pn.deliverypush.action.notificationview;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.action.details.DocumentCreationResponseActionDetails;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.documentcreation.DocumentCreationTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.datavault.RecipientTypeInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.mandate.DelegateInfoInt;
import it.pagopa.pn.deliverypush.dto.radd.RaddInfo;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationViewedCreationRequestDetailsInt;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.PaperNotificationFailedService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Optional;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NO_RECIPIENT_IN_NOTIFICATION;

class NotificationViewLegalFactCreationResponseHandlerTest {
    @Mock
    private NotificationService notificationService;
    @Mock
    private TimelineService timelineService;
    @Mock
    private NotificationCost notificationCost;
    @Mock
    private PaperNotificationFailedService paperNotificationFailedService;
    @Mock
    private NotificationUtils notificationUtils;
    @Mock
    private TimelineUtils timelineUtils;

    private NotificationViewLegalFactCreationResponseHandler handler;

    @BeforeEach
    public void setup() {
        
        handler = new NotificationViewLegalFactCreationResponseHandler(
                notificationService,
                timelineService,
                notificationCost,
                paperNotificationFailedService,
                new NotificationUtils(),
                timelineUtils
        );
    }
    
    @Test
    @ExtendWith(MockitoExtension.class)
    void handleLegalFactCreationResponseWithDelegateInfo() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        int recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        String legalFactId = "fileKey";
        DocumentCreationResponseActionDetails actionDetails = DocumentCreationResponseActionDetails.builder()
                .timelineId("testTimelineId")
                .documentCreationType(DocumentCreationTypeInt.RECIPIENT_ACCESS)
                .key(legalFactId)
                .build();

        Mockito.when( notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notification);

        String internalId = "internalId";

        DelegateInfoInt delegateInfo = DelegateInfoInt.builder()
                .internalId(internalId)
                .mandateId("mandate")
                .operatorUuid("operatorUU")
                .denomination("denomination")
                .taxId("taxId")
                .delegateType(RecipientTypeInt.PF)
                .build();
        
        NotificationViewedCreationRequestDetailsInt timelineDetails = NotificationViewedCreationRequestDetailsInt.builder()
                .recIndex(recIndex)
                .legalFactId(legalFactId)
                .eventTimestamp(Instant.now())
                .delegateInfo(delegateInfo)
                .build();
        Mockito.when( timelineService.getTimelineElementDetails(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(Optional.of(timelineDetails));
        
        int notificationCost = 10;

        Mockito.when(this.notificationCost.getNotificationCost(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(Mono.just(Optional.of(notificationCost)));
        TimelineElementInternal timelineElement = TimelineElementInternal.builder().build();
        Mockito.when(timelineUtils.buildNotificationViewedTimelineElement(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(timelineElement);
        
        //WHEN
        handler.handleLegalFactCreationResponse(notification.getIun(), recIndex, actionDetails);

        //THEN
        
        Mockito.verify(timelineUtils).buildNotificationViewedTimelineElement(notification, recIndex, legalFactId, notificationCost,
                        null, delegateInfo, timelineDetails.getEventTimestamp());

        Mockito.verify(paperNotificationFailedService).deleteNotificationFailed(recipient.getInternalId(), notification.getIun());
        Mockito.verify(timelineService).addTimelineElement(timelineElement, notification);
    }

    @Test
    @ExtendWith(MockitoExtension.class)
    void handleLegalFactCreationResponseWithRaddInfo() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        int recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        String legalFactId = "fileKey";
        DocumentCreationResponseActionDetails actionDetails = DocumentCreationResponseActionDetails.builder()
                .timelineId("testTimelineId")
                .documentCreationType(DocumentCreationTypeInt.RECIPIENT_ACCESS)
                .key(legalFactId)
                .build();

        Mockito.when( notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notification);

        NotificationViewedCreationRequestDetailsInt timelineDetails = NotificationViewedCreationRequestDetailsInt.builder()
                .recIndex(recIndex)
                .legalFactId(legalFactId)
                .eventTimestamp(Instant.now())
                .delegateInfo(null)
                .raddTransactionId("radTransactionIdTest")
                .raddType("radType")
                .build();
        
        Mockito.when( timelineService.getTimelineElementDetails(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(Optional.of(timelineDetails));

        int notificationCost = 10;

        Mockito.when(this.notificationCost.getNotificationCost(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(Mono.just(Optional.of(notificationCost)));
        
        TimelineElementInternal timelineElement = TimelineElementInternal.builder().build();
        Mockito.when(timelineUtils.buildNotificationViewedTimelineElement(Mockito.eq(notification), Mockito.eq(recIndex), Mockito.eq(legalFactId), Mockito.eq(notificationCost),
                        Mockito.any(RaddInfo.class), Mockito.isNull(), Mockito.eq(timelineDetails.getEventTimestamp())))
                .thenReturn(timelineElement);

        //WHEN
        handler.handleLegalFactCreationResponse(notification.getIun(), recIndex, actionDetails);

        //THEN
        Mockito.verify(paperNotificationFailedService).deleteNotificationFailed(recipient.getInternalId(), notification.getIun());
        Mockito.verify(timelineService).addTimelineElement(timelineElement, notification);
    }

    @Test
    @ExtendWith(MockitoExtension.class)
    void handleLegalFactCreationResponseWithoutDelegateInfoAndRaddInfo() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        int recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        String legalFactId = "fileKey";
        DocumentCreationResponseActionDetails actionDetails = DocumentCreationResponseActionDetails.builder()
                .timelineId("testTimelineId")
                .documentCreationType(DocumentCreationTypeInt.RECIPIENT_ACCESS)
                .key(legalFactId)
                .build();

        Mockito.when( notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notification);

        NotificationViewedCreationRequestDetailsInt timelineDetails = NotificationViewedCreationRequestDetailsInt.builder()
                .recIndex(recIndex)
                .legalFactId(legalFactId)
                .eventTimestamp(Instant.now())
                .build();
        Mockito.when( timelineService.getTimelineElementDetails(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(Optional.of(timelineDetails));

        int notificationCost = 10;

        Mockito.when(this.notificationCost.getNotificationCost(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(Mono.just(Optional.of(notificationCost)));
        TimelineElementInternal timelineElement = TimelineElementInternal.builder().build();
        Mockito.when(timelineUtils.buildNotificationViewedTimelineElement(Mockito.eq(notification), Mockito.eq(recIndex), Mockito.eq(legalFactId), Mockito.eq(notificationCost),
                        Mockito.isNull(), Mockito.isNull(), Mockito.eq(timelineDetails.getEventTimestamp())))
                .thenReturn(timelineElement);

        //WHEN
        handler.handleLegalFactCreationResponse(notification.getIun(), recIndex, actionDetails);

        //THEN
        Mockito.verify(paperNotificationFailedService).deleteNotificationFailed(recipient.getInternalId(), notification.getIun());
        Mockito.verify(timelineService).addTimelineElement(timelineElement, notification);
    }
    
    @Test
    @ExtendWith(MockitoExtension.class)
    void handleLegalFactCreationResponseErrorAuditLogNotAlreadyCreated() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        int recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        String legalFactId = "fileKey";
        DocumentCreationResponseActionDetails actionDetails = DocumentCreationResponseActionDetails.builder()
                .timelineId("testTimelineId")
                .documentCreationType(DocumentCreationTypeInt.RECIPIENT_ACCESS)
                .key(legalFactId)
                .build();
        
        Mockito.when( notificationService.getNotificationByIun(Mockito.anyString())).thenThrow(  new PnInternalException("Prova", ERROR_CODE_DELIVERYPUSH_NO_RECIPIENT_IN_NOTIFICATION));

        //WHEN
        String iun = notification.getIun();
        Assertions.assertThrows(PnInternalException.class, () -> handler.handleLegalFactCreationResponse(iun, recIndex, actionDetails));
        
        //THEN
        Mockito.verify(paperNotificationFailedService, Mockito.never()).deleteNotificationFailed(Mockito.any(), Mockito.any());
        Mockito.verify(timelineService, Mockito.never()).addTimelineElement(Mockito.any(), Mockito.any());
    }

    @Test
    @ExtendWith(MockitoExtension.class)
    void handleLegalFactCreationResponseErrorAuditLogAlreadyCreated() {
        //GIVEN
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withNotificationRecipient(recipient)
                .build();
        int recIndex = notificationUtils.getRecipientIndexFromTaxId(notification, recipient.getTaxId());

        String legalFactId = "fileKey";
        DocumentCreationResponseActionDetails actionDetails = DocumentCreationResponseActionDetails.builder()
                .timelineId("testTimelineId")
                .documentCreationType(DocumentCreationTypeInt.RECIPIENT_ACCESS)
                .key(legalFactId)
                .build();

        Mockito.when( notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notification);

        NotificationViewedCreationRequestDetailsInt timelineDetails = NotificationViewedCreationRequestDetailsInt.builder()
                .recIndex(recIndex)
                .legalFactId(legalFactId)
                .eventTimestamp(Instant.now())
                .raddTransactionId("radTransactionIdTest")
                .raddType("radType")
                .build();
        
        Mockito.when( timelineService.getTimelineElementDetails(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(Optional.of(timelineDetails));
        
        Mockito.when(this.notificationCost.getNotificationCost(Mockito.any(NotificationInt.class), Mockito.anyInt())).thenReturn(Mono.error(new RuntimeException("exception")));

        //WHEN
        Assertions.assertThrows(RuntimeException.class, () -> handler.handleLegalFactCreationResponse(notification.getIun(), recIndex, actionDetails));

        //THEN
        Mockito.verify(paperNotificationFailedService, Mockito.never()).deleteNotificationFailed(Mockito.any(), Mockito.any());
        Mockito.verify(timelineService, Mockito.never()).addTimelineElement(Mockito.any(), Mockito.any());
    }

}