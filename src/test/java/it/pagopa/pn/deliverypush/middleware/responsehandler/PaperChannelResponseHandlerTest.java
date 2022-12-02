package it.pagopa.pn.deliverypush.middleware.responsehandler;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.delivery.generated.openapi.clients.paperchannel.model.AnalogAddress;
import it.pagopa.pn.delivery.generated.openapi.clients.paperchannel.model.PaperChannelUpdate;
import it.pagopa.pn.delivery.generated.openapi.clients.paperchannel.model.PrepareEvent;
import it.pagopa.pn.delivery.generated.openapi.clients.paperchannel.model.SendEvent;
import it.pagopa.pn.deliverypush.action.analogworkflow.AnalogWorkflowPaperChannelResponseHandler;
import it.pagopa.pn.deliverypush.action.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.address.PhysicalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.PrepareEventInt;
import it.pagopa.pn.deliverypush.dto.ext.paperchannel.SendEventInt;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;

import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_GENERATEPDFFAILED;
import static it.pagopa.pn.deliverypush.exceptions.PnDeliveryPushExceptionCodes.ERROR_CODE_DELIVERYPUSH_PAPERUPDATEFAILED;

class PaperChannelResponseHandlerTest {

    private AnalogWorkflowPaperChannelResponseHandler analogWorkflowPaperChannelResponseHandler;

    private TimelineUtils timelineUtils;

    private PaperChannelResponseHandler handler;


    @BeforeEach
    void setup() {
        analogWorkflowPaperChannelResponseHandler = Mockito.mock(AnalogWorkflowPaperChannelResponseHandler.class);
        timelineUtils = Mockito.mock(TimelineUtils.class);
        handler = new PaperChannelResponseHandler(analogWorkflowPaperChannelResponseHandler, timelineUtils);
    }

    @Test
    void prepareUpdateTest_OK() {

        Instant instant = Instant.parse("2022-08-30T16:04:13.913859900Z");

        PrepareEvent prepareEvent = new PrepareEvent();
        prepareEvent.setStatusCode("OK");
        prepareEvent.setStatusDateTime(instant.atOffset(ZoneOffset.UTC));
        prepareEvent.setRequestId("iun_event_idx_0");
        prepareEvent.setStatusDetail("ok");
        prepareEvent.setReceiverAddress(new AnalogAddress());
        PaperChannelUpdate singleStatusUpdate = new PaperChannelUpdate();
        singleStatusUpdate.setPrepareEvent(prepareEvent);

        Mockito.when(timelineUtils.getIunFromTimelineId("iun_event_idx_0")).thenReturn("iun_event_idx_0");

        handler.paperChannelResponseReceiver(singleStatusUpdate);

        PrepareEventInt tmp = PrepareEventInt.builder()
                .iun("iun_event_idx_0")
                .requestId("iun_event_idx_0")
                .statusCode("OK")
                .statusDateTime(instant)
                .statusDetail("ok")
                .receiverAddress(new PhysicalAddressInt())
                .build();

        Mockito.verify(analogWorkflowPaperChannelResponseHandler, Mockito.times(1)).paperChannelPrepareResponseHandler(tmp);
    }

    @Test
    void prepareUpdateTest_KOUNREACHABLE() {

        Instant instant = Instant.parse("2022-08-30T16:04:13.913859900Z");

        PrepareEvent prepareEvent = new PrepareEvent();
        prepareEvent.setStatusCode("KOUNREACHABLE");
        prepareEvent.setStatusDateTime(instant.atOffset(ZoneOffset.UTC));
        prepareEvent.setRequestId("iun_event_idx_0");
        prepareEvent.setStatusDetail("ko");
        PaperChannelUpdate singleStatusUpdate = new PaperChannelUpdate();
        singleStatusUpdate.setPrepareEvent(prepareEvent);

        Mockito.when(timelineUtils.getIunFromTimelineId("iun_event_idx_0")).thenReturn("iun_event_idx_0");

        handler.paperChannelResponseReceiver(singleStatusUpdate);

        PrepareEventInt tmp = PrepareEventInt.builder()
                .iun("iun_event_idx_0")
                .requestId("iun_event_idx_0")
                .statusCode("KOUNREACHABLE")
                .statusDateTime(instant)
                .statusDetail("ko")
                .build();

        Mockito.verify(analogWorkflowPaperChannelResponseHandler, Mockito.times(1)).paperChannelPrepareResponseHandler(tmp);
    }


