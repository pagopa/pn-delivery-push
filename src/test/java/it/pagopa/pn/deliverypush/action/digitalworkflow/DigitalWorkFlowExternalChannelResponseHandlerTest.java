package it.pagopa.pn.deliverypush.action.digitalworkflow;

import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.deliverypush.config.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.EventCodeInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelDigitalSentResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ResponseStatusInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalDetailsInt;
import it.pagopa.pn.deliverypush.service.AuditLogService;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.SchedulerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class DigitalWorkFlowExternalChannelResponseHandlerTest {
    @Mock
    private NotificationService notificationService;
    @Mock
    private SchedulerService schedulerService;
    @Mock
    private DigitalWorkFlowUtils digitalWorkFlowUtils;
    @Mock
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private SendAndUnscheduleNotification sendAndUnscheduleNotification;
    @InjectMocks
    private DigitalWorkFlowExternalChannelResponseHandler digitalWorkFlowExternalChannelResponseHandler;
    
    @Test
    void handleExternalChannelResponse() {
        //GIVEN
        ExtChannelDigitalSentResponseInt response = ExtChannelDigitalSentResponseInt.builder()
                .eventCode(EventCodeInt.C011)
                .iun("testIun")
                .requestId("testRequestId")
                .build();
        SendDigitalDetailsInt sendDigitalDetailsInt = SendDigitalDetailsInt.builder()
                .recIndex(0)
                .retryNumber(0)
                .build();
        
        TimelineElementInternal sendDigitalTimelineElement = TimelineElementInternal.builder()
                .details(sendDigitalDetailsInt)
                .build();
        
        PnDeliveryPushConfigs.ExternalChannel externalChannel = Mockito.mock(PnDeliveryPushConfigs.ExternalChannel.class);
        Mockito.when(pnDeliveryPushConfigs.getExternalChannel()).thenReturn(externalChannel);
        Mockito.when(externalChannel.getDigitalCodesProgress()).thenReturn(List.of("C001"));
        Mockito.when(externalChannel.getDigitalCodesSuccess()).thenReturn(List.of("C001"));
        Mockito.when(externalChannel.getDigitalCodesFail()).thenReturn(List.of("C002","C004","C006","C009","C011"));
        
        Mockito.when(digitalWorkFlowUtils.getTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(Optional.of(sendDigitalTimelineElement));
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(NotificationInt.builder()
                        .iun("iun")
                .build());
        PnAuditLogEvent auditLogEvent = Mockito.mock(PnAuditLogEvent.class);
        Mockito.when( auditLogService.buildAuditLogEvent(Mockito.anyString(), Mockito.anyInt(), Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(auditLogEvent);
        Mockito.when(auditLogEvent.generateWarning(Mockito.anyString(), Mockito.any())).thenReturn(auditLogEvent);

        //WHEN
        digitalWorkFlowExternalChannelResponseHandler.handleExternalChannelResponse(response);
        
        //THEN
        ArgumentCaptor<ResponseStatusInt> responseStatusCaptor = ArgumentCaptor.forClass(ResponseStatusInt.class);

        Mockito.verify(digitalWorkFlowUtils).addDigitalFeedbackTimelineElement(
                Mockito.any(),
                Mockito.any(),
                responseStatusCaptor.capture(),
                Mockito.anyInt(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any()
        );

        ResponseStatusInt responseStatus = responseStatusCaptor.getValue();
        Assertions.assertEquals(ResponseStatusInt.KO, responseStatus);
    }
}