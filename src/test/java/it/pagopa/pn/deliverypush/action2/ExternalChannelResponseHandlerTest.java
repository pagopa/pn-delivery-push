package it.pagopa.pn.deliverypush.action2;

import it.pagopa.pn.api.dto.extchannel.ExtChannelResponse;
import it.pagopa.pn.api.dto.extchannel.ExtChannelResponseStatus;
import it.pagopa.pn.api.dto.notification.address.DigitalAddress;
import it.pagopa.pn.api.dto.notification.address.DigitalAddressType;
import it.pagopa.pn.deliverypush.action2.utils.ExternalChannelUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

class ExternalChannelResponseHandlerTest {
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
                .taxId("TaxId")
                .notificationDate(Instant.now())
                .digitalUsedAddress(DigitalAddress.builder()
                        .type(DigitalAddressType.PEC)
                        .address("account@dominio.it")
                        .build()).build();

        handler.extChannelResponseReceiver(extChannelResponse);

        Mockito.verify(digitalWorkFlowHandler).handleExternalChannelResponse(Mockito.any(ExtChannelResponse.class));

    }

    @ExtendWith(MockitoExtension.class)
    @Test
    void extChannelResponseReceiverForAnalog() {
        ExtChannelResponse extChannelResponse = ExtChannelResponse.builder()
                .responseStatus(ExtChannelResponseStatus.OK)
                .iun("IUN")
                .taxId("TaxId")
                .notificationDate(Instant.now())
                .digitalUsedAddress(null).build();

        handler.extChannelResponseReceiver(extChannelResponse);

        Mockito.verify(analogWorkflowHandler).extChannelResponseHandler(Mockito.any(ExtChannelResponse.class));
    }
}
