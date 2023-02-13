package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.deliverypush.action.details.NotificationValidationActionDetails;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.startworkflow.ReceivedLegalFactCreationRequest;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationRefusedErrorCodeInt;
import it.pagopa.pn.deliverypush.exceptions.PnValidationFileNotFoundException;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.Mockito.doThrow;

class NotificationValidationActionHandlerTest {
    @Mock
    private AttachmentUtils attachmentUtils;
    @Mock
    private TaxIdPivaValidator taxIdPivaValidator;
    @Mock
    private TimelineService timelineService;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ReceivedLegalFactCreationRequest receivedLegalFactCreationRequest;
    @Mock
    private NotificationValidationScheduler notificationValidationScheduler;

    private NotificationValidationActionHandler handler;

    @BeforeEach
    public void setup() {
        handler = new NotificationValidationActionHandler(attachmentUtils, taxIdPivaValidator,
                timelineService, timelineUtils, notificationService, receivedLegalFactCreationRequest,
                notificationValidationScheduler);
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void validateNotificationOK() {
        //GIVEN
        NotificationInt notification = TestUtils.getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(1)
                .build();
        
        //WHEN
        handler.validateNotification(notification.getIun(), details);
        
        //THEN
        Mockito.verify(attachmentUtils).validateAttachment(notification);
        Mockito.verify(receivedLegalFactCreationRequest).saveNotificationReceivedLegalFacts(notification);
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void validateNotificationKO() {
        //GIVEN
        NotificationInt notification = TestUtils.getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        doThrow(new PnValidationFileNotFoundException(NotificationRefusedErrorCodeInt.FILE_NOTFOUND, "detail", new RuntimeException())).when(attachmentUtils).validateAttachment(notification);

        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(1)
                .build();

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();
        Mockito.when( timelineUtils.buildRefusedRequestTimelineElement(Mockito.any(NotificationInt.class), Mockito.any()))
                .thenReturn(timelineElementInternal);
        
        //WHEN
        handler.validateNotification(notification.getIun(), details);

        //THEN
        Mockito.verify(receivedLegalFactCreationRequest, Mockito.never()).saveNotificationReceivedLegalFacts(notification);
        Mockito.verify(timelineService).addTimelineElement(timelineElementInternal, notification);
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void validateNotificationErrorCheckRetry() {
        //GIVEN
        NotificationInt notification = TestUtils.getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        
        //Simulazione runtimeException generica (servizio non risponde ecc)
        doThrow(new RuntimeException()).when(attachmentUtils).validateAttachment(notification);

        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(1)
                .build();

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();
        Mockito.when( timelineUtils.buildRefusedRequestTimelineElement(Mockito.any(NotificationInt.class), Mockito.any()))
                .thenReturn(timelineElementInternal);

        //WHEN
        handler.validateNotification(notification.getIun(), details);

        //THEN
        Mockito.verify(receivedLegalFactCreationRequest, Mockito.never()).saveNotificationReceivedLegalFacts(notification);
        Mockito.verify(notificationValidationScheduler).scheduleNotificationValidation(notification, details.getRetryAttempt());
    }

    

}