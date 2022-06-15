package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.LegalMessageSentDetails;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.PaperProgressStatusEvent;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.ProgressEventCategory;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.SingleStatusUpdate;
import it.pagopa.pn.deliverypush.action2.utils.ExternalChannelUtils;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.dto.timeline.details.SendDigitalDetailsInt;
import it.pagopa.pn.deliverypush.dto.timeline.details.TimelineElementCategoryInt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

class ExternalChannelHandlerTest {
    @Mock
    private DigitalWorkFlowHandler digitalWorkFlowHandler;
    @Mock
    private AnalogWorkflowHandler analogWorkflowHandler;
    @Mock
    private ExternalChannelUtils externalChannelUtils;
    @Mock
    private TimelineUtils timelineUtils;

    private ExternalChannelResponseHandler handler;

    @BeforeEach
    public void setup() {
        handler = new ExternalChannelResponseHandler(digitalWorkFlowHandler, analogWorkflowHandler, externalChannelUtils, timelineUtils);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void extChannelResponseReceiverForDigital() {
        LegalMessageSentDetails extChannelResponse = new LegalMessageSentDetails();
        extChannelResponse.setStatus(ProgressEventCategory.OK);
        extChannelResponse.setEventTimestamp(Instant.now());
        extChannelResponse.setRequestId("iun_event_idx_0");
        SingleStatusUpdate singleStatusUpdate = new SingleStatusUpdate();
        singleStatusUpdate.setDigitalLegal(extChannelResponse);

        SendDigitalDetailsInt details = SendDigitalDetailsInt.builder()
                .recIndex(0)
                .digitalAddress(
                        LegalDigitalAddressInt.builder()
                                .address("TEST")
                                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC).build())
                .build();


        Mockito.when(externalChannelUtils.getExternalChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(TimelineElementInternal.builder()
                        .category(TimelineElementCategoryInt.SEND_DIGITAL_DOMICILE)
                        .details(details)
                        .build());
        
        Mockito.when(timelineUtils.getIunFromTimelineId(Mockito.anyString())).thenReturn("iun");

        handler.extChannelResponseReceiver(singleStatusUpdate);

        Mockito.verify(digitalWorkFlowHandler).handleExternalChannelResponse(Mockito.any(LegalMessageSentDetails.class), Mockito.any(TimelineElementInternal.class));

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void extChannelResponseReceiverForAnalog() {
        PaperProgressStatusEvent extChannelResponse = new PaperProgressStatusEvent();
        extChannelResponse.setStatusCode("__004__");
        extChannelResponse.setRequestId("iun_event_idx_0");
        extChannelResponse.setIun("iun");
        SingleStatusUpdate singleStatusUpdate = new SingleStatusUpdate();
        singleStatusUpdate.setAnalogMail(extChannelResponse);

        Mockito.when(timelineUtils.getIunFromTimelineId(Mockito.anyString())).thenReturn("iun");

        Mockito.when(externalChannelUtils.getExternalChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(TimelineElementInternal.builder()
                        .category(TimelineElementCategoryInt.SEND_ANALOG_DOMICILE)
                        .build());

        handler.extChannelResponseReceiver(singleStatusUpdate);

        Mockito.verify(analogWorkflowHandler).extChannelResponseHandler(Mockito.any(PaperProgressStatusEvent.class), Mockito.any(TimelineElementInternal.class));
    }
}
