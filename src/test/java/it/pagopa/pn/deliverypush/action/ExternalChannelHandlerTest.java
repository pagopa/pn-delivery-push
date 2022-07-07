package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.LegalMessageSentDetails;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.PaperProgressStatusEvent;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.ProgressEventCategory;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.SingleStatusUpdate;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelAnalogSentResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelDigitalSentResponseInt;
import it.pagopa.pn.deliverypush.middleware.responsehandler.ExternalChannelResponseHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneOffset;

class ExternalChannelHandlerTest {
    @Mock
    private DigitalWorkFlowHandler digitalWorkFlowHandler;
    @Mock
    private AnalogWorkflowHandler analogWorkflowHandler;
    @Mock
    private TimelineUtils timelineUtils;

    private ExternalChannelResponseHandler handler;

    @BeforeEach
    public void setup() {
        handler = new ExternalChannelResponseHandler(digitalWorkFlowHandler, analogWorkflowHandler, timelineUtils);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void extChannelResponseReceiverForDigital() {
        LegalMessageSentDetails extChannelResponse = new LegalMessageSentDetails();
        extChannelResponse.setStatus(ProgressEventCategory.OK);
        extChannelResponse.setEventTimestamp(Instant.now().atOffset(ZoneOffset.UTC));
        extChannelResponse.setRequestId("iun_event_idx_0");
        SingleStatusUpdate singleStatusUpdate = new SingleStatusUpdate();
        singleStatusUpdate.setDigitalLegal(extChannelResponse);
        
        Mockito.when(timelineUtils.getIunFromTimelineId(Mockito.anyString())).thenReturn("iun");

        handler.extChannelResponseReceiver(singleStatusUpdate);

        Mockito.verify(digitalWorkFlowHandler).handleExternalChannelResponse(Mockito.any(ExtChannelDigitalSentResponseInt.class));

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

        handler.extChannelResponseReceiver(singleStatusUpdate);

        Mockito.verify(analogWorkflowHandler).extChannelResponseHandler(Mockito.any(ExtChannelAnalogSentResponseInt.class));
    }
}
