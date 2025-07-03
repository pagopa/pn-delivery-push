package it.pagopa.pn.deliverypush.middleware.queue.consumer.channel;

import it.pagopa.pn.deliverypush.dto.ext.publicregistry.NationalRegistriesResponse;
import it.pagopa.pn.deliverypush.middleware.externalclient.pnclient.nationalregistries.NationalRegistriesClient;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.middleware.responsehandler.NationalRegistriesResponseHandler;
import it.pagopa.pn.deliverypush.utils.NationalRegistriesMessageUtil;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.AddressSQSMessage;
import it.pagopa.pn.deliverypush.generated.openapi.msclient.nationalregistries.model.AddressSQSMessageDigitalAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.List;
import java.util.function.Consumer;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class NationalRegistriesChannel {
    private final NationalRegistriesResponseHandler nationalRegistriesResponseHandler;

    @Bean
    public Consumer<Message<AddressSQSMessage>> pnNationalRegistriesEventInboundConsumer() {
        return ChannelWrapper.withMDC(message -> {
            try {
                log.info("Handle message from {} with content {}", NationalRegistriesClient.CLIENT_NAME, message);

                List<AddressSQSMessageDigitalAddress> digitalAddresses = message.getPayload().getDigitalAddress();
                String correlationId = message.getPayload().getCorrelationId();
                NationalRegistriesResponse response = NationalRegistriesMessageUtil.buildPublicRegistryResponse(correlationId, digitalAddresses);
                nationalRegistriesResponseHandler.handleResponse(response);

            } catch (Exception ex) {
                HandleEventUtils.handleException(message.getHeaders(), ex);
                throw ex;
            }
        });
    }

}
