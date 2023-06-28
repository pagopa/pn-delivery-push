package it.pagopa.pn.deliverypush.action.startworkflow.notificationvalidation;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.details.NotificationValidationActionDetails;
import it.pagopa.pn.deliverypush.action.it.utils.TestUtils;
import it.pagopa.pn.deliverypush.action.startworkflow.NormalizeAddressHandler;
import it.pagopa.pn.deliverypush.action.startworkflow.ReceivedLegalFactCreationRequest;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.exceptions.PnValidationFileNotFoundException;
import it.pagopa.pn.deliverypush.exceptions.PnValidationNotMatchingShaException;
import it.pagopa.pn.deliverypush.exceptions.PnValidationTaxIdNotValidException;
import it.pagopa.pn.deliverypush.service.AuditLogService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

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
    @Mock
    private AddressValidator addressValidator;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NormalizeAddressHandler normalizeAddressHandler;
    @Mock
    private SchedulerService schedulerService;
    private NotificationValidationActionHandler handler;
    @Mock
    private PnDeliveryPushConfigs cfg;

    @BeforeEach
    public void setup() {
        handler = new NotificationValidationActionHandler(attachmentUtils, taxIdPivaValidator,
                timelineService, timelineUtils, notificationService,
                notificationValidationScheduler, addressValidator, auditLogService, normalizeAddressHandler, schedulerService, cfg);
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void validateNotificationOK() {
        //GIVEN
        Mockito.when(cfg.isCheckCfEnabled())
                .thenReturn(true);

        NotificationInt notification = TestUtils.getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(1)
                .build();

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.eq(notification.getIun()), Mockito.eq(PnAuditLogEventType.AUD_NT_VALID), Mockito.anyString(), Mockito.any()))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);

        Mockito.when(addressValidator.requestValidateAndNormalizeAddresses(notification)).thenReturn(Mono.empty());
                
        //WHEN
        handler.validateNotification(notification.getIun(), details);
        
        //THEN
        Mockito.verify(attachmentUtils).validateAttachment(notification);
        Mockito.verify(auditLogEvent).generateSuccess();
        Mockito.verify(notificationValidationScheduler, Mockito.never()).scheduleNotificationValidation(Mockito.eq(notification), Mockito.anyInt(), Mockito.any());

    }
    
    @ExtendWith(SpringExtension.class)
    @Test
    void validateNotificationKONotFound_isSafeStorageFileNotFoundRetry_true() {
        //GIVEN
        Mockito.when(cfg.isCheckCfEnabled())
                .thenReturn(true);

        NotificationInt notification = TestUtils.getNotification();
        Mockito.when(cfg.isSafeStorageFileNotFoundRetry())
                .thenReturn(true);
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        PnValidationFileNotFoundException ex = new PnValidationFileNotFoundException("detail", new RuntimeException());
        doThrow(ex).when(attachmentUtils).validateAttachment(notification);

        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(1)
                .build();

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();
        Mockito.when( timelineUtils.buildRefusedRequestTimelineElement(Mockito.any(NotificationInt.class), Mockito.any()))
                .thenReturn(timelineElementInternal);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.eq(notification.getIun()), Mockito.eq(PnAuditLogEventType.AUD_NT_VALID), Mockito.anyString(), Mockito.any()))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        //WHEN
        handler.validateNotification(notification.getIun(), details);

        //THEN
        Mockito.verify(notificationValidationScheduler).scheduleNotificationValidation(notification, details.getRetryAttempt(), ex);
        Mockito.verify(auditLogEvent).generateWarning(Mockito.any(), Mockito.any());
        Mockito.verify(addressValidator, Mockito.never()).requestValidateAndNormalizeAddresses(notification);
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void validateNotificationKONotFound_isSafeStorageFileNotFoundRetry_false() {
        //GIVEN
        Mockito.when(cfg.isCheckCfEnabled())
                .thenReturn(true);
        NotificationInt notification = TestUtils.getNotification();
        Mockito.when(cfg.isSafeStorageFileNotFoundRetry())
                .thenReturn(false);
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        PnValidationFileNotFoundException ex = new PnValidationFileNotFoundException("detail", new RuntimeException());
        doThrow(ex).when(attachmentUtils).validateAttachment(notification);

        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(1)
                .build();

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();
        Mockito.when( timelineUtils.buildRefusedRequestTimelineElement(Mockito.any(NotificationInt.class), Mockito.any()))
                .thenReturn(timelineElementInternal);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.eq(notification.getIun()), Mockito.eq(PnAuditLogEventType.AUD_NT_VALID), Mockito.anyString(), Mockito.any()))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        //WHEN
        handler.validateNotification(notification.getIun(), details);

        //THEN
        Mockito.verify(addressValidator, Mockito.never()).requestValidateAndNormalizeAddresses(notification);
        Mockito.verify(notificationValidationScheduler, Mockito.never()).scheduleNotificationValidation(notification, details.getRetryAttempt(), ex);
        Mockito.verify(auditLogEvent, Mockito.never()).generateWarning(Mockito.any(), Mockito.any());
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void validateNotificationKOFileShaError() {
        //GIVEN
        Mockito.when(cfg.isCheckCfEnabled())
                .thenReturn(true);

        NotificationInt notification = TestUtils.getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        doThrow(new PnValidationNotMatchingShaException("detail")).when(attachmentUtils).validateAttachment(notification);
        
        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(1)
                .build();

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();
        Mockito.when( timelineUtils.buildRefusedRequestTimelineElement(Mockito.any(NotificationInt.class), Mockito.any()))
                .thenReturn(timelineElementInternal);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.eq(notification.getIun()), Mockito.eq(PnAuditLogEventType.AUD_NT_VALID), Mockito.anyString(), Mockito.any()))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        //WHEN
        handler.validateNotification(notification.getIun(), details);

        //THEN
        Mockito.verify(addressValidator, Mockito.never()).requestValidateAndNormalizeAddresses(notification);
        Mockito.verify(timelineService).addTimelineElement(timelineElementInternal, notification);
        Mockito.verify(auditLogEvent).generateWarning(Mockito.any(), Mockito.any());
        Mockito.verify(notificationValidationScheduler, Mockito.never()).scheduleNotificationValidation(Mockito.eq(notification), Mockito.anyInt(), Mockito.any());
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void validateNotificationKOTaxIdNotValid() {
        //GIVEN
        Mockito.when(cfg.isCheckCfEnabled())
                .thenReturn(true);

        NotificationInt notification = TestUtils.getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        doThrow(new PnValidationTaxIdNotValidException("detail")).when(taxIdPivaValidator).validateTaxIdPiva(notification);
        
        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(1)
                .build();

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();
        Mockito.when( timelineUtils.buildRefusedRequestTimelineElement(Mockito.any(NotificationInt.class), Mockito.any()))
                .thenReturn(timelineElementInternal);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.eq(notification.getIun()), Mockito.eq(PnAuditLogEventType.AUD_NT_VALID), Mockito.anyString(), Mockito.any()))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        //WHEN
        handler.validateNotification(notification.getIun(), details);

        //THEN
        Mockito.verify(addressValidator, Mockito.never()).requestValidateAndNormalizeAddresses(notification);
        Mockito.verify(timelineService).addTimelineElement(timelineElementInternal, notification);
        Mockito.verify(auditLogEvent).generateWarning(Mockito.any(), Mockito.any());
        Mockito.verify(notificationValidationScheduler, Mockito.never()).scheduleNotificationValidation(Mockito.eq(notification), Mockito.anyInt(), Mockito.any());
    }

    @ExtendWith(SpringExtension.class)
    @Test
    void validateNotificationTaxIdSkipped() {
        //GIVEN
        Mockito.when(cfg.isCheckCfEnabled())
                .thenReturn(false);

        NotificationInt notification = TestUtils.getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);

        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(1)
                .build();

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();
        Mockito.when( timelineUtils.buildRefusedRequestTimelineElement(Mockito.any(NotificationInt.class), Mockito.any()))
                .thenReturn(timelineElementInternal);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(auditLogEvent);

        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        Mockito.when(addressValidator.requestValidateAndNormalizeAddresses(notification)).thenReturn(Mono.empty());

        //WHEN
        handler.validateNotification(notification.getIun(), details);

        //THEN
        Mockito.verify(addressValidator).requestValidateAndNormalizeAddresses(notification);
        Mockito.verify(timelineService, Mockito.never()).addTimelineElement(timelineElementInternal, notification);
        Mockito.verify(taxIdPivaValidator, Mockito.never()).validateTaxIdPiva(notification);
        Mockito.verify(notificationValidationScheduler, Mockito.never()).scheduleNotificationValidation(Mockito.eq(notification), Mockito.anyInt(), Mockito.any());
    }
    
    @ExtendWith(SpringExtension.class)
    @Test
    void validateNotificationErrorCheckRetry() {
        //GIVEN
        Mockito.when(cfg.isCheckCfEnabled())
                .thenReturn(true);

        NotificationInt notification = TestUtils.getNotification();
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString()))
                .thenReturn(notification);
        
        //Simulazione runtimeException generica (servizio non risponde ecc)
        RuntimeException ex = new RuntimeException();
        doThrow(ex).when(attachmentUtils).validateAttachment(notification);

        NotificationValidationActionDetails details = NotificationValidationActionDetails.builder()
                .retryAttempt(1)
                .build();

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();
        Mockito.when( timelineUtils.buildRefusedRequestTimelineElement(Mockito.any(NotificationInt.class), Mockito.any()))
                .thenReturn(timelineElementInternal);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(Mockito.eq(notification.getIun()), Mockito.eq(PnAuditLogEventType.AUD_NT_VALID), Mockito.anyString(), Mockito.any()))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        //WHEN
        handler.validateNotification(notification.getIun(), details);

        //THEN
        Mockito.verify(addressValidator, Mockito.never()).requestValidateAndNormalizeAddresses(notification);
        Mockito.verify(notificationValidationScheduler).scheduleNotificationValidation(notification, details.getRetryAttempt(),ex);
        Mockito.verify(auditLogEvent).generateWarning(Mockito.any(), Mockito.any());
    }

}