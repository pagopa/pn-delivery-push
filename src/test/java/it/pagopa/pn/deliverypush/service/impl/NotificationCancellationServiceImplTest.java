package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationRecipientTestBuilder;
import it.pagopa.pn.deliverypush.action.it.utils.NotificationTestBuilder;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.cancellation.StatusDetailInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.status.NotificationStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.NotificationCancelledDetailsInt;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.service.AuditLogService;
import it.pagopa.pn.deliverypush.service.NotificationCancellationService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.AuthUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTFOUND;
import static it.pagopa.pn.deliverypush.service.impl.NotificationCancellationServiceImpl.NOTIFICATION_ALREADY_CANCELLED;
import static it.pagopa.pn.deliverypush.service.impl.NotificationCancellationServiceImpl.NOTIFICATION_CANCELLATION_ACCEPTED;
import static org.mockito.ArgumentMatchers.*;

class NotificationCancellationServiceImplTest {
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuthUtils authUtils;
    @Mock
    private TimelineService timelineService;
    @Mock
    private TimelineUtils timelineUtils;
    @Mock
    private AuditLogService auditLogService;
    
    private NotificationCancellationService notificationCancellationService;
    
    @BeforeEach
    public void init(){
        notificationCancellationService = new NotificationCancellationServiceImpl(notificationService,authUtils, timelineService, timelineUtils, auditLogService);
    }
    