    @Test
    void prepareUpdateTest_PROGRESS() {

        Instant instant = Instant.parse("2022-08-30T16:04:13.913859900Z");

        PrepareEvent prepareEvent = new PrepareEvent();
        prepareEvent.setStatusCode("PROGRESS");
        prepareEvent.setStatusDateTime(instant.atOffset(ZoneOffset.UTC));
        prepareEvent.setRequestId("iun_event_idx_0");
        prepareEvent.setStatusDetail("progress");
        PaperChannelUpdate singleStatusUpdate = new PaperChannelUpdate();
        singleStatusUpdate.setPrepareEvent(prepareEvent);

        Mockito.when(timelineUtils.getIunFromTimelineId("iun_event_idx_0")).thenReturn("iun_event_idx_0");

        handler.paperChannelResponseReceiver(singleStatusUpdate);

        PrepareEventInt tmp = PrepareEventInt.builder()
                .iun("iun_event_idx_0")
                .requestId("iun_event_idx_0")
                .statusCode("PROGRESS")
                .statusDateTime(instant)
                .statusDetail("progress")
                .build();

        Mockito.verify(analogWorkflowPaperChannelResponseHandler, Mockito.times(1)).paperChannelPrepareResponseHandler(tmp);
    }


    @Test
    void sendUpdateTest_OK() {

        Instant instant = Instant.parse("2022-08-30T16:04:13.913859900Z");

        SendEvent sendEvent = new SendEvent();
        sendEvent.setStatusCode("OK");
        sendEvent.setStatusDateTime(instant.atOffset(ZoneOffset.UTC));
        sendEvent.setRequestId("iun_event_idx_0");
        sendEvent.setStatusDetail("ok");
        sendEvent.setAttachments(new ArrayList<>());
        PaperChannelUpdate singleStatusUpdate = new PaperChannelUpdate();
        singleStatusUpdate.setSendEvent(sendEvent);

        Mockito.when(timelineUtils.getIunFromTimelineId("iun_event_idx_0")).thenReturn("iun_event_idx_0");

        handler.paperChannelResponseReceiver(singleStatusUpdate);

        SendEventInt tmp = SendEventInt.builder()
                .iun("iun_event_idx_0")
                .requestId("iun_event_idx_0")
                .statusCode("OK")
                .statusDateTime(instant)
                .statusDetail("ok")
                .attachments(new ArrayList<>())
                .build();

        Mockito.verify(analogWorkflowPaperChannelResponseHandler, Mockito.times(1)).paperChannelSendResponseHandler(tmp);
    }

    @Test
    void sendUpdateTest_KO() {

        Instant instant = Instant.parse("2022-08-30T16:04:13.913859900Z");

        SendEvent sendEvent = new SendEvent();
        sendEvent.setStatusCode("OK");
        sendEvent.setStatusDateTime(instant.atOffset(ZoneOffset.UTC));
        sendEvent.setRequestId("iun_event_idx_0");
        sendEvent.setStatusDetail("ok");
        sendEvent.setAttachments(new ArrayList<>());
        PaperChannelUpdate singleStatusUpdate = new PaperChannelUpdate();
        singleStatusUpdate.setSendEvent(sendEvent);

        Mockito.when(timelineUtils.getIunFromTimelineId("iun_event_idx_0")).thenReturn("iun_event_idx_0");

        handler.paperChannelResponseReceiver(singleStatusUpdate);

        SendEventInt tmp = SendEventInt.builder()
                .iun("iun_event_idx_0")
                .requestId("iun_event_idx_0")
                .statusCode("OK")
                .statusDateTime(instant)
                .statusDetail("ok")
                .attachments(new ArrayList<>())
                .build();

        Mockito.verify(analogWorkflowPaperChannelResponseHandler, Mockito.times(1)).paperChannelSendResponseHandler(tmp);
    }

    @Test
    void prepareEventPnInternalExceptionTest() {

        Instant instant = Instant.parse("2022-08-30T16:04:13.913859900Z");

        PrepareEvent prepareEvent = new PrepareEvent();
        prepareEvent.setStatusCode("PROGRESS");
        prepareEvent.setStatusDateTime(instant.atOffset(ZoneOffset.UTC));
        prepareEvent.setRequestId("iun_event_idx_0");
        prepareEvent.setStatusDetail("progress");
        PaperChannelUpdate singleStatusUpdate = new PaperChannelUpdate();
        singleStatusUpdate.setPrepareEvent(prepareEvent);

        Mockito.doThrow(new PnInternalException("errore", ERROR_CODE_DELIVERYPUSH_GENERATEPDFFAILED)).when(analogWorkflowPaperChannelResponseHandler).paperChannelPrepareResponseHandler(Mockito.any());

        PnInternalException pnInternalException = Assertions.assertThrows(PnInternalException.class, () -> {
            handler.paperChannelResponseReceiver(singleStatusUpdate);
        });

        Assertions.assertEquals(ERROR_CODE_DELIVERYPUSH_GENERATEPDFFAILED, pnInternalException.getProblem().getErrors().get(0).getCode());
    }

