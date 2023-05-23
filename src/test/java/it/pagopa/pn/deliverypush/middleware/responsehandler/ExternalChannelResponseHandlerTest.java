package it.pagopa.pn.deliverypush.middleware.responsehandler;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalchannel.model.LegalMessageSentDetails;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalchannel.model.ProgressEventCategory;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.externalchannel.model.SingleStatusUpdate;
import it.pagopa.pn.deliverypush.action.digitalworkflow.DigitalWorkFlowExternalChannelResponseHandler;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.EventCodeInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelDigitalSentResponseInt;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelProgressEventCat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.ZoneOffset;

class ExternalChannelResponseHandlerTest {

    private DigitalWorkFlowExternalChannelResponseHandler digitalWorkFlowHandler;

    private TimelineUtils timelineUtils;

    private ExternalChannelResponseHandler handler;


    @BeforeEach
    void setup() {
        digitalWorkFlowHandler = Mockito.mock(DigitalWorkFlowExternalChannelResponseHandler.class);
        timelineUtils = Mockito.mock(TimelineUtils.class);
        handler = new ExternalChannelResponseHandler(digitalWorkFlowHandler, timelineUtils);
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
    void legalUpdatePnInternalExceptionTest() {

        String expectErrorMsg = "PN_GENERIC_ERROR";

        LegalMessageSentDetails extChannelResponse = new LegalMessageSentDetails();
        SingleStatusUpdate singleStatusUpdate = new SingleStatusUpdate();
        singleStatusUpdate.setDigitalLegal(extChannelResponse);

        Mockito.when(timelineUtils.getIunFromTimelineId(Mockito.any())).thenThrow(new PnInternalException("Exception legalUpdate", "PN_GENERIC_ERROR"));

        PnInternalException pnInternalException = Assertions.assertThrows(PnInternalException.class, () -> {
            handler.extChannelResponseReceiver(singleStatusUpdate);
        });

        Assertions.assertEquals(expectErrorMsg, pnInternalException.getProblem().getErrors().get(0).getCode());
    }


}