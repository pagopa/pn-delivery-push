package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.deliverypush.dto.address.LegalDigitalAddressInt;
import it.pagopa.pn.deliverypush.dto.ext.publicregistry.PublicRegistryResponse;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.middleware.responsehandler.PublicRegistryResponseHandler;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.model.AddressSQSMessage;
import it.pagopa.pn.nationalregistries.generated.openapi.clients.nationalregistries.model.AddressSQSMessageDigitalAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.util.CollectionUtils;

import java.util.function.Consumer;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class NationalRegistriesEventHandler {
    private final PublicRegistryResponseHandler publicRegistryResponseHandler;


    @Bean
    public Consumer<Message<AddressSQSMessage>> pnNationalRegistriesEventInboundConsumer() {
        return message -> {
            try {
                log.info("National registries event received, message {}", message);

                AddressSQSMessage payload = message.getPayload();
                PublicRegistryResponse response = PublicRegistryResponse.builder()
                        .correlationId(payload.getCorrelationId())
                        .digitalAddress(CollectionUtils.isEmpty(payload.getDigitalAddress()) ? null :
                                mapToLegalDigitalAddressInt(payload.getDigitalAddress().get(0)))
                        .build();
                publicRegistryResponseHandler.handleResponse(response);
            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        };
    }

    private LegalDigitalAddressInt mapToLegalDigitalAddressInt(AddressSQSMessageDigitalAddress digitalAddress) {
        return LegalDigitalAddressInt.builder()
                .address(digitalAddress.getAddress())
                .type(LegalDigitalAddressInt.LEGAL_DIGITAL_ADDRESS_TYPE.PEC)
                .build();
    }

}