    @Test
    void prepareEventExceptionTest() {
        Instant instant = Instant.parse("2022-08-30T16:04:13.913859900Z");

        PrepareEvent prepareEvent = new PrepareEvent();
        prepareEvent.setStatusCode("PROGRESS");
        prepareEvent.setStatusDateTime(instant.atOffset(ZoneOffset.UTC));
        prepareEvent.setRequestId("iun_event_idx_0");
        prepareEvent.setStatusDetail("progress");
        PaperChannelUpdate singleStatusUpdate = new PaperChannelUpdate();
        singleStatusUpdate.setPrepareEvent(prepareEvent);

        Mockito.doThrow(new RuntimeException()).when(analogWorkflowPaperChannelResponseHandler).paperChannelPrepareResponseHandler(Mockito.any());

        PnInternalException pnInternalException = Assertions.assertThrows(PnInternalException.class, () -> {
            handler.paperChannelResponseReceiver(singleStatusUpdate);
        });

        Assertions.assertEquals(ERROR_CODE_DELIVERYPUSH_PAPERUPDATEFAILED, pnInternalException.getProblem().getErrors().get(0).getCode());
    }

    @Test
    void sendEventPnInternalExceptionTest() {

        Instant instant = Instant.parse("2022-08-30T16:04:13.913859900Z");

        SendEvent sendEvent = new SendEvent();
        sendEvent.setStatusCode("OK");
        sendEvent.setStatusDateTime(instant.atOffset(ZoneOffset.UTC));
        sendEvent.setRequestId("iun_event_idx_0");
        sendEvent.setStatusDetail("ok");
        sendEvent.setAttachments(new ArrayList<>());
        PaperChannelUpdate singleStatusUpdate = new PaperChannelUpdate();
        singleStatusUpdate.setSendEvent(sendEvent);

        Mockito.when(timelineUtils.getIunFromTimelineId("iun_event_idx_0")).thenReturn("iun_event_idx_0");

        Mockito.doThrow(new PnInternalException("errore", ERROR_CODE_DELIVERYPUSH_GENERATEPDFFAILED)).when(analogWorkflowPaperChannelResponseHandler).paperChannelSendResponseHandler(Mockito.any());

        PnInternalException pnInternalException = Assertions.assertThrows(PnInternalException.class, () -> {
            handler.paperChannelResponseReceiver(singleStatusUpdate);
        });

        Assertions.assertEquals(ERROR_CODE_DELIVERYPUSH_GENERATEPDFFAILED, pnInternalException.getProblem().getErrors().get(0).getCode());
    }

    @Test
    void sendEventExceptionTest() {

        Instant instant = Instant.parse("2022-08-30T16:04:13.913859900Z");

        SendEvent sendEvent = new SendEvent();
        sendEvent.setStatusCode("OK");
        sendEvent.setStatusDateTime(instant.atOffset(ZoneOffset.UTC));
        sendEvent.setRequestId("iun_event_idx_0");
        sendEvent.setStatusDetail("ok");
        sendEvent.setAttachments(new ArrayList<>());
        PaperChannelUpdate singleStatusUpdate = new PaperChannelUpdate();
        singleStatusUpdate.setSendEvent(sendEvent);

        Mockito.when(timelineUtils.getIunFromTimelineId("iun_event_idx_0")).thenReturn("iun_event_idx_0");


        Mockito.doThrow(new RuntimeException()).when(analogWorkflowPaperChannelResponseHandler).paperChannelSendResponseHandler(Mockito.any());

        PnInternalException pnInternalException = Assertions.assertThrows(PnInternalException.class, () -> {
            handler.paperChannelResponseReceiver(singleStatusUpdate);
        });

        Assertions.assertEquals(ERROR_CODE_DELIVERYPUSH_PAPERUPDATEFAILED, pnInternalException.getProblem().getErrors().get(0).getCode());
    }


}