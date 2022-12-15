package it.pagopa.pn.deliverypush.action.analogworkflow;

import it.pagopa.pn.deliverypush.PnDeliveryPushConfigs;
import it.pagopa.pn.deliverypush.action.completionworkflow.CompletionWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.completionworkflow.RefinementScheduler;
import it.pagopa.pn.deliverypush.action.utils.InstantNowSupplier;
import it.pagopa.pn.deliverypush.action.utils.NotificationUtils;
import it.pagopa.pn.deliverypush.action.utils.PaperChannelUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.delivery.notification.NotificationInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.AttachmentDetailsInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.PrepareEventInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.SendEventInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendAnalogDetailsInt;
import it.pagopa.pn.deliverypush.service.NotificationService;
import it.pagopa.pn.deliverypush.service.PaperChannelService;
import it.pagopa.pn.deliverypush.service.PublicRegistryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnalogWorkflowPaperChannelResponseHandlerTest {

    private AnalogWorkflowPaperChannelResponseHandler analogWorkflowPaperChannelResponseHandler;

    @Mock
    private NotificationService notificationService;
    @Mock
    private PaperChannelService paperChannelService;
    @Mock
    private CompletionWorkFlowHandler completionWorkFlow;
    @Mock
    private AnalogWorkflowUtils analogWorkflowUtils;
    @Mock
    private PublicRegistryService publicRegistryService;
    @Mock
    private InstantNowSupplier instantNowSupplier;
    @Mock
    private PnDeliveryPushConfigs pnDeliveryPushConfigs;
    @Mock
    private AnalogWorkflowHandler analogWorkflowHandler;
    @Mock
    private PaperChannelUtils paperChannelUtils;
    @Mock
    private RefinementScheduler refinementScheduler;

    @BeforeEach
    public void setup() {
        analogWorkflowPaperChannelResponseHandler = new AnalogWorkflowPaperChannelResponseHandler(notificationService,
                paperChannelService,
                completionWorkFlow,
                analogWorkflowUtils,
                instantNowSupplier,
                pnDeliveryPushConfigs,
                analogWorkflowHandler, paperChannelUtils, refinementScheduler);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void paperChannelPrepareResponseHandler() {
        // GIVEN
        PrepareEventInt prepareEventInt = PrepareEventInt.builder()
                .iun("IUN-01")
                .statusCode("OK")
                .receiverAddress(PhysicalAddressInt.builder().address("via casa").build())
                .requestId("IUN-01_abcd")
                .build();

        NotificationInt notificationInt = NotificationInt.builder().iun("IUN-01").build();
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .details(SendAnalogDetailsInt.builder().sentAttemptMade(0).build())
                .build();


        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);
        Mockito.when(paperChannelUtils.getPaperChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString())).thenReturn(timelineElementInternal);

        // WHEN
        Assertions.assertDoesNotThrow(() -> analogWorkflowPaperChannelResponseHandler.paperChannelPrepareResponseHandler(prepareEventInt));
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void paperChannelSendResponseHandler() {
        // GIVEN
        SendEventInt sendEventInt = SendEventInt.builder()
                .iun("IUN_01")
                .statusDetail("001")
                .statusCode("001")
                .discoveredAddress(PhysicalAddressInt.builder().address("via casa").build())
                .requestId("IUN-01_abcd")
                .build();

        NotificationInt notificationInt = NotificationInt.builder().iun("IUN-01").build();
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .details(SendAnalogDetailsInt.builder().sentAttemptMade(0).build())
                .build();

        SendAnalogDetailsInt sendPaperDetails = SendAnalogDetailsInt.builder().sentAttemptMade(0).build();

        PnDeliveryPushConfigs.ExternalChannel externalChannel = new PnDeliveryPushConfigs.ExternalChannel();
        externalChannel.setAnalogCodesSuccess(List.of("004"));
        externalChannel.setAnalogCodesFail(List.of("005"));
        externalChannel.setAnalogCodesProgress(List.of("001"));

        Mockito.when(pnDeliveryPushConfigs.getExternalChannel()).thenReturn(externalChannel);
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);
        Mockito.when(analogWorkflowUtils.getSendAnalogNotificationDetails(Mockito.anyString(), Mockito.anyString())).thenReturn(sendPaperDetails);

        // WHEN
        Assertions.assertDoesNotThrow(() -> analogWorkflowPaperChannelResponseHandler.paperChannelSendResponseHandler(sendEventInt));

    }


    @ExtendWith(MockitoExtension.class)
    @Test
    void paperChannelSendResponseHandlerWithAtt() {
        // GIVEN
        SendEventInt sendEventInt = SendEventInt.builder()
                .iun("IUN_01")
                .statusDetail("001")
                .statusCode("001")
                .discoveredAddress(PhysicalAddressInt.builder().address("via casa").build())
                .requestId("IUN-01_abcd")
                .attachments(List.of(AttachmentDetailsInt.builder().documentType("A").date(Instant.EPOCH).id("abc").url("http").build()))
                .build();

        NotificationInt notificationInt = NotificationInt.builder().iun("IUN-01").build();
        TimelineElementInternal timelineElementInternal = TimelineElementInternal.builder()
                .details(SendAnalogDetailsInt.builder().sentAttemptMade(0).build())
                .build();

        SendAnalogDetailsInt sendPaperDetails = SendAnalogDetailsInt.builder().sentAttemptMade(0).build();

        PnDeliveryPushConfigs.ExternalChannel externalChannel = new PnDeliveryPushConfigs.ExternalChannel();
        externalChannel.setAnalogCodesSuccess(List.of("004"));
        externalChannel.setAnalogCodesFail(List.of("005"));
        externalChannel.setAnalogCodesProgress(List.of("001"));

        Mockito.when(pnDeliveryPushConfigs.getExternalChannel()).thenReturn(externalChannel);
        Mockito.when(notificationService.getNotificationByIun(Mockito.anyString())).thenReturn(notificationInt);
        Mockito.when(analogWorkflowUtils.getSendAnalogNotificationDetails(Mockito.anyString(), Mockito.anyString())).thenReturn(sendPaperDetails);

        // WHEN
        Assertions.assertDoesNotThrow(() -> analogWorkflowPaperChannelResponseHandler.paperChannelSendResponseHandler(sendEventInt));

    }
}