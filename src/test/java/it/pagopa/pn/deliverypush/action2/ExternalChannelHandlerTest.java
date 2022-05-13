package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.deliverypush.action2.utils.ExternalChannelUtils;
import it.pagopa.pn.deliverypush.action2.utils.TimelineUtils;
import it.pagopa.pn.deliverypush.dto.ext.externalchannel.ExtChannelResponse;
import it.pagopa.pn.deliverypush.dto.timeline.TimelineElementInternal;
import it.pagopa.pn.deliverypush.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.deliverypush.service.mapper.SmartMapper;
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

    private ExternalChannelResponseHandler handler;

    @BeforeEach
    public void setup() {
        handler = new ExternalChannelResponseHandler(digitalWorkFlowHandler, analogWorkflowHandler, externalChannelUtils);
    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void extChannelResponseReceiverForDigital() {
        ExtChannelResponse extChannelResponse = ExtChannelResponse.builder()
                .responseStatus(ResponseStatus.OK)
                .iun("IUN")
                .notificationDate(Instant.now())
                .eventId("Test event id")
                .build();

        SendDigitalDetails details = SendDigitalDetails.builder()
                .recIndex(0)
                .digitalAddress(
                        DigitalAddress.builder()
                                .address("TEST")
                                .type(DigitalAddress.TypeEnum.PEC).build())
                .build();

        TimelineElementDetails genericDetails = SmartMapper.mapToClass(details, TimelineElementDetails.class);

        Mockito.when(externalChannelUtils.getExternalChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(TimelineElementInternal.timelineInternalBuilder()
                        .category(TimelineElementCategory.SEND_DIGITAL_DOMICILE)
                        .details(genericDetails)
                        .build());

        handler.extChannelResponseReceiver(extChannelResponse);

        Mockito.verify(digitalWorkFlowHandler).handleExternalChannelResponse(Mockito.any(ExtChannelResponse.class), Mockito.any(TimelineElementInternal.class));

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void extChannelResponseReceiverForAnalog() {
        ExtChannelResponse extChannelResponse = ExtChannelResponse.builder()
                .responseStatus(ResponseStatus.OK)
                .iun("IUN")
                .eventId("test event id")
                .notificationDate(Instant.now())
                .build();

        Mockito.when(externalChannelUtils.getExternalChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(TimelineElementInternal.timelineInternalBuilder()
                        .category(TimelineElementCategory.SEND_ANALOG_DOMICILE)
                        .build());

        handler.extChannelResponseReceiver(extChannelResponse);

        Mockito.verify(analogWorkflowHandler).extChannelResponseHandler(Mockito.any(ExtChannelResponse.class), Mockito.any(TimelineElementInternal.class));
    }
}
