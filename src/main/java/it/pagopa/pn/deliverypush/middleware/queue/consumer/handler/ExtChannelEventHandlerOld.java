package it.pagopa.pn.deliverypush.middleware.queue.consumer.handler;

import it.pagopa.pn.api.dto.events.PnExtChnProgressStatusEvent;
import it.pagopa.pn.api.dto.events.PnExtChnProgressStatusEventPayload;
import it.pagopa.pn.deliverypush.middleware.queue.consumer.handler.utils.HandleEventUtils;
import it.pagopa.pn.deliverypush.middleware.responsehandler.ExternalChannelResponseHandlerOld;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;


/**
 * @deprecated
 * Deprecata in attesa di un mock di externalChannel con le nuove api
 */

@Deprecated(since = "PN-612", forRemoval = true)
@Configuration
@Slf4j
@ConditionalOnProperty( name = "pn.delivery-push.featureflags.externalchannel", havingValue = "old")
public class ExtChannelEventHandlerOld {
   private final ExternalChannelResponseHandlerOld externalChannelResponseHandlerOld;

    public ExtChannelEventHandlerOld(ExternalChannelResponseHandlerOld externalChannelResponseHandlerOld) {
        log.info("HO CARICATO OLD");

        this.externalChannelResponseHandlerOld = externalChannelResponseHandlerOld;
    }

    @Deprecated(since = "PN-612", forRemoval = true)
    @Bean
    public Consumer<Message<PnExtChnProgressStatusEventPayload>> pnExtChannelEventInboundConsumer() {
        return message -> {
            log.info("External channel event received OLD, message {}", message);

            PnExtChnProgressStatusEvent evt = PnExtChnProgressStatusEvent.builder()
                    .payload(message.getPayload())
                    .header(HandleEventUtils.mapStandardEventHeader(message.getHeaders()))
                    .build();

            externalChannelResponseHandlerOld.extChannelResponseReceiver(evt);
        };
    }
}
