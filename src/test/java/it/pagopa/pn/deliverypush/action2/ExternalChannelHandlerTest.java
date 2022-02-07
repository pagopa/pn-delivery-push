package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.extchannel.ExtChannelResponse;
import it.pagopa.pn.api.dto.extchannel.ExtChannelResponseStatus;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.api.dto.notification.timeline.SendDigitalDetails;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElement;
import it.pagopa.pn.api.dto.notification.timeline.TimelineElementCategory;
import it.pagopa.pn.deliverypush.action2.utils.ExternalChannelUtils;
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
                .responseStatus(ExtChannelResponseStatus.OK)
                .iun("IUN")
                .notificationDate(Instant.now())
                .eventId("Test event id")
                .build();

        Mockito.when(externalChannelUtils.getExternalChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(TimelineElement.builder()
                        .category(TimelineElementCategory.SEND_DIGITAL_DOMICILE)
                        .details(
                                SendDigitalDetails.sendBuilder()
                                        .taxId("TAXID")
                                        .address(DigitalAddress.builder()
                                                .address("TEST")
                                                .type(DigitalAddressType.PEC).build())
                                        .build()
                        )
                        .build());

        handler.extChannelResponseReceiver(extChannelResponse);

        Mockito.verify(digitalWorkFlowHandler).handleExternalChannelResponse(Mockito.any(ExtChannelResponse.class), Mockito.any(TimelineElement.class));

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void extChannelResponseReceiverForAnalog() {
        ExtChannelResponse extChannelResponse = ExtChannelResponse.builder()
                .responseStatus(ExtChannelResponseStatus.OK)
                .iun("IUN")
                .eventId("test event id")
                .notificationDate(Instant.now())
                .build();

        Mockito.when(externalChannelUtils.getExternalChannelNotificationTimelineElement(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(TimelineElement.builder()
                        .category(TimelineElementCategory.SEND_ANALOG_DOMICILE)
                        .build());

        handler.extChannelResponseReceiver(extChannelResponse);

        Mockito.verify(analogWorkflowHandler).extChannelResponseHandler(Mockito.any(ExtChannelResponse.class), Mockito.any(TimelineElement.class));
    }
}
