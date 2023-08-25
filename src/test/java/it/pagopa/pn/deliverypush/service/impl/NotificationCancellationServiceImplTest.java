package it.pagopa.pn.deliverypush.service.impl;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationRecipientInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationSenderInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.exceptions.PnNotFoundException;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.deliverypush.service.AuditLogService;
import it.pagopa.pn.deliverypush.service.NotificationCancellationService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.TimelineService;
import it.pagopa.pn.deliverypush.utils.AuthUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.Collections;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_NOTFOUND;
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

        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when(auditLogService.buildAuditLogEvent(eq(notification.getIun()), eq(PnAuditLogEventType.AUD_NT_CANCELLED), anyString(), eq(1)))
                .thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateSuccess()).thenReturn(auditLogEvent);
        
        //WHEN
        notificationCancellationService.startCancellationProcess(notification.getIun(), notification.getSender().getPaId(), CxTypeAuthFleet.PA)
                .block();
        
        //THEN
        Mockito.verify(timelineService).addTimelineElement(timelineElementInternal, notification);
        Mockito.verify(auditLogEvent).generateSuccess();
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

        Mono<Void> monoResp = notificationCancellationService.startCancellationProcess(notification.getIun(), notification.getSender().getPaId(), CxTypeAuthFleet.PA);
        
        Assertions.assertThrows(PnNotFoundException.class, monoResp::block);
        
        //THEN
        Mockito.verify(timelineService, Mockito.never()).addTimelineElement(Mockito.any(), Mockito.any());
        Mockito.verify(auditLogEvent).generateFailure(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any());
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
                                .payment(null)
                                .build()
                ))
                .build();

    }
}