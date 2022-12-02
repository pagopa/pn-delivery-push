package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.*;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowExternalChannelResponseHandler;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelDigitalSentResponseInt;
import it.pagopa.pn.deliverypush.middleware.responsehandler.ExternalChannelResponseHandler;
import org.junit.jupiter.api.Assertions;
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
    private DigitalWorkFlowExternalChannelResponseHandler digitalWorkFlowHandler;
    @Mock
    private TimelineUtils timelineUtils;

    private ExternalChannelResponseHandler handler;

    @BeforeEach
    public void setup() {
        handler = new ExternalChannelResponseHandler(digitalWorkFlowHandler, timelineUtils);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void extChannelResponseReceiverForCourtesy() {
        CourtesyMessageProgressEvent extChannelResponse = new CourtesyMessageProgressEvent();
        extChannelResponse.setStatus(ProgressEventCategory.OK);
        extChannelResponse.setEventTimestamp(Instant.now().atOffset(ZoneOffset.UTC));
        extChannelResponse.setRequestId("iun_event_idx_0");
        SingleStatusUpdate singleStatusUpdate = new SingleStatusUpdate();
        singleStatusUpdate.setDigitalCourtesy(extChannelResponse);
        
        Assertions.assertDoesNotThrow( () ->
                handler.extChannelResponseReceiver(singleStatusUpdate)
        );
    }
    
    @ExtendWith(MockitoExtension.class)
    @Test
    void extChannelResponseReceiverForDigital() {
        LegalMessageSentDetails extChannelResponse = new LegalMessageSentDetails();
        extChannelResponse.setStatus(ProgressEventCategory.OK);
        extChannelResponse.setEventTimestamp(Instant.now().atOffset(ZoneOffset.UTC));
        extChannelResponse.setRequestId("iun_event_idx_0");

        DigitalMessageReference reference = new DigitalMessageReference();
        reference.setId("id");
        extChannelResponse.setGeneratedMessage(
                reference
        );
        
        SingleStatusUpdate singleStatusUpdate = new SingleStatusUpdate();
        singleStatusUpdate.setDigitalLegal(extChannelResponse);
        extChannelResponse.setEventCode(LegalMessageSentDetails.EventCodeEnum.C001);
        
        Mockito.when(timelineUtils.getIunFromTimelineId(Mockito.anyString())).thenReturn("iun");

        handler.extChannelResponseReceiver(singleStatusUpdate);

        Mockito.verify(digitalWorkFlowHandler).handleExternalChannelResponse(Mockito.any(ExtChannelDigitalSentResponseInt.class));
    }

}