    @Test
    @ExtendWith(MockitoExtension.class)
    void startCancellationProcess() {
        //GIVEN
        NotificationInt notification = getNotification();
        Mockito.when(notificationService.getNotificationByIunReactive(Mockito.any()))
                .thenReturn(Mono.just(notification));
                
        Mockito.when(authUtils.checkPaId(eq(notification), anyString(), any(CxTypeAuthFleet.class)))
                .thenReturn(Mono.empty());

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();
        Mockito.when(timelineUtils.buildCancelRequestTimelineElement(notification))
                .thenReturn(timelineElementInternal);
        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(notification.getIun()))
                .thenReturn(false);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(eq(notification.getIun()), eq(PnAuditLogEventType.AUD_NT_CANCELLED), anyString(), eq(1)))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);
        
        //WHEN
        StatusDetailInt res = notificationCancellationService.startCancellationProcess(notification.getIun(), notification.getSender().getPaId(), CxTypeAuthFleet.PA)
                .block();
        
        //THEN
        Assertions.assertNotNull(res);
        Assertions.assertEquals(NOTIFICATION_CANCELLATION_ACCEPTED, res.getCode());
        Assertions.assertEquals("INFO", res.getLevel());
        Mockito.verify(timelineService).addTimelineElement(timelineElementInternal, notification);
        Mockito.verify(auditLogEvent).generateSuccess();
    }

    @Test
    @ExtendWith(MockitoExtension.class)
    void startCancellationProcessAlreadyCancelled() {
        //GIVEN
        NotificationInt notification = getNotification();
        Mockito.when(notificationService.getNotificationByIunReactive(Mockito.any()))
                .thenReturn(Mono.just(notification));

        Mockito.when(authUtils.checkPaId(eq(notification), anyString(), any(CxTypeAuthFleet.class)))
                .thenReturn(Mono.empty());

        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder().build();

        Mockito.when(timelineUtils.checkIsNotificationCancellationRequested(notification.getIun()))
                .thenReturn(true);

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(eq(notification.getIun()), eq(PnAuditLogEventType.AUD_NT_CANCELLED), anyString(), eq(1)))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString(), Mockito.anyString())).thenReturn(auditLogEvent);

        //WHEN
        StatusDetailInt res = notificationCancellationService.startCancellationProcess(notification.getIun(), notification.getSender().getPaId(), CxTypeAuthFleet.PA)
                .block();

        //THEN
        Assertions.assertNotNull(res);
        Assertions.assertEquals(NOTIFICATION_ALREADY_CANCELLED, res.getCode());
        Assertions.assertEquals("WARN", res.getLevel());
        Mockito.verify(timelineService, Mockito.never()).addTimelineElement(timelineElementInternal, notification);
        Mockito.verify(auditLogEvent).generateWarning(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    @ExtendWith(MockitoExtension.class)
    void startCancellationProcessError() {
        //GIVEN
        NotificationInt notification = getNotification();
        Mockito.when(notificationService.getNotificationByIunReactive(Mockito.any()))
                .thenReturn(Mono.just(notification));

        Mockito.when(authUtils.checkPaId(eq(notification), anyString(), any(CxTypeAuthFleet.class)))
                .thenReturn(Mono.error(() -> {
                    throw new PnNotFoundException("Not found", "error", ERROR_CODE_DELIVERYPUSH_NOTFOUND);
                }));
        
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(eq(notification.getIun()), eq(PnAuditLogEventType.AUD_NT_CANCELLED), anyString(), eq(1)))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateFailure(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        //WHEN

        Mono<StatusDetailInt> monoResp = notificationCancellationService.startCancellationProcess(notification.getIun(), notification.getSender().getPaId(), CxTypeAuthFleet.PA);
        
        Assertions.assertThrows(PnNotFoundException.class, monoResp::block);
        
        //THEN
        Mockito.verify(timelineService, Mockito.never()).addTimelineElement(Mockito.any(), Mockito.any());
        Mockito.verify(auditLogEvent).generateFailure(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any());
    }


    @Test
    @ExtendWith(MockitoExtension.class)
    void startCancellationProcessErrorNotFound2() {
        //GIVEN
        NotificationInt notification = getNotification();
        Mockito.when(notificationService.getNotificationByIunReactive(Mockito.any()))
                .thenReturn(Mono.error(() -> {
                    throw new PnNotFoundException("Not found", "error", ERROR_CODE_DELIVERYPUSH_NOTFOUND);
                }));


        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(eq(notification.getIun()), eq(PnAuditLogEventType.AUD_NT_CANCELLED), anyString(), eq(1)))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateFailure(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        //WHEN

        Mono<StatusDetailInt> monoResp = notificationCancellationService.startCancellationProcess(notification.getIun(), notification.getSender().getPaId(), CxTypeAuthFleet.PA);

        Assertions.assertThrows(PnNotFoundException.class, monoResp::block);

        //THEN
        Mockito.verify(timelineService, Mockito.never()).addTimelineElement(Mockito.any(), Mockito.any());
        Mockito.verify(auditLogEvent).generateFailure(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any());
    }

    @Test
    @ExtendWith(SpringExtension.class)
    void cancelNotification() {
        //Given
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withNotificationRecipient(recipient)
                .build();


        final TimelineElementInternal timelineElement = TimelineElementInternal.builder()
                .details(NotificationCancelledDetailsInt.builder()
                        .build())
                .timestamp(Instant.now())
                .build();
        Mockito.when(timelineUtils.buildCancelledTimelineElement(notification)).thenReturn(timelineElement);
        Mockito.when(timelineService.addTimelineElement(Mockito.any(), Mockito.any())).thenReturn(false);
        Mockito.when(notificationService.removeAllNotificationCostsByIun(notification.getIun())).thenReturn(Mono.empty());
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(notificationService.updateStatus(notification.getIun(), NotificationStatusInt.CANCELLED, timelineElement.getTimestamp())).thenReturn(Mono.empty());
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(eq(notification.getIun()), eq(PnAuditLogEventType.AUD_NT_CANCELLED), anyString(), eq(2)))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);

        //WHEN
        notificationCancellationService.completeCancellationProcess(notification.getIun());

        //THEN
        Mockito.verify(timelineService).addTimelineElement(timelineElement, notification);
        Mockito.verify(notificationService).removeAllNotificationCostsByIun(notification.getIun());
        Mockito.verify(auditLogEvent).generateSuccess();
    }


    @Test
    @ExtendWith(SpringExtension.class)
    void cancelNotificationAlreadyInserted() {
        //Given
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withNotificationRecipient(recipient)
                .build();

        final TimelineElementInternal timelineElementOLD = TimelineElementInternal.builder()
                .details(NotificationCancelledDetailsInt.builder()
                        .build())
                .timestamp(Instant.now().minusMillis(1000))
                .build();
        final TimelineElementInternal timelineElement = TimelineElementInternal.builder()
                .details(NotificationCancelledDetailsInt.builder()
                        .build())
                .timestamp(Instant.now())
                .build();
        Mockito.when(timelineUtils.buildCancelledTimelineElement(notification)).thenReturn(timelineElement);
        Mockito.when(timelineService.addTimelineElement(Mockito.any(), Mockito.any())).thenReturn(true);
        Mockito.when(timelineService.getTimelineElement(notification.getIun(), timelineElement.getElementId())).thenReturn(Optional.ofNullable(timelineElementOLD));
        Mockito.when(notificationService.removeAllNotificationCostsByIun(notification.getIun())).thenReturn(Mono.empty());
        Mockito.when(notificationService.getNotificationByIun(notification.getIun())).thenReturn(notification);
        Mockito.when(notificationService.updateStatus(notification.getIun(), NotificationStatusInt.CANCELLED, timelineElementOLD.getTimestamp())).thenReturn(Mono.empty());
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(eq(notification.getIun()), eq(PnAuditLogEventType.AUD_NT_CANCELLED), anyString(), eq(2)))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);

        //WHEN
        notificationCancellationService.completeCancellationProcess(notification.getIun());

        //THEN
        Mockito.verify(timelineService).addTimelineElement(timelineElement, notification);
        Mockito.verify(notificationService).removeAllNotificationCostsByIun(notification.getIun());
        Mockito.verify(auditLogEvent).generateSuccess();
    }


    @Test
    @ExtendWith(SpringExtension.class)
    void cancelNotificationException() {
        //Given
        NotificationRecipientInt recipient = NotificationRecipientTestBuilder.builder().build();
        NotificationInt notification = NotificationTestBuilder.builder()
                .withIun("iun")
                .withNotificationRecipient(recipient)
                .build();

        Mockito.when(notificationService.removeAllNotificationCostsByIun(notification.getIun())).thenThrow(new NullPointerException());
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(eq(notification.getIun()), eq(PnAuditLogEventType.AUD_NT_CANCELLED), anyString(), eq(2)))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateFailure(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        //WHEN
        String iun = notification.getIun();
        Assert.assertThrows(NullPointerException.class, ()-> notificationCancellationService.completeCancellationProcess(iun));

        //THEN

        Mockito.verify(auditLogEvent).generateFailure(Mockito.anyString(), Mockito.anyString(), Mockito.any());
    }

    private NotificationInt getNotification(){
        return NotificationInt.builder()
                .iun("RUYX-MLZA-JUPJ-202308-W-1")
                .paProtocolNumber("protocol_01")
                .sender(NotificationSenderInt.builder()
                        .paId("Milano1")
                        .build()
                )
                .recipients(Collections.singletonList(
                        NotificationRecipientInt.builder()
                                .taxId("testIdRecipient")
                                .denomination("Nome Cognome/Ragione Sociale")
                                .physicalAddress(
                                        PhysicalAddressInt.builder()
                                                .address("test address")
                                                .build()
                                )
                                .payments(null)
                                .build()
                ))
                .build();

    }
}