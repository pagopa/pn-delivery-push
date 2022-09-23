package it.pagopa.pn.deliverypush.middleware.responsehandler;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.LegalMessageSentDetails;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.PaperProgressStatusEvent;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.ProgressEventCategory;
import it.pagopa.pn.delivery.generated.openapi.clients.externalchannel.model.SingleStatusUpdate;
import it.pagopa.pn.deliverypush.action.AnalogWorkflowHandler;
import it.pagopa.pn.deliverypush.action.DigitalWorkFlowExternalChannelResponseHandler;
import it.pagopa.pn.deliverypush.action.DigitalWorkFlowHandler;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.EventCodeInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelAnalogSentResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelDigitalSentResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelProgressEventCat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

class ExternalChannelResponseHandlerTest {

    @Mock
    private DigitalWorkFlowExternalChannelResponseHandler digitalWorkFlowHandler;

    @Mock
    private AnalogWorkflowHandler analogWorkflowHandler;

    @Mock
    private TimelineUtils timelineUtils;

    private ExternalChannelResponseHandler handler;


    @BeforeEach
    void setup() {
        digitalWorkFlowHandler = Mockito.mock(DigitalWorkFlowExternalChannelResponseHandler.class);
        analogWorkflowHandler = Mockito.mock(AnalogWorkflowHandler.class);
        timelineUtils = Mockito.mock(TimelineUtils.class);
        handler = new ExternalChannelResponseHandler(digitalWorkFlowHandler, analogWorkflowHandler, timelineUtils);
    }

    @Test
    void legalUpdateTest() {

        Instant instant = Instant.parse("2022-08-30T16:04:13.913859900Z");

        LegalMessageSentDetails extChannelResponse = new LegalMessageSentDetails();
        extChannelResponse.setStatus(ProgressEventCategory.OK);
        extChannelResponse.setEventTimestamp(instant.atOffset(ZoneOffset.UTC));
        extChannelResponse.setRequestId("iun_event_idx_0");
        extChannelResponse.setEventCode(LegalMessageSentDetails.EventCodeEnum.C001);
        SingleStatusUpdate singleStatusUpdate = new SingleStatusUpdate();
        singleStatusUpdate.setDigitalLegal(extChannelResponse);

        Mockito.when(timelineUtils.getIunFromTimelineId("iun_event_idx_0")).thenReturn("iun_event_idx_0");

        handler.extChannelResponseReceiver(singleStatusUpdate);

        ExtChannelDigitalSentResponseInt tmp = ExtChannelDigitalSentResponseInt.builder()
                .iun("iun_event_idx_0")
                .requestId("iun_event_idx_0")
                .status(ExtChannelProgressEventCat.OK)
                .eventTimestamp(instant)
                .eventCode(EventCodeInt.C001)
                .build();

        Mockito.verify(digitalWorkFlowHandler, Mockito.times(1)).handleExternalChannelResponse(tmp);
    }

    @Test
    void paperUpdateTest() {

        Instant instant = Instant.now();

        String now = instant.toString();

        OffsetDateTime off = OffsetDateTime.parse(now, DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        PaperProgressStatusEvent extChannelResponse = new PaperProgressStatusEvent();
        extChannelResponse.setStatusCode("__004__");
        extChannelResponse.setRequestId("iun_event_idx_0");
        extChannelResponse.setIun("iun");
        extChannelResponse.setStatusDateTime(off);
        SingleStatusUpdate singleStatusUpdate = new SingleStatusUpdate();
        singleStatusUpdate.setAnalogMail(extChannelResponse);

        handler.extChannelResponseReceiver(singleStatusUpdate);

        ExtChannelAnalogSentResponseInt tmp = ExtChannelAnalogSentResponseInt.builder()
                .requestId("iun_event_idx_0")
                .iun("iun")
                .statusCode("__004__")
                .statusDateTime(instant)
                .build();

        Mockito.verify(analogWorkflowHandler, Mockito.times(1)).extChannelResponseHandler(tmp);
    }

    @Test
    void legalUpdatePnInternalExceptionTest() {

        String expectErrorMsg = "PN_GENERIC_ERROR";

        LegalMessageSentDetails extChannelResponse = new LegalMessageSentDetails();
        SingleStatusUpdate singleStatusUpdate = new SingleStatusUpdate();
        singleStatusUpdate.setDigitalLegal(extChannelResponse);

        Mockito.when(timelineUtils.getIunFromTimelineId(Mockito.any())).thenThrow(new PnInternalException("Exception legalUpdate"));

        PnInternalException pnInternalException = Assertions.assertThrows(PnInternalException.class, () -> {
            handler.extChannelResponseReceiver(singleStatusUpdate);
        });

        Assertions.assertEquals(expectErrorMsg, pnInternalException.getProblem().getErrors().get(0).getCode());
    }

    @Test
    void paperUpdatePnInternalExceptionTest() {

        String expectErrorMsg = "PN_GENERIC_ERROR";

        PaperProgressStatusEvent extChannelResponse = new PaperProgressStatusEvent();
        SingleStatusUpdate singleStatusUpdate = new SingleStatusUpdate();
        singleStatusUpdate.setAnalogMail(extChannelResponse);

        PnInternalException pnInternalException = Assertions.assertThrows(PnInternalException.class, () -> {
            handler.extChannelResponseReceiver(singleStatusUpdate);
        });

        Assertions.assertEquals(expectErrorMsg, pnInternalException.getProblem().getErrors().get(0).getCode());
    }


}