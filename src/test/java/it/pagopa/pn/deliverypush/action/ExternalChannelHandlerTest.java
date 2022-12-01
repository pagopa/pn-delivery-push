package it.pagopa.pn.deliverypush.action;

import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.*;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowExternalChannelResponseHandler;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.ExtChannelAnalogSentResponseInt;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

class ExternalChannelHandlerTest {
    @Mock
    private DigitalWorkFlowExternalChannelResponseHandler digitalWorkFlowHandler;
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

    @ExtendWith(MockitoExtension.class)
    @Test
    void extChannelResponseReceiverForAnalog() {
        PaperProgressStatusEvent extChannelResponse = new PaperProgressStatusEvent();
        extChannelResponse.setStatusCode("__004__");
        extChannelResponse.setRequestId("iun_event_idx_0");
        extChannelResponse.setIun("iun");
        extChannelResponse.setStatusDateTime(OffsetDateTime.now());

        DiscoveredAddress address = new DiscoveredAddress();
        address.setAddress("test");
        extChannelResponse.setDiscoveredAddress(
                address
        );

        List<AttachmentDetails> attachments = new ArrayList<>();
        AttachmentDetails details = new AttachmentDetails();
        details.setId("xx");
        details.setDate(OffsetDateTime.now());
        attachments.add(details);
        extChannelResponse.setAttachments(
                attachments 
        );
        
        SingleStatusUpdate singleStatusUpdate = new SingleStatusUpdate();
        singleStatusUpdate.setAnalogMail(extChannelResponse);


        handler.extChannelResponseReceiver(singleStatusUpdate);

        Mockito.verify(analogWorkflowHandler).extChannelResponseHandler(Mockito.any(ExtChannelAnalogSentResponseInt.class));
    }
}
